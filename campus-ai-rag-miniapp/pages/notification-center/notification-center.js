import { listReminderNoticeApi, readAllReminderNoticeApi, readReminderNoticeApi } from '../../api/reminder'
import { showError } from '../../utils/ui'

Page({
  data: {
    list: [],
    unreadCount: 0
  },

  onShow() {
    this.refresh()
  },

  async refresh() {
    try {
      const res = await listReminderNoticeApi()
      const data = res?.data || {}
      this.setData({
        list: Array.isArray(data.list) ? data.list : [],
        unreadCount: Number(data.unreadCount || 0)
      })
    } catch (e) {
      showError(e.message || '加载通知失败')
    }
  },

  async onTapNotice(e) {
    const id = e.currentTarget.dataset?.id
    if (!id) return
    try {
      await readReminderNoticeApi(id)
      this.refresh()
    } catch (e) {
      showError(e.message || '操作失败')
    }
  },

  async onMarkAllRead() {
    try {
      await readAllReminderNoticeApi()
      this.refresh()
    } catch (e) {
      showError(e.message || '操作失败')
    }
  }
})
