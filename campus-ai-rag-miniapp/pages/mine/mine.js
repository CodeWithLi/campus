import { getStorage, removeStorage } from '../../utils/storage'
import { showSuccess } from '../../utils/ui'

const TOKEN_KEY = 'TOKEN'
const USER_INFO_KEY = 'USER_INFO'
const LOGIN_STATUS_KEY = 'LOGIN_STATUS'

Page({
  data: {
    token: '',
    username: '',
    email: '',
    isLogin: false
  },

  onShow() {
    const token = getStorage(TOKEN_KEY) || ''
    const userInfo = getStorage(USER_INFO_KEY) || {}
    const isLogin = Boolean(getStorage(LOGIN_STATUS_KEY))
    this.setData({
      token,
      isLogin,
      username: userInfo.username || '',
      email: userInfo.email || ''
    })
  },

  goLogin() {
    wx.navigateTo({
      url: '/pages/login/login'
    })
  },

  onLogout() {
    removeStorage(TOKEN_KEY)
    removeStorage(USER_INFO_KEY)
    removeStorage(LOGIN_STATUS_KEY)
    this.setData({
      token: '',
      username: '',
      email: '',
      isLogin: false
    })
    showSuccess('已退出登录')
  }
})
