// src/router/index.js
import { createRouter, createWebHistory } from 'vue-router'
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
    redirect: '/employee',
    children: [
      {
        path: '/employee',
        name: 'Employee',
        component: () => import('@/views/employee/Employee.vue'),
        meta: { 
          title: '员工管理',
          permission: 'employee:view'
        }
      },
      {
        path: '/department',
        name: 'Department',
        component: () => import('@/views/department/Department.vue'),
        meta: { 
          title: '部门管理',
          permission: 'department:view'
        }
      },
      {
        path: '/approval',
        name: 'Approval',
        component: () => import('@/views/approval/Approval.vue'),
        meta: { 
          title: '审批管理',
          permission: 'approval:view'
        }
      },
      {
        path: '/notice',
        name: 'Notice',
        component: () => import('@/views/notice/Notice.vue'),
        meta: { 
          title: '公告管理',
          permission: 'notice:view'
        }
      },
      {
        path: '/attendance',
        name: 'Attendance',
        component: () => import('@/views/attendance/Attendance.vue'),
        meta: { 
          title: '考勤管理',
          permission: 'attendance:view'
        }
      },
      {
        path: '/meetingRoom',
        name: 'MeetingRoom',
        component: () => import('@/views/meeting/MeetingRoom.vue'),
        meta: { 
          title: '会议室管理',
          permission: 'meeting:view'
        }
      },
      {
        path: '/workflow',
        name: 'Workflow',
        component: () => import('@/views/workflow/Workflow.vue'),
        meta: { 
          title: '工作流管理',
          permission: 'workflow:view'
        }
      },
      {
        path: '/statistics',
        name: 'Statistics',
        component: () => import('@/views/statistics/Statistics.vue'),
        meta: { 
          title: '数据统计',
          permission: 'statistics:view'
        }
      },
      {
        path: '/message',
        name: 'Message',
        component: () => import('@/views/message/Message.vue'),
        meta: { 
          title: '消息中心',
          permission: 'message:view'
        }
      },
      {
        path: '/system',
        name: 'System',
        component: () => import('@/views/system/System.vue'),
        meta: { 
          title: '系统管理',
          permission: 'system:menu'
        }
      },
      {
        path: '/profile',
        name: 'Profile',
        component: () => import('@/views/profile/Profile.vue'),
        meta: { title: '个人中心' }
      },
      {
        path: '/changePassword',
        name: 'ChangePassword',
        component: () => import('@/views/profile/ChangePassword.vue'),
        meta: { title: '修改密码' }
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

// 路由守卫
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  const userInfo = getUserInfo()
  
  // 登录页
  if (to.path === '/login') {
    if (token) {
      next('/employee')
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
      next('/employee')
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