const RECENT_ENTRY_KEY = 'AI_RECENT_ENTRY_LIST'
const CHAT_HISTORY_KEY = 'CHAT_HISTORY_LIST'
const RAG_CHAT_HISTORY_KEY = 'RAG_CHAT_HISTORY_LIST'

Page({
  data: {
    greeting: '你好',
    highlightTitle: '今天也要高效推进',
    highlightDesc: '把问题交给 AI，省下时间去做更重要的事。',
    entries: [
      {
        key: 'chat',
        title: 'AI 对话',
        desc: '多模型问答、实时语音、上下文连续记忆',
        tag: '高频推荐',
        icon: '💬',
        gradient: 'linear-gradient(135deg, #eef2ff, #dbeafe)',
        delay: 0,
        action: 'go-chat'
      },
      {
        key: 'rag',
        title: 'RAG 问答',
        desc: '上传资料后精准检索，回答自带参考来源',
        tag: '学习提效',
        icon: '📚',
        gradient: 'linear-gradient(135deg, #ecfeff, #d1fae5)',
        delay: 100,
        action: 'go-rag'
      },
      {
        key: 'resume',
        title: '简历生成',
        desc: '对话润色简历，一键产出可下载版本',
        tag: '求职加速',
        icon: '🧾',
        gradient: 'linear-gradient(135deg, #f5f3ff, #fce7f3)',
        delay: 200,
        action: 'go-resume'
      }
    ],
    inspirationList: [
      '帮我把今天课程重点整理成 5 条复习卡片',
      '根据我上传的文档，生成一份可执行学习计划',
      '模拟一次面试，围绕我的简历连续追问',
      '把复杂通知拆成待办清单和截止时间'
    ],
    recentEntries: [],
    recentSummaryCards: []
  },

  onLoad() {
    this.updateGreeting()
  },

  onShow() {
    this.loadRecentEntries()
    this.loadRecentSummaries()
  },

  updateGreeting() {
    const hour = new Date().getHours()
    let greeting = '你好'
    if (hour < 6) greeting = '夜深了'
    else if (hour < 12) greeting = '早上好'
    else if (hour < 18) greeting = '下午好'
    else greeting = '晚上好'
    this.setData({ greeting })
  },

  onEntryTap(event) {
    const { action, key } = event.currentTarget.dataset
    this.recordRecentEntry(key)
    this.navigateByAction(action)
  },

  onTapRecent(event) {
    const { action, key } = event.currentTarget.dataset
    this.recordRecentEntry(key)
    this.navigateByAction(action)
  },

  onPickInspiration(event) {
    const text = event.currentTarget.dataset.text || ''
    wx.setClipboardData({
      data: text,
      success: () => {
        wx.showToast({ title: '灵感已复制，去对话试试', icon: 'none' })
      }
    })
  },

  navigateByAction(action) {
    if (action === 'go-chat') {
      wx.navigateTo({ url: '/pages/ai-chat/ai-chat' })
      return
    }
    if (action === 'go-rag') {
      wx.navigateTo({ url: '/pages/ai-rag/ai-rag' })
      return
    }
    if (action === 'go-resume') {
      wx.navigateTo({ url: '/pages/ai-resume/ai-resume' })
      return
    }
    if (action === 'go-history') {
      wx.navigateTo({ url: '/pages/ai-history/ai-history' })
    }
  },

  recordRecentEntry(key) {
    const target = this.data.entries.find((item) => item.key === key)
    if (!target) return
    const list = wx.getStorageSync(RECENT_ENTRY_KEY) || []
    const payload = {
      key: target.key,
      title: target.title,
      action: target.action,
      icon: target.icon,
      updatedAt: Date.now()
    }
    const filtered = list.filter((item) => item.key !== target.key)
    wx.setStorageSync(RECENT_ENTRY_KEY, [payload, ...filtered].slice(0, 3))
    this.loadRecentEntries()
  },

  loadRecentEntries() {
    const list = wx.getStorageSync(RECENT_ENTRY_KEY) || []
    this.setData({ recentEntries: list })
  },

  loadRecentSummaries() {
    const chatList = wx.getStorageSync(CHAT_HISTORY_KEY) || []
    const ragList = wx.getStorageSync(RAG_CHAT_HISTORY_KEY) || []

    const chatCards = chatList
      .slice(0, 8)
      .map((item) => {
        const messages = Array.isArray(item?.messages) ? item.messages : []
        const lastUser = [...messages].reverse().find((m) => m?.role === 'user' && m?.content)
        const lastAssistant = [...messages].reverse().find((m) => m?.role === 'assistant' && m?.content)
        return {
          id: `chat_${item?.conversationId || item?.id || ''}`,
          type: 'chat',
          title: item?.conversationTitle || item?.title || 'AI 对话',
          summary: (lastAssistant?.content || '').slice(0, 48) || '点击继续对话',
          question: (lastUser?.content || '').slice(0, 30) || '',
          updatedAt: Number(item?.updatedAt || 0),
          conversationId: item?.conversationId || ''
        }
      })
      .filter((item) => item.title)

    const ragCards = ragList
      .slice(0, 8)
      .map((item) => ({
        id: `rag_${item?.id || ''}`,
        type: 'rag',
        title: item?.title || 'RAG 问答',
        summary: (item?.answer || '').slice(0, 48) || '点击继续提问',
        question: (item?.question || '').slice(0, 30) || '',
        updatedAt: Number(item?.updatedAt || 0),
        conversationId: item?.conversationId || ''
      }))
      .filter((item) => item.title)

    const recentSummaryCards = [...chatCards, ...ragCards]
      .sort((a, b) => b.updatedAt - a.updatedAt)
      .slice(0, 4)

    this.setData({ recentSummaryCards })
  },

  onTapSummary(event) {
    const { type, conversationId } = event.currentTarget.dataset
    if (type === 'chat') {
      const url = conversationId
        ? `/pages/ai-chat/ai-chat?conversationId=${conversationId}`
        : '/pages/ai-chat/ai-chat'
      wx.navigateTo({ url })
      return
    }
    const ragUrl = conversationId
      ? `/pages/ai-rag/ai-rag?conversationId=${conversationId}`
      : '/pages/ai-rag/ai-rag'
    wx.navigateTo({ url: ragUrl })
  },

  goHistory() {
    this.navigateByAction('go-history')
  }
})
