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