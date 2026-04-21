import {
  createReminderApi,
  createReminderByAgentApi,
  deleteNoticeAgentRecordApi,
  deleteReminderApi,
  listNoticeAgentRecordsApi,
  listReminderApi,
  parseNoticeByImageVisionApi,
} from '../../api/reminder'
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
    typeOptions,
    parsingNotice: false,
    creatingByAgent: false,
    agentResult: null,
    agentRecords: [],
    showAgentRecordDialog: false,
    activeAgentRecord: null,
    touchStartX: 0
  },

  onShow() {
    this.refreshList()
    this.loadAgentRecords()
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
  },

  async onUploadNoticeByImageVision() {
    if (this.data.parsingNotice) return
    wx.chooseMedia({
      count: 1,
      mediaType: ['image'],
      sourceType: ['album', 'camera'],
      success: async (res) => {
        const filePath = res?.tempFiles?.[0]?.tempFilePath || ''
        if (!filePath) return
        this.setData({ parsingNotice: true })
        wx.showLoading({ title: '图像理解中...' })
        try {
          const response = await parseNoticeByImageVisionApi(filePath)
          const parsed = response?.data || null
          if (parsed && Array.isArray(parsed.items)) {
            parsed.items = parsed.items.map((it) => {
              const remindAt = Number(it?.remindAt || 0)
              return {
                ...it,
                remindAt,
                dateText: this.toDateText(remindAt),
                timeText: this.toTimeText(remindAt),
                typeIndex: this.typeToIndex(it?.type)
              }
            })
          }
          this.setData({ agentResult: parsed })
          this.loadAgentRecords()
          showSuccess('图像与视频理解解析完成')
        } catch (e) {
          showError(e.message || '图像与视频理解解析失败')
        } finally {
          wx.hideLoading()
          this.setData({ parsingNotice: false })
        }
      }
    })
  },

  async onCreateByAgent() {
    const parsed = this.data.agentResult || {}
    const items = (parsed.items || []).map((it) => ({
      title: (it?.title || '').trim(),
      type: this.data.typeOptions[Number(it?.typeIndex || 0)]?.value || 'activity',
      remindAt: this.toTimestamp(it?.dateText, it?.timeText)
    })).filter((it) => it.title && Number.isFinite(it.remindAt) && it.remindAt > Date.now())
    if (!items.length) {
      showError('暂无可创建的提醒项')
      return
    }
    if (this.data.creatingByAgent) return
    this.requestSubscribePermission()
    this.setData({ creatingByAgent: true })
    try {
      await createReminderByAgentApi(items, parsed.recordId)
      showSuccess('智能体提醒创建成功')
      this.setData({ agentResult: null })
      this.refreshList()
    } catch (e) {
      showError(e.message || '智能体创建失败')
    } finally {
      this.setData({ creatingByAgent: false })
    }
  },

  async loadAgentRecords() {
    try {
      const res = await listNoticeAgentRecordsApi()
      const list = Array.isArray(res?.data) ? res.data : []
      const normalized = list.map((r) => ({
        ...r,
        rawText: r?.rawText || r?.raw_text || '',
        itemsJson: r?.itemsJson ?? r?.items_json ?? '',
        swipeX: 0
      }))
      this.setData({ agentRecords: normalized })
    } catch (e) {
      this.setData({ agentRecords: [] })
    }
  },

  onTapAgentRecord(e) {
    const idx = Number(e.currentTarget.dataset?.index ?? -1)
    if (idx < 0) return
    const record = this.data.agentRecords[idx]
    if (!record) return
    if (Number(record.swipeX || 0) < -20) return
    const rawText = record?.rawText || record?.raw_text || ''
    const itemsRaw = record?.itemsJson ?? record?.items_json ?? ''
    let parsedItems = []
    let itemsRawText = ''
    try {
      if (Array.isArray(itemsRaw)) {
        parsedItems = itemsRaw
      } else if (itemsRaw && typeof itemsRaw === 'object') {
        parsedItems = [itemsRaw]
      } else {
        const arr = JSON.parse(itemsRaw || '[]')
        parsedItems = Array.isArray(arr) ? arr : []
      }
    } catch (err) {
      parsedItems = []
      itemsRawText = String(itemsRaw || '')
    }
    if (!itemsRawText && !parsedItems.length && itemsRaw && itemsRaw !== '[]') {
      itemsRawText = String(itemsRaw)
    }
    this.setData({
      showAgentRecordDialog: true,
      activeAgentRecord: {
        ...record,
        rawText,
        itemsJson: itemsRaw,
        parsedItems,
        itemsRawText
      }
    })
  },

  onAgentRecordTouchStart(e) {
    const x = Number(e.changedTouches?.[0]?.clientX || 0)
    this.setData({ touchStartX: x })
  },

  onAgentRecordTouchMove(e) {
    const idx = Number(e.currentTarget.dataset?.index ?? -1)
    if (idx < 0) return
    const moveX = Number(e.changedTouches?.[0]?.clientX || 0)
    const delta = moveX - Number(this.data.touchStartX || 0)
    let swipeX = 0
    if (delta < 0) {
      swipeX = Math.max(-150, delta)
    }
    this.setData({ [`agentRecords[${idx}].swipeX`]: swipeX })
  },

  onAgentRecordTouchEnd(e) {
    const idx = Number(e.currentTarget.dataset?.index ?? -1)
    if (idx < 0) return
    const current = Number(this.data.agentRecords[idx]?.swipeX || 0)
    const finalX = current <= -60 ? -150 : 0
    this.setData({ [`agentRecords[${idx}].swipeX`]: finalX })
  },

  async onDeleteAgentRecord(e) {
    const idx = Number(e.currentTarget.dataset?.index ?? -1)
    const id = Number(e.currentTarget.dataset?.id || 0)
    if (idx < 0 || !id) return
    try {
      await deleteNoticeAgentRecordApi(id)
      showSuccess('已删除解析记录')
      this.loadAgentRecords()
    } catch (err) {
      showError(err.message || '删除失败')
      this.setData({ [`agentRecords[${idx}].swipeX`]: 0 })
    }
  },

  onCloseAgentRecordDialog() {
    this.setData({
      showAgentRecordDialog: false,
      activeAgentRecord: null
    })
  },

  noop() {},

  onAgentTitleInput(e) {
    const index = Number(e.currentTarget.dataset?.index || -1)
    if (index < 0) return
    const value = e.detail?.value || ''
    this.setData({ [`agentResult.items[${index}].title`]: value })
  },

  onAgentTypeChange(e) {
    const index = Number(e.currentTarget.dataset?.index || -1)
    if (index < 0) return
    this.setData({ [`agentResult.items[${index}].typeIndex`]: Number(e.detail?.value || 0) })
  },

  onAgentDateChange(e) {
    const index = Number(e.currentTarget.dataset?.index || -1)
    if (index < 0) return
    this.setData({ [`agentResult.items[${index}].dateText`]: e.detail?.value || '' })
  },

  onAgentTimeChange(e) {
    const index = Number(e.currentTarget.dataset?.index || -1)
    if (index < 0) return
    this.setData({ [`agentResult.items[${index}].timeText`]: e.detail?.value || '' })
  },

  toDateText(ts) {
    if (!Number.isFinite(ts) || ts <= 0) return todayDate()
    const d = new Date(ts)
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
  },

  toTimeText(ts) {
    if (!Number.isFinite(ts) || ts <= 0) return nowTime()
    const d = new Date(ts)
    return `${pad(d.getHours())}:${pad(d.getMinutes())}`
  },

  toTimestamp(dateText, timeText) {
    if (!dateText || !timeText) return 0
    return new Date(`${dateText.replace(/-/g, '/')} ${timeText}`).getTime()
  },

  typeToIndex(type) {
    const t = (type || 'activity').toLowerCase()
    const idx = this.data.typeOptions.findIndex((x) => x.value === t)
    return idx >= 0 ? idx : 0
  }
})
