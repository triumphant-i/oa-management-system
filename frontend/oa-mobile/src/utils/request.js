import axios from 'axios'
import { showToast } from 'vant'
import router from '@/router'

// 创建axios实例
const request = axios.create({
  baseURL: 'http://localhost:8080', // 后端API地址
  timeout: 10000, // 请求超时时间
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器
request.interceptors.request.use(
  config => {
    // 从localStorage获取token
    const token = localStorage.getItem('token')
    if (token) {
      // 在请求头中添加JWT Token
      config.headers['Authorization'] = `Bearer ${token}`
    }
    return config
  },
  error => {
    console.error('请求错误：', error)
    return Promise.reject(error)
  }
)

// 响应拦截器
request.interceptors.response.use(
  response => {
    const res = response.data
    
    // 根据后端返回的code判断请求是否成功
    if (res.code === 0) {
      return res
    } else {
      // 业务错误
      showToast({
        message: res.message || '请求失败',
        position: 'top'
      })
      return Promise.reject(new Error(res.message || '请求失败'))
    }
  },
  error => {
    console.error('响应错误：', error)
    
    if (error.response) {
      switch (error.response.status) {
        case 401:
          // Token过期或未登录
          showToast({
            message: '登录已过期，请重新登录',
            position: 'top'
          })
          // 清除本地存储
          localStorage.removeItem('token')
          localStorage.removeItem('userInfo')
          localStorage.removeItem('isLogin')
          // 跳转到登录页
          router.push('/login')
          break
        case 403:
          showToast({
            message: '没有权限访问',
            position: 'top'
          })
          break
        case 404:
          showToast({
            message: '请求的资源不存在',
            position: 'top'
          })
          break
        case 500:
          showToast({
            message: '服务器错误',
            position: 'top'
          })
          break
        default:
          showToast({
            message: `请求失败：${error.message}`,
            position: 'top'
          })
      }
    } else {
      showToast({
        message: '网络连接失败，请检查网络',
        position: 'top'
      })
    }
    
    return Promise.reject(error)
  }
)

export default request