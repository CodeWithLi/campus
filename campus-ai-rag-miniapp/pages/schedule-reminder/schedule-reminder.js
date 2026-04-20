import { createReminderApi, deleteReminderApi, listReminderApi } from '../../api/reminder'
import { showError, showSuccess } from '../../utils/ui'
import { REMINDER_SUBSCRIBE_TEMPLATE_ID } from '../../config/env'

const typeOptions = [
  { value: 'exam', label: '考试' },
  { value: 'ddl', label: 'DDL' },
  { value: 'activity', label: '活动' }
]

const pad = (n) => `${n}`.padStart(2, '0')

const todayDate = () => {
  const d = new Date()
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

const nowTime = () => {
  const d = new Date()
  return `${pad(d.getHours())}:${pad(d.getMinutes())}`
}

Page({
  data: {
    title: '',
    typeIndex: 0,
    date: todayDate(),
    time: nowTime(),
    list: [],
    typeOptions
  },

  onShow() {
    this.refreshList()
  },

  async refreshList() {
    try {
      const res = await listReminderApi()
      const list = Array.isArray(res?.data) ? res.data : []
      this.setData({ list })
    } catch (e) {
      showError(e.message || '加载提醒失败')
    }
  },

  onTitleInput(e) {
    this.setData({ title: e.detail?.value || '' })
  },

  onTypeChange(e) {
    this.setData({ typeIndex: Number(e.detail?.value || 0) })
  },

  onDateChange(e) {
    this.setData({ date: e.detail?.value || this.data.date })
  },

  onTimeChange(e) {
    this.setData({ time: e.detail?.value || this.data.time })
  },

  async onCreateReminder() {
    const title = (this.data.title || '').trim()
    if (!title) {
      showError('请输入提醒标题')
      return
    }
    const type = this.data.typeOptions[this.data.typeIndex]?.value || 'activity'
    const remindAt = new Date(`${this.data.date.replace(/-/g, '/')} ${this.data.time}`).getTime()
    if (!Number.isFinite(remindAt)) {
      showError('提醒时间格式无效')
      return
    }
    if (remindAt <= Date.now()) {
      showError('提醒时间需晚于当前时间')
      return
    }
    try {
      this.requestSubscribePermission()
      await createReminderApi({ title, type, remindAt })
      showSuccess('提醒已创建')
      this.setData({ title: '' })
      this.refreshList()
    } catch (e) {
      showError(e.message || '创建提醒失败')
    }
  },

  async onDeleteReminder(e) {
    const id = e.currentTarget.dataset?.id
    if (!id) return
    try {
      await deleteReminderApi(id)
      showSuccess('已删除提醒')
      this.refreshList()
    } catch (e) {
      showError(e.message || '删除失败')
    }
  },

  requestSubscribePermission() {
    const tmplId = (REMINDER_SUBSCRIBE_TEMPLATE_ID || '').trim()
    if (!tmplId) return
    wx.requestSubscribeMessage({
      tmplIds: [tmplId],
      complete: () => {}
    })
  }
})
