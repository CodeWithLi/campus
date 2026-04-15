import request from '@/utils/request'

export const getKbCategoryList = () => {
  return request({
    url: '/kb/categories',
    method: 'get'
  })
}

export const getKbCategoryDetail = (id) => {
  return request({
    url: `/kb/categories/${id}`,
    method: 'get'
  })
}

export const createKbCategory = (data) => {
  return request({
    url: '/kb/categories',
    method: 'post',
    data
  })
}

export const updateKbCategory = (id, data) => {
  return request({
    url: `/kb/categories/${id}`,
    method: 'put',
    data
  })
}

export const deleteKbCategory = (id) => {
  return request({
    url: `/kb/categories/${id}`,
    method: 'delete'
  })
}

export const getKbDocumentList = (params) => {
  return request({
    url: '/kb/documents',
    method: 'get',
    params
  })
}

export const getKbDocumentDetail = (id) => {
  return request({
    url: `/kb/documents/${id}`,
    method: 'get'
  })
}

export const uploadKbDocument = ({ meta, file }) => {
  const formData = new FormData()
  formData.append('meta', new Blob([JSON.stringify(meta)], { type: 'application/json' }))
  formData.append('file', file)

  return request({
    url: '/kb/documents/upload',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

export const uploadAndIndexKbDocument = ({ meta, file }) => {
  const formData = new FormData()
  formData.append('meta', new Blob([JSON.stringify(meta)], { type: 'application/json' }))
  formData.append('file', file)

  return request({
    url: '/kb/documents/upload-and-index',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

export const updateKbDocumentStatus = (id, data) => {
  return request({
    url: `/kb/documents/${id}/status`,
    method: 'put',
    data
  })
}

export const retryKbDocument = (id) => {
  return request({
    url: `/kb/documents/${id}/retry`,
    method: 'put'
  })
}

export const parseKbDocument = (id) => {
  return request({
    url: `/kb/documents/${id}/parse`,
    method: 'put'
  })
}

export const indexKbDocument = (id) => {
  return request({
    url: `/kb/documents/${id}/index`,
    method: 'put'
  })
}

export const previewKbDocumentImageUnderstand = (id) => {
  return request({
    url: `/kb/documents/${id}/image-understand/preview`,
    method: 'post'
  })
}

export const indexKbDocumentByImageUnderstand = (id) => {
  return request({
    url: `/kb/documents/${id}/image-understand/index`,
    method: 'post'
  })
}

export const submitKbImageUnderstandPreviewTask = (id) => {
  return request({
    url: `/kb/documents/${id}/image-understand/preview-async`,
    method: 'post'
  })
}

export const submitKbImageUnderstandIndexTask = (id, data) => {
  return request({
    url: `/kb/documents/${id}/image-understand/index-async`,
    method: 'post',
    data
  })
}

export const submitKbImageUnderstandIndexTaskByTaskId = (taskId, data) => {
  return request({
    url: `/kb/documents/image-understand/tasks/${taskId}/index-async`,
    method: 'post',
    data
  })
}

export const getKbImageUnderstandTask = (taskId) => {
  return request({
    url: `/kb/documents/image-understand/tasks/${taskId}`,
    method: 'get'
  })
}

export const getKbImageUnderstandTaskList = (params) => {
  return request({
    url: '/kb/documents/image-understand/tasks',
    method: 'get',
    params
  })
}
