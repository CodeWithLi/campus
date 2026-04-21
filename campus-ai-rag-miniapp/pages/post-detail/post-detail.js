import {
  createPostCommentApi,
  getPostDetailApi,
  listPostCommentsApi,
  reportPostApi,
  togglePostFavoriteApi,
  togglePostLikeApi
} from '../../api/post'
import { getStorage } from '../../utils/storage'
import { showError, showSuccess } from '../../utils/ui'
import { buildApiUrl } from '../../config/env'

const TOKEN_KEY = 'TOKEN'
const COMMENT_MAX_LINES = 6
const COMMENT_LINE_HEIGHT_RPX = 42
const COMMENT_VERTICAL_PADDING_RPX = 32
const COMMENT_MIN_HEIGHT_RPX = 160

Page({
  data: {
    id: '',
    detail: null,
    loading: false,
    liking: false,
    favoriting: false,
    commentLoading: false,
    commentSubmitting: false,
    reporting: false,
    commentText: '',
    commentInputHeight: COMMENT_MIN_HEIGHT_RPX,
    replyTarget: null,
    commentList: [],
    commentFlatList: []
  },

  onLoad(options = {}) {
    const id = options?.id || ''
    this.setData({ id })
    this.loadAll()
  },

  async loadAll() {
    await this.loadDetail()
    await this.loadComments()
  },

  async loadDetail() {
    if (!this.data.id) return
    this.setData({ loading: true })
    try {
      const res = await getPostDetailApi(this.data.id)
      const detail = res?.data || null
      if (detail) {
        detail.fullImageUrls = (detail.imageUrls || []).map((u) => this.fullUrl(u))
      }
      this.setData({ detail })
    } catch (e) {
      showError(e.message || '加载详情失败')
    } finally {
      this.setData({ loading: false })
    }
  },

  onPreviewImage(e) {
    const current = e.currentTarget.dataset?.url || ''
    if (!current) return
    wx.previewImage({
      current,
      urls: this.data.detail?.fullImageUrls || []
    })
  },

  async loadComments() {
    if (!this.data.id) return
    this.setData({ commentLoading: true })
    try {
      const res = await listPostCommentsApi(this.data.id)
      const commentList = Array.isArray(res?.data) ? res.data : []
      this.setData({
        commentList,
        commentFlatList: this.flattenComments(commentList)
      })
    } catch (e) {
      showError(e.message || '加载评论失败')
    } finally {
      this.setData({ commentLoading: false })
    }
  },

  onCommentInput(e) {
    this.setData({ commentText: e.detail?.value || '' })
  },

  onCommentLineChange(e) {
    const lineCount = Number(e?.detail?.lineCount || 1)
    const safeLineCount = Math.max(1, Math.min(lineCount, COMMENT_MAX_LINES))
    const nextHeight = Math.max(
      COMMENT_MIN_HEIGHT_RPX,
      safeLineCount * COMMENT_LINE_HEIGHT_RPX + COMMENT_VERTICAL_PADDING_RPX
    )
    if (nextHeight !== this.data.commentInputHeight) {
      this.setData({ commentInputHeight: nextHeight })
    }
  },

  onCancelReply() {
    this.setData({ replyTarget: null })
  },

  onReplyComment(e) {
    const commentId = Number(e.currentTarget.dataset?.id || 0)
    if (!commentId) return
    const parentCommentId = Number(e.currentTarget.dataset?.parentId || commentId)
    const nickname = e.currentTarget.dataset?.nickname || ''
    this.setData({
      replyTarget: {
        commentId,
        parentCommentId,
        nickname
      }
    })
  },

  async onSubmitComment() {
    if (!this.ensureLogin()) return
    const content = (this.data.commentText || '').trim()
    if (!content) {
      showError('评论内容不能为空')
      return
    }
    if (content.length > 1000) {
      showError('评论最多1000字')
      return
    }
    const reply = this.data.replyTarget
    const payload = {
      content
    }
    if (reply?.parentCommentId) {
      payload.parentCommentId = reply.parentCommentId
    }
    if (reply?.commentId) {
      payload.replyToCommentId = reply.commentId
    }
    this.setData({ commentSubmitting: true })
    try {
      await createPostCommentApi(this.data.id, payload)
      showSuccess('评论成功')
      this.setData({
        commentText: '',
        replyTarget: null,
        commentInputHeight: COMMENT_MIN_HEIGHT_RPX
      })
      await this.loadComments()
      await this.loadDetail()
    } catch (e) {
      showError(e.message || '发表评论失败')
    } finally {
      this.setData({ commentSubmitting: false })
    }
  },

  async onToggleLike() {
    if (!this.ensureLogin()) return
    if (this.data.liking) return
    this.setData({ liking: true })
    try {
      const res = await togglePostLikeApi(this.data.id)
      const like = res?.data || {}
      const detail = { ...(this.data.detail || {}) }
      detail.likeCount = Number(like.likeCount || 0)
      detail.likedByMe = Boolean(like.liked)
      this.setData({ detail })
    } catch (e) {
      showError(e.message || '点赞失败')
    } finally {
      this.setData({ liking: false })
    }
  },

  async onToggleFavorite() {
    if (!this.ensureLogin()) return
    if (this.data.favoriting) return
    this.setData({ favoriting: true })
    try {
      const res = await togglePostFavoriteApi(this.data.id)
      const favorite = res?.data || {}
      const detail = { ...(this.data.detail || {}) }
      detail.favoritedByMe = Boolean(favorite.favorited)
      this.setData({ detail })
      showSuccess(detail.favoritedByMe ? '已收藏' : '已取消收藏')
    } catch (e) {
      showError(e.message || '收藏失败')
    } finally {
      this.setData({ favoriting: false })
    }
  },

  async onReportPost() {
    if (!this.ensureLogin()) return
    if (this.data.reporting) return
    const reasons = ['涉嫌违法违规', '色情低俗', '辱骂攻击', '广告营销', '其他原因（手动输入）']
    const action = await new Promise((resolve) => {
      wx.showActionSheet({
        itemList: reasons,
        success: (res) => resolve(res?.tapIndex),
        fail: () => resolve(-1)
      })
    })
    if (action < 0) return
    let reason = reasons[action]
    if (action === reasons.length - 1) {
      const inputRes = await new Promise((resolve) => {
        wx.showModal({
          title: '举报理由',
          editable: true,
          placeholderText: '请输入举报理由（最多200字）',
          success: (res) => resolve(res),
          fail: () => resolve(null)
        })
      })
      if (!inputRes?.confirm) return
      reason = String(inputRes.content || '').trim()
    }
    if (!reason) {
      showError('举报理由不能为空')
      return
    }
    this.setData({ reporting: true })
    try {
      await reportPostApi(this.data.id, { reason })
      const detail = { ...(this.data.detail || {}), reportedByMe: true }
      this.setData({ detail })
      showSuccess('举报成功，平台将尽快处理')
    } catch (e) {
      showError(e.message || '举报失败')
    } finally {
      this.setData({ reporting: false })
    }
  },

  ensureLogin() {
    const token = getStorage(TOKEN_KEY)
    if (token) return true
    showError('请先登录')
    setTimeout(() => {
      wx.navigateTo({ url: '/pages/login/login' })
    }, 300)
    return false
  },

  flattenComments(tree = [], level = 0, parentId = null, out = []) {
    tree.forEach((item) => {
      const id = Number(item?.id || 0)
      const nickname = item?.nickname || ''
      const avatarText = (nickname || '用户').charAt(0)
      const data = {
        ...item,
        level,
        parentId: item?.parentCommentId || parentId,
        indentStyle: `margin-left:${Math.min(level * 24, 120)}rpx;`,
        avatarText
      }
      out.push(data)
      if (Array.isArray(item?.children) && item.children.length) {
        this.flattenComments(item.children, level + 1, id, out)
      }
    })
    return out
  },

  fullUrl(path) {
    if (!path) return ''
    if (/^https?:\/\//.test(path)) return path
    return buildApiUrl(path)
  }
})
