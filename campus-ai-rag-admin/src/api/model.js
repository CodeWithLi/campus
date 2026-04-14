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
