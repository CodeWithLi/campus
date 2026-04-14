import { post } from '../utils/request'

export const chatApi = (payload) =>
  post('/wx/chat', payload)
