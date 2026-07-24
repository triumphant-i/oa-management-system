// src/config/menu.js
import {
  User, OfficeBuilding, Document, Bell, Calendar,
  Location, Connection, DataAnalysis, ChatDotRound, Setting,
  Tickets, Files, Share
} from '@element-plus/icons-vue'
import { PERMISSIONS } from '@/utils/permission'

/**
 * 菜单配置
 * 每个菜单项包含：
 * - path: 路由路径
 * - title: 显示标题
 * - icon: 图标组件
 * - permission: 需要的权限（无权限则所有用户可见）
 * - children: 子菜单
 */
export const MENU_CONFIG = [
  {
    path: '/employee',
    title: '员工管理',
    icon: User,
    permission: PERMISSIONS.EMPLOYEE_VIEW
  },
  {
    path: '/department',
    title: '部门管理',
    icon: OfficeBuilding,
    permission: PERMISSIONS.DEPARTMENT_VIEW
  },
  {
    path: '/approval',
    title: '审批管理',
    icon: Document,
    permission: PERMISSIONS.APPROVAL_VIEW,
    children: [
      { path: '/approval/my', title: '我的申请' },
      { path: '/approval/pending', title: '待我审批' },
      { path: '/approval/all', title: '全部审批', permission: PERMISSIONS.APPROVAL_ALL }
    ]
  },
  {
    path: '/notice',
    title: '公告管理',
    icon: Bell,
    permission: PERMISSIONS.NOTICE_VIEW
  },
  {
    path: '/attendance',
    title: '考勤管理',
    icon: Calendar,
    permission: PERMISSIONS.ATTENDANCE_VIEW
  },
  {
    path: '/meetingRoom',
    title: '会议室管理',
    icon: Location,
    permission: PERMISSIONS.MEETING_VIEW
  },
  {
    path: '/workflow',
    title: '工作流管理',
    icon: Connection,
    permission: PERMISSIONS.WORKFLOW_VIEW
  },
  {
    path: '/statistics',
    title: '数据统计',
    icon: DataAnalysis,
    permission: PERMISSIONS.STATISTICS_VIEW
  },
  {
    path: '/message',
    title: '消息中心',
    icon: ChatDotRound,
    permission: PERMISSIONS.MESSAGE_VIEW
  },
  {
    path: '/system',
    title: '系统管理',
    icon: Setting,
    permission: PERMISSIONS.SYSTEM_MENU,
    children: [
      { path: '/system/config', title: '系统配置' },
      { path: '/system/log', title: '操作日志' }
    ]
  }
]

/**
 * 根据权限过滤菜单
 */
export function filterMenuByPermission(menus) {
  return menus
    .filter(menu => {
      if (!menu.permission) return true
      return hasPermission(menu.permission)
    })
    .map(menu => {
      if (menu.children) {
        return {
          ...menu,
          children: filterMenuByPermission(menu.children)
        }
      }
      return menu
    })
}