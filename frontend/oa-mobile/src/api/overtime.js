import request from '@/utils/request'

export function overtimeCheckIn(params) {
  return request({
    url: '/overtime/checkIn',
    method: 'post',
    params
  })
}

export function overtimeCheckOut(params) {
  return request({
    url: '/overtime/checkOut',
    method: 'post',
    params
  })
}

export function getTodayOvertime() {
  return request({
    url: '/overtime/today',
    method: 'get'
  })
}

export function getOvertimeList(startDate, endDate) {
  return request({
    url: '/overtime/list',
    method: 'get',
    params: {
      startDate,
      endDate
    }
  })
}

export function getTotalOvertimeDuration(startDate, endDate) {
  return request({
    url: '/overtime/totalDuration',
    method: 'get',
    params: {
      startDate,
      endDate
    }
  })
}
