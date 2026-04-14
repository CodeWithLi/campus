export const showSuccess = (title) => {
  wx.showToast({
    title,
    icon: 'success'
  })
}

export const showError = (title) => {
  wx.showToast({
    title,
    icon: 'none'
  })
}
