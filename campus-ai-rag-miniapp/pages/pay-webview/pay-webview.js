import { showError } from '../../utils/ui'

Page({
  data: {
    url: '',
    orderNo: ''
  },

  onLoad(options = {}) {
    const url = decodeURIComponent(options.url || '')
    const orderNo = options.orderNo || ''
    if (!url) {
      showError('支付链接为空')
      return
    }
    this.setData({ url, orderNo })
  }
})

