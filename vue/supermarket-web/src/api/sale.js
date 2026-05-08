import request from '../utils/request'

export function getSalePage(params) {
  return request.get('/sale/order/page', { params })
}

export function createSaleOrder(data) {
  return request.post('/sale/order', data)
}

export function returnOrder(originalOrderNo, reason) {
  return request.post('/sale/return', null, {
    params: { originalOrderNo, reason }
  })
}
