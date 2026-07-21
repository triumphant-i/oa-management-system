// src/router/index.js
import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'
import { hasPermission, getUserInfo } from '@/utils/permission'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/',
    component: () => import('@/views/Layout.vue'),
    redirect: '/dashboard',
    children: [
      // ========== 仪表盘 ==========
      {
        path: '/dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/Dashboard.vue'),
        meta: { 
          title: '工作台',
          icon: 'House'
        }
      },
      // ========== 员工管理 ==========
      {
        path: '/employee',
        name: 'Employee',
        component: () => import('@/views/employee/Employee.vue'),
        meta: { 
          title: '员工管理',
          permission: 'employee:view',
          icon: 'User'
        }
      },
      // ========== 部门管理 ==========
      {
        path: '/department',
        name: 'Department',
        component: () => import('@/views/department/Department.vue'),
        meta: { 
          title: '部门管理',
          permission: 'department:view',
          icon: 'OfficeBuilding'
        }
      },
      // ========== 审批管理 ==========
      {
        path: '/approval',
        name: 'Approval',
        component: () => import('@/views/approval/Approval.vue'),
        meta: { 
          title: '审批管理',
          permission: 'approval:view',
          icon: 'Document'
        }
      },
      // ========== 公告管理 ==========
      {
        path: '/notice',
        name: 'Notice',
        component: () => import('@/views/notice/Notice.vue'),
        meta: { 
          title: '公告管理',
          permission: 'notice:view',
          icon: 'Bell'
        }
      },
      // ========== 考勤管理 ==========
      {
        path: '/attendance',
        name: 'Attendance',
        component: () => import('@/views/attendance/Attendance.vue'),
        meta: { 
          title: '考勤管理',
          permission: 'attendance:view',
          icon: 'Calendar'
        }
      },
      // ========== 会议室管理 ==========
      {
        path: '/meetingRoom',
        name: 'MeetingRoom',
        component: () => import('@/views/meeting/MeetingRoom.vue'),
        meta: { 
          title: '会议室管理',
          permission: 'meeting:view',
          icon: 'Location'
        }
      },
      // ========== 工作流管理 ==========
      {
        path: '/workflow',
        name: 'Workflow',
        component: () => import('@/views/workflow/Workflow.vue'),
        meta: { 
          title: '工作流管理',
          permission: 'workflow:view',
          icon: 'Connection'
        }
      },
      // ========== 数据统计 ==========
      {
        path: '/statistics',
        name: 'Statistics',
        component: () => import('@/views/statistics/Statistics.vue'),
        meta: { 
          title: '数据统计',
          permission: 'statistics:view',
          icon: 'DataAnalysis'
        }
      },
      // ========== 消息中心 ==========
      {
        path: '/message',
        name: 'Message',
        component: () => import('@/views/message/Message.vue'),
        meta: { 
          title: '消息中心',
          permission: 'message:view',
          icon: 'ChatDotRound'
        }
      },
      // ========== 系统管理 ==========
      {
        path: '/system',
        name: 'System',
        component: () => import('@/views/system/System.vue'),
        meta: { 
          title: '系统管理',
          permission: 'system:menu',
          icon: 'Setting'
        }
      },
      // ========== 个人中心 ==========
      {
        path: '/profile',
        name: 'Profile',
        component: () => import('@/views/profile/Profile.vue'),
        meta: { 
          title: '个人中心',
          icon: 'User'
        }
      },
      // ========== 修改密码 ==========
      {
        path: '/changePassword',
        name: 'ChangePassword',
        component: () => import('@/views/profile/ChangePassword.vue'),
        meta: { 
          title: '修改密码',
          icon: 'Lock'
        }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/dashboard'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  const userInfo = getUserInfo()
  
  // 登录页
  if (to.path === '/login') {
    if (token) {
      next('/dashboard')
    } else {
      next()
    }
    return
  }
  
  // 检查登录状态
  if (!token) {
    next('/login')
    return
  }
  
  // 检查页面权限
  const requiredPermission = to.meta?.permission
  if (requiredPermission) {
    if (!hasPermission(requiredPermission)) {
      ElMessage.warning('您没有访问该页面的权限')
      next('/dashboard')
      return
    }
  }
  
  // 更新页面标题
  if (to.meta?.title) {
    document.title = `OA系统 - ${to.meta.title}`
  }
  
  next()
})

export default router