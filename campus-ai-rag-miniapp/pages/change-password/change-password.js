import { sendPasswordCodeApi, updatePasswordApi } from '../../api/auth'
import { getWxLoginCode } from '../../utils/wechat'
import { showError, showSuccess } from '../../utils/ui'

Page({
  data: {
    verifyType: 'EMAIL_CODE',
    emailCode: '',
    newPassword: '',
    confirmPassword: '',
    loading: false,
    sendingCode: false
  },

  onSwitchVerifyType(e) {
    const type = e.currentTarget.dataset?.type
    if (!type || this.data.verifyType === type) return
    this.setData({ verifyType: type, emailCode: '' })
  },

  onInputEmailCode(e) {
    this.setData({ emailCode: e.detail?.value || '' })
  },

  onInputNewPassword(e) {
    this.setData({ newPassword: e.detail?.value || '' })
  },

  onInputConfirmPassword(e) {
    this.setData({ confirmPassword: e.detail?.value || '' })
  },

  async onSendCode() {
    if (this.data.sendingCode) return
    this.setData({ sendingCode: true })
    try {
      await sendPasswordCodeApi()
      showSuccess('验证码已发送到当前账号邮箱')
    } catch (e) {
      showError(e.message || '发送验证码失败')
    } finally {
      this.setData({ sendingCode: false })
    }
  },

  async onSubmit() {
    const { verifyType, emailCode, newPassword, confirmPassword, loading } = this.data
    if (loading) return
    const pwd = (newPassword || '').trim()
    const confirm = (confirmPassword || '').trim()
    if (!pwd) {
      showError('请输入新密码')
      return
    }
    if (pwd.length < 6) {
      showError('新密码至少6位')
      return
    }
    if (pwd !== confirm) {
      showError('两次输入密码不一致')
      return
    }
    this.setData({ loading: true })
    try {
      if (verifyType === 'EMAIL_CODE') {
        if (!(emailCode || '').trim()) {
          showError('请输入邮箱验证码')
          return
        }
        await updatePasswordApi({
          verifyType: 'EMAIL_CODE',
          emailCode: (emailCode || '').trim(),
          newPassword: pwd
        })
      } else {
        const wxCode = await getWxLoginCode()
        await updatePasswordApi({
          verifyType: 'WX_ONE_CLICK',
          wxCode,
          newPassword: pwd
        })
      }
      showSuccess('密码修改成功')
      setTimeout(() => {
        wx.navigateBack({ delta: 1 })
      }, 300)
    } catch (e) {
      showError(e.message || '修改密码失败')
    } finally {
      this.setData({ loading: false })
    }
  }
})
