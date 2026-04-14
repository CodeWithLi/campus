export const getWxLoginCode = () =>
  new Promise((resolve, reject) => {
    wx.login({
      success: (res) => {
        if (res.code) {
          resolve(res.code)
          return
        }
        reject(new Error('未获取到微信登录code'))
      },
      fail: (error) => reject(error)
    })
  })
