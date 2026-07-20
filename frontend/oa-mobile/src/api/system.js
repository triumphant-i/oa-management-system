import request from '@/utils/request'

export function getRoleList() {
  return request({
    url: '/role/list',
    method: 'get'
  })
}

export function getRoleById(id) {
  return request({
    url: `/role/findById/${id}`,
    method: 'get'
  })
}

export function saveRole(data) {
  return request({
    url: '/role/save',
    method: 'post',
    data
  })
}

export function updateRole(data) {
  return request({
    url: '/role/update',
    method: 'put',
    data
  })
}

export function deleteRole(id) {
  return request({
    url: `/role/deleteById/${id}`,
    method: 'delete'
  })
}

export function getAllPermissions() {
  return request({
    url: '/role/getAllPermissions',
    method: 'get'
  })
}

export function getOperationLogList() {
  return request({
    url: '/operationLog/list',
    method: 'get'
  })
}

export function saveOperationLog(data) {
  return request({
    url: '/operationLog/save',
    method: 'post',
    data
  })
}

export function clearOperationLog() {
  return request({
    url: '/operationLog/clear',
    method: 'delete'
  })
}

export function countTodayLogs() {
  return request({
    url: '/operationLog/countToday',
    method: 'get'
  })
}

export function getDictList() {
  return request({
    url: '/dict/list',
    method: 'get'
  })
}

export function getDictById(id) {
  return request({
    url: `/dict/findById/${id}`,
    method: 'get'
  })
}

export function getDictByKey(key) {
  return request({
    url: `/dict/findByKey/${key}`,
    method: 'get'
  })
}

export function saveDict(data) {
  return request({
    url: '/dict/save',
    method: 'post',
    data
  })
}

export function updateDict(data) {
  return request({
    url: '/dict/update',
    method: 'put',
    data
  })
}

export function deleteDict(id) {
  return request({
    url: `/dict/deleteById/${id}`,
    method: 'delete'
  })
}