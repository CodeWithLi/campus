import { listPostsApi } from '../../api/post'
import { listReminderNoticeApi } from '../../api/reminder'
import { showError } from '../../utils/ui'

Page({
  data: {
    banners: [
      {
        key: 'b1',
        badge: 'AI',
        title: '一键开始智能问答',
        desc: '多模型 + 上下文连续记忆，支持语音实时转写',
        btnText: '去对话',
        gradient: 'linear-gradient(135deg, rgba(59,130,246,0.18), rgba(99,102,241,0.16))',
        action: 'go-chat'
      },
      {
        key: 'b2',
        badge: 'RAG',
        title: '上传资料后精准问答',
        desc: '从文档中检索并回答，适合复习与通知解读',
        btnText: '去 RAG',
        gradient: 'linear-gradient(135deg, rgba(20,184,166,0.16), rgba(34,197,94,0.12))',
        action: 'go-rag'
      },
      {
        key: 'b3',
        badge: '社区',
        title: '看经验、提问题、找答案',
        desc: '把校园生活的坑与经验分享出来',
        btnText: '去社区',
        gradient: 'linear-gradient(135deg, rgba(236,72,153,0.14), rgba(168,85,247,0.12))',
        action: 'go-community'
      }
    ],
    quickEntries: [
      { key: 'qa', icon: '🤖', title: '智能问答', tag: '热', desc: '多模型 + RAG', iconBg: 'linear-gradient(135deg, rgba(59,130,246,0.18), rgba(99,102,241,0.16))' },
      { key: 'notice', icon: '📣', title: '通知解读', tag: '新', desc: '提炼重点', iconBg: 'linear-gradient(135deg, rgba(245,158,11,0.16), rgba(59,130,246,0.10))' },
      { key: 'guide', icon: '🧭', title: '办事指南', tag: '中', desc: '流程清单', iconBg: 'linear-gradient(135deg, rgba(16,185,129,0.14), rgba(59,130,246,0.10))' },
      { key: 'community', icon: '💬', title: '社区', tag: '聊', desc: '发帖互动', iconBg: 'linear-gradient(135deg, rgba(236,72,153,0.14), rgba(168,85,247,0.10))' },
      { key: 'mine', icon: '👤', title: '我的', tag: '', desc: '余额/记录', iconBg: 'linear-gradient(135deg, rgba(100,116,139,0.14), rgba(59,130,246,0.08))' }
    ],
    inspirationList: [
      '帮我把这条校园通知提炼成待办清单',
      '奖学金申请一般需要哪些材料？',
      '帮我做一个本周学习计划（按课程安排）',
      '给我一份实验报告大纲模板'
    ],
    unreadNoticeCount: 0,
    postLoading: false,
    posts: []
  },

  onShow() {
    this.loadUnreadNoticeCount()
    this.loadRecommendPosts()
  },

  async onPullDownRefresh() {
    await Promise.all([this.loadUnreadNoticeCount(), this.loadRecommendPosts()])
    wx.stopPullDownRefresh()
  },

  onBannerTap(e) {
    const action = e.currentTarget.dataset?.action
    this.navigateByAction(action)
  },

  onTapEntry(e) {
    const key = e.currentTarget.dataset?.key
    if (key === 'qa') return wx.switchTab({ url: '/pages/ai/ai' })
    if (key === 'mine') return wx.switchTab({ url: '/pages/mine/mine' })
    if (key === 'community') return this.goCommunity()
    if (key === 'notice') return this.goNotificationCenter()
    if (key === 'guide') return wx.showToast({ title: '办事指南开发中', icon: 'none' })
    wx.showToast({ title: '功能开发中', icon: 'none' })
  },

  navigateByAction(action) {
    if (action === 'go-chat') return this.goAiChat()
    if (action === 'go-rag') return wx.navigateTo({ url: '/pages/ai-rag/ai-rag' })
    if (action === 'go-community') return this.goCommunity()
  },

  goAi() {
    wx.switchTab({ url: '/pages/ai/ai' })
  },

  goAiChat() {
    wx.navigateTo({ url: '/pages/ai-chat/ai-chat' })
  },

  goCommunity() {
    wx.navigateTo({ url: '/pages/post-list/post-list' })
  },

  goNotificationCenter() {
    wx.navigateTo({ url: '/pages/notification-center/notification-center' })
  },

  onOpenPost(e) {
    const id = e.currentTarget.dataset?.id
    if (!id) return
    wx.navigateTo({ url: `/pages/post-detail/post-detail?id=${id}` })
  },

  onPickInspiration(e) {
    const text = e.currentTarget.dataset?.text || ''
    if (!text) return
    wx.setClipboardData({
      data: text,
      success: () => {
        wx.showToast({ title: '已复制，去 AI 直接粘贴提问', icon: 'none' })
        setTimeout(() => {
          this.goAiChat()
        }, 250)
      }
    })
  },

  async loadUnreadNoticeCount() {
    try {
      const res = await listReminderNoticeApi()
      const count = Number(res?.data?.unreadCount || 0)
      this.setData({ unreadNoticeCount: Number.isFinite(count) ? count : 0 })
    } catch (e) {
      this.setData({ unreadNoticeCount: 0 })
    }
  },

  async loadRecommendPosts() {
    this.setData({ postLoading: true })
    try {
      const res = await listPostsApi({ pageNo: 1, pageSize: 3 })
      const data = res?.data || {}
      const rows = Array.isArray(data.list) ? data.list : []
      this.setData({ posts: rows })
    } catch (e) {
      this.setData({ posts: [] })
      showError(e.message || '推荐内容加载失败')
    } finally {
      this.setData({ postLoading: false })
    }
  }
})
