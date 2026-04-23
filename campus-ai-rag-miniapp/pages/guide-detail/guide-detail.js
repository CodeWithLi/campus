import { getGuideDetailApi, toggleGuideFavoriteApi, updateGuideStepProgressApi } from '../../api/guide'
import { createReminderApi } from '../../api/reminder'
import { showError, showSuccess } from '../../utils/ui'
import { API_BASE_URL, buildApiUrl } from '../../config/env'

Page({
  data: {
    id: 0,
    loading: false,
    guide: null,
    stepList: []
  },

  onLoad(options = {}) {
    const id = Number(options.id || 0)
    if (!id) {
      showError('指南ID无效')
      return
    }
    this.setData({ id })
    this.loadDetail()
  },

  onPullDownRefresh() {
    this.loadDetail(true)
  },

  async loadDetail(fromPullDown = false) {
    if (!this.data.id) return
    this.setData({ loading: true })
    try {
      const res = await getGuideDetailApi(this.data.id)
      const guideRaw = res?.data || null
      const guide = guideRaw
        ? {
            ...guideRaw,
            coverUrlFull: this.resolveCoverUrl(guideRaw.coverUrl)
          }
        : null
      const steps = Array.isArray(guide?.steps) ? guide.steps : []
      const doneStepIds = Array.isArray(guide?.doneStepIds) ? guide.doneStepIds : []
      const doneStepIdSet = new Set(doneStepIds.map((x) => Number(x)))
      this.setData({
        guide,
        stepList: steps.map((it) => ({
          ...it,
          actionPayloadObj: this.parseJsonSafe(it?.actionPayload),
          requiredMaterialsList: this.parseArraySafe(it?.requiredMaterials),
          done: doneStepIdSet.has(Number(it?.id))
        }))
      })
    } catch (e) {
      showError(e.message || '加载详情失败')
    } finally {
      this.setData({ loading: false })
      if (fromPullDown) wx.stopPullDownRefresh()
    }
  },

  async onToggleFavorite() {
    if (!this.data.id || !this.data.guide) return
    try {
      const res = await toggleGuideFavoriteApi(this.data.id)
      const nextFavorite = !!res?.data
      this.setData({
        guide: {
          ...this.data.guide,
          favorite: nextFavorite
        }
      })
      showSuccess(nextFavorite ? '已收藏' : '已取消收藏')
    } catch (e) {
      showError(e.message || '操作失败')
    }
  },

  async onToggleStepDone(e) {
    const index = Number(e.currentTarget.dataset?.index ?? -1)
    const step = this.data.stepList[index]
    if (!step) return
    const nextDone = !step.done
    try {
      await updateGuideStepProgressApi(this.data.id, step.id, nextDone)
      const nextList = this.data.stepList.map((s, i) => (i === index ? { ...s, done: nextDone } : s))
      const doneCount = nextList.filter((s) => s.done).length
      this.setData({
        stepList: nextList,
        guide: {
          ...this.data.guide,
          doneCount,
          totalSteps: nextList.length,
          doneStepIds: nextList.filter((s) => s.done).map((s) => s.id)
        }
      })
    } catch (e) {
      showError(e.message || '更新失败')
    }
  },

  onGoRag() {
    const idsJson = this.data?.guide?.ragCategoryIdsJson
    let ids = []
    try {
      const parsed = JSON.parse(idsJson || '[]')
      ids = Array.isArray(parsed) ? parsed.map((x) => Number(x)).filter((x) => Number.isFinite(x) && x > 0) : []
    } catch (e) {
      ids = []
    }
    const categoryId = ids[0] || 0
    const url = categoryId ? `/pages/ai-rag/ai-rag?categoryId=${categoryId}` : '/pages/ai-rag/ai-rag'
    wx.navigateTo({ url })
  },

  async onStepActionTap(e) {
    const index = Number(e.currentTarget.dataset?.index ?? -1)
    const step = this.data.stepList[index]
    if (!step) return
    const actionType = (step.actionType || 'NONE').toUpperCase()
    const payload = step.actionPayloadObj || {}

    if (actionType === 'LINK') {
      const url = payload.url || payload.link || ''
      if (!url) {
        showError('未配置链接')
        return
      }
      wx.navigateTo({
        url: `/pages/guide-webview/guide-webview?title=${encodeURIComponent(step.title || '办事链接')}&url=${encodeURIComponent(url)}`
      })
      return
    }

    if (actionType === 'REMINDER') {
      const offsetHours = Number(payload.offsetHours || 24)
      const remindAt = Date.now() + Math.max(offsetHours, 1) * 60 * 60 * 1000
      try {
        await createReminderApi({
          title: payload.title || step.title || '办事提醒',
          type: payload.type || 'activity',
          remindAt
        })
        showSuccess('提醒创建成功')
      } catch (e) {
        showError(e.message || '创建提醒失败')
      }
      return
    }

    if (actionType === 'ASK_AI') {
      const prompt = payload.prompt || `请解释这个步骤：${step.title || ''}\n${step.content || ''}`
      wx.navigateTo({
        url: `/pages/ai-chat/ai-chat?initialMessage=${encodeURIComponent(prompt)}`
      })
      return
    }

    wx.showToast({ title: '该步骤暂无动作', icon: 'none' })
  },

  onPreviewCover() {
    const url = this.data?.guide?.coverUrlFull || ''
    if (!url) return
    wx.previewImage({
      current: url,
      urls: [url]
    })
  },

  parseJsonSafe(value) {
    if (!value) return {}
    if (typeof value === 'object') return value
    try {
      const parsed = JSON.parse(value)
      return parsed && typeof parsed === 'object' ? parsed : {}
    } catch (e) {
      return {}
    }
  },

  parseArraySafe(value) {
    if (!value) return []
    if (Array.isArray(value)) return value.filter(Boolean)
    try {
      const parsed = JSON.parse(value)
      return Array.isArray(parsed) ? parsed.filter(Boolean) : []
    } catch (e) {
      return []
    }
  },

  resolveCoverUrl(coverUrl) {
    const path = String(coverUrl || '').trim()
    if (!path) return ''
    if (/^https?:\/\//.test(path)) return path
    if (path.startsWith('/campus/')) {
      const host = String(API_BASE_URL || '').replace(/\/campus\/?$/, '')
      return `${host}${path}`
    }
    return buildApiUrl(path)
  }
})
