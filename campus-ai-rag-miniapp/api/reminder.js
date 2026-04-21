import { get, post, uploadFiles } from '../utils/request'

export const createReminderApi = (payload) => post('/wx/reminder/create', payload)

export const listReminderApi = () => get('/wx/reminder/list')

export const deleteReminderApi = (reminderId) => post(`/wx/reminder/${reminderId}/delete`)

export const listReminderNoticeApi = () => get('/wx/reminder/notice/list')

export const readReminderNoticeApi = (noticeId) => post(`/wx/reminder/notice/${noticeId}/read`)

export const readAllReminderNoticeApi = () => post('/wx/reminder/notice/read-all')

export const parseNoticeByAgentApi = async (filePath) => {
  if (!filePath) throw new Error('请选择文件')
  return uploadFiles('/wx/reminder/agent/parse', [filePath], 'file')
}

export const parseNoticeByImageUnderstandApi = async (filePath) => {
  if (!filePath) throw new Error('请选择文件')
  return uploadFiles('/wx/reminder/agent/parse-image-understand', [filePath], 'file')
}

export const parseNoticeByImageModelApi = async (filePath) => {
  if (!filePath) throw new Error('请选择文件')
  return uploadFiles('/wx/reminder/agent/parse-image-model', [filePath], 'file')
}

export const parseNoticeByImageVisionApi = async (filePath) => {
  if (!filePath) throw new Error('请选择文件')
  return uploadFiles('/wx/reminder/agent/parse-image-vision', [filePath], 'file')
}

export const createReminderByAgentApi = (items = [], recordId = null) =>
  post('/wx/reminder/agent/create', { items, recordId })

export const listNoticeAgentRecordsApi = () => get('/wx/reminder/agent/records')

export const deleteNoticeAgentRecordApi = (recordId) =>
  post(`/wx/reminder/agent/records/${recordId}/delete`)
