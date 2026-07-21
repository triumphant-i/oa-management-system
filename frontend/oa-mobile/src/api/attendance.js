import request from '@/utils/request'

export function checkIn(data) {
  return request({
    url: '/attendance/checkIn',
    method: 'post',
    data
  })
}

export function checkOut(data) {
  return request({
    url: '/attendance/checkOut',
    method: 'post',
    data
  })
}

export function getTodayStatus(employeeId) {
  return request({
    url: `/attendance/todayStatus/${employeeId}`,
    method: 'get'
  })
}

export function getMyRecords(employeeId) {
  return request({
    url: `/attendance/myRecords/${employeeId}`,
    method: 'get'
  })
}

export function getAllRecords(page, size) {
  return request({
    url: `/attendance/list/${page}/${size}`,
    method: 'get'
  })
}

export function findByDate(date) {
  return request({
    url: `/attendance/findByDate/${date}`,
    method: 'get'
  })
}

export function getCompanyLocation() {
  return request({
    url: '/attendance/companyLocation',
    method: 'get'
  })
}

export function calculateDistance(latitude, longitude) {
  return request({
    url: `/attendance/calculateDistance/${latitude}/${longitude}`,
    method: 'get'
  })
}

export function getCorrectionList(employeeId) {
  return request({
    url: `/attendance/correctionList/${employeeId}`,
    method: 'get'
  })
}