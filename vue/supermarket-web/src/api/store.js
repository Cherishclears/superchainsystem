import request from '../utils/request'

export function getStoreList() {
  return request.get('/store/list')
}

export function addStore(data) {
  return request.post('/store', data)
}

export function updateStore(data) {
  return request.put('/store', data)
}
