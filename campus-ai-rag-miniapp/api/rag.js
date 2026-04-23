import { get, post, postStream } from '../utils/request'

export const ragApi = (payload) => post('/wx/rag', payload)

export const ragStreamApi = (payload, options = {}) => postStream('/wx/rag/stream', payload, options)

export const listKbCategoriesApi = () => get('/wx/kb/categories')

export const listRagConversationsApi = () =>
  get('/wx/rag/conversations')

export const getRagConversationHistoryApi = (conversationId) =>
  get(`/wx/rag/conversations/${conversationId}`)

