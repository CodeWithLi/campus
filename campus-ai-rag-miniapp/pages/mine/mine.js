import { getStorage, removeStorage, setStorage } from '../../utils/storage'
import { showError, showSuccess } from '../../utils/ui'
import { listReminderNoticeApi } from '../../api/reminder'
import { checkSessionApi, getProfileApi, updateNicknameApi, uploadAvatarApi } from '../../api/auth'
import { buildApiUrl } from '../../config/env'
import { isLoggedIn } from '../../utils/auth-guard'

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
    powerPoints: '0.00',
    isLogin: false,
    unreadNoticeCount: 0,
    avatarUploading: false
  },

  async onShow() {
    // 先用本地状态快速渲染，再用后端接口校验会话有效性，避免 token 过期但 UI 仍显示已登录。
    this.syncLoginState()
    if (this.data.isLogin) {
      await this.checkSessionAndSync()
    }
    if (this.data.isLogin) {
      this.loadProfile()
      this.loadUnreadNoticeCount()
    } else {
      this.setData({ unreadNoticeCount: 0 })
    }
  },

  async checkSessionAndSync() {
    try {
      await checkSessionApi()
      // 会话有效，无需处理
    } catch (e) {
      // token 过期时：request.js 会静默清缓存（X-Silent-Auth=1），这里刷新 UI
      this.syncLoginState()
    }
  },

  syncLoginState() {
    const token = getStorage(TOKEN_KEY) || ''
    const userInfo = getStorage(USER_INFO_KEY) || {}
    const ok = isLoggedIn()
    this.setData({
      token: ok ? token : '',
      isLogin: ok,
      username: ok ? (userInfo.username || '') : '',
      nickname: ok ? (userInfo.nickname || '') : '',
      email: ok ? (userInfo.email || '') : '',
      avatarUrl: ok ? (userInfo.avatarUrl || '') : '',
      avatarPreviewUrl: ok ? this.toFullUrl(userInfo.avatarUrl || '') : '',
      powerPoints: ok ? this.formatPoints(userInfo.powerPoints) : '0.00'
    })
  },

  onAuthInvalid() {
    // request.js 在 401 时会清理缓存并通知页面，这里立刻刷新 UI
    this.syncLoginState()
    this.setData({ unreadNoticeCount: 0 })
  },

  async loadProfile() {
    try {
      const res = await getProfileApi()
      const data = res?.data || {}
      const userInfo = {
        username: data.username || '',
        email: data.email || '',
        nickname: data.nickname || '',
        avatarUrl: data.avatarUrl || '',
        powerPoints: data.powerPoints
      }
      setStorage(USER_INFO_KEY, userInfo)
      this.setData({
        username: userInfo.username,
        email: userInfo.email,
        nickname: userInfo.nickname,
        avatarUrl: userInfo.avatarUrl,
        avatarPreviewUrl: this.toFullUrl(userInfo.avatarUrl),
        powerPoints: this.formatPoints(userInfo.powerPoints)
      })
    } catch (e) {
      // 当 token 过期时，request.js 会清理 storage 并弹登录提示；这里同步 UI 到未登录态
      this.syncLoginState()
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

  goMyFavorites() {
    wx.navigateTo({
      url: '/pages/my-favorites/my-favorites'
    })
  },

  goMyInteractions() {
    wx.navigateTo({
      url: '/pages/my-interactions/my-interactions'
    })
  },

  goResumeRecords() {
    wx.navigateTo({
      url: '/pages/resume-records/resume-records'
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

  goRechargeOrders() {
    wx.navigateTo({
      url: '/pages/recharge-orders/recharge-orders'
    })
  },

  goPowerPointsCenter() {
    wx.navigateTo({
      url: '/pages/power-points-center/power-points-center'
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

  onEditNickname() {
    if (!this.data.isLogin) return
    wx.showModal({
      title: '修改昵称',
      editable: true,
      placeholderText: '请输入2-20个字符',
      content: this.data.nickname || '',
      success: async (res) => {
        if (!res.confirm) return
        const nickname = (res.content || '').trim()
        if (!nickname) {
          showError('昵称不能为空')
          return
        }
        if (nickname.length < 2 || nickname.length > 20) {
          showError('昵称长度需在2-20个字符之间')
          return
        }
        try {
          await updateNicknameApi(nickname)
          const userInfo = getStorage(USER_INFO_KEY) || {}
          const nextUserInfo = {
            ...userInfo,
            nickname
          }
          setStorage(USER_INFO_KEY, nextUserInfo)
          this.setData({ nickname })
          showSuccess('昵称修改成功')
        } catch (e) {
          showError(e.message || '昵称修改失败')
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
  },

  formatPoints(value) {
    const num = Number(value || 0)
    if (!Number.isFinite(num)) return '0.00'
    return num.toFixed(2)
  }
})
