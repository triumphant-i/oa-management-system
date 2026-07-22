import request from '@/utils/request'

export function getMeetingRoomList() {
  return request({
    url: '/meetingRoom/list',
    method: 'get'
  })
}

export function getMeetingRoomById(id) {
  return request({
    url: `/meetingRoom/detail/${id}`,
    method: 'get'
  })
}

export function bookMeeting(data) {
  return request({
    url: '/meeting/book',
    method: 'post',
    data
  })
}

export function getMyMeetings(organizerId) {
  return request({
    url: `/meeting/myMeetings/${organizerId}`,
    method: 'get'
  })
}

export function getRoomMeetings(roomId) {
  return request({
    url: `/meeting/roomMeetings/${roomId}`,
    method: 'get'
  })
}

export function cancelMeeting(id) {
  return request({
    url: `/meeting/cancel/${id}`,
    method: 'put'
  })
}

export function updateMeeting(data) {
  return request({
    url: '/meeting/update',
    method: 'put',
    data
  })
}

export function startMeeting(id) {
  return request({
    url: `/meeting/start/${id}`,
    method: 'put'
  })
}

export function endMeeting(id) {
  return request({
    url: `/meeting/end/${id}`,
    method: 'put'
  })
}

export function extendMeeting(id, minutes) {
  return request({
    url: `/meeting/extend/${id}`,
    method: 'put',
    params: { minutes }
  })
}

export function getCurrentMeeting(employeeId) {
  return request({
    url: `/meeting/current/${employeeId}`,
    method: 'get'
  })
}

export function getUpcomingMeeting(employeeId) {
  return request({
    url: `/meeting/upcoming/${employeeId}`,
    method: 'get'
  })
}

export function checkRoomFull(roomId, date) {
  return request({
    url: `/meeting/checkFull/${roomId}/${date}`,
    method: 'get'
  })
}

export function getAllMeetings(status) {
  return request({
    url: '/meeting/all',
    method: 'get',
    params: status ? { status } : {}
  })
}

export function getMyInvitedMeetings(employeeId) {
  return request({
    url: `/meeting/myInvited/${employeeId}`,
    method: 'get'
  })
}

export function addMeetingRoom(data) {
  return request({
    url: '/meetingRoom/save',
    method: 'post',
    data
  })
}

export function updateMeetingRoom(data) {
  return request({
    url: '/meetingRoom/update',
    method: 'put',
    data
  })
}

export function deleteMeetingRoom(id) {
  return request({
    url: `/meetingRoom/deleteById/${id}`,
    method: 'delete'
  })
}