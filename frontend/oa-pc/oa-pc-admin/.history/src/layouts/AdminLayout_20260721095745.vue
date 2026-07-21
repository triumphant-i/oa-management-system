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
        <template v-for="item in menuList" :key="item.path">
          <!-- 有子菜单 -->
          <el-sub-menu v-if="item.children && item.children.length > 0" :index="item.path">
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
          
          <!-- 无子菜单 -->
          <el-menu-item v-else :index="item.path">
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
          <!-- 消息通知 -->
          <el-badge :value="unreadCount" class="badge" @click="goToMessage" style="cursor:pointer;">
            <el-icon :size="20"><Bell /></el-icon>
          </el-badge>
          
          <!-- 用户信息 -->
          <el-dropdown @command="handleCommand">
            <span class="user-info">
              <el-avatar :size="32" :src="userInfo?.avatar || ''">
                {{ userInfo?.name?.charAt(0) || '管' }}
              </el-avatar>
              <span class="username">{{ userInfo?.name || '管理员' }}</span>
              <span class="role-tag" :style="{ backgroundColor: roleColor }">
                {{ roleName }}
              </span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                <el-dropdown-item command="password">修改密码</el-dropdown-item>
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
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Fold, Expand, Bell, ArrowDown
} from '@element-plus/icons-vue'
import { MENU_CONFIG } from '@/config/menu'
import { 
  hasPermission, 
  getUserInfo, 
  getRoleName, 
  getUserRole,
  ROLE_NAME_MAP
} from '@/utils/permission'

const route = useRoute()
const router = useRouter()
const isCollapse = ref(false)
const userInfo = ref({})
const unreadCount = ref(0)

// 角色颜色映射
const roleColorMap = {
  'SYSTEM_ADMIN': '#ff6b6b',
  'DEPARTMENT_MANAGER': '#4ecdc4',
  'PROCESS_ADMIN': '#45b7d1',
  'EMPLOYEE': '#52c41a'
}

// 当前角色名称
const roleName = computed(() => {
  const role = userInfo.value?.role || 'EMPLOYEE'
  return ROLE_NAME_MAP[role] || '员工'
})

// 角色颜色
const roleColor = computed(() => {
  const role = userInfo.value?.role || 'EMPLOYEE'
  return roleColorMap[role] || '#409eff'
})

// 过滤后的菜单
const menuList = computed(() => {
  return MENU_CONFIG
    .filter(menu => {
      // 如果没有权限要求，显示
      if (!menu.permission) return true
      // 有权限要求，检查权限
      return hasPermission(menu.permission)
    })
    .map(menu => {
      // 过滤子菜单
      if (menu.children) {
        return {
          ...menu,
          children: menu.children.filter(child => {
            if (!child.permission) return true
            return hasPermission(child.permission)
          })
        }
      }
      return menu
    })
    .filter(menu => {
      // 如果有子菜单但子菜单为空，不显示
      if (menu.children && menu.children.length === 0) return false
      return true
    })
})

// 当前激活的菜单
const activeMenu = computed(() => {
  const path = route.path
  // 检查是否是子菜单路径
  for (const menu of MENU_CONFIG) {
    if (menu.children) {
      for (const child of menu.children) {
        if (path.startsWith(child.path)) {
          return child.path
        }
      }
    }
  }
  return path
})

// 当前页面标题
const currentTitle = computed(() => {
  // 从路由meta获取
  if (route.meta?.title) {
    return route.meta.title
  }
  
  // 从菜单配置查找
  for (const menu of MENU_CONFIG) {
    if (route.path === menu.path) {
      return menu.title
    }
    if (menu.children) {
      for (const child of menu.children) {
        if (route.path === child.path) {
          return child.title
        }
      }
    }
  }
  return '页面'
})

// 折叠切换
const toggleCollapse = () => {
  isCollapse.value = !isCollapse.value
}

// 跳转消息
const goToMessage = () => {
  router.push('/message')
}

// 下拉菜单命令
const handleCommand = (command) => {
  switch (command) {
    case 'logout':
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
      localStorage.removeItem('employeeId')
      router.push('/login')
      ElMessage.success('已退出登录')
      break
    case 'profile':
      router.push('/profile')
      break
    case 'password':
      router.push('/changePassword')
      break
  }
}

// 获取未读消息数
const fetchUnreadCount = async () => {
  try {
    const { default: request } = await import('@/api/request')
    const res = await request.get('/message/unread/count')
    if (res.code === 0) {
      unreadCount.value = res.data || 0
    }
  } catch (error) {
    console.error('获取未读消息数失败:', error)
  }
}

// 监听路由变化，更新未读消息
watch(route, () => {
  // 可以在这里刷新未读消息
})

onMounted(() => {
  // 获取用户信息
  const info = getUserInfo()
  if (info) {
    userInfo.value = info
    console.log('当前用户:', info)
    console.log('用户角色:', info.role)
    console.log('角色名称:', ROLE_NAME_MAP[info.role])
    console.log('可用菜单:', menuList.value.map(m => m.title))
  }
  
  // 获取未读消息数
  fetchUnreadCount()
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

.menu .el-menu-item,
.menu .el-sub-menu .el-sub-menu__title {
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

/* 菜单折叠动画 */
.el-menu--collapse .el-menu-item .el-icon,
.el-menu--collapse .el-sub-menu .el-icon {
  margin: 0;
}

/* 响应式 */
@media (max-width: 768px) {
  .sidebar {
    position: fixed;
    z-index: 1000;
    height: 100vh;
  }
  
  .header {
    padding: 0 12px;
  }
  
  .content {
    padding: 12px;
  }
}
</style>