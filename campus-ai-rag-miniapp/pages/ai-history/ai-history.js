import { listConversationsApi } from '../../api/chat'
import { getStorage } from '../../utils/storage'
import { showError } from '../../utils/ui'

const CHAT_HISTORY_KEY = 'CHAT_HISTORY_LIST' // fallback

const formatTime = (ts) => {
  if (!ts) return '-'
  const ms = Number(ts) < 1e12 ? Number(ts) * 1000 : Number(ts)
  const date = new Date(ms)
  const m = `${date.getMonth() + 1}`.padStart(2, '0')
  const d = `${date.getDate()}`.padStart(2, '0')
  const h = `${date.getHours()}`.padStart(2, '0')
  const min = `${date.getMinutes()}`.padStart(2, '0')
  return `${m}-${d} ${h}:${min}`
}

Page({
  data: {
    historyList: []
  },

  async onShow() {
    try {
      const response = await listConversationsApi()
      const list = response?.data || []
      const historyList = list.map((item) => ({
        ...item,
        updatedAtText: formatTime(item.updatedAt)
      }))
      this.setData({ historyList })
    } catch (error) {
      // fallback：后端异常时临时展示本地缓存
      const localList = getStorage(CHAT_HISTORY_KEY) || []
      const historyList = localList.map((item) => ({
        ...item,
        updatedAtText: formatTime(item.updatedAt)
      }))
      this.setData({ historyList })
      showError(error.message || '会话列表加载失败')
    }
  },

  onOpenConversation(event) {
    const { conversationId } = event.currentTarget.dataset
    wx.navigateTo({
      url: `/pages/ai-chat/ai-chat?conversationId=${conversationId}`
    })
  },

  onNewConversation() {
    wx.navigateTo({
      url: '/pages/ai-chat/ai-chat'
    })
  }
})
