import request from '@/utils/request'

export const getWxUserList = () => {
  return request({
    url: '/wx-users',
    method: 'get'
  })
}

export const getWxUserDetail = (id) => {
  return request({
    url: `/wx-users/${id}`,
    method: 'get'
  })
}

export const updateWxUser = (id, data) => {
  return request({
    url: `/wx-users/${id}`,
    method: 'put',
    data
  })
}

export const updateWxUserPowerPoints = (id, data) => {
  return request({
    url: `/wx-users/${id}/power-points`,
    method: 'put',
    data
  })
}

export const freezeWxUser = (id) => {
  return request({
    url: `/wx-users/${id}/freeze`,
    method: 'put'
  })
}

export const unfreezeWxUser = (id) => {
  return request({
    url: `/wx-users/${id}/unfreeze`,
    method: 'put'
  })
}

