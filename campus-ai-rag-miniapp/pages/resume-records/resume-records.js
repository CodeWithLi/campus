import { listResumeRecordsApi } from '../../api/resume'
import { showError } from '../../utils/ui'

Page({
  data: {
    list: [],
    pageNo: 1,
    pageSize: 10,
    total: 0,
    hasMore: true,
    loading: false,
    loadingMore: false
  },

  onShow() {
    this.loadList(true)
  },

  async loadList(reset = false) {
    const pageNo = reset ? 1 : this.data.pageNo
    const pageSize = this.data.pageSize
    this.setData({ loading: reset, loadingMore: !reset })
    try {
      const res = await listResumeRecordsApi({ pageNo, pageSize })
      const data = res?.data || {}
      const total = Number(data.total || 0)
      const rows = Array.isArray(data.list) ? data.list : []
      const list = reset ? rows : [...this.data.list, ...rows]
      this.setData({
        list,
        total,
        pageNo: pageNo + 1,
        hasMore: list.length < total
      })
    } catch (e) {
      showError(e.message || '加载简历记录失败')
    } finally {
      this.setData({ loading: false, loadingMore: false })
    }
  },

  onLoadMore() {
    if (!this.data.hasMore || this.data.loadingMore) return
    this.loadList(false)
  },

  onCopyLink(e) {
    const link = e.currentTarget.dataset?.url || ''
    if (!link) return
    wx.setClipboardData({ data: link })
  },

  onOpenLink(e) {
    const link = e.currentTarget.dataset?.url || ''
    if (!link) return
    wx.setClipboardData({
      data: link,
      success: () => {
        wx.showToast({ title: '链接已复制，请在浏览器打开', icon: 'none' })
      }
    })
  },

  onGoResumeGenerate() {
    wx.navigateTo({ url: '/pages/ai-resume/ai-resume' })
  }
})
