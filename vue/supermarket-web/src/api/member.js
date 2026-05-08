import request from '../utils/request'

export function getMemberPage(params) {
  return request.get('/member/page', { params })
}

export function registerMember(data) {
  return request.post('/member/register', data)
}



export function getMemberByPhone(phone) {
  return request.get(`/member/phone/${phone}`)
}
