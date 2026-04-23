import { ensureLoginOrRedirect, isLoggedIn, isPublicPage } from './utils/auth-guard'

let firstShowChecked = false

App({
  onLaunch() {
    // 真机环境中 wx API 方法属性可能是只读，不能在这里重写。
    // 登录拦截仍由 onShow 与各业务请求前置校验兜底。
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
