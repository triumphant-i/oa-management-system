import request from './request'

// ==================== 会议室管理 ====================

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

// ==================== 会议管理（新增） ====================

/**
 * 预约会议
 */
export const bookMeeting = (data) => {
  return request({
    url: '/meeting/book',
    method: 'POST',
    data
  })
}

/**
 * 查询我的会议（我组织的）
 */
export const getMyMeetings = (organizerId) => {
  return request({
    url: `/meeting/myMeetings/${organizerId}`,
    method: 'GET'
  })
}

/**
 * 分页查询所有会议
 */
export const getMeetingList = (page, size) => {
  return request({
    url: `/meeting/list/${page}/${size}`,
    method: 'GET'
  })
}

/**
 * 查询所有会议（管理员用，支持按状态过滤）
 */
export const getAllMeetings = (params) => {
  return request({
    url: '/meeting/all',
    method: 'GET',
    params
  })
}

/**
 * 查询我需要参加的会议（作为参会人被邀请）
 */
export const getMyInvitedMeetings = (employeeId) => {
  return request({
    url: `/meeting/myInvited/${employeeId}`,
    method: 'GET'
  })
}

/**
 * 取消会议
 */
export const cancelMeeting = (id) => {
  return request({
    url: `/meeting/cancel/${id}`,
    method: 'PUT'
  })
}

/**
 * 查询会议室的预约情况
 */
export const getRoomMeetings = (roomId) => {
  return request({
    url: `/meeting/roomMeetings/${roomId}`,
    method: 'GET'
  })
}

/**
 * 更新会议信息
 */
export const updateMeeting = (data) => {
  return request({
    url: '/meeting/update',
    method: 'PUT',
    data
  })
}

/**
 * 删除会议
 */
export const deleteMeeting = (id) => {
  return request({
    url: `/meeting/deleteById/${id}`,
    method: 'DELETE'
  })
}

/**
 * 开始会议
 */
export const startMeeting = (id) => {
  return request({
    url: `/meeting/start/${id}`,
    method: 'PUT'
  })
}

/**
 * 结束会议
 */
export const endMeeting = (id) => {
  return request({
    url: `/meeting/end/${id}`,
    method: 'PUT'
  })
}

/**
 * 延长会议时间
 */
export const extendMeeting = (id, minutes) => {
  return request({
    url: `/meeting/extend/${id}`,
    method: 'PUT',
    params: { minutes }
  })
}

/**
 * 查询当前正在进行的会议
 */
export const getCurrentMeeting = (employeeId) => {
  return request({
    url: `/meeting/current/${employeeId}`,
    method: 'GET'
  })
}

/**
 * 查询即将开始的会议（15分钟内）
 */
export const getUpcomingMeeting = (employeeId) => {
  return request({
    url: `/meeting/upcoming/${employeeId}`,
    method: 'GET'
  })
}

/**
 * 检查会议室某天是否全部约满
 */
export const checkRoomFull = (roomId, date) => {
  return request({
    url: `/meeting/checkFull/${roomId}/${date}`,
    method: 'GET'
  })
}

/**
 * 自动结束过期会议（定时任务调用）
 */
export const autoEndExpiredMeetings = () => {
  return request({
    url: '/meeting/autoEnd',
    method: 'POST'
  })
}

// ==================== 员工相关（参会人选择用） ====================

/**
 * 查询所有在职员工（用于参会人选择）
 */
export const getAllEmployees = () => {
  return request({
    url: '/employee/findAll',
    method: 'GET'
  })
}