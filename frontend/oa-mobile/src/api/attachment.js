import request from '@/utils/request'

export function uploadAttachment(file, businessId, businessType = 'approval') {
  const formData = new FormData()
  formData.append('file', file)
  if (businessId) {
    formData.append('businessId', businessId)
  }
  formData.append('businessType', businessType)
  
  return request({
    url: '/attachment/upload',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

export function getAttachmentList(businessId, businessType = 'approval') {
  return request({
    url: '/attachment/list',
    method: 'get',
    params: {
      businessId,
      businessType
    }
  })
}

export function downloadAttachment(id) {
  return request({
    url: `/attachment/download/${id}`,
    method: 'get',
    responseType: 'blob'
  })
}

export function deleteAttachment(id) {
  return request({
    url: `/attachment/delete/${id}`,
    method: 'delete'
  })
}
