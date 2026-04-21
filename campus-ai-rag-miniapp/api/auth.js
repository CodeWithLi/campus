import { get, post, uploadFiles } from '../utils/request'

export const sendEmailCodeApi = (email) =>
  post('/wx/auth/email-code', { email })

export const registerApi = (payload) =>
  post('/wx/auth/register', payload)

export const loginApi = (payload) =>
  post('/wx/auth/login', payload)

export const sendPasswordCodeApi = () =>
  post('/wx/auth/password-code', {})

export const updatePasswordApi = (payload) =>
  post('/wx/auth/password/update', payload)

export const getProfileApi = () =>
  get('/wx/auth/profile')

export const getPowerPointLogsApi = (params) =>
  get('/wx/auth/power-points/logs', params)

export const uploadAvatarApi = async (filePath) => {
  if (!filePath) {
    throw new Error('请先选择头像')
  }
  const res = await uploadFiles('/wx/auth/avatar/upload', [filePath], 'file')
  return res
}

export const updateNicknameApi = (nickname) =>
  post('/wx/auth/nickname/update', { nickname })
