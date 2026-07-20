import request from '@/utils/request'

export function getAnnouncementList() {
  return request({
    url: '/announcement/list',
    method: 'get'
  })
}

export function getAnnouncementListPage(page, size) {
  return request({
    url: `/announcement/list/${page}/${size}`,
    method: 'get'
  })
}

export function getAnnouncementById(id) {
  return request({
    url: `/announcement/findById/${id}`,
    method: 'get'
  })
}

export function publishAnnouncement(data) {
  return request({
    url: '/announcement/publish',
    method: 'post',
    data
  })
}

export function updateAnnouncement(data) {
  return request({
    url: '/announcement/update',
    method: 'put',
    data
  })
}

export function deleteAnnouncement(id) {
  return request({
    url: `/announcement/deleteById/${id}`,
    method: 'delete'
  })
}

export function setTopAnnouncement(id) {
  return request({
    url: `/announcement/setTop/${id}`,
    method: 'put'
  })
}

export function cancelTopAnnouncement(id) {
  return request({
    url: `/announcement/cancelTop/${id}`,
    method: 'put'
  })
}

export function markReadAnnouncement(id) {
  return request({
    url: `/announcement/markRead/${id}`,
    method: 'put'
  })
}

export function countUnreadAnnouncements() {
  return request({
    url: '/announcement/countUnread',
    method: 'get'
  })
}

export function markAllReadAnnouncements() {
  return request({
    url: '/announcement/markAllRead',
    method: 'put'
  })
}

export function getAnnouncementReadStats() {
  return request({
    url: '/announcement/readStats',
    method: 'get'
  })
}