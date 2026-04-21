import { ensureLoginOrRedirect, isPublicPage } from './utils/auth-guard'
import { getStorage, setStorage } from './utils/storage'

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

const STARTUP_LOGIN_PROMPT_KEY = 'STARTUP_LOGIN_PROMPT_SHOWN'

App({
  onLaunch() {
    wrapNavMethod('navigateTo')
    wrapNavMethod('redirectTo')
    wrapNavMethod('reLaunch')
    wrapNavMethod('switchTab')
  },
  onShow() {
    // 首次进入提示一次：用户可选择“去登录/先逛逛”
    try {
      const already = Boolean(getStorage(STARTUP_LOGIN_PROMPT_KEY))
      if (already) return
      // 只弹一次：无论用户选啥都记住，避免反复打扰
      setStorage(STARTUP_LOGIN_PROMPT_KEY, true)
      // 这里不直接跳转，走选择弹窗；冷启动延迟一下更稳
      setTimeout(() => {
        const pages = getCurrentPages()
        const currentRoute = pages[pages.length - 1]?.route || ''
        if (currentRoute === 'pages/login/login') return
        ensureLoginOrRedirect('登录后可使用 AI、提醒解析、发帖互动等功能；不登录也可以先逛社区。')
      }, 200)
    } catch (e) {
      // ignore
    }
  },
  globalData: {
    userInfo: null
  }
})
