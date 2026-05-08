import request from '../utils/request'

export function getTransferPage(params) {
  return request.get('/transfer/page', { params })
}

export function createTransfer(data) {
  return request.post('/transfer', data)
}

export function submitTransfer(id) {
  return request.put(`/transfer/${id}/submit`)
}

export function approveTransfer(id) {
  return request.put(`/transfer/${id}/approve`)
}

export function shipTransfer(id) {
  return request.put(`/transfer/${id}/ship`)
}

export function receiveTransfer(id) {
  return request.put(`/transfer/${id}/receive`)
}

export function rejectTransfer(id) {
  return request.put(`/transfer/${id}/reject`)
}

export function getTransferItems(id) {
  return request.get(`/transfer/${id}/items`)
}
