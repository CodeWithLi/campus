import { getStorage } from './storage'

const TOKEN_KEY = 'TOKEN'
const LOGIN_STATUS_KEY = 'LOGIN_STATUS'
const LOGIN_PAGE = '/pages/login/login'
let loginPrompting = false

const PUBLIC_PAGE_ROUTES = new Set([
  'pages/home/home',
  'pages/post-list/post-list',
  'pages/post-detail/post-detail',
  'pages/login/login',
  'pages/register/register'
])

const normalizeRoute = (path = '') => {
  const noQuery = String(path || '').split('?')[0]
  return noQuery.replace(/^\//, '')
}

export const isLoggedIn = () => {
  const token = getStorage(TOKEN_KEY)
  const loginStatus = Boolean(getStorage(LOGIN_STATUS_KEY))
  return Boolean(token) && loginStatus
}

export const isPublicPage = (path = '') => PUBLIC_PAGE_ROUTES.has(normalizeRoute(path))

const openLoginModal = (message, attempt = 0) => {
  // 冷启动时 UI 可能尚未 ready，稍微延迟更稳
  setTimeout(() => {
    wx.showModal({
      title: '需要登录',
      content: message,
      confirmText: '去登录',
      cancelText: '先逛逛',
      success: (res) => {
        if (res.confirm) {
          const pages = getCurrentPages()
          const currentRoute = pages[pages.length - 1]?.route || ''
          if (currentRoute !== normalizeRoute(LOGIN_PAGE)) {
            wx.navigateTo({ url: LOGIN_PAGE })
          }
        }
      },
      fail: () => {
        // 启动阶段偶发弹窗失败，做一次轻量重试
        if (attempt < 1) {
          openLoginModal(message, attempt + 1)
          return
        }
        loginPrompting = false
      },
      complete: () => {
        loginPrompting = false
      }
    })
  }, attempt === 0 ? 80 : 220)
}

export const ensureLoginOrRedirect = (message = '未登录或登录已过期') => {
  if (isLoggedIn()) {
    return true
  }
  if (loginPrompting) {
    return false
  }
  loginPrompting = true
  openLoginModal(message, 0)
  return false
}

