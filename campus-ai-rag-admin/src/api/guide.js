import request from '@/utils/request'

export const getGuidePage = (params) => {
  return request({
    url: '/guides',
    method: 'get',
    params
  })
}

export const getGuideDetail = (id) => {
  return request({
    url: `/guides/${id}`,
    method: 'get'
  })
}

export const createGuide = (data) => {
  return request({
    url: '/guides',
    method: 'post',
    data
  })
}

export const updateGuide = (id, data) => {
  return request({
    url: `/guides/${id}`,
    method: 'put',
    data
  })
}

export const publishGuide = (id) => {
  return request({
    url: `/guides/${id}/publish`,
    method: 'post'
  })
}

export const offlineGuide = (id) => {
  return request({
    url: `/guides/${id}/offline`,
    method: 'post'
  })
}

export const deleteGuide = (id) => {
  return request({
    url: `/guides/${id}`,
    method: 'delete'
  })
}

export const uploadGuideCover = (file) => {
  const formData = new FormData()
  formData.append('file', file)
  return request({
    url: '/guides/cover/upload',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}
