import { getStorage, removeStorage } from './storage'
import { buildApiUrl } from '../config/env'
import { ensureLoginOrRedirect } from './auth-guard'

const TOKEN_KEY = 'TOKEN'
const USER_INFO_KEY = 'USER_INFO'
const LOGIN_STATUS_KEY = 'LOGIN_STATUS'
const PUBLIC_GET_PATTERNS = [
  /^\/wx\/post\/list$/,
  /^\/wx\/post\/\d+$/,
  /^\/wx\/post\/\d+\/comments$/,
  /^\/wx\/guides$/,
  /^\/wx\/guides\/categories$/,
  /^\/wx\/guides\/\d+$/
]
const AUTH_FREE_PATTERNS = [
  /^\/wx\/auth\/login$/,
  /^\/wx\/auth\/register$/,
  /^\/wx\/auth\/email-code$/
]

const clearLoginStorage = () => {
  removeStorage(TOKEN_KEY)
  removeStorage(USER_INFO_KEY)
  removeStorage(LOGIN_STATUS_KEY)
}

const notifyAuthInvalid = () => {
  try {
    const pages = getCurrentPages ? getCurrentPages() : []
    ;(pages || []).forEach((p) => {
      if (p && typeof p.onAuthInvalid === 'function') {
        try {
          p.onAuthInvalid()
        } catch (e) {
          // ignore
        }
      }
    })
  } catch (e) {
    // ignore
  }
}

const handleUnauthorized = (silentAuth) => {
  if (silentAuth) {
    clearLoginStorage()
    notifyAuthInvalid()
    return
  }
  redirectToLogin()
}

const redirectToLogin = () => {
  clearLoginStorage()
  notifyAuthInvalid()
  ensureLoginOrRedirect()
}

const isPublicRequest = (url = '', method = 'GET') => {
  const upperMethod = String(method || 'GET').toUpperCase()
  if (AUTH_FREE_PATTERNS.some((pattern) => pattern.test(url))) {
    return true
  }
  if (upperMethod !== 'GET') {
    return false
  }
  return PUBLIC_GET_PATTERNS.some((pattern) => pattern.test(url))
}

