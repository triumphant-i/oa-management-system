import { createRouter, createWebHistory } from 'vue-router'
import { hasPermission, getUserRole } from '@/utils/permission'

// ... 路由配置

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  
  // 登录页放行
  if (to.path === '/login') {
    if (token) {
      next('/employee')
    } else {
      next()
    }
    return
  }
  
  // 检查是否登录
  if (!token) {
    next('/login')
    return
  }
  
  // 检查页面权限
  const requiredPermission = to.meta?.permission
  if (requiredPermission) {
    if (!hasPermission(requiredPermission)) {
      // 无权限，跳转到员工页面
      next('/employee')
      return
    }
  }
  
  next()
})