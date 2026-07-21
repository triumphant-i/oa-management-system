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
        <!-- 动态渲染菜单 -->
        <template v-for="item in menuList" :key="item.path">
          <el-menu-item v-if="!item.children" :index="item.path">
            <el-icon><component :is="item.icon" /></el-icon>
            <span>{{ item.title }}</span>
          </el-menu-item>
          <el-sub-menu v-else :index="item.path">
            <template #title>
              <el-icon><component :is="item.icon" /></el-icon>
              <span>{{ item.title }}</span>
            </template>
            <el-menu-item
              v-for="child in item.children"
              :key="child.path"
              :index="child.path"
            >
              {{ child.title }}
            </el-menu-item>
          </el-sub-menu>
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
              <span class="username">{{ userInfo?.name || '超级管理员' }}</span>
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
        <router-view :key="route.fullPath" />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Fold, Expand, Bell, ArrowDown,
  User, OfficeBuilding, Document,
  Calendar, Location, Connection, Setting,
  DataAnalysis, ChatDotRound
} from '@element-plus/icons-vue'
import { hasPermission, PERMISSIONS } from '@/utils/permission'

const route = useRoute()
const router = useRouter()
const isCollapse = ref(false)
const userInfo = ref({})
const unreadCount = ref(5)

// 角色标签映射
const roleLabelMap = {
  'SYSTEM_ADMIN': '超级管理员',
  'GENERAL_MANAGER': '总经理',
  'DEPARTMENT_MANAGER': '部门经理',
  'PROCESS_ADMIN': '流程管理员',
  'EMPLOYEE': '普通员工'
}

const roleLabel = computed(() => {
  const role = userInfo.value?.role || 'EMPLOYEE'
  return roleLabelMap[role] || '员工'
})

// 🔧 动态菜单配置（移除仪表盘）
const allMenus = [
  // 移除了 Dashboard 菜单项
  { 
    path: '/employee', 
    title: '员工管理', 
    icon: 'User',
    permission: PERMISSIONS.EMPLOYEE_VIEW
  },
  { 
    path: '/department', 
    title: '部门管理', 
    icon: 'OfficeBuilding',
    permission: PERMISSIONS.DEPARTMENT_VIEW
  },
  { 
    path: '/approval', 
    title: '审批管理', 
    icon: 'Document',
    permission: PERMISSIONS.APPROVAL_VIEW
  },
  { 
    path: '/notice', 
    title: '公告管理', 
    icon: 'Bell',
    permission: PERMISSIONS.NOTICE_VIEW
  },
  { 
    path: '/attendance', 
    title: '考勤管理', 
    icon: 'Calendar',
    permission: PERMISSIONS.ATTENDANCE_VIEW
  },
  { 
    path: '/meetingRoom', 
    title: '会议室管理', 
    icon: 'Location',
    permission: PERMISSIONS.MEETING_VIEW
  },
  { 
    path: '/workflow', 
    title: '工作流管理', 
    icon: 'Connection',
    permission: PERMISSIONS.WORKFLOW_VIEW
  },
  { 
    path: '/statistics', 
    title: '数据统计', 
    icon: 'DataAnalysis',
    permission: PERMISSIONS.STATISTICS_VIEW
  },
  { 
    path: '/message', 
    title: '消息中心', 
    icon: 'ChatDotRound',
    permission: PERMISSIONS.MESSAGE_VIEW
  },
  { 
    path: '/system', 
    title: '系统管理', 
    icon: 'Setting',
    permission: PERMISSIONS.SYSTEM_MENU
  }
]

// 根据权限过滤菜单
const menuList = computed(() => {
  return allMenus.filter(menu => {
    if (!menu.permission) return true
    // 系统管理员拥有所有权限
    if (userInfo.value?.role === 'SYSTEM_ADMIN') return true
    return hasPermission(menu.permission)
  })
})

const activeMenu = computed(() => route.path)
const currentTitle = computed(() => {
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
    router.push('/login')
    ElMessage.success('已退出')
  } else if (command === 'profile') {
    router.push('/profile')
  }
}

watch(
  () => route.path,
  (newPath, oldPath) => {
    console.log('路由切换:', oldPath, '→', newPath)
  }
)

onMounted(() => {
  const info = localStorage.getItem('userInfo')
  if (info) {
    userInfo.value = JSON.parse(info)
    console.log('当前用户信息:', userInfo.value)
    console.log('用户权限:', userInfo.value.permissions)
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
  padding: 4px 8px;
  border-radius: 4px;
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
  font-size: 12px;
  color: #409eff;
  background: rgba(64, 158, 255, 0.1);
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