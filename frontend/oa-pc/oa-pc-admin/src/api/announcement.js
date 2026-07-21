import request from './request'

/**
 * 发布公告
 */
export const publishAnnouncement = (data) => {
  return request({
    url: '/announcement/publish',
    method: 'POST',
    data
  })
}

/**
 * 查询所有已发布公告
 */
export const getAnnouncementList = () => {
  return request({
    url: '/announcement/list',
    method: 'GET'
  })
}

/**
 * 分页查询公告
 */
export const getAnnouncementPage = (page, size) => {
  return request({
    url: `/announcement/list/${page}/${size}`,
    method: 'GET'
  })
}

/**
 * 根据ID查询公告
 */
export const getAnnouncementById = (id) => {
  return request({
    url: `/announcement/findById/${id}`,
    method: 'GET'
  })
}

/**
 * 更新公告
 */
export const updateAnnouncement = (data) => {
  return request({
    url: '/announcement/update',
    method: 'PUT',
    data
  })
}

/**
 * 删除公告
 */
export const deleteAnnouncement = (id) => {
  return request({
    url: `/announcement/deleteById/${id}`,
    method: 'DELETE'
  })
}

/**
 * 撤回公告
 */
export const withdrawAnnouncement = (id) => {
  return request({
    url: `/announcement/withdraw/${id}`,
    method: 'PUT'
  })
}

/**
 * 置顶公告
 */
export const setTopAnnouncement = (id) => {
  return request({
    url: `/announcement/setTop/${id}`,
    method: 'PUT'
  })
}

/**
 * 取消置顶
 */
export const cancelTopAnnouncement = (id) => {
  return request({
    url: `/announcement/cancelTop/${id}`,
    method: 'PUT'
  })
}

/**
 * 按分类查询
 */
export const getAnnouncementByCategory = (category) => {
  return request({
    url: `/announcement/findByCategory/${category}`,
    method: 'GET'
  })
}