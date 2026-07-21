import request from './request'

/**
 * 员工登录
 */
export const employeeLogin = (data) => {
  return request({
    url: '/employee/login',
    method: 'POST',
    data
  })
}

/**
 * 添加员工
 */
export const saveEmployee = (data) => {
  return request({
    url: '/employee/save',
    method: 'POST',
    data
  })
}

/**
 * 根据ID查询员工
 */
export const getEmployeeById = (id) => {
  return request({
    url: `/employee/findById/${id}`,
    method: 'GET'
  })
}

/**
 * 员工列表（分页）
 */
export const getEmployeePage = (page, size, departmentId) => {
  let url = `/employee/list/${page}/${size}`
  if (departmentId) {
    url += `?departmentId=${departmentId}`
  }
  return request({
    url,
    method: 'GET'
  })
}

/**
 * 搜索员工
 */
export const searchEmployee = (key, value, page, size) => {
  return request({
    url: '/employee/search',
    method: 'GET',
    params: { key, value, page, size }
  })
}

/**
 * 更新员工信息
 */
export const updateEmployee = (data) => {
  return request({
    url: '/employee/update',
    method: 'PUT',
    data
  })
}

/**
 * 删除员工
 */
export const deleteEmployee = (id) => {
  return request({
    url: `/employee/deleteById/${id}`,
    method: 'DELETE'
  })
}

/**
 * 按部门查询员工
 */
export const getEmployeesByDepartment = (departmentId) => {
  return request({
    url: `/employee/findByDepartment/${departmentId}`,
    method: 'GET'
  })
}

/**
 * 修改密码
 */
export const updatePassword = (data) => {
  return request({
    url: '/employee/updatePassword',
    method: 'PUT',
    data
  })
}

/**
 * 更新员工状态
 */
export const updateEmployeeStatus = (data) => {
  return request({
    url: '/employee/updateStatus',
    method: 'PUT',
    data
  })
}

/**
 * 更新员工角色
 */
export const updateEmployeeRole = (data) => {
  return request({
    url: '/employee/updateRole',
    method: 'PUT',
    data
  })
}

/**
 * 上传头像
 */
export const uploadAvatar = (employeeId, file) => {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('employeeId', employeeId)
  return request({
    url: '/employee/uploadAvatar',
    method: 'POST',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}