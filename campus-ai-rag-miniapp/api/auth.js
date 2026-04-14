import { post } from '../utils/request'

export const sendEmailCodeApi = (email) =>
  post('/wx/auth/email-code', { email })

export const registerApi = (payload) =>
  post('/wx/auth/register', payload)

export const loginApi = (payload) =>
  post('/wx/auth/login', payload)
