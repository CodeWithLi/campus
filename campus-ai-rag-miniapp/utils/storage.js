export const setStorage = (key, data) => {
  try {
    wx.setStorageSync(key, data)
  } catch (error) {
    console.error(`存储 ${key} 失败`, error)
  }
}

export const getStorage = (key) => {
  try {
    return wx.getStorageSync(key)
  } catch (error) {
    console.error(`读取 ${key} 失败`, error)
    return null
  }
}

export const removeStorage = (key) => {
  try {
    wx.removeStorageSync(key)
  } catch (error) {
    console.error(`删除 ${key} 失败`, error)
  }
}

export const clearStorage = () => {
  try {
    wx.clearStorageSync()
  } catch (error) {
    console.error('清空缓存失败', error)
  }
}
