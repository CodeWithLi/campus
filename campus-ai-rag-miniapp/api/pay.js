import { get, post } from '../utils/request'

export const createAlipayWapOrderApi = (payload) =>
  post('/wx/pay/alipay/wap/create', payload)

export const listMyRechargeOrdersApi = () =>
  get('/wx/pay/alipay/orders')

export const getMyRechargeOrderApi = (orderNo) =>
  get(`/wx/pay/alipay/orders/${orderNo}`)

