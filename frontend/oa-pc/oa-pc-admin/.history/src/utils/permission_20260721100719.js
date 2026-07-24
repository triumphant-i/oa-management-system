// src/utils/permission.js

// ==========================================
// 权限标识定义
// ==========================================
export const PERMISSIONS = {
  EMPLOYEE_VIEW: 'employee:view',
  EMPLOYEE_ADD: 'employee:add',
  EMPLOYEE_EDIT: 'employee:edit',
  EMPLOYEE_DELETE: 'employee:delete',
  EMPLOYEE_IMPORT: 'employee:import',
  EMPLOYEE_STATUS: 'employee:status',
  EMPLOYEE_ROLE: 'employee:role',
  
  DEPARTMENT_VIEW: 'department:view',
  DEPARTMENT_ADD: 'department:add',
  DEPARTMENT_EDIT: 'department:edit',
  DEPARTMENT_DELETE: 'department:delete',
  
  APPROVAL_VIEW: 'approval:view',
  APPROVAL_APPLY: 'approval:apply',
  APPROVAL_AUDIT: 'approval:audit',
  APPROVAL_ALL: 'approval:all',
  
  NOTICE_VIEW: 'notice:view',
  NOTICE_ADD: 'notice:add',
  NOTICE_EDIT: 'notice:edit',
  NOTICE_DELETE: 'notice:delete',
  
  ATTENDANCE_VIEW: 'attendance:view',
  ATTENDANCE_CHECK: 'attendance:check',
  ATTENDANCE_MANAGE: 'attendance:manage',
  
  MEETING_VIEW: 'meeting:view',
  MEETING_RESERVE: 'meeting:reserve',
  MEETING_MANAGE: 'meeting:manage',
  
  MESSAGE_VIEW: 'message:view',
  MESSAGE_SEND: 'message:send',
  
  STATISTICS_VIEW: 'statistics:view',
  
  SYSTEM_MENU: 'system:menu',
  SYSTEM_CONFIG: 'system:config',
  SYSTEM_LOG: 'system:log',
  
  WORKFLOW_VIEW: 'workflow:view'
}

