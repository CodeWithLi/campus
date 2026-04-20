import { listPostsApi } from '../../api/post'
import { showError } from '../../utils/ui'
import { buildApiUrl } from '../../config/env'

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

  onLoad() {
    this.loadList(true)
  },

  async loadList(reset = false) {
    const pageNo = reset ? 1 : this.data.pageNo
    const pageSize = this.data.pageSize
    this.setData({ loading: reset, loadingMore: !reset })
    try {
      const res = await listPostsApi({ pageNo, pageSize })
      const data = res?.data || {}
      const total = Number(data.total || 0)
      const rows = Array.isArray(data.list) ? data.list : []
      const normalized = rows.map((item) => ({
        ...item,
        fullImageUrls: (item.imageUrls || []).map((u) => this.fullUrl(u))
      }))
      const list = reset ? normalized : [...this.data.list, ...normalized]
      const hasMore = list.length < total
      this.setData({
        list,
        total,
        pageNo: pageNo + 1,
        hasMore
      })
    } catch (e) {
      showError(e.message || '加载帖子失败')
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
  },

  onCreatePost() {
    this.navigateSafe('/pages/post-create/post-create')
  },

  onMyPosts() {
    this.navigateSafe('/pages/my-posts/my-posts')
  },

  navigateSafe(targetPath) {
    const pages = getCurrentPages()
    const targetRoute = targetPath.replace(/^\//, '')
    const currentRoute = pages[pages.length - 1]?.route
    if (currentRoute === targetRoute) return

    const existingIndex = pages.findIndex((page) => page.route === targetRoute)
    if (existingIndex >= 0) {
      const delta = pages.length - 1 - existingIndex
      if (delta > 0) {
        wx.navigateBack({ delta })
      }
      return
    }

    if (pages.length >= 9) {
      wx.redirectTo({ url: targetPath })
      return
    }

    wx.navigateTo({ url: targetPath })
  },

  fullUrl(path) {
    if (!path) return ''
    if (/^https?:\/\//.test(path)) return path
    return buildApiUrl(path)
  }
})
