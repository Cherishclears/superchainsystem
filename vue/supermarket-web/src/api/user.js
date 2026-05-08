import request from '../utils/request'

// 获取当前用户信息
export function getUserInfo() {
  return request.get('/user/info')
}

// 修改个人信息
export function updateUserInfo(data) {
  return request.put('/user/info', data)
}

// 修改密码
export function updatePassword(data) {
  return request.put('/user/password', data)
}

// 分页查询用户列表（总部）
export function getUserPage(params) {
  return request.get('/user/page', { params })
}

// 新增用户（总部）
export function createUser(data) {
  return request.post('/user', data)
}

// 启用/禁用用户
export function updateUserStatus(id, status) {
  return request.put(`/user/${id}/status/${status}`)
}

// 重置密码
export function resetUserPassword(id) {
  return request.put(`/user/${id}/reset-password`)
}
