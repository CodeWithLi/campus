Page({
  data: {
    quickEntries: [
      { key: 'qa', title: '智能问答', desc: '支持多模型与RAG问答' },
      { key: 'notice', title: '通知解读', desc: '政策通知快速提炼重点' },
      { key: 'guide', title: '办事指南', desc: '流程卡片与材料清单' },
      { key: 'community', title: '社区', desc: '发帖互动与经验分享' }
    ]
  },

  onTapEntry(e) {
    const key = e.currentTarget.dataset?.key
    if (key === 'community') {
      wx.navigateTo({ url: '/pages/post-create/post-create' })
      return
    }
    if (key === 'qa') {
      wx.switchTab({ url: '/pages/ai/ai' })
      return
    }
    wx.showToast({ title: '该能力开发中', icon: 'none' })
  }
})
