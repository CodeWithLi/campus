import request from '@/utils/request'

export const getSystemPromptList = () => {
  return request({
    url: '/prompts/system',
    method: 'get'
  })
}

export const getSystemPromptDetail = (id) => {
  return request({
    url: `/prompts/system/${id}`,
    method: 'get'
  })
}

export const createSystemPrompt = (data) => {
  return request({
    url: '/prompts/system',
    method: 'post',
    data
  })
}

export const updateSystemPrompt = (id, data) => {
  return request({
    url: `/prompts/system/${id}`,
    method: 'put',
    data
  })
}

export const deleteSystemPrompt = (id) => {
  return request({
    url: `/prompts/system/${id}`,
    method: 'delete'
  })
}
