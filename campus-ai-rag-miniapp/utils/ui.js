export const showSuccess = (title) => {
  wx.showToast({
    title,
    icon: 'success'
  })
}

export const showError = (title) => {
  const text = String(title || '')
  if (text === '未登录或登录已过期' || text === '登录已失效，请重新登录') {
    return
  }
  wx.showToast({
    title: text,
    icon: 'none'
  })
}
