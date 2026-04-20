import { getStorage, removeStorage } from './storage'
import { buildApiUrl } from '../config/env'

const TOKEN_KEY = 'TOKEN'
const USER_INFO_KEY = 'USER_INFO'
const LOGIN_STATUS_KEY = 'LOGIN_STATUS'

const redirectToLogin = () => {
  removeStorage(TOKEN_KEY)
  removeStorage(USER_INFO_KEY)
  removeStorage(LOGIN_STATUS_KEY)
}

const request = ({ url, method = 'GET', data = {}, header = {} }) =>
  new Promise((resolve, reject) => {
    const token = getStorage(TOKEN_KEY)
    const authorization = token ? `Bearer ${token}` : ''

    wx.request({
      url: buildApiUrl(url),
      method,
      data,
      header: {
        'content-type': 'application/json',
        Authorization: authorization,
        ...header
      },
      success: (res) => {
        const { statusCode } = res
        if (statusCode === 401) {
          redirectToLogin()
          reject(new Error('登录已失效，请重新登录'))
          return
        }
        if (statusCode >= 200 && statusCode < 300) {
          const responseData = res.data || {}
          if (typeof responseData.code === 'number') {
            if (responseData.code === 200) {
              resolve(responseData)
              return
            }
            reject(new Error(responseData.msg || '请求失败'))
            return
          }
          resolve(responseData)
          return
        }
        reject(new Error(`请求失败，状态码：${statusCode}`))
      },
      fail: (error) => reject(error)
    })
  })

export const get = (url, data = {}, header = {}) =>
  request({ url, method: 'GET', data, header })

export const post = (url, data = {}, header = {}) =>
  request({ url, method: 'POST', data, header })

export const uploadFiles = (url, filePaths = [], name = 'files', formData = {}) =>
  new Promise((resolve, reject) => {
    if (!Array.isArray(filePaths) || filePaths.length === 0) {
      reject(new Error('请先选择图片'))
      return
    }
    const token = getStorage(TOKEN_KEY)
    const authorization = token ? `Bearer ${token}` : ''
    wx.uploadFile({
      url: buildApiUrl(url),
      filePath: filePaths[0],
      name,
      formData,
      header: {
        Authorization: authorization
      },
      success: (res) => {
        const statusCode = res.statusCode
        if (statusCode === 401) {
          redirectToLogin()
          reject(new Error('登录已失效，请重新登录'))
          return
        }
        if (statusCode < 200 || statusCode >= 300) {
          reject(new Error(`上传失败，状态码：${statusCode}`))
          return
        }
        try {
          const data = JSON.parse(res.data || '{}')
          if (typeof data.code === 'number' && data.code !== 200) {
            reject(new Error(data.msg || '上传失败'))
            return
          }
          resolve(data)
        } catch (e) {
          reject(new Error('上传返回解析失败'))
        }
      },
      fail: (error) => reject(error)
    })
  })
