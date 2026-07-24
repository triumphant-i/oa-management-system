import request from './request'

/**
 * 获取我的消息列表
 * @param {Object} params - 查询参数
 * @param {number} params.receiverId - 接收人ID
 * @param {string[]} params.bizTypes - 业务类型数组 ['approval', 'meeting']
 * @param {number} params.page - 页码
 * @param {number} params.size - 每页大小
 */
export const getMyMessages = (params) => {
  return request({
    url: '/message/myMessages',
    method: 'GET',
    params
  })
}

/**
 * 标记消息为已读
 * @param {number} id - 消息ID
 */
export const markMessageRead = (id) => {
  return request({
    url: `/message/markRead/${id}`,
    method: 'PUT'
  })
}

/**
 * 全部标记为已读
 * @param {number} receiverId - 接收人ID
 */
export const markAllMessagesRead = (receiverId) => {
  return request({
    url: `/message/markAllRead/${receiverId}`,
    method: 'PUT'
  })
}

/**
 * 清空已读消息
 * @param {number} receiverId - 接收人ID
 */
export const deleteReadMessages = (receiverId) => {
  return request({
    url: `/message/deleteRead/${receiverId}`,
    method: 'DELETE'
  })
}

/**
 * 获取消息详情
 * @param {number} id - 消息ID
 */
export const getMessageDetail = (id) => {
  return request({
    url: `/message/detail/${id}`,
    method: 'GET'
  })
}

/**
 * 获取消息统计数据
 * @param {number} receiverId - 接收人ID
 */
export const getMessageStats = (receiverId) => {
  return request({
    url: `/message/stats/${receiverId}`,
    method: 'GET'
  })
}

/**
 * 发送消息
 */
export const sendMessage = (data) => {
  return request({
    url: '/message/send',
    method: 'POST',
    data
  })
}

/**
 * 获取待办消息数量（用于导航栏红点）
 */
export const getTodoCount = (receiverId) => {
  return request({
    url: `/message/todoCount/${receiverId}`,
    method: 'GET'
  })
}