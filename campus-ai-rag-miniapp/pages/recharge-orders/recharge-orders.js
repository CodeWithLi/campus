import { listMyRechargeOrdersApi, queryMyRechargeOrderApi } from '../../api/pay'
import { showError, showSuccess } from '../../utils/ui'

const formatTime = (value) => {
  if (!value) return '-'
  const ms = Date.parse(String(value).replace(/-/g, '/'))
  if (!Number.isFinite(ms)) return String(value)
  const d = new Date(ms)
  const y = d.getFullYear()
  const m = `${d.getMonth() + 1}`.padStart(2, '0')
  const day = `${d.getDate()}`.padStart(2, '0')
  const h = `${d.getHours()}`.padStart(2, '0')
  const min = `${d.getMinutes()}`.padStart(2, '0')
  return `${y}-${m}-${day} ${h}:${min}`
}

const statusText = (status) => {
  if (status === 'PAID') return '已支付'
  if (status === 'CLOSED') return '已关闭'
  if (status === 'FAILED') return '失败'
  return '待支付'
}

Page({
  data: {
    list: [],
    loading: false
  },

  onShow() {
    this.loadOrders()
  },

  async loadOrders() {
    this.setData({ loading: true })
    try {
      const res = await listMyRechargeOrdersApi()
      const raw = Array.isArray(res?.data) ? res.data : []
      const list = raw.map((item) => ({
        ...item,
        amountText: Number(item?.amountYuan || 0).toFixed(2),
        pointsText: Number(item?.points || 0).toFixed(2),
        statusText: statusText(item?.status),
        timeText: formatTime(item?.createdAt)
      }))
      this.setData({ list })
    } catch (e) {
      showError(e.message || '订单加载失败')
    } finally {
      this.setData({ loading: false })
    }
  },

  async onQueryOrder(event) {
    const { orderNo } = event.currentTarget.dataset || {}
    if (!orderNo) return
    wx.showLoading({ title: '查询中...' })
    try {
      const res = await queryMyRechargeOrderApi(orderNo)
      const item = res?.data || {}
      const nextStatus = item?.status || ''
      showSuccess(nextStatus === 'PAID' ? '订单已支付' : '状态已刷新')
      await this.loadOrders()
    } catch (e) {
      showError(e.message || '查询失败')
    } finally {
      wx.hideLoading()
    }
  }
})

