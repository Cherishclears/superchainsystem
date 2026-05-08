import request from '../utils/request'

export function getInventoryList(storeId) {
  return request.get(`/inventory/list/${storeId}`)
}

export function getWarningList(storeId) {
  return request.get(`/inventory/warning/${storeId}`)
}

export function inbound(params) {
  return request.post('/inventory/inbound', null, { params })
}

export function outbound(params) {
  return request.post('/inventory/outbound', null, { params })
}

export function adjust(params) {
  return request.post('/inventory/adjust', null, { params })
}

export function updateWarningQty(params) {
  return request.put('/inventory/warning', null, { params })
}
