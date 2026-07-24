// src/router/index.js
import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'
import { hasPermission, getUserInfo } from '@/utils/permission'

// 导入布局组件
import Layout from '@/views/Layout.vue'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/',
    component: Layout,
    redirect: '/dashboard',
    children: [
      // ========== 仪表盘 ==========
      {
        path: '/dashboard',
        name: 'Dashboard',
        component: () => import('@/views/Dashboard.vue'),
        meta: { 
          title: '工作台',
          permission: null  // 所有用户可见
        }
      },
      // ========== 员工管理 ==========
      {
        path: '/employee',
        name: 'EmployeeManage',
        component: () => import('@/views/EmployeeManage/Index.vue'),
        meta: { 
          title: '员工管理',
          permission: 'employee:view'
        }
      },
      // ========== 部门管理 ==========
      {
        path: '/department',
        name: 'DepartmentManage',
        component: () => import('@/views/DepartmentManage/Index.vue'),
        meta: { 
          title: '部门管理',
          permission: 'department:view'
        }
      },
      // ========== 审批管理 ==========
      {
        path: '/approval',
        name: 'ApprovalManage',
        component: () => import('@/views/ApprovalManage/Index.vue'),
        meta: { 
          title: '审批管理',
          permission: 'approval:view'
        }
      },
      // ========== 公告管理 ==========
      {
        path: '/notice',
        name: 'NoticeManage',
        component: () => import('@/views/NoticeManage/Index.vue'),
        meta: { 
          title: '公告管理',
          permission: 'notice:view'
        }
      },
      // ========== 考勤管理 ==========
      {
        path: '/attendance',
        name: 'AttendanceManage',
        component: () => import('@/views/AttendanceManage/Index.vue'),
        meta: { 
          title: '考勤管理',
          permission: 'attendance:view'
        }
      },
      // ========== 会议室管理 ==========
      {
        path: '/meetingRoom',
        name: 'MeetingRoomManage',
        component: () => import('@/views/MeetingRoomManage/Index.vue'),
        meta: { 
          title: '会议室管理',
          permission: 'meeting:view'
        }
      },
      // ========== 工作流管理 ==========
      {
        path: '/workflow',
        name: 'WorkflowManage',
        component: () => import('@/views/WorkflowManage/Index.vue'),
        meta: { 
          title: '工作流管理',
          permission: 'workflow:view'
        }
      },
      // ========== 数据统计 ==========
      {
        path: '/statistics',
        name: 'Statistics',
        component: () => import('@/views/Statistics.vue'),
        meta: { 
          title: '数据统计',
          permission: 'statistics:view'
        }
      },
      // ========== 消息中心 ==========
      {
        path: '/message',
        name: 'MessageCenter',
        component: () => import('@/views/MessageCenter.vue'),
        meta: { 
          title: '消息中心',
          permission: 'message:view'
        }
      },
      // ========== 系统管理 ==========
      {
        path: '/system',
        name: 'SystemManage',
        component: () => import('@/views/SystemManage/Index.vue'),
        meta: { 
          title: '系统管理',
          permission: 'system:menu'
        }
      },
      // ========== 个人中心 ==========
      {
        path: '/profile',
        name: 'Profile',
        component: () => import('@/views/Profile.vue'),
        meta: { 
          title: '个人中心'
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