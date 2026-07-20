import request from '@/utils/request'

export function getMyInfo() {
  return request({
    url: '/profile/myInfo',
    method: 'get'
  })
}

export function updateProfile(data) {
  return request({
    url: '/profile/update',
    method: 'put',
    data
  })
}

export function updatePassword(data) {
  return request({
    url: '/profile/updatePassword',
    method: 'put',
    data
  })
}