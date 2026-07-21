import request from './request'

/**
 * 添加会议室
 */
export const saveMeetingRoom = (data) => {
  return request({
    url: '/meetingRoom/save',
    method: 'POST',
    data
  })
}

/**
 * 查询所有会议室
 */
export const getMeetingRoomList = () => {
  return request({
    url: '/meetingRoom/list',
    method: 'GET'
  })
}

/**
 * 分页查询会议室
 */
export const getMeetingRoomPage = (page, size) => {
  return request({
    url: `/meetingRoom/list/${page}/${size}`,
    method: 'GET'
  })
}

/**
 * 根据ID查询会议室
 */
export const getMeetingRoomById = (id) => {
  return request({
    url: `/meetingRoom/findById/${id}`,
    method: 'GET'
  })
}

/**
 * 更新会议室
 */
export const updateMeetingRoom = (data) => {
  return request({
    url: '/meetingRoom/update',
    method: 'PUT',
    data
  })
}

/**
 * 删除会议室
 */
export const deleteMeetingRoom = (id) => {
  return request({
    url: `/meetingRoom/deleteById/${id}`,
    method: 'DELETE'
  })
}

/**
 * 查询可用会议室
 */
export const getAvailableRooms = () => {
  return request({
    url: '/meetingRoom/available',
    method: 'GET'
  })
}

/**
 * 更新会议室状态
 */
export const updateRoomStatus = (id, status) => {
  return request({
    url: `/meetingRoom/updateStatus/${id}/${status}`,
    method: 'PUT'
  })
}

/**
 * 按容量查询
 */
export const getRoomsByCapacity = (minCapacity) => {
  return request({
    url: `/meetingRoom/findByCapacity/${minCapacity}`,
    method: 'GET'
  })
}