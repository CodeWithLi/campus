import { postAiGenerate, postPublish, uploadPostImages } from '../../api/post'
import { getStorage } from '../../utils/storage'
import { showError, showSuccess } from '../../utils/ui'
import { buildApiUrl } from '../../config/env'

const TOKEN_KEY = 'TOKEN'

Page({
  data: {
    hint: '',
    title: '',
    content: '',
    contentLength: 0,
    draftId: '',
    aiTitle: '',
    aiContent: '',
    imageUrls: [],
    imagePreviewUrls: [],
    generating: false,
    publishing: false,
    uploading: false
  },

  onHintInput(e) {
    this.setData({ hint: e.detail?.value || '' })
  },

  onTitleInput(e) {
    const value = e.detail?.value || ''
    const updates = { title: value }
    if (this.data.draftId && value !== this.data.aiTitle) {
      updates.draftId = ''
    }
    this.setData(updates)
  },

  onContentInput(e) {
    const value = e.detail?.value || ''
    const updates = {
      content: value,
      contentLength: value.length
    }
    if (this.data.draftId && value !== this.data.aiContent) {
      updates.draftId = ''
    }
    this.setData(updates)
  },

  async onGenerateByAi() {
    const hint = (this.data.hint || '').trim()
    if (!hint) {
      showError('请先输入发帖想法')
      return
    }
    this.setData({ generating: true })
    try {
      const res = await postAiGenerate({ hint })
      const data = res?.data || {}
      this.setData({
        title: data.title || '',
        content: data.content || '',
        contentLength: (data.content || '').length,
        draftId: data.draftId || '',
        aiTitle: data.title || '',
        aiContent: data.content || ''
      })
      showSuccess('AI 生成成功')
    } catch (e) {
      showError(e.message || 'AI 生成失败')
    } finally {
      this.setData({ generating: false })
    }
  },

  async onChooseImages() {
    if (this.data.uploading) return
    const remain = 5 - this.data.imageUrls.length
    if (remain <= 0) {
      showError('最多上传5张图片')
      return
    }
    wx.chooseMedia({
      count: remain,
      mediaType: ['image'],
      sourceType: ['album', 'camera'],
      success: async (res) => {
        const tempFiles = res?.tempFiles || []
        const paths = tempFiles.map((f) => f.tempFilePath).filter(Boolean)
        if (!paths.length) return
        this.setData({ uploading: true })
        try {
          const uploadRes = await uploadPostImages(paths)
          const urls = uploadRes?.data?.imageUrls || []
          const imageUrls = [...this.data.imageUrls, ...urls].slice(0, 5)
          this.setData({
            imageUrls,
            imagePreviewUrls: this.toPreviewUrls(imageUrls)
          })
          showSuccess('图片上传成功')
        } catch (e) {
          showError(e.message || '图片上传失败')
        } finally {
          this.setData({ uploading: false })
        }
      }
    })
  },

  onRemoveImage(e) {
    const index = Number(e.currentTarget.dataset?.index)
    if (Number.isNaN(index)) return
    const list = [...this.data.imageUrls]
    list.splice(index, 1)
    this.setData({
      imageUrls: list,
      imagePreviewUrls: this.toPreviewUrls(list)
    })
  },

  onPreviewImage(e) {
    const url = e.currentTarget.dataset?.url || ''
    if (!url) return
    wx.previewImage({
      current: this.fullUrl(url),
      urls: this.data.imageUrls.map((item) => this.fullUrl(item))
    })
  },

  async onPublish() {
    if (!this.ensureLogin()) return
    const title = (this.data.title || '').trim()
    const content = (this.data.content || '').trim()
    if (!title) {
      showError('标题不能为空')
      return
    }
    if (!content) {
      showError('正文不能为空')
      return
    }
    if (content.length > 1000) {
      showError('正文最多1000字')
      return
    }
    this.setData({ publishing: true })
    try {
      const res = await postPublish({
        title,
        content,
        draftId: this.data.draftId || null,
        imageUrls: this.data.imageUrls
      })
      showSuccess('发布成功')
      const postId = res?.data?.postId
      setTimeout(() => {
        if (postId) {
          wx.redirectTo({ url: `/pages/post-detail/post-detail?id=${postId}` })
        } else {
          wx.navigateBack({ delta: 1 })
        }
      }, 500)
    } catch (e) {
      showError(e.message || '发布失败')
    } finally {
      this.setData({ publishing: false })
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

  fullUrl(path) {
    if (!path) return ''
    if (/^https?:\/\//.test(path)) return path
    return buildApiUrl(path)
  },

  toPreviewUrls(imageUrls = []) {
    return imageUrls.map((item) => this.fullUrl(item))
  },

  onGoPostList() {
    this.navigateSafe('/pages/post-list/post-list')
  },

  onGoMyPosts() {
    this.navigateSafe('/pages/my-posts/my-posts')
  },

  navigateSafe(targetPath) {
    const pages = getCurrentPages()
    const targetRoute = targetPath.replace(/^\//, '')
    const currentRoute = pages[pages.length - 1]?.route
    if (currentRoute === targetRoute) return

    const existingIndex = pages.findIndex((page) => page.route === targetRoute)
    if (existingIndex >= 0) {
      const delta = pages.length - 1 - existingIndex
      if (delta > 0) {
        wx.navigateBack({ delta })
      }
      return
    }

    if (pages.length >= 9) {
      wx.redirectTo({ url: targetPath })
      return
    }

    wx.navigateTo({ url: targetPath })
  }
})
