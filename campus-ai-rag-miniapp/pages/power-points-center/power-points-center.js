import { getProfileApi } from '../../api/auth'
import { createAlipayWapOrderApi } from '../../api/pay'
import { buildApiUrl } from '../../config/env'
import { getStorage, setStorage } from '../../utils/storage'
import { showError, showSuccess } from '../../utils/ui'

const USER_INFO_KEY = 'USER_INFO'

const formatPoints = (value) => {
  const num = Number(value || 0)
  if (!Number.isFinite(num)) return '0.00'
  return num.toFixed(2)
}

Page({
  data: {
    powerPoints: '0.00',
    creating: false,
    lastPayUrl: '',
    packages: [
      { price: 1, points: 100 },
      { price: 30, points: 3000 },
      { price: 50, points: 5000 },
      { price: 100, points: 10000 }
    ]
  },

  async onShow() {
    const userInfo = getStorage(USER_INFO_KEY) || {}
    this.setData({ powerPoints: formatPoints(userInfo.powerPoints) })
    await this.refreshBalance()
  },

  async refreshBalance() {
    try {
      const res = await getProfileApi()
      const data = res?.data || {}
      const userInfo = getStorage(USER_INFO_KEY) || {}
      const next = {
        ...userInfo,
        powerPoints: data.powerPoints
      }
      setStorage(USER_INFO_KEY, next)
      this.setData({ powerPoints: formatPoints(next.powerPoints) })
    } catch (e) {
      // ignore
    }
  },

  async onRefreshBalance() {
    wx.showLoading({ title: '刷新中...' })
    try {
      await this.refreshBalance()
      showSuccess('余额已刷新')
    } catch (e) {
      showError(e.message || '刷新失败')
    } finally {
      wx.hideLoading()
    }
  },

  onTapPackage(event) {
    if (this.data.creating) return
    const { price, points } = event.currentTarget.dataset || {}
    if (!price || !points) return
    this.createOrderAndPay(price)
  },

  onExplain() {
    showSuccess('1元=100算力点（展示版）')
  },

  goRechargeOrders() {
    wx.navigateTo({
      url: '/pages/recharge-orders/recharge-orders'
    })
  },

  async createOrderAndPay(price) {
    this.setData({ creating: true })
    wx.showLoading({ title: '创建订单中...' })
    try {
      const res = await createAlipayWapOrderApi({
        amountYuan: Number(price)
      })
      const data = res?.data || {}
      const orderNo = data.orderNo || ''
      if (!orderNo) {
        throw new Error('订单号获取失败')
      }
      const transitUrl = buildApiUrl(`/pay/h5?orderNo=${encodeURIComponent(orderNo)}`)
      this.setData({ lastPayUrl: transitUrl })
      wx.showModal({
        title: '请在外部浏览器支付',
        content: '微信小程序内无法直接唤起支付宝。点击“复制链接”，到系统浏览器打开并完成支付。',
        confirmText: '复制链接',
        cancelText: '稍后再说',
        success: (modalRes) => {
          if (!modalRes.confirm) return
          this.copyPayUrl()
        }
      })
    } catch (e) {
      showError(e.message || '下单失败')
    } finally {
      wx.hideLoading()
      this.setData({ creating: false })
    }
  },

  copyPayUrl() {
    const url = this.data.lastPayUrl || ''
    if (!url) {
      showError('支付链接为空')
      return
    }
    wx.setClipboardData({
      data: url,
      success: () => showSuccess('支付链接已复制')
    })
  }
})

