import request from '@/utils/request'

/**
 * 员工相关API
 */

// 员工登录
export function login(data) {
  return request({
    url: '/employee/login',
    method: 'post',
    data
  })
}

// 获取员工列表（分页）
export function getEmployeeList(page, size, departmentId) {
  return request({
    url: `/employee/list/${page}/${size}`,
    method: 'get',
    params: { departmentId }
  })
}

// 根据ID查询员工详情
export function getEmployeeById(id) {
  return request({
    url: `/employee/findById/${id}`,
    method: 'get'
  })
}

// 搜索员工
export function searchEmployee(key, value, page = 1, size = 10) {
  return request({
    url: '/employee/search',
    method: 'get',
    params: { key, value, page, size }
  })
}

// 按部门查询员工
export function getEmployeeByDepartment(departmentId) {
  return request({
    url: `/employee/findByDepartment/${departmentId}`,
    method: 'get'
  })
}

// 添加员工
export function addEmployee(data) {
  return request({
    url: '/employee/save',
    method: 'post',
    data
  })
}

// 更新员工信息
export function updateEmployee(data) {
  return request({
    url: '/employee/update',
    method: 'put',
    data
  })
}

// 删除员工
export function deleteEmployee(id) {
  return request({
    url: `/employee/deleteById/${id}`,
    method: 'delete'
  })
}

// 修改密码
export function updatePassword(data) {
  return request({
    url: '/employee/updatePassword',
    method: 'put',
    data
  })
}

// 更新员工状态
export function updateEmployeeStatus(data) {
  return request({
    url: '/employee/updateStatus',
    method: 'put',
    data
  })
}

// 更新员工角色
export function updateEmployeeRole(data) {
  return request({
    url: '/employee/updateRole',
    method: 'put',
    data
  })
}

// 批量导入员工
export function importEmployees(data) {
  return request({
    url: '/employee/import',
    method: 'post',
    data
  })
}

// 获取部门列表（用于选择部门）
export function getDepartmentList() {
  return request({
    url: '/department/list',
    method: 'get'
  })
}