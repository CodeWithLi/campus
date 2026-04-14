import request from '@/utils/request'

export const getAdminUserList = () => {
  return request({
    url: '/users',
    method: 'get'
  })
}

export const getAdminUserDetail = (id) => {
  return request({
    url: `/users/${id}`,
    method: 'get'
  })
}

export const createAdminUser = (data) => {
  return request({
    url: '/users',
    method: 'post',
    data
  })
}

export const updateAdminUser = (id, data) => {
  return request({
    url: `/users/${id}`,
    method: 'put',
    data
  })
}

export const resetAdminUserPassword = (id, data) => {
  return request({
    url: `/users/${id}/reset-password`,
    method: 'put',
    data
  })
}

export const deleteAdminUser = (id) => {
  return request({
    url: `/users/${id}`,
    method: 'delete'
  })
}
