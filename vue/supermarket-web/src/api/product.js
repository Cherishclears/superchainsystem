import request from '../utils/request'

// 分页查询
export function getProductPage(params) {
  return request.get('/product/page', { params })
}

// 新增商品
export function addProduct(data) {
  return request.post('/product', data)
}

// 修改商品
export function updateProduct(data) {
  return request.put('/product', data)
}

// 根据ID查询
export function getProductById(id) {
  return request.get(`/product/${id}`)
}

// 删除商品
export function deleteProduct(id) {
  return request.delete(`/product/${id}`)
}

// 上下架
export function updateProductStatus(id, status) {
  return request.put(`/product/${id}/status/${status}`)
}

// 查询门店在售商品
export function getStoreProductPage(storeId, params) {
  return request.get(`/product/store/${storeId}/page`, { params })
}

export function getProductByBarcode(barcode, storeId) {
  return request.get(`/product/barcode/${barcode}`, { params: { storeId } })
}
