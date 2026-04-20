import { getPowerPointLogsApi } from '../../api/auth'
import { showError } from '../../utils/ui'

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

Page({
  data: {
    list: [],
    loading: false,
    pageNum: 1,
    pageSize: 20,
    total: 0,
    hasMore: true
  },

  onShow() {
    this.reload()
  },

  async onReachBottom() {
    if (this.data.loading || !this.data.hasMore) return
    await this.loadPage(this.data.pageNum + 1)
  },

  async reload() {
    this.setData({
      list: [],
      loading: false,
      pageNum: 1,
      total: 0,
      hasMore: true
    })
    await this.loadPage(1)
  },

  async loadPage(pageNum) {
    this.setData({ loading: true })
    try {
      const res = await getPowerPointLogsApi({
        pageNum,
        pageSize: this.data.pageSize
      })
      const data = res?.data || {}
      const total = Number(data.total || 0)
      const records = Array.isArray(data.list) ? data.list : []
      const mapped = records.map((item) => ({
        sceneName: item.sceneName || '其他扣费',
        consumedPoints: Number(item.consumedPoints || 0),
        createdAtText: formatTime(item.createdAt)
      }))

      const list = pageNum === 1 ? mapped : this.data.list.concat(mapped)
      const hasMore = list.length < total
      this.setData({
        list,
        pageNum,
        total,
        hasMore
      })
    } catch (e) {
      showError(e.message || '扣费明细加载失败')
    } finally {
      this.setData({ loading: false })
    }
  }
})