const request = ({ url, method = 'GET', data = {}, header = {} }) =>
  new Promise((resolve, reject) => {
    const token = getStorage(TOKEN_KEY)
    if (!token && !isPublicRequest(url, method)) {
      ensureLoginOrRedirect()
      reject(new Error('未登录或登录已过期'))
      return
    }
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
          const silentAuth = String(header?.['X-Silent-Auth'] || '') === '1'
          handleUnauthorized(silentAuth)
          reject(new Error('登录已失效，请重新登录'))
          return
        }
        if (statusCode >= 200 && statusCode < 300) {
          const responseData = res.data || {}
          if (typeof responseData.code === 'number') {
            if (responseData.code === 401) {
              const silentAuth = String(header?.['X-Silent-Auth'] || '') === '1'
              handleUnauthorized(silentAuth)
              reject(new Error(responseData.msg || '登录已失效，请重新登录'))
              return
            }
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

export const del = (url, data = {}, header = {}) =>
  request({ url, method: 'DELETE', data, header })

const decodeChunk = (chunk) => {
  if (!chunk) return ''
  if (typeof chunk === 'string') return chunk
  try {
    if (typeof TextDecoder !== 'undefined') {
      return new TextDecoder('utf-8').decode(chunk)
    }
  } catch (e) {
    // ignore
  }
  try {
    const arr = new Uint8Array(chunk)
    let out = ''
    for (let i = 0; i < arr.length; i += 1) out += String.fromCharCode(arr[i])
    return decodeURIComponent(escape(out))
  } catch (e) {
    return ''
  }
}

export const postStream = (url, data = {}, options = {}) =>
  new Promise((resolve, reject) => {
    const token = getStorage(TOKEN_KEY)
    if (!token && !isPublicRequest(url, 'POST')) {
      ensureLoginOrRedirect()
      reject(new Error('未登录或登录已过期'))
      return
    }
    const authorization = token ? `Bearer ${token}` : ''
    const onMessage = typeof options?.onMessage === 'function' ? options.onMessage : () => {}
    const onError = typeof options?.onError === 'function' ? options.onError : () => {}
    const header = options?.header || {}
    let doneData = null
    let sseBuffer = ''
    let streamError = null
    let hasChunkCallbackData = false

    const task = wx.request({
      url: buildApiUrl(url),
      method: 'POST',
      enableChunked: true,
      data,
      header: {
        'content-type': 'application/json',
        Accept: 'text/event-stream',
        Authorization: authorization,
        ...header
      },
      success: (res) => {
        const { statusCode } = res
        if (statusCode === 401) {
          const silentAuth = String(header?.['X-Silent-Auth'] || '') === '1'
          handleUnauthorized(silentAuth)
          const err = new Error('登录已失效，请重新登录')
          onError(err)
          reject(err)
          return
        }
        if (statusCode < 200 || statusCode >= 300) {
          const err = new Error(`请求失败，状态码：${statusCode}`)
          onError(err)
          reject(err)
          return
        }
        // 兜底：某些真机 + 代理缓冲场景下 onChunkReceived 不触发，但 success.res.data 会带完整 SSE 文本
        if (!hasChunkCallbackData) {
          const rawBody = typeof res?.data === 'string' ? res.data : ''
          if (rawBody) {
            sseBuffer += rawBody
          }
        }
        flushSse(true)
        if (streamError) {
          reject(streamError)
          return
        }
        resolve(doneData)
      },
      fail: (error) => {
        onError(error)
        reject(error)
      }
    })

    if (!task || typeof task.onChunkReceived !== 'function') {
      const err = new Error('当前环境不支持流式请求')
      onError(err)
      reject(err)
      return
    }

    const handlePayloadText = (payloadText) => {
      if (!payloadText || payloadText === '[DONE]') return
      try {
        const payload = JSON.parse(payloadText)
        const type = payload?.type
        if (type === 'chunk') {
          onMessage(payload?.data || {})
        } else if (type === 'done') {
          doneData = payload?.data || null
        } else if (type === 'error') {
          const err = new Error(payload?.data?.message || '流式请求失败')
          streamError = err
          const msg = String(err.message || '')
          if (msg.includes('未登录') || msg.includes('登录已过期')) {
            handleUnauthorized(false)
          }
          onError(err)
        }
      } catch (e) {
        // ignore non-json chunks
      }
    }

    const flushSse = (force = false) => {
      // 兼容真机：部分环境不会严格按 \n\n 分 event，按“完整行”实时解析 data: 更稳。
      const normalized = sseBuffer.replace(/\r\n/g, '\n').replace(/\r/g, '\n')
      const lines = normalized.split('\n')
      const tail = lines.pop() || ''
      sseBuffer = force ? '' : tail
      lines.forEach((line) => {
        const l = String(line || '').trim()
        if (!l.startsWith('data:') && !l.startsWith('data :')) return
        const idx = l.indexOf(':')
        const payloadText = idx >= 0 ? l.slice(idx + 1).trim() : ''
        handlePayloadText(payloadText)
      })
      if (force) {
        const l = String(tail || '').trim()
        if (l.startsWith('data:') || l.startsWith('data :')) {
          const idx = l.indexOf(':')
          const payloadText = idx >= 0 ? l.slice(idx + 1).trim() : ''
          handlePayloadText(payloadText)
        }
      }
    }

    task.onChunkReceived((chunkRes) => {
      const text = decodeChunk(chunkRes?.data)
      if (!text) return
      hasChunkCallbackData = true
      sseBuffer += text
      flushSse()
    })
  })

export const uploadFiles = (url, filePaths = [], name = 'files', formData = {}) =>
  new Promise((resolve, reject) => {
    if (!Array.isArray(filePaths) || filePaths.length === 0) {
      reject(new Error('请先选择图片'))
      return
    }
    const token = getStorage(TOKEN_KEY)
    if (!token) {
      ensureLoginOrRedirect()
      reject(new Error('未登录或登录已过期'))
      return
    }
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
          const silentAuth = String(formData?.['X-Silent-Auth'] || '') === '1'
          handleUnauthorized(silentAuth)
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
            if (data.code === 401) {
              const silentAuth = String(formData?.['X-Silent-Auth'] || '') === '1'
              handleUnauthorized(silentAuth)
            }
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
