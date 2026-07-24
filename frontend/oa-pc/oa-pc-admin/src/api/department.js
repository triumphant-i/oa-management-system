import request from './request'

/**
 * 添加部门
 */
export const saveDepartment = (data) => {
  return request({
    url: '/department/save',
    method: 'POST',
    data
  })
}

/**
 * 查询所有部门
 */
export const getDepartmentList = () => {
  return request({
    url: '/department/list',
    method: 'GET'
  })
}

/**
 * 分页查询部门
 */
export const getDepartmentPage = (page, size) => {
  return request({
    url: `/department/list/${page}/${size}`,
    method: 'GET'
  })
}

/**
 * 根据ID查询部门
 */
export const getDepartmentById = (id) => {
  return request({
    url: `/department/findById/${id}`,
    method: 'GET'
  })
}

/**
 * 更新部门
 */
export const updateDepartment = (data) => {
  return request({
    url: '/department/update',
    method: 'PUT',
    data
  })
}

/**
 * 删除部门
 */
export const deleteDepartment = (id) => {
  return request({
    url: `/department/deleteById/${id}`,
    method: 'DELETE'
  })
}

/**
 * 查询部门员工数量
 */
export const countDepartmentEmployees = (id) => {
  return request({
    url: `/department/countEmployees/${id}`,
    method: 'GET'
  })
}

/**
 * 指定部门负责人
 */
export const assignManager = (data) => {
  return request({
    url: '/department/assignManager',
    method: 'PUT',
    data
  })
}

/**
 * 员工调岗
 */
export const transferEmployee = (data) => {
  return request({
    url: '/department/transferEmployee',
    method: 'POST',
    data
  })
}

/**
 * 获取部门树形结构
 */
export const getDepartmentTree = () => {
  return request({
    url: '/department/tree',
    method: 'GET'
  })
}