// ==========================================
// 角色权限映射（根据后端角色编码）
// ==========================================
const ROLE_PERMISSIONS = {
  // 系统管理员 - 拥有所有权限
  'SYSTEM_ADMIN': [
    PERMISSIONS.EMPLOYEE_VIEW,
    PERMISSIONS.EMPLOYEE_ADD,
    PERMISSIONS.EMPLOYEE_EDIT,
    PERMISSIONS.EMPLOYEE_DELETE,
    PERMISSIONS.EMPLOYEE_IMPORT,
    PERMISSIONS.EMPLOYEE_STATUS,
    PERMISSIONS.EMPLOYEE_ROLE,
    PERMISSIONS.DEPARTMENT_VIEW,
    PERMISSIONS.DEPARTMENT_ADD,
    PERMISSIONS.DEPARTMENT_EDIT,
    PERMISSIONS.DEPARTMENT_DELETE,
    PERMISSIONS.APPROVAL_VIEW,
    PERMISSIONS.APPROVAL_APPLY,
    PERMISSIONS.APPROVAL_AUDIT,
    PERMISSIONS.APPROVAL_ALL,
    PERMISSIONS.NOTICE_VIEW,
    PERMISSIONS.NOTICE_ADD,
    PERMISSIONS.NOTICE_EDIT,
    PERMISSIONS.NOTICE_DELETE,
    PERMISSIONS.ATTENDANCE_VIEW,
    PERMISSIONS.ATTENDANCE_CHECK,
    PERMISSIONS.ATTENDANCE_MANAGE,
    PERMISSIONS.MEETING_VIEW,
    PERMISSIONS.MEETING_RESERVE,
    PERMISSIONS.MEETING_MANAGE,
    PERMISSIONS.MESSAGE_VIEW,
    PERMISSIONS.MESSAGE_SEND,
    PERMISSIONS.STATISTICS_VIEW,
    PERMISSIONS.SYSTEM_MENU,
    PERMISSIONS.SYSTEM_CONFIG,
    PERMISSIONS.SYSTEM_LOG,
    PERMISSIONS.WORKFLOW_VIEW
  ],
  
  // 部门主管
  'DEPARTMENT_MANAGER': [
    PERMISSIONS.EMPLOYEE_VIEW,
    PERMISSIONS.EMPLOYEE_ADD,
    PERMISSIONS.EMPLOYEE_EDIT,
    PERMISSIONS.EMPLOYEE_DELETE,
    PERMISSIONS.DEPARTMENT_VIEW,
    PERMISSIONS.APPROVAL_VIEW,
    PERMISSIONS.APPROVAL_APPLY,
    PERMISSIONS.APPROVAL_AUDIT,
    PERMISSIONS.NOTICE_VIEW,
    PERMISSIONS.ATTENDANCE_VIEW,
    PERMISSIONS.ATTENDANCE_CHECK,
    PERMISSIONS.MEETING_VIEW,
    PERMISSIONS.MEETING_RESERVE,
    PERMISSIONS.MESSAGE_VIEW,
    PERMISSIONS.STATISTICS_VIEW
  ],
  
  // 普通员工
  'EMPLOYEE': [
    PERMISSIONS.EMPLOYEE_VIEW,
    PERMISSIONS.DEPARTMENT_VIEW,
    PERMISSIONS.APPROVAL_VIEW,
    PERMISSIONS.APPROVAL_APPLY,
    PERMISSIONS.NOTICE_VIEW,
    PERMISSIONS.ATTENDANCE_VIEW,
    PERMISSIONS.ATTENDANCE_CHECK,
    PERMISSIONS.MEETING_VIEW,
    PERMISSIONS.MEETING_RESERVE,
    PERMISSIONS.MESSAGE_VIEW
  ],
  
  // 流程管理员
  'PROCESS_ADMIN': [
    PERMISSIONS.EMPLOYEE_VIEW,
    PERMISSIONS.DEPARTMENT_VIEW,
    PERMISSIONS.APPROVAL_VIEW,
    PERMISSIONS.APPROVAL_AUDIT,
    PERMISSIONS.NOTICE_VIEW,
    PERMISSIONS.ATTENDANCE_VIEW,
    PERMISSIONS.ATTENDANCE_CHECK,
    PERMISSIONS.MEETING_VIEW,
    PERMISSIONS.MEETING_RESERVE,
    PERMISSIONS.MESSAGE_VIEW
  ]
}

// ==========================================
// 核心函数
// ==========================================

/**
 * 获取当前用户信息
 */
export function getUserInfo() {
  try {
    const info = localStorage.getItem('userInfo')
    return info ? JSON.parse(info) : null
  } catch {
    return null
  }
}

/**
 * 获取当前用户角色
 */
export function getUserRole() {
  const userInfo = getUserInfo()
  return userInfo?.role || 'EMPLOYEE'
}

/**
 * 检查是否有指定权限
 * ✅ 核心修复：根据角色判断权限
 */
export function hasPermission(permission) {
  if (!permission) return true
  
  const role = getUserRole()
  
  // 系统管理员拥有所有权限
  if (role === 'SYSTEM_ADMIN') return true
  
  const permissions = ROLE_PERMISSIONS[role] || []
  return permissions.includes(permission)
}

/**
 * 检查是否有任意一个权限
 */
export function hasAnyPermission(permissionList) {
  if (!permissionList || permissionList.length === 0) return true
  return permissionList.some(p => hasPermission(p))
}

/**
 * 检查是否是指定角色
 */
export function isRole(role) {
  return getUserRole() === role
}

/**
 * 检查是否是系统管理员
 */
export function isAdmin() {
  return isRole('SYSTEM_ADMIN')
}