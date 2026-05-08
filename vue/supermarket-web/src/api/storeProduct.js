import request from '../utils/request'

export function getStoreProductList(storeId) {
  return request.get(`/store-product/list/${storeId}`)
}

export function saveStoreProductConfig(data) {
  return request.post('/store-product/save', data)
}

export function batchSaveConfig(data) {
  return request.post('/store-product/batch', data)
}
