import request from '@/utils/request'

export const getPostReportPage = (params) => {
  return request({
    url: '/post-reports',
    method: 'get',
    params
  })
}

export const getPostReportDetail = (reportId) => {
  return request({
    url: `/post-reports/${reportId}`,
    method: 'get'
  })
}

export const handlePostReport = (reportId, data) => {
  return request({
    url: `/post-reports/${reportId}/handle`,
    method: 'put',
    data
  })
}
