import { post } from '../utils/request'

export const ragApi = (payload) => post('/wx/rag', payload)

