import request from './request'

/**
 * 签到
 * @param {Object} data - { employeeId, employeeName }
 */
export const checkIn = (data) => {
  return request({
    url: '/attendance/checkIn',
    method: 'POST',
    data
  })
}

/**
 * 签退
 * @param {Object} data - { employeeId }
 */
export const checkOut = (data) => {
  return request({
    url: '/attendance/checkOut',
    method: 'POST',
    data
  })
}

/**
 * 查询今日考勤状态
 * @param {Number} employeeId - 员工ID
 */
export const getTodayStatus = (employeeId) => {
  return request({
    url: `/attendance/todayStatus/${employeeId}`,
    method: 'GET'
  })
}

/**
 * 查询员工个人考勤记录
 * @param {Number} employeeId - 员工ID
 */
export const getMyAttendanceRecords = (employeeId) => {
  return request({
    url: `/attendance/myRecords/${employeeId}`,
    method: 'GET'
  })
}

/**
 * 查询所有考勤记录（管理员）
 * @param {Number} page - 页码
 * @param {Number} size - 每页条数
 */
export const getAttendancePage = (page, size) => {
  return request({
    url: `/attendance/list/${page}/${size}`,
    method: 'GET'
  })
}

/**
 * 查询所有考勤记录（不分页，用于导出）
 */
export const getAllAttendance = () => {
  return request({
    url: '/attendance/list/all',
    method: 'GET'
  })
}

/**
 * 按日期查询考勤（管理员）
 * @param {String} date - 日期 YYYY-MM-DD
 */
export const getAttendanceByDate = (date) => {
  return request({
    url: `/attendance/findByDate/${date}`,
    method: 'GET'
  })
}

/**
 * 补卡申请
 * @param {Object} data - { employeeId, employeeName, date, checkInTime, remark }
 */
export const applyForCorrection = (data) => {
  return request({
    url: '/attendance/applyForCorrection',
    method: 'POST',
    data
  })
}