import { getStorage, removeStorage, setStorage } from '../../utils/storage'
import { showSuccess } from '../../utils/ui'
import { listReminderNoticeApi } from '../../api/reminder'
import { getProfileApi, uploadAvatarApi } from '../../api/auth'
import { buildApiUrl } from '../../config/env'

const TOKEN_KEY = 'TOKEN'
const USER_INFO_KEY = 'USER_INFO'
const LOGIN_STATUS_KEY = 'LOGIN_STATUS'

Page({
  data: {
    token: '',
    username: '',
    nickname: '',
    email: '',
    avatarUrl: '',
    avatarPreviewUrl: '',
    isLogin: false,
    unreadNoticeCount: 0,
    avatarUploading: false
  },

  onShow() {
    const token = getStorage(TOKEN_KEY) || ''
    const userInfo = getStorage(USER_INFO_KEY) || {}
    const isLogin = Boolean(getStorage(LOGIN_STATUS_KEY))
    this.setData({
      token,
      isLogin,
      username: userInfo.username || '',
      nickname: userInfo.nickname || '',
      email: userInfo.email || '',
      avatarUrl: userInfo.avatarUrl || '',
      avatarPreviewUrl: this.toFullUrl(userInfo.avatarUrl || '')
    })
    if (isLogin) {
      this.loadProfile()
      this.loadUnreadNoticeCount()
    } else {
      this.setData({ unreadNoticeCount: 0 })
    }
  },

  async loadProfile() {
    try {
      const res = await getProfileApi()
      const data = res?.data || {}
      const userInfo = {
        username: data.username || '',
        email: data.email || '',
        nickname: data.nickname || '',
        avatarUrl: data.avatarUrl || ''
      }
      setStorage(USER_INFO_KEY, userInfo)
      this.setData({
        username: userInfo.username,
        email: userInfo.email,
        nickname: userInfo.nickname,
        avatarUrl: userInfo.avatarUrl,
        avatarPreviewUrl: this.toFullUrl(userInfo.avatarUrl)
      })
    } catch (e) {
      // ignore profile failure
    }
  },

  async loadUnreadNoticeCount() {
    try {
      const res = await listReminderNoticeApi()
      this.setData({ unreadNoticeCount: Number(res?.data?.unreadCount || 0) })
    } catch (e) {
      this.setData({ unreadNoticeCount: 0 })
    }
  },

  goLogin() {
    wx.navigateTo({
      url: '/pages/login/login'
    })
  },

  goMyPosts() {
    wx.navigateTo({
      url: '/pages/my-posts/my-posts'
    })
  },

  goScheduleReminder() {
    wx.navigateTo({
      url: '/pages/schedule-reminder/schedule-reminder'
    })
  },

  goNotificationCenter() {
    wx.navigateTo({
      url: '/pages/notification-center/notification-center'
    })
  },

  goPowerPointLogs() {
    wx.navigateTo({
      url: '/pages/power-points-log/power-points-log'
    })
  },

  goChangePassword() {
    wx.navigateTo({
      url: '/pages/change-password/change-password'
    })
  },

  async onChooseAvatar() {
    if (!this.data.isLogin || this.data.avatarUploading) {
      return
    }
    wx.chooseMedia({
      count: 1,
      mediaType: ['image'],
      sourceType: ['album', 'camera'],
      success: async (res) => {
        const filePath = res?.tempFiles?.[0]?.tempFilePath || ''
        if (!filePath) return
        this.setData({ avatarUploading: true })
        try {
          const resp = await uploadAvatarApi(filePath)
          const avatarUrl = resp?.data || ''
          const userInfo = getStorage(USER_INFO_KEY) || {}
          const nextUserInfo = {
            ...userInfo,
            avatarUrl
          }
          setStorage(USER_INFO_KEY, nextUserInfo)
          this.setData({
            avatarUrl,
            avatarPreviewUrl: this.toFullUrl(avatarUrl)
          })
          showSuccess('头像上传成功')
        } catch (e) {
          wx.showToast({ title: e.message || '头像上传失败', icon: 'none' })
        } finally {
          this.setData({ avatarUploading: false })
        }
      }
    })
  },

  onLogout() {
    removeStorage(TOKEN_KEY)
    removeStorage(USER_INFO_KEY)
    removeStorage(LOGIN_STATUS_KEY)
    this.setData({
      token: '',
      username: '',
      nickname: '',
      email: '',
      avatarUrl: '',
      avatarPreviewUrl: '',
      isLogin: false
    })
    showSuccess('已退出登录')
  },

  toFullUrl(path) {
    if (!path) return ''
    if (/^https?:\/\//.test(path)) return path
    return buildApiUrl(path)
  }
})
