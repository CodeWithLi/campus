import { sendEmailCodeApi, registerApi } from '../../api/auth'
import { getWxLoginCode } from '../../utils/wechat'
import { showError, showSuccess } from '../../utils/ui'
import { setStorage } from '../../utils/storage'

const TOKEN_KEY = 'TOKEN'
const USER_INFO_KEY = 'USER_INFO'
const LOGIN_STATUS_KEY = 'LOGIN_STATUS'

Page({
  data: {
    username: '',
    password: '',
    email: '',
    emailCode: '',
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

  async onSendEmailCode() {
    const { email } = this.data
    if (!email) {
      showError('请输入邮箱')
      return
    }
    try {
      await sendEmailCodeApi(email)
      showSuccess('验证码已发送')
    } catch (error) {
      showError(error.message || '发送失败')
    }
  },

  async onSubmitRegister() {
    const { username, password, email, emailCode } = this.data
    if (!username || !password || !email || !emailCode) {
      showError('请完整填写注册信息')
      return
    }
    try {
      const wxCode = await getWxLoginCode()
      const response = await registerApi({
        username,
        password,
        email,
        emailCode,
        wxCode
      })
      const token = response?.data?.token || ''
      if (token) {
        setStorage(TOKEN_KEY, token)
        setStorage(USER_INFO_KEY, { username, email })
        setStorage(LOGIN_STATUS_KEY, true)
      }
      showSuccess('注册成功')
      setTimeout(() => {
        wx.switchTab({
          url: '/pages/mine/mine'
        })
      }, 500)
    } catch (error) {
      showError(error.message || '注册失败')
    }
  }
})
