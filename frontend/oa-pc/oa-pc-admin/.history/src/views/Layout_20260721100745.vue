<template>
  <el-container class="admin-layout">
    <!-- 侧边栏 -->
    <el-aside :width="isCollapse ? '64px' : '220px'" class="sidebar">
      <div class="logo-wrap">
        <span class="logo-icon">OA</span>
        <span v-if="!isCollapse" class="logo-text">智慧OA</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        :collapse="isCollapse"
        :collapse-transition="false"
        router
        background-color="#001529"
        text-color="#bfbfbf"
        active-text-color="#409eff"
        class="menu"
      >
        <!-- 动态渲染菜单 - 只显示有权限的 -->
        <template v-for="item in menuList" :key="item.path">
          <el-menu-item :index="item.path">
            <el-icon><component :is="item.icon" /></el-icon>
            <span>{{ item.title }}</span>
          </el-menu-item>
        </template>
      </el-menu>
      <div class="collapse-btn" @click="toggleCollapse">
        <el-icon><Fold v-if="!isCollapse" /><Expand v-else /></el-icon>
      </div>
    </el-aside>

    <!-- 主内容 -->
    <el-container class="main-container">
      <!-- 头部 -->
      <el-header class="header">
        <div class="header-left">
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item>{{ currentTitle }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <el-badge :value="unreadCount" class="badge" @click="goToMessage" style="cursor:pointer;">
            <el-icon :size="20"><Bell /></el-icon>
          </el-badge>
          <el-dropdown @command="handleCommand">
            <span class="user-info">
              <el-avatar :size="32" :src="userInfo?.avatar || ''">
                {{ userInfo?.name?.charAt(0) || '管' }}
              </el-avatar>
              <span class="username">{{ userInfo?.name || '管理员' }}</span>
              <span class="role-tag">{{ roleLabel }}</span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <!-- 内容 -->
      <el-main class="content">
        <router-view :key="$route.fullPath" />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Fold, Expand, Bell, ArrowDown,
  User, OfficeBuilding, Document,
  Calendar, Location, Connection, Setting,
  DataAnalysis, ChatDotRound
} from '@element-plus/icons-vue'
import { hasPermission, PERMISSIONS, getUserInfo, getUserRole } from '@/utils/permission'

const route = useRoute()
const router = useRouter()
const isCollapse = ref(false)
const userInfo = ref({})
const unreadCount = ref(5)

// 角色标签映射
const roleLabelMap = {
  'SYSTEM_ADMIN': '超级管理员',
  'GENERAL_MANAGER': '总经理',
  'DEPARTMENT_MANAGER': '部门主管',
  'PROCESS_ADMIN': '流程管理员',
  'EMPLOYEE': '普通员工'
}

const roleLabel = computed(() => {
  const role = userInfo.value?.role || 'EMPLOYEE'
  return roleLabelMap[role] || '员工'
})

// ==========================================
// 所有菜单配置
// ==========================================
const allMenus = [
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
    permission: PERMISSIONS.APPROVAL_VIEW
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
    permission: PERMISSIONS.SYSTEM_MENU
  }
]

// ==========================================
// ✅ 核心：根据权限过滤菜单
// ==========================================
const menuList = computed(() => {
  const role = getUserRole()
  console.log('========== 菜单过滤 ==========')
  console.log('当前用户角色:', role)
  
  const filtered = allMenus.filter(menu => {
    // 如果没有权限要求，显示
    if (!menu.permission) return true
    
    // 检查是否有权限
    const hasPerm = hasPermission(menu.permission)
    console.log(`菜单 "${menu.title}" (${menu.permission}): ${hasPerm ? '✅ 显示' : '❌ 隐藏'}`)
    return hasPerm
  })
  
  console.log('最终显示菜单:', filtered.map(m => m.title))
  console.log('===============================')
  
  return filtered
})

const activeMenu = computed(() => route.path)

const currentTitle = computed(() => {
  if (route.meta && route.meta.title) {
    return route.meta.title
  }
  const item = allMenus.find(m => route.path.startsWith(m.path))
  return item?.title || '页面'
})

const toggleCollapse = () => {
  isCollapse.value = !isCollapse.value
}

const goToMessage = () => {
  router.push('/message')
}

const handleCommand = (command) => {
  if (command === 'logout') {
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
    localStorage.removeItem('employeeId')
    router.push('/login')
    ElMessage.success('已退出')
  } else if (command === 'profile') {
    router.push('/profile')
  }
}

onMounted(() => {
  const info = getUserInfo()
  if (info) {
    userInfo.value = info
    console.log('========== 用户信息 ==========')
    console.log('用户:', info.name)
    console.log('角色:', info.role)
    console.log('部门:', info.departmentId)
    console.log('===============================')
  }
})
</script>

<style scoped>
.admin-layout {
  height: 100vh;
}

.sidebar {
  background: #001529;
  height: 100vh;
  overflow-y: auto;
  transition: width 0.3s;
  display: flex;
  flex-direction: column;
}

.logo-wrap {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 0 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
}

.logo-icon {
  font-size: 22px;
  font-weight: bold;
  color: #409eff;
  background: rgba(64, 158, 255, 0.15);
  padding: 4px 10px;
  border-radius: 8px;
}

.logo-text {
  font-size: 18px;
  font-weight: bold;
  color: #fff;
  white-space: nowrap;
}

.menu {
  flex: 1;
  border-right: none;
}

.menu:not(.el-menu--collapse) {
  width: 100%;
}

.menu .el-menu-item {
  height: 48px;
  line-height: 48px;
}

.collapse-btn {
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #bfbfbf;
  cursor: pointer;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
  transition: color 0.2s;
}

.collapse-btn:hover {
  color: #fff;
}

.main-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.header {
  height: 64px;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  border-bottom: 1px solid #e8e8e8;
  flex-shrink: 0;
}

.header-left {
  display: flex;
  align-items: center;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 20px;
}

.badge {
  cursor: pointer;
}

.badge:hover {
  opacity: 0.7;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 12px 4px 8px;
  border-radius: 20px;
  transition: background 0.2s;
}

.user-info:hover {
  background: #f5f5f5;
}

.username {
  font-size: 14px;
  color: #333;
}

.role-tag {
  font-size: 11px;
  color: #fff;
  background: #409eff;
  padding: 2px 10px;
  border-radius: 10px;
  margin-left: 4px;
}

.content {
  padding: 20px;
  background: #f0f2f5;
  overflow-y: auto;
  flex: 1;
}

.sidebar::-webkit-scrollbar {
  width: 4px;
}

.sidebar::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.2);
  border-radius: 4px;
}
</style>