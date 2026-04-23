import { chatApi, chatStreamApi, getConversationHistoryApi, listConversationsApi } from '../../api/chat'
import { listEnabledModelsApi } from '../../api/model'
import { getStorage, setStorage } from '../../utils/storage'
import { showError } from '../../utils/ui'
import { buildWsUrl } from '../../config/env'

const CHAT_HISTORY_KEY = 'CHAT_HISTORY_LIST'
const REALTIME_PATH = '/ws/wx/speech/realtime'

const buildId = (prefix) => `${prefix}_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`

Page({
  data: {
    modelOptions: [],
    selectedModelCode: '',
    selectedModelIndex: 0,
    chatList: [],
    conversationId: '',
    conversationTitle: '',
    scrollIntoView: '',
    scrollTop: 0,
    anchorKey: 0,
    loading: false,
    recording: false,
    voiceLoading: false,
    liveSpeechText: '',
    showHistoryDrawer: false,
    historyList: [],
    pendingImagePath: ''
  },

  recorderManager: null,
  recordingStartAt: 0,
  speechSocketTask: null,
  speechSocketOpened: false,
  realtimeFinalText: '',
  pendingInitialMessage: '',

  async onLoad(options) {
    const conversationId = options?.conversationId || ''
    const initialMessage = decodeURIComponent(options?.initialMessage || '')
    this.initRecorder()
    this.pendingInitialMessage = initialMessage
    this.setData({ conversationId })
    await this.loadModels()
    if (conversationId) {
      await this.restoreConversation(conversationId)
    }
    if (this.pendingInitialMessage) {
      const message = this.pendingInitialMessage
      this.pendingInitialMessage = ''
      await this.sendMessage(message)
    }
  },

  onUnload() {
    this.stopRealtimeSocket()
    if (this.recorderManager && this.data.recording) {
      try {
        this.recorderManager.stop()
      } catch (e) {
        // ignore
      }
    }
  },

  async loadModels() {
    try {
      const response = await listEnabledModelsApi()
      const modelOptions = response?.data || []
      const selectedModelCode = modelOptions.length ? modelOptions[0].modelCode : ''
      this.setData({
        modelOptions,
        selectedModelCode,
        selectedModelIndex: 0
      })
    } catch (error) {
      showError(error.message || '模型加载失败')
    }
  },

  onModelChange(event) {
    const selectedModelIndex = Number(event.detail.value)
    const selectedModel = this.data.modelOptions[selectedModelIndex]
    this.setData({
      selectedModelIndex,
      selectedModelCode: selectedModel?.modelCode || ''
    })
  },

  onSendMessage(event) {
    const message = event.detail?.message || ''
    const pendingImagePath = this.data.pendingImagePath || ''
    const imagePayload = pendingImagePath ? { filePath: pendingImagePath } : null
    this.sendMessage(message, imagePayload)
    if (pendingImagePath) {
      this.setData({ pendingImagePath: '' })
    }
  },

  onImageSelected(event) {
    const filePath = event.detail?.filePath || ''
    if (!filePath) return
    const selectedModel = this.data.modelOptions[this.data.selectedModelIndex] || {}
    const supportsImageChat = Number(selectedModel?.supportsImageChat || 0) === 1
    if (!supportsImageChat) {
      showError('当前大模型不支持上传图片')
      return
    }
    this.setData({ pendingImagePath: filePath })
  },

  onClearPendingImage() {
    this.setData({ pendingImagePath: '' })
  },

  onQuickAsk(event) {
    const message = event.currentTarget.dataset.text || ''
    this.sendMessage(message)
  },

  onVoiceTap() {
    if (this.data.recording) {
      this.stopRealtimeRecord()
      return
    }
    if (this.data.loading || this.data.voiceLoading) {
      return
    }
    if (!this.recorderManager) {
      showError('当前设备不支持录音')
      return
    }
    this.startRecord()
  },

  async goHistory() {
    this.setData({ showHistoryDrawer: true })
    await this.loadConversationList()
  },

  closeHistoryDrawer() {
    this.setData({ showHistoryDrawer: false })
  },

  async loadConversationList() {
    try {
      const response = await listConversationsApi()
      const historyList = response?.data?.data || response?.data || response || []
      this.setData({ historyList })
    } catch (error) {
      const localList = getStorage(CHAT_HISTORY_KEY) || []
      this.setData({ historyList: localList })
      showError(error.message || '历史会话加载失败')
    }
  },

  async onSelectHistory(event) {
    const { conversationId } = event.currentTarget.dataset
    if (!conversationId || this.data.loading) {
      return
    }
    this.closeHistoryDrawer()
    this.setData({
      conversationId,
      conversationTitle: '',
      chatList: [],
      scrollIntoView: ''
    })
    await this.restoreConversation(conversationId)
  },

  onNewConversation() {
    this.closeHistoryDrawer()
    this.setData({
      conversationId: '',
      conversationTitle: '',
      chatList: [],
      scrollIntoView: ''
    })
  },

  async sendMessage(message, imagePayload = null) {
    const hasImage = Boolean(imagePayload?.filePath)
    if ((!message && !hasImage) || this.data.loading) {
      return
    }
    let imageBase64 = ''
    let imageMimeType = ''
    if (hasImage) {
      try {
        const compressedPath = await this.compressImageIfNeeded(imagePayload.filePath)
        const imageData = await this.readImageAsBase64(compressedPath)
        imageBase64 = imageData.base64
        imageMimeType = imageData.mimeType
        // 注意：base64 会膨胀体积，线上网关常有较小请求体限制（413）。
        // 这里做一个软上限，避免用户反复失败。
        if ((imageBase64 || '').length > 2_800_000) {
          throw new Error('图片过大，请裁剪或重新选择更小的图片')
        }
      } catch (e) {
        showError(e.message || '图片读取失败')
        return
      }
    }
    const userDisplayText = hasImage ? `[图片]${message ? `\n${message}` : ''}` : message
    const userMessageId = buildId('u')
    const thinkingId = buildId('a')
    const userMessage = {
      id: userMessageId,
      role: 'user',
      content: userDisplayText,
      createdAt: Date.now()
    }
    const thinkingMessage = {
      id: thinkingId,
      role: 'assistant',
      content: '',
      thinking: true,
      streaming: false,
      createdAt: Date.now()
    }

    const chatList = [...this.data.chatList, userMessage, thinkingMessage]
    this.setData({ chatList, loading: true }, () => {
      this.scrollToBottom(thinkingId)
    })

    try {
      let streamed = ''
      const doneData = await chatStreamApi({
        modelCode: this.data.selectedModelCode || null,
        conversationId: this.data.conversationId || null,
        message: message || null,
        imageBase64: imageBase64 || null,
        imageMimeType: imageMimeType || null
      }, {
        onMessage: (chunk) => {
          const piece = String(chunk?.content || '')
          if (!piece) return
          streamed += piece
          const updatedList = this.data.chatList.map((item) => {
            if (item.id !== thinkingId) return item
            return { ...item, thinking: false, streaming: true, content: streamed }
          })
          this.setData({ chatList: updatedList }, () => this.scrollToBottom(thinkingId))
        }
      })

      const conversationId = doneData?.conversationId || this.data.conversationId
      const conversationTitle = doneData?.conversationTitle || this.data.conversationTitle
      const finalAnswer = doneData?.answer || streamed || '（无回复）'
      const finalizedList = this.data.chatList.map((item) => {
        if (item.id !== thinkingId) return item
        return { ...item, thinking: false, streaming: false, content: finalAnswer }
      })
      this.setData({ chatList: finalizedList, conversationId, conversationTitle, loading: false }, () => {
        this.scrollToBottom(thinkingId)
        this.persistConversation()
      })
    } catch (error) {
      const failedList = this.data.chatList.map((item) => {
        if (item.id !== thinkingId) {
          return item
        }
        return {
          ...item,
          thinking: false,
          streaming: false,
          content: '请求失败，请稍后重试。'
        }
      })
      this.setData({ chatList: failedList, loading: false }, () => {
        this.scrollToBottom(thinkingId)
        this.persistConversation()
      })
      showError(error.message || '对话失败')
    }
  },

  initRecorder() {
    if (!wx.getRecorderManager) return
    const recorderManager = wx.getRecorderManager()
    this.recorderManager = recorderManager

    recorderManager.onStop(async (res) => {
      const duration = Date.now() - (this.recordingStartAt || 0)
      this.setData({ recording: false })
      if (duration < 600) {
        this.stopRealtimeSocket()
        showError('录音时间太短')
        return
      }
      this.finishRealtimeTranscribe()
    })

    recorderManager.onError((error) => {
      this.setData({ recording: false, voiceLoading: false, liveSpeechText: '' })
      this.stopRealtimeSocket()
      showError(error?.errMsg || '录音失败')
    })

    recorderManager.onFrameRecorded((res) => {
      const frameBuffer = res?.frameBuffer
      if (!frameBuffer || !this.speechSocketTask) return
      try {
        this.speechSocketTask.send({ data: frameBuffer })
      } catch (e) {
        // ignore
      }
    })
  },

  startRecord() {
    wx.authorize({
      scope: 'scope.record',
      success: async () => {
        const connected = await this.openRealtimeSocket()
        if (!connected) return
        this.recordingStartAt = Date.now()
        this.realtimeFinalText = ''
        this.setData({ recording: true, voiceLoading: true, liveSpeechText: '' })
        this.recorderManager.start({
          duration: 60000,
          sampleRate: 16000,
          numberOfChannels: 1,
          encodeBitRate: 48000,
          format: 'PCM',
          frameSize: 1
        })
      },
      fail: () => {
        showError('请先开启麦克风权限')
      }
    })
  },

  stopRealtimeRecord() {
    if (!this.data.recording) {
      this.setData({ recording: false, voiceLoading: false })
      return
    }
    try {
      this.setData({ recording: false, voiceLoading: true })
      if (this.speechSocketTask) {
        this.speechSocketTask.send({ data: JSON.stringify({ action: 'end' }) })
      }
      this.recorderManager.stop()
    } catch (e) {
      this.setData({ recording: false, voiceLoading: false })
      this.stopRealtimeSocket()
      showError('结束录音失败')
    }
  },

  finishRealtimeTranscribe() {
    // 给服务端一个短窗口推送最终分段，避免最后一句丢失。
    setTimeout(() => {
      const text = (this.realtimeFinalText || this.data.liveSpeechText || '').trim()
      this.setData({ voiceLoading: false })
      this.stopRealtimeSocket()
      if (!text) {
        showError('未识别到语音内容')
        return
      }
      this.sendMessage(text)
    }, 500)
  },

  openRealtimeSocket() {
    return new Promise((resolve) => {
      const wsUrl = buildWsUrl(REALTIME_PATH)
      const socketTask = wx.connectSocket({ url: wsUrl, timeout: 10000 })
      this.speechSocketTask = socketTask

      let settled = false
      const done = (ok) => {
        if (settled) return
        settled = true
        resolve(ok)
      }

      socketTask.onOpen(() => {
        this.speechSocketOpened = true
        socketTask.send({
          data: JSON.stringify({
            action: 'start',
            lang: 'autodialect'
          })
        })
        done(true)
      })

      socketTask.onMessage((res) => {
        try {
          const msg = JSON.parse(res?.data || '{}')
          if (msg.event === 'transcript') {
            const fullText = msg.fullText || msg.text || ''
            this.realtimeFinalText = fullText
            this.setData({ liveSpeechText: fullText })
            return
          }
          if (msg.event === 'completed') {
            const finalText = (msg.finalText || this.realtimeFinalText || '').trim()
            this.realtimeFinalText = finalText
            this.setData({ liveSpeechText: finalText })
            return
          }
          if (msg.event === 'error') {
            showError(msg.message || '实时转写失败')
          }
        } catch (e) {
          // ignore
        }
      })

      socketTask.onError(() => {
        done(false)
        this.speechSocketTask = null
        this.speechSocketOpened = false
        this.setData({ recording: false, voiceLoading: false })
        showError('实时转写连接失败')
      })

      socketTask.onClose(() => {
        this.speechSocketTask = null
        this.speechSocketOpened = false
      })
    })
  },

  stopRealtimeSocket() {
    try {
      if (this.speechSocketTask && this.speechSocketOpened) {
        this.speechSocketTask.close({ code: 1000, reason: 'normal close' })
      }
    } catch (e) {
      // ignore
    }
    this.speechSocketTask = null
    this.speechSocketOpened = false
  },

  streamAssistantReply(messageId, finalText) {
    // 保留空实现避免旧调用报错（当前页面已切换为后端流式）。
    const updatedList = this.data.chatList.map((item) => {
      if (item.id !== messageId) return item
      return { ...item, thinking: false, content: finalText || '' }
    })
    this.setData({ chatList: updatedList }, () => this.scrollToBottom(messageId))
  },

  scrollToBottom(lastId) {
    const nextAnchorKey = this.data.anchorKey + 1
    const nextScrollTop = this.data.scrollTop + 100000
    this.setData({
      scrollIntoView: `bottom-anchor-${nextAnchorKey}`,
      anchorKey: nextAnchorKey,
      scrollTop: nextScrollTop
    })
  },

  async restoreConversation(conversationId) {
    try {
      const response = await getConversationHistoryApi(conversationId)
      // 兼容不同后端包装层级：{code,msg,data} 或 {data:{...}} 或直接 {...}
      const detail = response?.data?.data || response?.data || response || {}
      const rawMessages = Array.isArray(detail?.messages)
        ? detail.messages
        : Array.isArray(detail?.data?.messages)
          ? detail.data.messages
          : []
      const messages = rawMessages.map((item) => ({
        id: buildId('h'),
        role: String(item?.role || '').toLowerCase() === 'user' ? 'user' : 'assistant',
        content: item.content || '',
        thinking: false,
        createdAt: Date.now()
      }))
      this.setData({
        conversationTitle: detail?.conversationTitle || '历史会话',
        chatList: messages
      })
      if (messages.length) {
        const lastId = messages[messages.length - 1].id
        this.scrollToBottom(lastId)
      }
      return
    } catch (error) {
      // fallback：后端详情失败时回退本地缓存
    }

    const localList = getStorage(CHAT_HISTORY_KEY) || []
    const target = localList.find((item) => item.conversationId === conversationId)
    if (!target) return
    this.setData({
      chatList: target.messages || [],
      conversationTitle: target.conversationTitle || ''
    })
    if (target.messages?.length) {
      const lastId = target.messages[target.messages.length - 1].id
      this.scrollToBottom(lastId)
    }
  },

  persistConversation() {
    const { conversationId, conversationTitle, chatList, selectedModelCode } = this.data
    if (!conversationId || !chatList.length) {
      return
    }
    const list = getStorage(CHAT_HISTORY_KEY) || []
    const payload = {
      conversationId,
      conversationTitle: conversationTitle || '新会话',
      modelCode: selectedModelCode,
      updatedAt: Date.now(),
      messages: chatList.map((item) => ({
        id: item.id,
        role: item.role,
        content: item.content,
        thinking: false,
        createdAt: item.createdAt
      }))
    }
    const filtered = list.filter((item) => item.conversationId !== conversationId)
    setStorage(CHAT_HISTORY_KEY, [payload, ...filtered].slice(0, 30))
  },
  readImageAsBase64(filePath) {
    return new Promise((resolve, reject) => {
      if (!filePath) {
        reject(new Error('图片路径为空'))
        return
      }
      const fs = wx.getFileSystemManager()
      fs.readFile({
        filePath,
        encoding: 'base64',
        success: (res) => {
          const base64 = res?.data || ''
          if (!base64) {
            reject(new Error('图片内容为空'))
            return
          }
          const ext = (filePath.split('.').pop() || '').toLowerCase()
          const mimeType = ext === 'png' ? 'image/png' : (ext === 'bmp' ? 'image/bmp' : 'image/jpeg')
          resolve({ base64, mimeType })
        },
        fail: () => reject(new Error('读取图片失败'))
      })
    })
  },

  compressImageIfNeeded(filePath) {
    return new Promise((resolve) => {
      if (!filePath || !wx.compressImage) {
        resolve(filePath)
        return
      }
      wx.getFileInfo({
        filePath,
        success: (info) => {
          const size = Number(info?.size || 0)
          // 小图不压缩，避免画质损失
          if (!Number.isFinite(size) || size <= 350 * 1024) {
            resolve(filePath)
            return
          }
          // 尽量压到网关可接受的大小
          wx.compressImage({
            src: filePath,
            quality: size > 2 * 1024 * 1024 ? 55 : 70,
            success: (res) => resolve(res?.tempFilePath || filePath),
            fail: () => resolve(filePath)
          })
        },
        fail: () => resolve(filePath)
      })
    })
  },
})
