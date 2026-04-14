import { loginApi } from '../../api/auth'
import { getWxLoginCode } from '../../utils/wechat'
import { setStorage } from '../../utils/storage'
import { showError, showSuccess } from '../../utils/ui'

const TOKEN_KEY = 'TOKEN'
const USER_INFO_KEY = 'USER_INFO'
const LOGIN_STATUS_KEY = 'LOGIN_STATUS'

Page({
  data: {
    username: '',
    password: '',
    agreeProtocol: false,
    isPageReady: false
  },

  onReady() {
    setTimeout(() => {
      this.setData({ isPageReady: true })
    }, 30)
  },

  onFieldChange(event) {
    const { key } = event.currentTarget.dataset
    this.setData({
      [key]: event.detail
    })
  },

  onToggleAgree(event) {
    this.setData({
      agreeProtocol: event.detail
    })
  },

  goRegister() {
    wx.navigateTo({
      url: '/pages/register/register'
    })
  },

  saveLoginState(token, userInfo = {}) {
    setStorage(TOKEN_KEY, token)
    setStorage(USER_INFO_KEY, userInfo)
    setStorage(LOGIN_STATUS_KEY, true)
  },

  validateBeforeLogin() {
    const { agreeProtocol } = this.data
    if (!agreeProtocol) {
      showError('请先勾选用户协议')
      return false
    }
    return true
  },

  async onPasswordLogin() {
    const { username, password } = this.data
    if (!this.validateBeforeLogin()) {
      return
    }
    if (!username || !password) {
      showError('请输入用户名和密码')
      return
    }

    try {
      const response = await loginApi({
        loginType: 'PASSWORD',
        username,
        password
      })
      const token = response?.data?.token || ''
      if (!token) {
        showError('登录失败，未获取到Token')
        return
      }
      this.saveLoginState(token, { username })
      showSuccess('登录成功')
      wx.switchTab({
        url: '/pages/mine/mine'
      })
    } catch (error) {
      showError(error.message || '登录失败')
    }
  },

  async onQuickLogin() {
    if (!this.validateBeforeLogin()) {
      return
    }
    try {
      const wxCode = await getWxLoginCode()
      const response = await loginApi({
        loginType: 'WX_ONE_CLICK',
        wxCode
      })
      const token = response?.data?.token || ''
      if (!token) {
        showError('一键登录失败，未获取到Token')
        return
      }
      this.saveLoginState(token, {})
      showSuccess('一键登录成功')
      wx.switchTab({
        url: '/pages/mine/mine'
      })
    } catch (error) {
      showError(error.message || '一键登录失败')
    }
  }
})
