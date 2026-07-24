import request from './request'

/**
 * 提交申请
 */
export const submitApproval = (data) => {
  return request({
    url: '/approval/submit',
    method: 'POST',
    data
  })
}

/**
 * 查询我的申请
 */
export const getMyApplications = (applicantId) => {
  return request({
    url: `/approval/myApplications/${applicantId}`,
    method: 'GET'
  })
}

/**
 * 待审批列表（管理员）
 */
export const getPendingList = () => {
  return request({
    url: '/approval/pendingList',
    method: 'GET'
  })
}

/**
 * 分页查询所有申请
 */
export const getApprovalPage = (page, size) => {
  return request({
    url: `/approval/list/${page}/${size}`,
    method: 'GET'
  })
}

/**
 * 根据状态查询
 */
export const getApprovalByStatus = (status) => {
  return request({
    url: `/approval/findByStatus/${status}`,
    method: 'GET'
  })
}

/**
 * 根据类型查询
 */
export const getApprovalByType = (type) => {
  return request({
    url: `/approval/findByType/${type}`,
    method: 'GET'
  })
}

/**
 * 审批处理（同意/拒绝）
 */
export const handleApproval = (data) => {
  return request({
    url: '/approval/approve',
    method: 'PUT',
    data
  })
}

/**
 * 撤回申请
 */
export const withdrawApproval = (id) => {
  return request({
    url: `/approval/withdraw/${id}`,
    method: 'PUT'
  })
}

/**
 * 获取审批详情
 */
export const getApprovalDetail = (id) => {
  return request({
    url: `/approval/detail/${id}`,
    method: 'GET'
  })
}