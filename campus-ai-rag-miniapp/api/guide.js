import { get, post } from '../utils/request'

export const listGuidesApi = (params = {}) => get('/wx/guides', params)

export const listGuideCategoriesApi = () => get('/wx/guides/categories')

export const getGuideDetailApi = (guideId) => get(`/wx/guides/${guideId}`)

export const toggleGuideFavoriteApi = (guideId) => post(`/wx/guides/${guideId}/favorite/toggle`)

export const updateGuideStepProgressApi = (guideId, stepId, done) =>
  post(`/wx/guides/${guideId}/steps/${stepId}/progress`, { done: !!done })
