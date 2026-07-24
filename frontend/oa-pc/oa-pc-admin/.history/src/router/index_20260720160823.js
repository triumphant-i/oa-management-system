import { createRouter, createWebHistory } from 'vue-router'
import { hasPermission, PERMISSIONS } from '@/utils/permission'

// 定义所有路由的权限映射
const routePermissions = {
  '/employee': PERMISSIONS.EMPLOYEE_VIEW,
  '/department': PERMISSIONS.DEPARTMENT_VIEW,
  '/approval': PERMISSIONS.APPROVAL_VIEW,
  '/notice': PERMISSIONS.NOTICE_VIEW,
  '/attendance': PERMISSIONS.ATTENDANCE_VIEW,
  '/meetingRoom': PERMISSIONS.MEETING_VIEW,
  '/workflow': PERMISSIONS.WORKFLOW_VIEW,
  '/statistics': PERMISSIONS.STATISTICS_VIEW,
  '/system': PERMISSIONS.SYSTEM_MENU,
  '/message': PERMISSIONS.MESSAGE_VIEW,
  '/profile': PERMISSIONS.PROFILE_EDIT
}

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/message',
    component: () => import('@/views/Admin.vue'),
    meta: { requiresAuth: true },
    children: [
      {
        path: '',
        name: 'MessageCenter',
        component: () => import('@/views/MessageCenter.vue'),
        meta: { title: '消息中心', permission: PERMISSIONS.MESSAGE_VIEW }
      }
    ]
  },
  {
    path: '/profile',
    component: () => import('@/views/Admin.vue'),
    meta: { requiresAuth: true },
    children: [
      {
        path: '',
        name: 'Profile',
        component: () => import('@/views/Profile.vue'),
        meta: { title: '个人中心', permission: PERMISSIONS.PROFILE_EDIT }
      }
    ]
  },
  {
    path: '/',
    redirect: '/employee'
  },
  {
    path: '/employee',
    component: () => import('@/views/Admin.vue'),
    meta: { requiresAuth: true },
    children: [
      {
        path: '',
        name: 'EmployeeManage',
        component: () => import('@/views/EmployeeManage/Index.vue'),
        meta: { title: '员工管理', permission: PERMISSIONS.EMPLOYEE_VIEW }
      }
    ]
  },
  {
    path: '/department',
    component: () => import('@/views/Admin.vue'),
    meta: { requiresAuth: true },
    children: [
      {
        path: '',
        name: 'DepartmentManage',
        component: () => import('@/views/DepartmentManage/Index.vue'),
        meta: { title: '部门管理', permission: PERMISSIONS.DEPARTMENT_VIEW }
      }
    ]
  },
  {
    path: '/approval',
    component: () => import('@/views/Admin.vue'),
    meta: { requiresAuth: true },
    children: [
      {
        path: '',
        name: 'ApprovalManage',
        component: () => import('@/views/ApprovalManage/Index.vue'),
        meta: { title: '审批管理', permission: PERMISSIONS.APPROVAL_VIEW }
      }
    ]
  },
  {
    path: '/notice',
    component: () => import('@/views/Admin.vue'),
    meta: { requiresAuth: true },
    children: [
      {
        path: '',
        name: 'NoticeManage',
        component: () => import('@/views/NoticeManage/Index.vue'),
        meta: { title: '公告管理', permission: PERMISSIONS.NOTICE_VIEW }
      }
    ]
  },
  {
    path: '/attendance',
    component: () => import('@/views/Admin.vue'),
    meta: { requiresAuth: true },
    children: [
      {
        path: '',
        name: 'AttendanceManage',
        component: () => import('@/views/AttendanceManage/Index.vue'),
        meta: { title: '考勤管理', permission: PERMISSIONS.ATTENDANCE_VIEW }
      }
    ]
  },
  {
    path: '/meetingRoom',
    component: () => import('@/views/Admin.vue'),
    meta: { requiresAuth: true },
    children: [
      {
        path: '',
        name: 'MeetingRoomManage',
        component: () => import('@/views/MeetingRoomManage/Index.vue'),
        meta: { title: '会议室管理', permission: PERMISSIONS.MEETING_VIEW }
      }
    ]
  },
  {
    path: '/workflow',
    component: () => import('@/views/Admin.vue'),
    meta: { requiresAuth: true },
    children: [
      {
        path: '',
        name: 'WorkflowManage',
        component: () => import('@/views/WorkflowManage/Index.vue'),
        meta: { title: '工作流管理', permission: PERMISSIONS.WORKFLOW_VIEW }
      }
    ]
  },
  {
    path: '/statistics',
    component: () => import('@/views/Admin.vue'),
    meta: { requiresAuth: true },
    children: [
      {
        path: '',
        name: 'Statistics',
        component: () => import('@/views/Statistics.vue'),
        meta: { title: '数据统计', permission: PERMISSIONS.STATISTICS_VIEW }
      }
    ]
  },
  {
    path: '/system',
    component: () => import('@/views/Admin.vue'),
    meta: { requiresAuth: true },
    children: [
      {
        path: '',
        name: 'SystemManage',
        component: () => import('@/views/SystemManage/Index.vue'),
        meta: { title: '系统管理', permission: PERMISSIONS.SYSTEM_MENU }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/employee'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫 - 使用新语法（不调用 next）
router.beforeEach((to) => {
  const token = localStorage.getItem('token')
  const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}')

  // 如果需要登录但未登录，跳转到登录页
  if (to.meta.requiresAuth && !token) {
    return '/login'
  }

  // 已登录但访问登录页，跳转到员工管理
  if (token && to.path === '/login') {
    return '/employee'
  }

  // 权限检查
  if (to.meta && to.meta.permission) {
    const hasPerm = hasPermission(to.meta.permission)
    if (!hasPerm) {
      // 没有权限跳转到员工管理页面
      return '/employee'
    }
  }

  // 允许访问
  return true
})

// 捕获路由重复导航错误
const originalPush = router.push
router.push = function push(location) {
  return originalPush.call(this, location).catch(err => {
    if (err.name !== 'NavigationDuplicated') {
      console.error(err)
    }
  })
}

export default router