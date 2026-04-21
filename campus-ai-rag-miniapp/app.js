import { ensureLoginOrRedirect, isLoggedIn, isPublicPage } from './utils/auth-guard'

let firstShowChecked = false

const wrapNavMethod = (methodName) => {
  const original = wx[methodName]
  if (typeof original !== 'function') return
  wx[methodName] = (options = {}) => {
    const url = options?.url || ''
    if (url && !isPublicPage(url) && !ensureLoginOrRedirect()) {
      return
    }
    return original.call(wx, options)
  }
}

App({
  onLaunch() {
    wrapNavMethod('navigateTo')
    wrapNavMethod('redirectTo')
    wrapNavMethod('reLaunch')
    wrapNavMethod('switchTab')
  },
  onShow() {
    // 首次进入：未登录则强制跳登录页
    if (firstShowChecked) return
    firstShowChecked = true
    if (isLoggedIn()) return
    const pages = getCurrentPages()
    const currentRoute = pages[pages.length - 1]?.route || ''
    if (currentRoute === 'pages/login/login') return
    wx.navigateTo({ url: '/pages/login/login' })
  },
  globalData: {
    userInfo: null
  }
})
