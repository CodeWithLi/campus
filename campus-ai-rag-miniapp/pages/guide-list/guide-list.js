import { listGuideCategoriesApi, listGuidesApi } from '../../api/guide'
import { showError } from '../../utils/ui'

Page({
  data: {
    keyword: '',
    categoryOptions: ['全部分类'],
    selectedCategoryIndex: 0,
    list: [],
    loading: false
  },

  onLoad() {
    this.loadCategories()
    this.loadList()
  },

  onPullDownRefresh() {
    this.loadList(true)
  },

  onKeywordInput(e) {
    this.setData({ keyword: e.detail?.value || '' })
  },

  onCategoryChange(e) {
    const selectedCategoryIndex = Number(e.detail?.value || 0)
    this.setData({ selectedCategoryIndex })
  },

  onSearch() {
    this.loadList()
  },

  async loadCategories() {
    try {
      const res = await listGuideCategoriesApi()
      const list = Array.isArray(res?.data) ? res.data : []
      const categoryOptions = ['全部分类', ...list.filter((it) => String(it || '').trim())]
      this.setData({ categoryOptions, selectedCategoryIndex: 0 })
    } catch (e) {
      this.setData({ categoryOptions: ['全部分类'], selectedCategoryIndex: 0 })
    }
  },

  async loadList(fromPullDown = false) {
    this.setData({ loading: true })
    try {
      const params = {}
      const keyword = (this.data.keyword || '').trim()
      const selectedCategory = this.data.categoryOptions[this.data.selectedCategoryIndex] || '全部分类'
      const category = selectedCategory === '全部分类' ? '' : String(selectedCategory).trim()
      if (keyword) params.keyword = keyword
      if (category) params.category = category
      const res = await listGuidesApi(params)
      this.setData({ list: Array.isArray(res?.data) ? res.data : [] })
    } catch (e) {
      showError(e.message || '加载办事指南失败')
    } finally {
      this.setData({ loading: false })
      if (fromPullDown) wx.stopPullDownRefresh()
    }
  },

  onOpenDetail(e) {
    const id = e.currentTarget.dataset?.id
    if (!id) return
    wx.navigateTo({ url: `/pages/guide-detail/guide-detail?id=${id}` })
  }
})
