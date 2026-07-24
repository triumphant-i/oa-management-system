// src/utils/permission.js

// 权限映射
export const PERMISSIONS = {
  // 仪表盘
  DASHBOARD_VIEW: 'dashboard:view',
  
  // 员工管理
  EMPLOYEE_MENU: 'employee:menu',
  EMPLOYEE_VIEW: 'employee:view',
  EMPLOYEE_ADD: 'employee:add',
  EMPLOYEE_EDIT: 'employee:edit',
  EMPLOYEE_DELETE: 'employee:delete',
  EMPLOYEE_STATUS: 'employee:status',
  EMPLOYEE_ROLE: 'employee:role',
  EMPLOYEE_IMPORT: 'employee:import',
  EMPLOYEE_EXPORT: 'employee:export',
  
  // 部门管理
  DEPARTMENT_MENU: 'department:menu',
  DEPARTMENT_VIEW: 'department:view',
  DEPARTMENT_ADD: 'department:add',
  DEPARTMENT_EDIT: 'department:edit',
  DEPARTMENT_DELETE: 'department:delete',
  
  // 审批管理
  APPROVAL_MENU: 'approval:menu',
  APPROVAL_VIEW: 'approval:view',
  APPROVAL_APPROVE: 'approval:approve',
  APPROVAL_REJECT: 'approval:reject',
  APPROVAL_TRANSFER: 'approval:transfer',
  APPROVAL_WITHDRAW: 'approval:withdraw',
  APPROVAL_SUBMIT: 'approval:submit',
  APPROVAL_REPORT: 'approval:report',
  
  // 公告管理
  NOTICE_MENU: 'notice:menu',
  NOTICE_VIEW: 'notice:view',
  NOTICE_PUBLISH: 'notice:publish',
  NOTICE_TOP: 'notice:top',
  NOTICE_WITHDRAW: 'notice:withdraw',
  NOTICE_DELETE: 'notice:delete',
  
  // 考勤管理
  ATTENDANCE_MENU: 'attendance:menu',
  ATTENDANCE_VIEW: 'attendance:view',
  ATTENDANCE_EXPORT: 'attendance:export',
  ATTENDANCE_APPROVE: 'attendance:approve',
  
  // 会议室管理
  MEETING_MENU: 'meeting:menu',
  MEETING_VIEW: 'meeting:view',
  MEETING_ADD: 'meeting:add',
  MEETING_EDIT: 'meeting:edit',
  MEETING_DELETE: 'meeting:delete',
  MEETING_BOOK: 'meeting:book',
  
  // 工作流管理
  WORKFLOW_MENU: 'workflow:menu',
  WORKFLOW_VIEW: 'workflow:view',
  WORKFLOW_CONFIG: 'workflow:config',
  
  // 数据统计
  STATISTICS_MENU: 'statistics:menu',
  STATISTICS_VIEW: 'statistics:view',
  STATISTICS_EXPORT: 'statistics:export',
  
  // 系统管理
  SYSTEM_MENU: 'system:menu',
  SYSTEM_CONFIG: 'system:config',
  SYSTEM_LOG: 'system:log',
  
  // 消息中心
  MESSAGE_MENU: 'message:menu',
  MESSAGE_VIEW: 'message:view',
  
  // 个人中心
  PROFILE_MENU: 'profile:menu',
  PROFILE_EDIT: 'profile:edit',
  PROFILE_PASSWORD: 'profile:password'
}

// 获取当前用户权限
export function getUserPermissions() {
  try {
    const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}')
    return userInfo.permissions || []
  } catch (e) {
    return []
  }
}

// 检查是否有某个权限
export function hasPermission(permission) {
  try {
    const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}')
    
    // 如果没有用户信息，返回false
    if (!userInfo || !userInfo.role) {
      return false
    }
    
    // 系统管理员拥有所有权限
    if (userInfo.role === 'SYSTEM_ADMIN') {
      return true
    }
    
    const permissions = userInfo.permissions || []
    return permissions.includes(permission)
  } catch (e) {
    return false
  }
}

// 检查是否有多个权限中的任意一个
export function hasAnyPermission(permissionList) {
  return permissionList.some(p => hasPermission(p))
}

// 检查是否有所有权限
export function hasAllPermissions(permissionList) {
  return permissionList.every(p => hasPermission(p))
}

// 根据权限过滤菜单
export function filterMenusByPermission(menus) {
  return menus.filter(menu => {
    if (!menu.permission) return true
    return hasPermission(menu.permission)
  })
}