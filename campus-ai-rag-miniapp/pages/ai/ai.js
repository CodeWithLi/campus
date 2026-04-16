Page({
  data: {
    entries: [
      {
        key: 'chat',
        title: 'AI 对话',
        desc: '多模型对话、历史上下文、流式回复',
        action: 'go-chat'
      },
      {
        key: 'rag',
        title: 'RAG 问答',
        desc: '基于校园知识库的精准问答（即将接入）',
        action: 'go-rag'
      }
    ]
  },

  onEntryTap(event) {
    const { action } = event.currentTarget.dataset
    if (action === 'go-chat') {
      wx.navigateTo({ url: '/pages/ai-chat/ai-chat' })
      return
    }
    if (action === 'go-rag') {
      wx.navigateTo({ url: '/pages/ai-rag/ai-rag' })
    }
  },

  goHistory() {
    wx.navigateTo({ url: '/pages/ai-history/ai-history' })
  }
})
