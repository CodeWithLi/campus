import { showError } from '../../utils/ui'

Page({
  data: {
    url: ''
  },

  onLoad(options = {}) {
    const url = decodeURIComponent(options.url || '')
    const title = decodeURIComponent(options.title || '办事链接')
    if (!url) {
      showError('链接为空')
      return
    }
    wx.setNavigationBarTitle({ title })
    this.setData({ url })
  }
})
