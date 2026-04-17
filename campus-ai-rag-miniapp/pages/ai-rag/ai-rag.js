import { listEnabledModelsApi } from '../../api/model'
import { ragApi, listRagConversationsApi, getRagConversationHistoryApi } from '../../api/rag'
import { getStorage, setStorage } from '../../utils/storage'
import { showError } from '../../utils/ui'

const CHAT_HISTORY_KEY = 'RAG_CHAT_HISTORY_LIST'
// const BASE_URL = 'http://10.0.4.168:8899'
const BASE_URL = 'https://admin.baomi-ai.com/campus'


const buildId = (prefix) => `${prefix}_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`

Page({
  data: {
    modelOptions: [],
    selectedModelCode: '',
    selectedModelIndex: 0,

    chatList: [],
    anchorKey: 0,
    scrollIntoView: '',
    scrollTop: 0,

    loading: false,

    // uploaded docs preview in composer
    docs: [],
    showDocSheet: false,
    uploading: false,

    // server-side rag conversation
    conversationId: '',
    conversationTitle: '',

    showHistoryDrawer: false,
    historyList: [],

    // RAG local history (optional UI parity)
  },

  async onLoad(options = {}) {
    const conversationId = options?.conversationId || ''
    this.setData({ conversationId })
    await this.loadModels()
    if (conversationId) {
      await this.restoreConversation(conversationId)
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

  onQuickAsk(event) {
    const message = event.currentTarget.dataset.text || ''
    this.sendRag(message)
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
      const response = await listRagConversationsApi()
      const list = response?.data?.data || response?.data || response || []
      this.setData({ historyList: list })
    } catch (error) {
      this.setData({ historyList: [] })
      showError(error.message || '历史会话加载失败')
    }
  },

  async onSelectHistory(event) {
    const { conversationId } = event.currentTarget.dataset
    if (!conversationId || this.data.loading) return
    this.closeHistoryDrawer()
    this.setData({
      conversationId,
      conversationTitle: '',
      chatList: [],
      docs: [],
      showDocSheet: false,
      loading: false,
      anchorKey: 0,
      scrollIntoView: '',
      scrollTop: 0
    })
    await this.restoreConversation(conversationId)
  },

  onNewConversation() {
    this.closeHistoryDrawer()
    this.setData({
      conversationId: '',
      conversationTitle: '',
      chatList: [],
      docs: [],
      showDocSheet: false,
      loading: false,
      anchorKey: 0,
      scrollIntoView: '',
      scrollTop: 0
    })
  },

  goUpload() {
    if (this.data.loading || this.data.uploading) return
    this.pickAndUploadFile()
  },

  onRemoveDoc() {
    this.setData({ docs: [], showDocSheet: false })
  },

  onConfirmDocSheet() {
    // keep docs attached; just hide the dark sheet
    this.setData({ showDocSheet: false })
  },

  async pickAndUploadFile() {
    try {
      this.setData({ uploading: true })
      wx.showLoading({ title: '上传中...' })

      wx.chooseMessageFile({
        count: 1,
        type: 'file',
        success: async (res) => {
          const tempFile = res?.tempFiles?.[0]
          if (!tempFile) {
            wx.hideLoading()
            this.setData({ uploading: false })
            return
          }

          const filePath = tempFile.path || tempFile.filePath
          const originalFileName = tempFile.name || tempFile.fileName || '文档'
          const title = originalFileName

          await this.uploadKbdocument({ filePath, title, remark: '' })

          wx.hideLoading()
          this.setData({ uploading: false })
        },
        fail: () => {
          wx.hideLoading()
          this.setData({ uploading: false })
        }
      })
    } catch (e) {
      wx.hideLoading()
      this.setData({ uploading: false })
      showError(e.message || '上传失败')
    }
  },

  uploadKbdocument({ filePath, title, remark }) {
    return new Promise((resolve, reject) => {
      const token = getStorage('TOKEN')
      const authorization = token ? `Bearer ${token}` : ''

      wx.uploadFile({
        url: `${BASE_URL}/wx/kb/documents/upload`,
        filePath,
        name: 'file',
        formData: { title, remark: remark || '' },
        header: { Authorization: authorization },
        success: (res) => {
          try {
            const parsed = typeof res.data === 'string' ? JSON.parse(res.data) : res.data
            if (parsed?.code === 200 && parsed?.data) {
              const doc = parsed.data
              this.setData({
                docs: [
                  {
                    id: doc.id,
                    title: doc.title || doc.originalFileName || title,
                    originalFileName: doc.originalFileName || title,
                    fileExt: doc.fileExt || ''
                  }
                ],
                showDocSheet: true
              })
              resolve()
              return
            }
            reject(new Error(parsed?.msg || '上传失败'))
          } catch (e) {
            reject(e)
          }
        },
        fail: (error) => reject(error)
      })
    })
  },

  async onSendMessage(event) {
    const message = (event.detail?.message || '').trim()
    await this.sendRag(message)
  },

  async sendRag(message) {
    if (this.data.loading) return
    if (!message) {
      showError('请输入问题')
      return
    }

    const userMessageId = buildId('u')
    const thinkingId = buildId('a')

    const attachments = (this.data.docs || []).map((d) => ({
      title: d.title || d.originalFileName || '文件'
    }))

    const docIds = (this.data.docs || [])
      .map((d) => Number(d.id))
      .filter((id) => Number.isFinite(id) && id > 0)

    const userMessage = {
      id: userMessageId,
      role: 'user',
      content: message,
      attachments,
      createdAt: Date.now()
    }

    const thinkingMessage = {
      id: thinkingId,
      role: 'assistant',
      content: '',
      thinking: true,
      attachments: [],
      createdAt: Date.now()
    }

    const chatList = [...this.data.chatList, userMessage, thinkingMessage]
    this.setData(
      {
        chatList,
        loading: true,
        showDocSheet: false
      },
      () => {
        this.scrollToBottom(thinkingId)
      }
    )

    try {
      const response = await ragApi({
        modelCode: this.data.selectedModelCode || null,
        conversationId: this.data.conversationId || null,
        question: message,
        categoryIds: null,
        topK: 5,
        fileDocumentIds: docIds
      })

      const answer = response?.data?.answer || ''
      const hits = response?.data?.hits || []
      const conversationId = response?.data?.conversationId || this.data.conversationId
      const conversationTitle = response?.data?.conversationTitle || this.data.conversationTitle
      const markdown = this.buildRagMarkdown(answer, hits)

      this.setData({ conversationId, conversationTitle })

      this.streamAssistantReply(thinkingId, markdown)
    } catch (error) {
      const failedList = this.data.chatList.map((item) => {
        if (item.id !== thinkingId) return item
        return {
          ...item,
          thinking: false,
          content: '请求失败，请稍后重试。'
        }
      })
      this.setData({ chatList: failedList, loading: false }, () => {
        this.scrollToBottom(thinkingId)
        this.persistLocalHistory()
      })
      showError(error.message || 'RAG 请求失败')
    }
  },

  buildRagMarkdown(answer, hits) {
    const safeAnswer = String(answer || '')
    const safeHits = Array.isArray(hits) ? hits : []
    const titles = safeHits.map((h) => h?.documentTitle).filter(Boolean)
    const uniqTitles = Array.from(new Set(titles)).slice(0, 6)
    if (!uniqTitles.length) return safeAnswer
    return `${safeAnswer}\n\n---\n### 参考资料\n${uniqTitles.map((t) => `- ${t}`).join('\n')}`
  },

  streamAssistantReply(messageId, finalText) {
    let index = 0
    const timer = setInterval(() => {
      index += 1
      const content = finalText.slice(0, index)
      const done = index >= finalText.length

      const updatedList = this.data.chatList.map((item) => {
        if (item.id !== messageId) return item
        return {
          ...item,
          thinking: false,
          content
        }
      })

      this.setData({ chatList: updatedList }, () => {
        this.scrollToBottom(messageId)
      })

      if (done) {
        clearInterval(timer)
        this.setData({ loading: false }, () => {
          this.persistLocalHistory()
        })
      }
    }, 22)
  },

  scrollToBottom() {
    const nextAnchorKey = this.data.anchorKey + 1
    const nextScrollTop = this.data.scrollTop + 100000
    this.setData({
      scrollIntoView: `bottom-anchor-${nextAnchorKey}`,
      anchorKey: nextAnchorKey,
      scrollTop: nextScrollTop
    })
  },

  async restoreConversation(conversationId) {
    if (!conversationId) return
    try {
      const response = await getRagConversationHistoryApi(conversationId)
      const detail = response?.data?.data || response?.data || response || {}

      const rawMessages = Array.isArray(detail?.messages) ? detail.messages : []
      const rawFiles = Array.isArray(detail?.files) ? detail.files : []

      const messages = rawMessages.map((item) => ({
        id: buildId('h'),
        role: String(item?.role || '').toLowerCase() === 'user' ? 'user' : 'assistant',
        content: item?.content || '',
        thinking: false,
        attachments: [],
        createdAt: Date.now()
      }))

      const docs = rawFiles.map((f) => ({
        id: f?.documentId,
        title: f?.title || f?.originalFileName || '',
        originalFileName: f?.originalFileName || '',
        fileExt: f?.fileExt || ''
      }))

      this.setData(
        {
          conversationId,
          conversationTitle: detail?.conversationTitle || '',
          chatList: messages,
          docs,
          showDocSheet: false,
          loading: false,
          anchorKey: 0,
          scrollIntoView: '',
          scrollTop: 0
        },
        () => {
          if (messages.length) {
            this.scrollToBottom()
          }
        }
      )
    } catch (error) {
      this.setData({ chatList: [], docs: [], loading: false })
      showError(error?.message || '会话恢复失败')
    }
  },

  persistLocalHistory() {
    try {
      const chatList = this.data.chatList || []
      if (!chatList.length) return
      const lastUser = [...chatList].reverse().find((m) => m.role === 'user' && m.content)
      const lastAssistant = [...chatList].reverse().find((m) => m.role === 'assistant' && m.content)
      if (!lastUser || !lastAssistant) return

      const list = getStorage(CHAT_HISTORY_KEY) || []
      const payload = {
        id: buildId('h'),
        title: lastUser.content.slice(0, 20),
        question: lastUser.content,
        answer: lastAssistant.content,
        updatedAt: Date.now()
      }

      const filtered = list.filter((i) => i.id !== payload.id)
      setStorage(CHAT_HISTORY_KEY, [payload, ...filtered].slice(0, 30))
    } catch (e) {
      // ignore
    }
  }
})
