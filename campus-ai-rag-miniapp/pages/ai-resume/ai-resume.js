import {
  resumeChatApi,
  resumeGenerateApi,
  listResumeConversationsApi,
  getResumeConversationHistoryApi
} from '../../api/resume'
import { listEnabledModelsApi } from '../../api/model'
import { showError } from '../../utils/ui'

const buildId = (prefix) => `${prefix}_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`
const BASE_URL = 'https://admin.baomi-ai.com/campus'
const REALTIME_PATH = '/ws/wx/speech/realtime'

Page({
  data: {
    modelOptions: [],
    selectedModelCode: '',
    selectedModelIndex: 0,
    loading: false,
    recording: false,
    voiceLoading: false,
    liveSpeechText: '',
    chatList: [],
    conversationId: '',
    conversationTitle: '',
    resumeText: '',
    generateResult: {},
    showHistoryDrawer: false,
    historyList: [],
    anchorKey: 0,
    scrollIntoView: '',
    templateOptions: [
      { value: 'CAMPUS', label: '校招' },
      { value: 'EXPERIENCED', label: '社招' },
      { value: 'INTERN', label: '实习' }
    ],
    templateIndex: 0,
    jobDirection: '',
    quickQuestionList: [
      '帮我先梳理一份适合应届生的简历结构',
      '根据岗位方向，给我输出可直接粘贴的项目经历模板',
      '帮我优化这份简历的自我评价和技能描述'
    ]
  },

  recorderManager: null,
  recordingStartAt: 0,
  speechSocketTask: null,
  speechSocketOpened: false,
  realtimeFinalText: '',

  async onLoad() {
    this.initRecorder()
    await this.loadModels()
  },

  onUnload() {
    this.stopRealtimeSocket()
    if (this.recorderManager) {
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
    const selectedModelIndex = Number(event.detail?.value || 0)
    const selectedModel = this.data.modelOptions[selectedModelIndex]
    this.setData({
      selectedModelIndex,
      selectedModelCode: selectedModel?.modelCode || ''
    })
  },

  async onSendMessage(event) {
    const message = (event.detail?.message || '').trim()
    this.sendMessage(message)
  },

  onSendQuickQuestion(event) {
    const message = String(event.currentTarget.dataset?.question || '').trim()
    this.sendMessage(message)
  },

  onVoiceTap() {
    if (this.data.recording) {
      this.stopRealtimeRecord()
      return
    }
    if (this.data.loading || this.data.voiceLoading) return
    if (!this.recorderManager) {
      showError('当前设备不支持录音')
      return
    }
    this.startRecord()
  },

  async sendMessage(message) {
    if (!message || this.data.loading) return

    const userId = buildId('u')
    const aiId = buildId('a')
    const chatList = [
      ...this.data.chatList,
      { id: userId, role: 'user', content: message },
      { id: aiId, role: 'assistant', content: '思考中...' }
    ]
    this.setData({ chatList, loading: true }, () => this.scrollToBottom())

    try {
      const res = await resumeChatApi({
        conversationId: this.data.conversationId || null,
        modelCode: this.data.selectedModelCode || null,
        message
      })
      const answer = res?.data?.answer || ''
      const conversationId = res?.data?.conversationId || this.data.conversationId
      const conversationTitle = res?.data?.conversationTitle || this.data.conversationTitle

      const updated = this.data.chatList.map((m) =>
        m.id === aiId ? { ...m, content: answer || '（无回复）' } : m
      )
      this.setData({ chatList: updated, conversationId, conversationTitle, loading: false }, () => this.scrollToBottom())
    } catch (e) {
      const updated = this.data.chatList.map((m) =>
        m.id === aiId ? { ...m, content: '请求失败，请稍后重试。' } : m
      )
      this.setData({ chatList: updated, loading: false }, () => this.scrollToBottom())
      showError(e.message || '简历对话失败')
    }
  },

  onResumeTextInput(e) {
    this.setData({ resumeText: e.detail?.value || '' })
  },

  onTemplateChange(e) {
    this.setData({ templateIndex: Number(e.detail?.value || 0) })
  },

  onJobDirectionInput(e) {
    this.setData({ jobDirection: e.detail?.value || '' })
  },

  initRecorder() {
    if (!wx.getRecorderManager) return
    const recorderManager = wx.getRecorderManager()
    this.recorderManager = recorderManager

    recorderManager.onStop(() => {
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
      const wsUrl = `${BASE_URL.replace(/^https/, 'wss').replace(/^http/, 'ws')}${REALTIME_PATH}`
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

  onFillFromLastAssistant() {
    const lastAssistant = [...(this.data.chatList || [])]
      .reverse()
      .find((m) => m.role === 'assistant' && m.content && m.content.trim())
    if (!lastAssistant) {
      showError('暂无可填充的AI回复')
      return
    }
    this.setData({ resumeText: lastAssistant.content })
    wx.showToast({ title: '已填充', icon: 'success' })
  },

  async onGenerateResume() {
    if (this.data.loading) return
    const text = (this.data.resumeText || '').trim()
    if (!text) {
      showError('请先填写或粘贴简历信息')
      return
    }
    this.setData({ loading: true })
    try {
      const selectedTemplate = this.data.templateOptions[this.data.templateIndex]?.value || 'CAMPUS'
      const res = await resumeGenerateApi({
        resumeText: text,
        templateType: selectedTemplate,
        jobDirection: (this.data.jobDirection || '').trim() || null
      })
      this.setData({ generateResult: res?.data || {}, loading: false })
    } catch (e) {
      this.setData({ loading: false })
      showError(e.message || '生成简历失败')
    }
  },

  onCopyLink(e) {
    const url = e.currentTarget.dataset?.url || ''
    if (!url) {
      showError('链接为空')
      return
    }
    wx.setClipboardData({
      data: url,
      success: () => wx.showToast({ title: '已复制', icon: 'success' }),
      fail: () => showError('复制失败')
    })
  },

  onDownloadWord(e) {
    const url = e.currentTarget.dataset?.url || ''
    if (!url) {
      showError('下载链接为空')
      return
    }
    wx.showLoading({ title: '下载中...' })
    wx.downloadFile({
      url,
      success: (res) => {
        wx.hideLoading()
        if (res.statusCode !== 200) {
          showError(`下载失败(${res.statusCode})`)
          return
        }
        wx.openDocument({
          filePath: res.tempFilePath,
          showMenu: true,
          fail: () => {
            showError('打开文档失败，请先复制链接手动下载')
          }
        })
      },
      fail: () => {
        wx.hideLoading()
        showError('下载失败，请检查链接是否有效')
      }
    })
  },

  async goHistory() {
    this.setData({ showHistoryDrawer: true })
    try {
      const res = await listResumeConversationsApi()
      this.setData({ historyList: res?.data || [] })
    } catch (e) {
      this.setData({ historyList: [] })
      showError(e.message || '历史会话加载失败')
    }
  },

  closeHistoryDrawer() {
    this.setData({ showHistoryDrawer: false })
  },

  onNewConversation() {
    this.setData({
      showHistoryDrawer: false,
      conversationId: '',
      conversationTitle: '',
      chatList: [],
      generateResult: {}
    })
  },

  async onSelectHistory(event) {
    const conversationId = event.currentTarget.dataset?.conversationId
    if (!conversationId) return
    this.setData({ showHistoryDrawer: false, conversationId, chatList: [] })
    try {
      const res = await getResumeConversationHistoryApi(conversationId)
      const detail = res?.data || {}
      const messages = (detail.messages || []).map((m) => ({
        id: buildId('h'),
        role: String(m?.role || '').toLowerCase() === 'user' ? 'user' : 'assistant',
        content: m?.content || ''
      }))
      this.setData({
        conversationTitle: detail.conversationTitle || '',
        chatList: messages
      }, () => this.scrollToBottom())
    } catch (e) {
      showError(e.message || '会话恢复失败')
    }
  },

  scrollToBottom() {
    const next = this.data.anchorKey + 1
    this.setData({
      anchorKey: next,
      scrollIntoView: `bottom-anchor-${next}`
    })
  }
})

