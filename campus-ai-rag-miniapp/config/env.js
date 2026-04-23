// 统一环境配置：只改这里即可切换本地/线上
const ENV = 'prod' // 'local' | 'prod'

const CONFIG = {
  local: {
    baseUrl: 'http://10.0.4.168:19832/campus',
    reminderSubscribeTemplateId: 'HRGjPD_lk8nZd84kmHQnkFg6za4YJfFTi-ejdlzScMo'
  },
  prod: {
    baseUrl: 'https://admin.baomi-ai.com/campus',
    reminderSubscribeTemplateId: 'HRGjPD_lk8nZd84kmHQnkFg6za4YJfFTi-ejdlzScMo'
  }
}

export const API_BASE_URL = (CONFIG[ENV] || CONFIG.local).baseUrl
export const REMINDER_SUBSCRIBE_TEMPLATE_ID = (CONFIG[ENV] || CONFIG.local).reminderSubscribeTemplateId || ''

export const buildApiUrl = (path = '') => {
  if (!path) return API_BASE_URL
  if (/^https?:\/\//.test(path)) return path
  return `${API_BASE_URL}${path.startsWith('/') ? path : `/${path}`}`
}

export const buildWsUrl = (path = '') => {
  const wsBase = API_BASE_URL.replace(/^https/, 'wss').replace(/^http/, 'ws')
  if (!path) return wsBase
  return `${wsBase}${path.startsWith('/') ? path : `/${path}`}`
}
