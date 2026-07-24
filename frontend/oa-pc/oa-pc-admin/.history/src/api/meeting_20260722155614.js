// src/api/meeting.js
import request from './request'

/**
 * 预约会议
 */
export function bookMeeting(data) {
  return request({
    url: '/meeting/book',
    method: 'POST',
    data
  })
}

/**
 * 保存会议（预约会议）
 */
export function saveMeeting(data) {
  return request({
    url: '/meeting/book',
    method: 'POST',
    data
  })
}

/**
 * 查询我的会议（按组织者ID）
 */
export function getMyMeetings(organizerId) {
  return request({
    url: `/meeting/myMeetings/${organizerId}`,
    method: 'GET'
  })
}

/**
 * 分页查询所有会议
 */
export function getMeetingPage(page, size) {
  return request({
    url: `/meeting/list/${page}/${size}`,
    method: 'GET'
  })
}

/**
 * 取消会议
 */
export function cancelMeeting(id) {
  return request({
    url: `/meeting/cancel/${id}`,
    method: 'PUT'
  })
}

/**
 * 查询会议室的预约情况
 */
export function getRoomMeetings(roomId) {
  return request({
    url: `/meeting/roomMeetings/${roomId}`,
    method: 'GET'
  })
}

/**
 * 更新会议信息
 */
export function updateMeeting(data) {
  return request({
    url: '/meeting/update',
    method: 'PUT',
    data
  })
}

/**
 * 删除会议
 */
export function deleteMeeting(id) {
  return request({
    url: `/meeting/deleteById/${id}`,
    method: 'DELETE'
  })
}