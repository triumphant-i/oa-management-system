import request from '@/utils/request'

/**
 * 部门管理相关API
 */

// 获取部门列表（所有）
export function getDepartmentList() {
  return request({
    url: '/department/list',
    method: 'get'
  })
}

// 获取部门列表（分页）
export function getDepartmentListPage(page, size) {
  return request({
    url: `/department/list/${page}/${size}`,
    method: 'get'
  })
}

// 根据ID查询部门详情
export function getDepartmentById(id) {
  return request({
    url: `/department/findById/${id}`,
    method: 'get'
  })
}

// 添加部门
export function addDepartment(data) {
  return request({
    url: '/department/save',
    method: 'post',
    data
  })
}

// 更新部门信息
export function updateDepartment(data) {
  return request({
    url: '/department/update',
    method: 'put',
    data
  })
}

// 删除部门
export function deleteDepartment(id) {
  return request({
    url: `/department/deleteById/${id}`,
    method: 'delete'
  })
}

// 查询部门员工数量
export function countDepartmentEmployees(id) {
  return request({
    url: `/department/countEmployees/${id}`,
    method: 'get'
  })
}

// 指定部门负责人
export function assignManager(data) {
  return request({
    url: '/department/assignManager',
    method: 'put',
    data
  })
}

// 员工调岗
export function transferEmployee(data) {
  return request({
    url: '/department/transferEmployee',
    method: 'post',
    data
  })
}

// 获取部门树形结构
export function getDepartmentTree() {
  return request({
    url: '/department/tree',
    method: 'get'
  })
}