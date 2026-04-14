import { sendEmailCodeApi, registerApi } from '../../api/auth'
import { listEnabledModelsApi } from '../../api/model'
import { chatApi } from '../../api/chat'
import { setStorage, getStorage, removeStorage } from '../../utils/storage'
import { getWxLoginCode } from '../../utils/wechat'
import { showSuccess, showError } from '../../utils/ui'

const TOKEN_KEY = 'TOKEN'
const USER_INFO_KEY = 'USER_INFO'
const LOGIN_STATUS_KEY = 'LOGIN_STATUS'

Page({
  data: {
    email: '',
    username: '',
    password: '',
    emailCode: '',
    message: '',
    selectedModelCode: '',
    modelOptions: [],
    answer: '',
    token: ''
  },

  async onLoad() {
    this.restoreLoginStatus()
    await this.loadModels()
  },

  restoreLoginStatus() {
    const token = getStorage(TOKEN_KEY) || ''
    this.setData({ token })
  },

  async loadModels() {
    try {
      const response = await listEnabledModelsApi()
      const options = response?.data || []
      this.setData({
        modelOptions: options,
        selectedModelCode: options.length ? options[0].modelCode : ''
      })
    } catch (error) {
      showError('模型加载失败')
    }
  },

  onFieldChange(event) {
    const { key } = event.currentTarget.dataset
    this.setData({
      [key]: event.detail
    })
  },

  onModelChange(event) {
    this.setData({
      selectedModelCode: event.detail
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

  async onRegister() {
    const { email, username, password, emailCode } = this.data
    if (!email || !username || !password || !emailCode) {
      showError('请完整填写注册信息')
      return
    }

    try {
      const wxCode = await getWxLoginCode()
      const response = await registerApi({
        email,
        username,
        password,
        emailCode,
        wxCode
      })
      const token = response?.data?.token || ''
      if (!token) {
        showError('注册成功但未获取到Token')
        return
      }

      setStorage(TOKEN_KEY, token)
      setStorage(USER_INFO_KEY, { username, email })
      setStorage(LOGIN_STATUS_KEY, true)

      this.setData({ token })
      showSuccess('注册并登录成功')
    } catch (error) {
      showError(error.message || '注册失败')
    }
  },

  onLogout() {
    removeStorage(TOKEN_KEY)
    removeStorage(USER_INFO_KEY)
    removeStorage(LOGIN_STATUS_KEY)
    this.setData({ token: '' })
    showSuccess('已退出登录')
  },

  async onSendMessage() {
    const { message, selectedModelCode } = this.data
    if (!message) {
      showError('请输入问题')
      return
    }
    try {
      const response = await chatApi({
        modelCode: selectedModelCode || null,
        message
      })
      this.setData({
        answer: response?.data?.answer || ''
      })
    } catch (error) {
      showError(error.message || '对话失败')
    }
  }
})
