import {
  resumeChatApi,
  resumeGenerateApi,
  listResumeConversationsApi,
  getResumeConversationHistoryApi
} from '../../api/resume'
import { listEnabledModelsApi } from '../../api/model'
import { showError } from '../../utils/ui'

const buildId = (prefix) => `${prefix}_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`

Page({
  data: {
    modelOptions: [],
    selectedModelCode: '',
    selectedModelIndex: 0,
    loading: false,
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

  async onLoad() {
    await this.loadModels()
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

