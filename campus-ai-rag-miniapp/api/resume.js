import { get, post } from '../utils/request'

export const resumeChatApi = (payload) => post('/wx/resume/chat', payload)

export const resumeGenerateApi = (payload) => post('/wx/resume/generate', payload)

export const listResumeConversationsApi = () => get('/wx/resume/conversations')

export const getResumeConversationHistoryApi = (conversationId) =>
  get(`/wx/resume/conversations/${conversationId}`)

export const listResumeRecordsApi = (params = {}) =>
  get('/wx/resume/records', params)

