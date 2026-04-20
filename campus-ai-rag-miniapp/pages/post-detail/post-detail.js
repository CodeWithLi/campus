import {
  createPostCommentApi,
  getPostDetailApi,
  listPostCommentsApi,
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
    commentLoading: false,
    commentSubmitting: false,
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
