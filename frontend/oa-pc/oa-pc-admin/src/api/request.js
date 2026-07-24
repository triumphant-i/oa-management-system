// src/api/request.js
import axios from 'axios'
import { ElMessage } from 'element-plus'

const request = axios.create({
  baseURL: 'http://localhost:8080',  // 注意：不要加 /api，因为后端 Controller 已经有 /employee 等路径
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器
request.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`
    }
    console.log('请求:', config.method.toUpperCase(), config.url, config.data || '')
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

// 响应拦截器
request.interceptors.response.use(
  response => {
    const res = response.data
    console.log('响应:', res)
    // 如果返回的 code 不是 0，说明有错误
    if (res.code !== 0 && res.code !== 200) {
      // 如果是权限错误，给出明确提示
      if (res.msg && res.msg.includes('无权')) {
        ElMessage.error(res.msg)
        return Promise.reject(new Error(res.msg))
      }
      ElMessage.error(res.msg || res.message || '请求失败')
      return Promise.reject(new Error(res.msg || res.message || '请求失败'))
    }
    return res
  },
  error => {
    console.error('请求错误:', error)
    if (error.response) {
      const status = error.response.status
      if (status === 401) {
        ElMessage.error('登录已过期，请重新登录')
        localStorage.removeItem('token')
        localStorage.removeItem('userInfo')
        window.location.href = '/login'
      } else if (status === 403) {
        ElMessage.error('没有权限访问，请联系管理员')
      } else if (status === 404) {
        ElMessage.error('接口不存在，请检查后端服务是否启动')
      } else if (status === 500) {
        ElMessage.error('服务器内部错误')
      } else {
        ElMessage.error(error.response.data?.msg || error.response.data?.message || '请求失败')
      }
    } else if (error.code === 'ECONNABORTED') {
      ElMessage.error('请求超时，请检查网络连接')
    } else if (error.message && error.message.includes('Network Error')) {
      ElMessage.error('网络连接失败，请确保后端服务已启动 (http://localhost:8080)')
    } else {
      ElMessage.error(error.message || '请求失败')
    }
    return Promise.reject(error)
  }
)

export default request