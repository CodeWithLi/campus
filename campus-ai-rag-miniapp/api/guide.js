import { get } from '../utils/request'

export const listGuidesApi = (params = {}) => get('/wx/guides', params)

export const listGuideCategoriesApi = () => get('/wx/guides/categories')

export const getGuideDetailApi = (guideId) => get(`/wx/guides/${guideId}`)
