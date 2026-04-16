import request from '@/utils/request'

export const ragDebug = (data) => {
  return request({
    url: '/kb/rag/debug',
    method: 'post',
    data
  })
}

