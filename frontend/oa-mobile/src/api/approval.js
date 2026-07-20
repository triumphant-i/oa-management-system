import request from '@/utils/request'

export function submitApproval(data) {
  return request({
    url: '/approval/submit',
    method: 'post',
    data
  })
}

export function getMyApplications(applicantId) {
  return request({
    url: `/approval/myApplications/${applicantId}`,
    method: 'get'
  })
}

export function getPendingList(approverId, role) {
  return request({
    url: '/approval/pendingList',
    method: 'get',
    params: {
      approverId,
      role
    }
  })
}

export function getPendingCount(approverId, role) {
  return request({
    url: '/approval/pendingCount',
    method: 'get',
    params: {
      approverId,
      role
    }
  })
}

export function handleApproval(data) {
  return request({
    url: '/approval/approve',
    method: 'put',
    data
  })
}

export function getApprovalDetail(id) {
  return request({
    url: `/approval/detail/${id}`,
    method: 'get'
  })
}

export function getApprovalByStatus(status) {
  return request({
    url: `/approval/findByStatus/${status}`,
    method: 'get'
  })
}

export function withdrawApproval(id) {
  return request({
    url: `/approval/withdraw/${id}`,
    method: 'put'
  })
}