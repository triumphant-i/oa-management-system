// src/router/index.js
import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'
import { hasPermission, getUserInfo, getUserRole } from '@/utils/permission'

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
    redirect: '/approval',  // 默认进入审批管理
    children: [
      {
        path: '/employee',
        name: 'EmployeeManage',
        component: () => import('@/views/EmployeeManage/Index.vue'),
        meta: { 
          title: '员工管理',
          permission: 'employee:view'
        }
      },
      {
        path: '/department',
        name: 'DepartmentManage',
        component: () => import('@/views/DepartmentManage/Index.vue'),
        meta: { 
          title: '部门管理',
          permission: 'department:view'
        }
      },
      {
        path: '/approval',
        name: 'ApprovalManage',
        component: () => import('@/views/ApprovalManage/Index.vue'),
        meta: { 
          title: '审批管理',
          permission: 'approval:view'
        }
      },
      {
        path: '/notice',
        name: 'NoticeManage',
        component: () => import('@/views/NoticeManage/Index.vue'),
        meta: { 
          title: '公告管理',
          permission: 'notice:view'
        }
      },
      {
        path: '/attendance',
        name: 'AttendanceManage',
        component: () => import('@/views/AttendanceManage/Index.vue'),
        meta: { 
          title: '考勤管理',
          permission: 'attendance:view'
        }
      },
      {
        path: '/meetingRoom',
        name: 'MeetingRoomManage',
        component: () => import('@/views/MeetingRoomManage/Index.vue'),
        meta: { 
          title: '会议室管理',
          permission: 'meeting:view'
        }
      },
      // ===== 已删除工作流管理 =====
      // {
      //   path: '/workflow',
      //   name: 'WorkflowManage',
      //   component: () => import('@/views/WorkflowManage/Index.vue'),
      //   meta: { 
      //     title: '工作流管理',
      //     permission: 'workflow:view'
      //   }
      // },
      {
        path: '/message',
        name: 'MessageCenter',
        component: () => import('@/views/MessageCenter.vue'),
        meta: { 
          title: '消息中心',
          permission: 'message:view'
        }
      },
      // ===== 已删除系统管理 =====
      // {
      //   path: '/system',
      //   name: 'SystemManage',
      //   component: () => import('@/views/SystemManage/Index.vue'),
      //   meta: { 
      //     title: '系统管理',
      //     permission: 'system:menu'
      //   }
      // },
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
    redirect: '/approval'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// ==========================================
// 根据角色获取默认首页
// ==========================================
function getDefaultHomePage(role) {
  switch (role) {
    case 'SYSTEM_ADMIN':
      return '/employee'      // 管理员 → 员工管理
    case 'DEPARTMENT_MANAGER':
      return '/approval'      // 部门主管 → 审批管理
    case 'EMPLOYEE':
      return '/approval'      // 普通员工 → 审批管理
    case 'PROCESS_ADMIN':
      return '/approval'      // 流程管理员 → 审批管理
    default:
      return '/approval'
  }
}

// ==========================================
// 路由守卫
// ==========================================
router.beforeEach((to, from) => {
  const token = localStorage.getItem('token')
  const userInfo = getUserInfo()
  
  // 登录页放行
  if (to.path === '/login') {
    if (token) {
      const role = getUserRole()
      return getDefaultHomePage(role)
    }
    return true
  }
  
  // 检查登录状态
  if (!token) {
    return '/login'
  }
  
  // 如果是根路径，根据角色跳转
  if (to.path === '/') {
    const role = getUserRole()
    return getDefaultHomePage(role)
  }
  
  // 检查页面权限
  const requiredPermission = to.meta?.permission
  if (requiredPermission) {
    if (!hasPermission(requiredPermission)) {
      ElMessage.warning('您没有访问该页面的权限')
      const role = getUserRole()
      return getDefaultHomePage(role)
    }
  }
  
  // 更新页面标题
  if (to.meta?.title) {
    document.title = `OA系统 - ${to.meta.title}`
  }
  
  return true
})

export default router