import request from './request'

/**
 * 检查会议室可用性
 */
export const checkRoomAvailability = (params) => {
  return request({
    url: '/api/database/room/availability',
    method: 'GET',
    params
  })
}

/**
 * 批量同步数据到ES
 */
export const batchSyncToES = (params) => {
  return request({
    url: '/api/database/es/sync',
    method: 'POST',
    params
  })
}

/**
 * 获取员工考勤统计
 */
export const getAttendanceStatistics = (employeeId, year, month) => {
  return request({
    url: '/api/database/attendance/statistics',
    method: 'GET',
    params: { employeeId, year, month }
  })
}

/**
 * 获取审批统计报表
 */
export const getApprovalStatistics = (startDate, endDate) => {
  return request({
    url: '/api/database/approval/statistics',
    method: 'GET',
    params: { startDate, endDate }
  })
}