// src/utils/permission.js

// ==========================================
// 权限标识定义
// ==========================================
export const PERMISSIONS = {
  // 员工管理
  EMPLOYEE_VIEW: 'employee:view',
  EMPLOYEE_ADD: 'employee:add',
  EMPLOYEE_EDIT: 'employee:edit',
  EMPLOYEE_DELETE: 'employee:delete',
  EMPLOYEE_IMPORT: 'employee:import',
  EMPLOYEE_STATUS: 'employee:status',
  EMPLOYEE_ROLE: 'employee:role',
  
  // 部门管理
  DEPARTMENT_VIEW: 'department:view',
  DEPARTMENT_ADD: 'department:add',
  DEPARTMENT_EDIT: 'department:edit',
  DEPARTMENT_DELETE: 'department:delete',
  
  // 审批管理
  APPROVAL_VIEW: 'approval:view',
  APPROVAL_APPLY: 'approval:apply',
  APPROVAL_AUDIT: 'approval:audit',
  APPROVAL_ALL: 'approval:all',
  
  // 公告管理
  NOTICE_VIEW: 'notice:view',
  NOTICE_ADD: 'notice:add',
  NOTICE_EDIT: 'notice:edit',
  NOTICE_DELETE: 'notice:delete',
  
  // 考勤管理
  ATTENDANCE_VIEW: 'attendance:view',
  ATTENDANCE_CHECK: 'attendance:check',      // 普通员工：打卡
  ATTENDANCE_MANAGE: 'attendance:manage',    // 管理员：考勤管理
  
  // 会议室管理
  MEETING_VIEW: 'meeting:view',
  MEETING_RESERVE: 'meeting:reserve',        // 普通员工：预约
  MEETING_MANAGE: 'meeting:manage',          // 管理员：管理会议室
  
  // 消息
  MESSAGE_VIEW: 'message:view',
  MESSAGE_SEND: 'message:send',
  
  // 统计
  STATISTICS_VIEW: 'statistics:view',
  
  // 系统管理
  SYSTEM_MENU: 'system:menu',
  SYSTEM_CONFIG: 'system:config',
  SYSTEM_LOG: 'system:log',
  
  // 工作流
  WORKFLOW_VIEW: 'workflow:view'
}

// ==========================================
// 角色权限映射
// ==========================================
const ROLE_PERMISSIONS = {
  // ========================================
  // 系统管理员 - 拥有所有权限
  // ========================================
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
  
  // ========================================
  // 部门主管 - 管理本部门
  // ========================================
  'DEPARTMENT_MANAGER': [
    // 员工管理：查看、添加、编辑、删除（本部门）
    PERMISSIONS.EMPLOYEE_VIEW,
    PERMISSIONS.EMPLOYEE_ADD,
    PERMISSIONS.EMPLOYEE_EDIT,
    PERMISSIONS.EMPLOYEE_DELETE,
    // 部门管理：只能查看
    PERMISSIONS.DEPARTMENT_VIEW,
    // 审批管理：查看、申请、审批
    PERMISSIONS.APPROVAL_VIEW,
    PERMISSIONS.APPROVAL_APPLY,
    PERMISSIONS.APPROVAL_AUDIT,
    // 公告管理：只能查看
    PERMISSIONS.NOTICE_VIEW,
    // 考勤管理：查看、打卡、管理
    PERMISSIONS.ATTENDANCE_VIEW,
    PERMISSIONS.ATTENDANCE_CHECK,
    PERMISSIONS.ATTENDANCE_MANAGE,
    // 会议室管理：查看、预约、管理
    PERMISSIONS.MEETING_VIEW,
    PERMISSIONS.MEETING_RESERVE,
    PERMISSIONS.MEETING_MANAGE,
    // 消息
    PERMISSIONS.MESSAGE_VIEW,
    // 统计
    PERMISSIONS.STATISTICS_VIEW
  ],
  
  // ========================================
  // 普通员工 - 基本操作
  // ========================================
  'EMPLOYEE': [
    // ✅ 员工管理：只能查看（自己的信息）
    PERMISSIONS.EMPLOYEE_VIEW,
    // ✅ 部门管理：只能查看（本部门）
    PERMISSIONS.DEPARTMENT_VIEW,
    // ✅ 审批管理：查看、申请
    PERMISSIONS.APPROVAL_VIEW,
    PERMISSIONS.APPROVAL_APPLY,
    // ✅ 公告管理：只能查看
    PERMISSIONS.NOTICE_VIEW,
    // ✅ 考勤管理：查看、打卡（不能管理）
    PERMISSIONS.ATTENDANCE_VIEW,
    PERMISSIONS.ATTENDANCE_CHECK,
    // ❌ 不能考勤管理
    // ✅ 会议室管理：查看、预约（不能管理）
    PERMISSIONS.MEETING_VIEW,
    PERMISSIONS.MEETING_RESERVE,
    // ❌ 不能会议室管理
    // ✅ 消息
    PERMISSIONS.MESSAGE_VIEW,
    // ❌ 不能统计
    // ❌ 不能系统管理
    // ❌ 不能工作流
  ],
  
  // ========================================
  // 流程管理员
  // ========================================
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
// 角色名称映射
// ==========================================
export const ROLE_NAME_MAP = {
  'SYSTEM_ADMIN': '超级管理员',
  'DEPARTMENT_MANAGER': '部门主管',
  'PROCESS_ADMIN': '流程管理员',
  'EMPLOYEE': '普通员工'
}

// ==========================================
// 工具函数
// ==========================================

export function getUserInfo() {
  try {
    const info = localStorage.getItem('userInfo')
    return info ? JSON.parse(info) : null
  } catch {
    return null
  }
}

export function getUserRole() {
  const userInfo = getUserInfo()
  return userInfo?.role || 'EMPLOYEE'
}

export function getRoleName(roleCode) {
  return ROLE_NAME_MAP[roleCode] || '员工'
}

export function hasPermission(permission) {
  if (!permission) return true
  
  const role = getUserRole()
  
  // 系统管理员拥有所有权限
  if (role === 'SYSTEM_ADMIN') return true
  
  const permissions = ROLE_PERMISSIONS[role] || []
  return permissions.includes(permission)
}

export function hasAnyPermission(permissionList) {
  if (!permissionList || permissionList.length === 0) return true
  return permissionList.some(p => hasPermission(p))
}

export function isRole(role) {
  return getUserRole() === role
}

export function isAdmin() {
  return isRole('SYSTEM_ADMIN')
}

export function isDepartmentManager() {
  return isRole('DEPARTMENT_MANAGER')
}

export function isEmployee() {
  return isRole('EMPLOYEE')
}