// src/router/index.js
router.beforeEach((to, from) => {
  const token = localStorage.getItem('token')
  const userInfo = getUserInfo()
  
  // 登录页
  if (to.path === '/login') {
    if (token) {
      return '/employee'  // ✅ 直接返回路径，不再使用 next()
    }
    return true
  }
  
  // 检查登录状态
  if (!token) {
    return '/login'
  }
  
  // 检查页面权限
  const requiredPermission = to.meta?.permission
  if (requiredPermission) {
    if (!hasPermission(requiredPermission)) {
      ElMessage.warning('您没有访问该页面的权限')
      return '/employee'
    }
  }
  
  // 更新页面标题
  if (to.meta?.title) {
    document.title = `OA系统 - ${to.meta.title}`
  }
  
  return true
})