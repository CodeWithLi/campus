import request from '@/utils/request'

export const getRechargeOrderPage = (params) => {
  return request({
    url: '/recharge-orders',
    method: 'get',
    params
  })
}

export const getRechargeOrderDetail = (id) => {
  return request({
    url: `/recharge-orders/${id}`,
    method: 'get'
  })
}
