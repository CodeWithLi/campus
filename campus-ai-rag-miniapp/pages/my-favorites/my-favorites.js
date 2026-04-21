import { listMyFavoritePostsApi } from '../../api/post'
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
      const res = await listMyFavoritePostsApi({ pageNo, pageSize })
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
      showError(e.message || '加载收藏失败')
    } finally {
      this.setData({ loading: false, loadingMore: false })
    }
  },

  onLoadMore() {
    if (!this.data.hasMore || this.data.loadingMore) return
    this.loadList(false)
  },

  onOpenDetail(e) {
    const id = e.currentTarget.dataset?.id
    if (!id) return
    wx.navigateTo({ url: `/pages/post-detail/post-detail?id=${id}` })
  }
})
