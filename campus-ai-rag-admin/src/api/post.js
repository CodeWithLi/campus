import request from '@/utils/request'

export const getPostPage = (params) => {
  return request({
    url: '/posts',
    method: 'get',
    params
  })
}

export const getPostDetail = (postId) => {
  return request({
    url: `/posts/${postId}`,
    method: 'get'
  })
}

export const deletePost = (postId) => {
  return request({
    url: `/posts/${postId}`,
    method: 'delete'
  })
}
