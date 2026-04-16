import { chatApi, getConversationHistoryApi, listConversationsApi } from '../../api/chat'
import { listEnabledModelsApi } from '../../api/model'
import { getStorage, setStorage } from '../../utils/storage'
import { showError } from '../../utils/ui'

const CHAT_HISTORY_KEY = 'CHAT_HISTORY_LIST'

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
    showHistoryDrawer: false,
    historyList: []
  },

  async onLoad(options) {
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

  onSendMessage(event) {
    const message = event.detail?.message || ''
    this.sendMessage(message)
  },

  onQuickAsk(event) {
    const message = event.currentTarget.dataset.text || ''
    this.sendMessage(message)
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

  async sendMessage(message) {
    if (!message || this.data.loading) {
      return
    }
    const userMessageId = buildId('u')
    const thinkingId = buildId('a')
    const userMessage = {
      id: userMessageId,
      role: 'user',
      content: message,
      createdAt: Date.now()
    }
    const thinkingMessage = {
      id: thinkingId,
      role: 'assistant',
      content: '',
      thinking: true,
      createdAt: Date.now()
    }

    const chatList = [...this.data.chatList, userMessage, thinkingMessage]
    this.setData({ chatList, loading: true }, () => {
      this.scrollToBottom(thinkingId)
    })

    try {
      const response = await chatApi({
        modelCode: this.data.selectedModelCode || null,
        conversationId: this.data.conversationId || null,
        message
      })
      const conversationId = response?.data?.conversationId || this.data.conversationId
      const conversationTitle = response?.data?.conversationTitle || this.data.conversationTitle
      const answer = response?.data?.answer || ''

      this.setData({ conversationId, conversationTitle })
      this.streamAssistantReply(thinkingId, answer)
    } catch (error) {
      const failedList = this.data.chatList.map((item) => {
        if (item.id !== thinkingId) {
          return item
        }
        return {
          ...item,
          thinking: false,
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

  streamAssistantReply(messageId, finalText) {
    let index = 0
    const timer = setInterval(() => {
      index += 1
      const content = finalText.slice(0, index)
      const done = index >= finalText.length
      const updatedList = this.data.chatList.map((item) => {
        if (item.id !== messageId) {
          return item
        }
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
          this.persistConversation()
        })
      }
    }, 28)
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
  }
})
