import request from '../utils/request'

export function getPurchasePage(params) {
  return request.get('/purchase/page', { params })
}

export function createPurchase(data) {
  return request.post('/purchase', data)
}

export function submitPurchase(id) {
  return request.put(`/purchase/${id}/submit`)
}

export function approvePurchase(id) {
  return request.put(`/purchase/${id}/approve`)
}

export function receivePurchase(id) {
  return request.put(`/purchase/${id}/receive`)
}

export function cancelPurchase(id) {
  return request.put(`/purchase/${id}/cancel`)
}

export function getAiSuggest(storeId) {
  return request.get('/purchase/ai-suggest', { params: { storeId } })
}
