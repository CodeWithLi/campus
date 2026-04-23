import { get, post, postStream } from '../utils/request'

export const chatApi = (payload) =>
  post('/wx/chat', payload)

export const chatStreamApi = (payload, options = {}) =>
  postStream('/wx/chat/stream', payload, options)

export const listConversationsApi = () =>
  get('/wx/chat/conversations')

export const getConversationHistoryApi = (conversationId) =>
  get(`/wx/chat/conversations/${conversationId}`)
