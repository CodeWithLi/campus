import { get } from '../utils/request'

export const listEnabledModelsApi = () =>
  get('/wx/models/enabled')
