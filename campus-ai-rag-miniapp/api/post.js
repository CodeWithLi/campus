import { del, get, post, uploadFiles } from '../utils/request'

/** AI 生成标题与正文（服务端场景 POST，模型见 app.wx.post.ai-model-code） */
export const postAiGenerate = (payload) => post('/wx/post/ai-generate', payload)

/**
 * 发布帖子。
 * - 若正文与 AI 生成结果一致，请传入 generate 接口返回的 draftId，可跳过文本合规（省合规 token）。
 * - 用户编辑过标题或正文后请勿传 draftId，或在前端清空 draftId，以触发文本合规。
 */
export const postPublish = (payload) => post('/wx/post/publish', payload)

export const uploadPostImages = async (filePaths = []) => {
  if (!Array.isArray(filePaths) || filePaths.length === 0) {
    throw new Error('请先选择图片')
  }
  if (filePaths.length > 5) {
    throw new Error('最多上传5张图片')
  }
  const all = []
  for (const filePath of filePaths) {
    const res = await uploadFiles('/wx/post/upload-images', [filePath], 'files')
    const urls = res?.data?.imageUrls || []
    all.push(...urls)
  }
  return {
    code: 200,
    data: {
      imageUrls: all
    }
  }
}

export const listPostsApi = (params = {}) => get('/wx/post/list', params)

export const getPostDetailApi = (postId) => get(`/wx/post/${postId}`)

export const listMyPostsApi = (params = {}) => get('/wx/post/my', params)

export const togglePostLikeApi = (postId) => post(`/wx/post/${postId}/like`)

export const togglePostFavoriteApi = (postId) => post(`/wx/post/${postId}/favorite`)

export const listPostCommentsApi = (postId) => get(`/wx/post/${postId}/comments`)

export const createPostCommentApi = (postId, payload) => post(`/wx/post/${postId}/comments`, payload)

export const deleteMyPostApi = (postId) => del(`/wx/post/${postId}`)

export const listMyFavoritePostsApi = (params = {}) => get('/wx/post/my-favorites', params)

export const listMyInteractionsApi = (params = {}) => get('/wx/post/my-interactions', params)

export const reportPostApi = (postId, payload) => post(`/wx/post/${postId}/report`, payload)
