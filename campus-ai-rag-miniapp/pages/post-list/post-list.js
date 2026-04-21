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

  onPullDownRefresh() {
    this.loadList(true, true)
  },

  async loadList(reset = false, fromPullDown = false) {
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
        displayTime: this.formatRelativeTime(item.createdAt),
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
      if (fromPullDown) {
        wx.stopPullDownRefresh()
      }
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
  },

  formatRelativeTime(datetime) {
    if (!datetime) return ''
    const parsed = new Date(datetime.replace(/-/g, '/'))
    if (Number.isNaN(parsed.getTime())) return datetime
    const diffMs = Date.now() - parsed.getTime()
    if (diffMs < 0) return datetime
    const minute = 60 * 1000
    const hour = 60 * minute
    const day = 24 * hour
    if (diffMs < minute) return '刚刚'
    if (diffMs < hour) return `${Math.floor(diffMs / minute)}分钟前`
    if (diffMs < day) return `${Math.floor(diffMs / hour)}小时前`
    if (diffMs < 7 * day) return `${Math.floor(diffMs / day)}天前`
    return datetime
  }
})
