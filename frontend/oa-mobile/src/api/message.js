import request from '@/utils/request'

export function getMessageList() {
  return request({
    url: '/message/list',
    method: 'get'
  })
}

export function getUnreadCount() {
  return request({
    url: '/message/countUnread',
    method: 'get'
  })
}

export function markAsRead(id) {
  return request({
    url: `/message/markRead/${id}`,
    method: 'post'
  })
}

export function markAllAsRead() {
  return request({
    url: '/message/markAllRead',
    method: 'post'
  })
}

export function sendMessage(data) {
  return request({
    url: '/message/send',
    method: 'post',
    data
  })
}

export function getMessageDetail(id) {
  return request({
    url: `/message/detail/${id}`,
    method: 'get'
  })
}

export function deleteMessage(id) {
  return request({
    url: `/message/delete/${id}`,
    method: 'delete'
  })
}