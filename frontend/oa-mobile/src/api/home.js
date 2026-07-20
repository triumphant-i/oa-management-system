import request from '@/utils/request'

export function getPendingApprovalCount(approverId, role) {
  return request({
    url: '/approval/pendingCount',
    method: 'get',
    params: {
      approverId,
      role
    }
  })
}

export function getTodayAttendanceStatus(employeeId) {
  return request({
    url: `/attendance/todayStatus/${employeeId}`,
    method: 'get'
  })
}

export function getUnreadCount() {
  return request({
    url: '/announcement/countUnread',
    method: 'get'
  })
}

export function getTodayMeetings(employeeId) {
  return request({
    url: `/meeting/myMeetings/${employeeId}`,
    method: 'get'
  })
}