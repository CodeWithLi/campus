import request from '@/utils/request'

export const getModelConfigList = () => {
  return request({
    url: '/models',
    method: 'get'
  })
}

export const getModelConfigDetail = (id) => {
  return request({
    url: `/models/${id}`,
    method: 'get'
  })
}

export const createModelConfig = (data) => {
  return request({
    url: '/models',
    method: 'post',
    data
  })
}

export const updateModelConfig = (id, data) => {
  return request({
    url: `/models/${id}`,
    method: 'put',
    data
  })
}

export const deleteModelConfig = (id) => {
  return request({
    url: `/models/${id}`,
    method: 'delete'
  })
}

export const testModelChat = (data) => {
  return request({
    url: '/models/test-chat',
    method: 'post',
    data
  })
}

export const getTokenBillingLogPage = (params) => {
  return request({
    url: '/token-billing-logs',
    method: 'get',
    params
  })
}

export const getAiCallTrend = (params) => {
  return request({
    url: '/token-billing-logs/stats/trend',
    method: 'get',
    params
  })
}

export const getTokenConsumptionTrend = (params) => {
  return request({
    url: '/token-billing-logs/stats/consumption',
    method: 'get',
    params
  })
}

export const getModelUsageShare = (params) => {
  return request({
    url: '/token-billing-logs/stats/model-share',
    method: 'get',
    params
  })
}
