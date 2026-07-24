import request from './request'

// ==================== 消息管理 ====================

/**
 * 分页查询当前登录人的消息
 * 支持按分类筛选：all=全部, todo=待办(is_todo=1且is_read=0), cc=抄送, system=系统通知
 */
export const getMyMessages = (params) => {
  return request({
    url: '/message/list',
    method: 'GET',
    params: {
      category: params.category || 'all',
      page: params.page || 1,
      size: params.size || 10
    }
  })
}

/**
 * 标记单条消息已读（返回jumpUrl）
 */
export const markMessageRead = (messageId) => {
  return request({
    url: `/message/read/${messageId}`,
    method: 'PUT'
  })
}

/**
 * 批量标记当前分类下的所有未读消息为已读
 */
export const markAllMessagesRead = (category) => {
  return request({
    url: '/message/readAll',
    method: 'PUT',
    params: { category: category || 'all' }
  })
}

/**
 * 获取未读待办数量（导航栏角标）
 */
export const getTodoCount = () => {
  return request({
    url: '/message/todoCount',
    method: 'GET'
  })
}

/**
 * 获取消息详情
 */
export const getMessageDetail = (id) => {
  return request({
    url: `/message/detail/${id}`,
    method: 'GET'
  })
}

/**
 * 删除消息
 */
export const deleteMessage = (id) => {
  return request({
    url: `/message/delete/${id}`,
    method: 'DELETE'
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