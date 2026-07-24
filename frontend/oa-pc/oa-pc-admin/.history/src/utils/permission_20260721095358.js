// src/utils/permission.js

// 权限标识定义
export const PERMISSIONS = {
  EMPLOYEE_VIEW: 'employee:view',
  EMPLOYEE_MANAGE: 'employee:manage',
  DEPARTMENT_VIEW: 'department:view',
  DEPARTMENT_MANAGE: 'department:manage',
  APPROVAL_VIEW: 'approval:view',
  APPROVAL_ALL: 'approval:all',
  NOTICE_VIEW: 'notice:view',
  NOTICE_MANAGE: 'notice:manage',
  ATTENDANCE_VIEW: 'attendance:view',
  ATTENDANCE_CHECK: 'attendance:check',
  MEETING_VIEW: 'meeting:view',
  MEETING_RESERVE: 'meeting:reserve',
  WORKFLOW_VIEW: 'workflow:view',
  STATISTICS_VIEW: 'statistics:view',
  MESSAGE_VIEW: 'message:view',
  SYSTEM_MENU: 'system:menu'
}

// 角色权限映射（基于你后端定义的3种角色）
const ROLE_PERMISSIONS = {
  'SYSTEM_ADMIN': [
    PERMISSIONS.EMPLOYEE_VIEW,
    PERMISSIONS.EMPLOYEE_MANAGE,
    PERMISSIONS.DEPARTMENT_VIEW,
    PERMISSIONS.DEPARTMENT_MANAGE,
    PERMISSIONS.APPROVAL_VIEW,
    PERMISSIONS.APPROVAL_ALL,
    PERMISSIONS.NOTICE_VIEW,
    PERMISSIONS.NOTICE_MANAGE,
    PERMISSIONS.ATTENDANCE_VIEW,
    PERMISSIONS.ATTENDANCE_CHECK,
    PERMISSIONS.MEETING_VIEW,
    PERMISSIONS.MEETING_RESERVE,
    PERMISSIONS.WORKFLOW_VIEW,
    PERMISSIONS.STATISTICS_VIEW,
    PERMISSIONS.MESSAGE_VIEW,
    PERMISSIONS.SYSTEM_MENU
  ],
  'DEPARTMENT_MANAGER': [
    PERMISSIONS.EMPLOYEE_VIEW,      // 只能看本部门
    PERMISSIONS.DEPARTMENT_VIEW,    // 只能看本部门
    PERMISSIONS.APPROVAL_VIEW,      // 只能看本部门审批
    PERMISSIONS.NOTICE_VIEW,
    PERMISSIONS.ATTENDANCE_VIEW,    // 只能看本部门考勤
    PERMISSIONS.ATTENDANCE_CHECK,
    PERMISSIONS.MEETING_VIEW,
    PERMISSIONS.MEETING_RESERVE,
    PERMISSIONS.MESSAGE_VIEW
  ],
  'EMPLOYEE': [
    PERMISSIONS.EMPLOYEE_VIEW,      // 只能看自己
    PERMISSIONS.NOTICE_VIEW,
    PERMISSIONS.ATTENDANCE_CHECK,   // 只能自己打卡
    PERMISSIONS.MEETING_VIEW,
    PERMISSIONS.MEETING_RESERVE,
    PERMISSIONS.MESSAGE_VIEW
  ]
}

// 获取用户角色
export function getUserRole() {
  try {
    const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}')
    return userInfo.role || 'EMPLOYEE'
  } catch {
    return 'EMPLOYEE'
  }
}

// 检查是否有指定权限
export function hasPermission(permission) {
  const role = getUserRole()
  const permissions = ROLE_PERMISSIONS[role] || []
  return permissions.includes(permission)
}

// 检查是否有任意一个权限
export function hasAnyPermission(permissionList) {
  return permissionList.some(p => hasPermission(p))
}

// 检查是否有所有权限
export function hasAllPermissions(permissionList) {
  return permissionList.every(p => hasPermission(p))
}