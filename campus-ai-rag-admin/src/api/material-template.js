import request from '@/utils/request'

export const getMaterialTemplateList = () => {
  return request({
    url: '/guides/material-templates',
    method: 'get'
  })
}

export const getMaterialTemplateDetail = (id) => {
  return request({
    url: `/guides/material-templates/${id}`,
    method: 'get'
  })
}

export const createMaterialTemplate = (data) => {
  return request({
    url: '/guides/material-templates',
    method: 'post',
    data
  })
}

export const updateMaterialTemplate = (id, data) => {
  return request({
    url: `/guides/material-templates/${id}`,
    method: 'put',
    data
  })
}

export const deleteMaterialTemplate = (id) => {
  return request({
    url: `/guides/material-templates/${id}`,
    method: 'delete'
  })
}

