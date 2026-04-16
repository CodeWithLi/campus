import { get, post } from '../utils/request'

export const chatApi = (payload) =>
  post('/wx/chat', payload)

export const listConversationsApi = () =>
  get('/wx/chat/conversations')

export const getConversationHistoryApi = (conversationId) =>
  get(`/wx/chat/conversations/${conversationId}`)
