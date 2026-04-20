import { get, post } from '../utils/request'

export const createReminderApi = (payload) => post('/wx/reminder/create', payload)

export const listReminderApi = () => get('/wx/reminder/list')

export const deleteReminderApi = (reminderId) => post(`/wx/reminder/${reminderId}/delete`)

export const listReminderNoticeApi = () => get('/wx/reminder/notice/list')

export const readReminderNoticeApi = (noticeId) => post(`/wx/reminder/notice/${noticeId}/read`)

export const readAllReminderNoticeApi = () => post('/wx/reminder/notice/read-all')
