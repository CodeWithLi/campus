import { getStorage } from '../utils/storage'
import { buildApiUrl } from '../config/env'

export const transcribeSpeechApi = ({ filePath, scene = 'chat', durationMs = 0 }) =>
  new Promise((resolve, reject) => {
    const token = getStorage('TOKEN')
    const authorization = token ? `Bearer ${token}` : ''
    wx.uploadFile({
      url: buildApiUrl('/wx/speech/transcribe'),
      filePath,
      name: 'file',
      formData: {
        scene,
        durationMs
      },
      header: {
        Authorization: authorization
      },
      success: (res) => {
        try {
          const parsed = typeof res.data === 'string' ? JSON.parse(res.data) : res.data
          if (parsed?.code === 200) {
            resolve(parsed)
            return
          }
          reject(new Error(parsed?.msg || '语音转写失败'))
        } catch (error) {
          reject(error)
        }
      },
      fail: (error) => reject(error)
    })
  })
