<template>
  <div class="oa-home">
    <!-- 顶部头部区域 -->
    <div class="header">
      <div class="user-info">
        <van-image 
          round 
          width="44" 
          height="44" 
          :src="fullAvatarUrl || defaultAvatar" 
          fit="cover"
          @error="handleAvatarError"
        />
        <div class="user-detail">
          <p class="user-name">欢迎回来，{{ userName }}</p>
          <p class="user-role">{{ roleName }} · {{ department }}</p>
        </div>
      </div>
      <div class="header-right">
        <van-badge dot>
          <van-icon name="bell-o" size="22" color="#fff" @click="goPage('notice')" />
        </van-badge>
        <van-icon name="revoke" size="20" color="#fff" style="margin-left:12px;" @click="handleLogout" />
      </div>
    </div>

    <!-- 快捷统计卡片 -->
    <div class="stat-cards">
      <div class="stat-item" @click="goPage('approval')">
        <span class="stat-num">{{ stats.pending }}</span>
        <span class="stat-label">待审批</span>
      </div>
      <div class="stat-item" @click="goPage('attendance')">
        <span class="stat-num">{{ stats.todayCheck }}</span>
        <span class="stat-label">今日签到</span>
      </div>
      <div class="stat-item" @click="goPage('notice')">
        <span class="stat-num">{{ stats.unread }}</span>
        <span class="stat-label">未读公告</span>
      </div>
      <div class="stat-item" @click="goPage('meeting')">
        <span class="stat-num">{{ stats.todayMeeting }}</span>
        <span class="stat-label">今日会议</span>
      </div>
    </div>

    <!-- 九宫格功能入口 -->
    <div class="func-grid">
      <!-- 审批中心 - 仅管理员和部门主管可见 -->
      <div class="func-item" v-if="isModuleVisible('approval') && (role === 'admin' || role === 'manager')" @click="goPage('approval')">
        <div class="icon-wrap" style="background: #e8f0ff; position: relative;">
          <van-icon name="records" size="24" color="#3677ef" />
          <van-badge 
            :content="stats.pending > 0 ? stats.pending : null" 
            class="approval-badge"
          />
        </div>
        <span class="func-text">审批中心</span>
      </div>

      <!-- 考勤打卡 - 所有角色可见 -->
      <div class="func-item" v-if="isModuleVisible('attendance')" @click="goPage('attendance')">
        <div class="icon-wrap" style="background: #fff0e8;">
          <van-icon name="clock-o" size="24" color="#ff6b35" />
        </div>
        <span class="func-text">考勤打卡</span>
      </div>

      <!-- 公告中心 - 所有角色可见 -->
      <div class="func-item" v-if="isModuleVisible('notice')" @click="goPage('notice')">
        <div class="icon-wrap" style="background: #e8f8f0;">
          <van-icon name="bullhorn-o" size="24" color="#00b894" />
        </div>
        <span class="func-text">公告中心</span>
      </div>

      <!-- 会议室 - 所有角色可见 -->
      <div class="func-item" v-if="isModuleVisible('meeting')" @click="goPage('meeting')">
        <div class="icon-wrap" style="background: #f0e8ff;">
          <van-icon name="location-o" size="24" color="#6c5ce7" />
        </div>
        <span class="func-text">会议室</span>
      </div>

      <!-- 员工管理 - 系统管理员：全部；部门主管：只读查看本部门 -->
      <div class="func-item" v-if="isModuleVisible('employeeManage') && (role === 'admin' || role === 'manager')" @click="goPage('employee/manage')">
        <div class="icon-wrap" style="background: #e8e8f0;">
          <van-icon name="manager" size="24" color="#6c5ce7" />
        </div>
        <span class="func-text">{{ role === 'admin' ? '员工管理' : '部门员工' }}</span>
      </div>

      <!-- 组织架构 - 所有角色可见 -->
      <div class="func-item" v-if="isModuleVisible('department')" @click="goPage('department')">
        <div class="icon-wrap" style="background: #fff8e8;">
          <van-icon name="cluster-o" size="24" color="#fdcb6e" />
        </div>
        <span class="func-text">组织架构</span>
      </div>

      <!-- 发起申请 - 所有角色可见 -->
      <div class="func-item" v-if="isModuleVisible('apply')" @click="goPage('apply')">
        <div class="icon-wrap" style="background: #ffe8f0;">
          <van-icon name="edit" size="24" color="#e17055" />
        </div>
        <span class="func-text">发起申请</span>
      </div>

      <!-- 个人中心 - 所有角色可见 -->
      <div class="func-item" v-if="isModuleVisible('profile')" @click="goPage('profile')">
        <div class="icon-wrap" style="background: #e8f8ff;">
          <van-icon name="user-o" size="24" color="#00b894" />
        </div>
        <span class="func-text">个人中心</span>
      </div>

      <!-- 系统管理 - 仅管理员可见 -->
      <div class="func-item" v-if="isModuleVisible('system') && role === 'admin'" @click="goPage('system')">
        <div class="icon-wrap" style="background: #f0e8e8;">
          <van-icon name="setting-o" size="24" color="#e17055" />
        </div>
        <span class="func-text">系统管理</span>
      </div>

    </div>

    <div class="safe-bottom"></div>

    <!-- ===== 底部导航栏 ===== -->
    <van-tabbar v-model="activeTab" active-color="#3677ef" inactive-color="#999" @change="onTabChange">
      <van-tabbar-item icon="chat-o" :badge="unreadCount > 0 ? (unreadCount > 99 ? '99+' : unreadCount) : null">消息</van-tabbar-item>
      <van-tabbar-item icon="apps-o">自定义</van-tabbar-item>
      <van-tabbar-item icon="wap-home-o">工作台</van-tabbar-item>
    </van-tabbar>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { showConfirmDialog, showToast } from 'vant'
import { getEmployeeById } from '@/api/employee'
import { getPendingApprovalCount, getTodayAttendanceStatus, getTodayMeetings, getUnreadCount as getAnnouncementUnreadCount } from '@/api/home'
import { getUnreadCount as getMessageUnreadCount } from '@/api/message'

const router = useRouter()

// =============================================
// ===== 🔧 核心配置：后端基础地址 =====
// =============================================
const BASE_BACKEND_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

// ===== 当前激活的Tab =====
const activeTab = ref(2)

// ===== 默认头像（SVG占位） =====
const defaultAvatar = 'data:image/svg+xml,%3Csvg xmlns="http://www.w3.org/2000/svg" width="80" height="80" viewBox="0 0 80 80"%3E%3Crect width="80" height="80" fill="%23e8f0ff" rx="40"/%3E%3Ctext x="40" y="48" font-size="32" text-anchor="middle" fill="%233677ef"%3E👤%3C/text%3E%3C/svg%3E'

// ===== 用户信息 =====
const role = ref('employee')
const roleName = ref('普通员工')
const userName = ref('用户')
const department = ref('未分配部门')
const userAvatar = ref('')
const employeeId = ref(null)

// ===== 自定义配置 =====
const visibleModules = ref({})

// ===== 统计数据 =====
const stats = ref({
  pending: 0,
  todayCheck: 0,
  unread: 0,
  todayMeeting: 0
})

const unreadCount = ref(0)

// =============================================
// ===== 📊 从后端获取统计数据 =====
// =============================================
const fetchStats = async () => {
  try {
    const [pendingRes, announcementRes, messageRes, meetingsRes] = await Promise.all([
      getPendingApprovalCount(employeeId.value, role.value.toUpperCase()),
      getAnnouncementUnreadCount(),
      getMessageUnreadCount(),
      getTodayMeetings(employeeId.value)
    ])
    
    if (pendingRes.code === 0 && pendingRes.data !== undefined) {
      stats.value.pending = pendingRes.data
    }
    
    if (announcementRes.code === 0 && announcementRes.data) {
      stats.value.unread = announcementRes.data.count || 0
    }
    
    if (messageRes.code === 0 && messageRes.data) {
      unreadCount.value = messageRes.data.count || 0
    }
    
    if (meetingsRes.code === 0 && meetingsRes.data) {
      const today = new Date().toDateString()
      const todayMeetings = meetingsRes.data.filter(m => {
        if (!m.startTime) return false
        return new Date(m.startTime).toDateString() === today
      })
      stats.value.todayMeeting = todayMeetings.length
    }
    
    console.log('✅ 首页统计数据更新完成:', stats.value)
    console.log('✅ 消息未读数量:', unreadCount.value)
  } catch (error) {
    console.error('获取统计数据失败:', error)
  }
}

// =============================================
// ===== 📋 获取今日签到人数 =====
// =============================================
const fetchTodayCheckCount = async () => {
  try {
    const res = await getTodayAttendanceStatus(employeeId.value)
    if (res.code === 0 && res.data) {
      stats.value.todayCheck = res.data.checkIn ? 1 : 0
    }
  } catch (error) {
    console.error('获取签到状态失败:', error)
  }
}

// =============================================
// ===== 🛠️ 工具函数：拼接完整头像URL =====
// =============================================
const getFullAvatarUrl = (avatarPath) => {
  if (!avatarPath) return ''
  // 如果已经是完整URL，直接返回
  if (avatarPath.startsWith('http://') || avatarPath.startsWith('https://')) {
    return avatarPath
  }
  // 拼接后端地址
  return `${BASE_BACKEND_URL}${avatarPath}`
}

// =============================================
// ===== 📷 计算属性：完整头像URL =====
// =============================================
const fullAvatarUrl = computed(() => {
  return getFullAvatarUrl(userAvatar.value)
})

// =============================================
// ===== 🖼️ 头像加载失败兜底 =====
// =============================================
const handleAvatarError = (event) => {
  console.warn('⚠️ 首页头像加载失败，使用默认头像')
  event.target.src = defaultAvatar
}

// =============================================
// ===== 📥 加载用户信息 =====
// =============================================
const loadUserInfo = () => {
  // 从 localStorage 读取基本信息
  const storedRole = localStorage.getItem('role')
  const storedUsername = localStorage.getItem('username')
  const storedName = localStorage.getItem('name')
  const storedRoleName = localStorage.getItem('roleName')
  const storedEmployeeId = localStorage.getItem('employeeId')
  const storedAvatar = localStorage.getItem('avatar')
  
  // 角色映射（后端返回的是大写，前端用小写判断）
  const roleMap = {
    'SYSTEM_ADMIN': 'admin',
    'DEPARTMENT_MANAGER': 'manager',
    'PROCESS_ADMIN': 'processAdmin',
    'EMPLOYEE': 'employee'
  }
  
  const roleDisplayMap = {
    'SYSTEM_ADMIN': '系统管理员',
    'DEPARTMENT_MANAGER': '部门主管',
    'PROCESS_ADMIN': '流程管理员',
    'EMPLOYEE': '普通员工'
  }
  
  // 获取角色
  const rawRole = storedRole || 'EMPLOYEE'
  role.value = roleMap[rawRole] || 'employee'
  roleName.value = storedRoleName || roleDisplayMap[rawRole] || '普通员工'
  
  // 姓名
  userName.value = storedName || storedUsername || '用户'
  
  // 部门（从 localStorage 读取，或使用角色名）
  const storedDept = localStorage.getItem('department')
  department.value = storedDept || roleName.value
  
  // ===== 🔑 头像（存储相对路径，显示时拼接） =====
  userAvatar.value = storedAvatar || ''
  
  // 员工ID
  if (storedEmployeeId) {
    employeeId.value = parseInt(storedEmployeeId)
  }
  
  console.log('📷 首页头像路径（原始）:', userAvatar.value)
  console.log('📷 首页头像路径（完整）:', fullAvatarUrl.value)
}

// =============================================
// ===== 从后端获取完整的用户信息 =====
// =============================================
const fetchUserInfo = async () => {
  if (!employeeId.value) return
  
  try {
    const res = await getEmployeeById(employeeId.value)
    if (res.code === 0 && res.data) {
      const data = res.data
      
      // 更新姓名
      if (data.name) {
        userName.value = data.name
        localStorage.setItem('name', data.name)
      }
      
      // 更新部门（如果后端返回了部门名称）
      if (data.departmentName || data.department) {
        const dept = data.departmentName || data.department
        department.value = dept
        localStorage.setItem('department', dept)
      }
      
      // ===== 🔑 更新头像（存储相对路径） =====
      if (data.avatar) {
        userAvatar.value = data.avatar
        localStorage.setItem('avatar', data.avatar)
      }
      
      // 更新角色名
      if (data.role) {
        const roleDisplayMap = {
          'SYSTEM_ADMIN': '系统管理员',
          'DEPARTMENT_MANAGER': '部门主管',
          'PROCESS_ADMIN': '流程管理员',
          'EMPLOYEE': '普通员工'
        }
        const newRoleName = roleDisplayMap[data.role] || '普通员工'
        roleName.value = newRoleName
        localStorage.setItem('roleName', newRoleName)
      }
      
      console.log('✅ 首页用户信息更新完成')
      console.log('📷 头像路径:', userAvatar.value)
      console.log('📷 完整头像URL:', fullAvatarUrl.value)
    }
  } catch (error) {
    console.error('获取用户详细信息失败:', error)
  }
}

// =============================================
// ===== 监听头像更新事件 =====
// =============================================
const handleAvatarUpdated = () => {
  console.log('🔄 检测到头像更新，刷新首页头像')
  // 从 localStorage 重新读取头像
  const storedAvatar = localStorage.getItem('avatar')
  if (storedAvatar) {
    userAvatar.value = storedAvatar
    console.log('📷 头像已更新:', fullAvatarUrl.value)
  }
}

// =============================================
// ===== 自定义模块加载 =====
// =============================================

const loadCustomModules = () => {
  const saved = localStorage.getItem('customModules')
  if (saved) {
    try {
      const parsed = JSON.parse(saved)
      parsed.forEach(item => {
        visibleModules.value[item.key] = item.visible
      })
    } catch (e) {
      console.error('加载自定义布局失败', e)
    }
  }
}

const isModuleVisible = (key) => {
  if (visibleModules.value[key] === undefined) return true
  return visibleModules.value[key]
}

// =============================================
// ===== 页面跳转 =====
// =============================================

const goPage = (path) => {
  router.push(`/${path}`)
}

// =============================================
// ===== Tab切换 =====
// =============================================

const onTabChange = (index) => {
  const tabMap = {
    0: '/message',
    1: '/custom',
    2: '/'
  }
  const path = tabMap[index]
  if (path && router.currentRoute.value.path !== path) {
    router.push(path)
  }
}

// =============================================
// ===== 退出登录 =====
// =============================================

const handleLogout = () => {
  showConfirmDialog({
    title: '确认退出',
    message: '确定要退出登录吗？',
    confirmButtonText: '确定退出'
  }).then(res => {
    if (res) {
      // 清除所有登录信息
      localStorage.removeItem('isLogin')
      localStorage.removeItem('token')
      localStorage.removeItem('username')
      localStorage.removeItem('name')
      localStorage.removeItem('employeeId')
      localStorage.removeItem('role')
      localStorage.removeItem('roleName')
      localStorage.removeItem('department')
      localStorage.removeItem('avatar')
      showToast('已退出登录')
      router.replace('/login')
    }
  }).catch(() => {})
}

// =============================================
// ===== 监听自定义变化 =====
// =============================================

const handleCustomChanged = () => {
  visibleModules.value = {}
  loadCustomModules()
}

// =============================================
// ===== 生命周期 =====
// =============================================

onMounted(async () => {
  // 先从 localStorage 加载基本信息
  loadUserInfo()
  
  // 再从后端获取完整信息（部门名称等）
  await fetchUserInfo()
  
  // 获取统计数据
  await fetchStats()
  await fetchTodayCheckCount()
  
  loadCustomModules()
  
  // 监听头像更新事件
  window.addEventListener('avatarUpdated', handleAvatarUpdated)
  window.addEventListener('customChanged', handleCustomChanged)
})

onUnmounted(() => {
  window.removeEventListener('avatarUpdated', handleAvatarUpdated)
  window.removeEventListener('customChanged', handleCustomChanged)
})
</script>

<style scoped>
.oa-home {
  max-width: 430px;
  margin: 0 auto;
  min-height: 100vh;
  background: #f5f7fa;
  padding: 0 16px 20px;
  padding-bottom: 60px;
  box-shadow: 0 0 20px rgba(0,0,0,0.08);
}

/* ===== 顶部头部 ===== */
.header {
  background: linear-gradient(135deg, #3677ef 0%, #5b8def 100%);
  margin: 0 -16px 0;
  padding: 40px 16px 24px;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  border-radius: 0 0 24px 24px;
}
.user-info { display: flex; align-items: center; gap: 12px; }
:deep(.van-image) {
  border: 2px solid rgba(255,255,255,0.3);
  flex-shrink: 0;
}
.user-name { color: #fff; font-size: 17px; font-weight: bold; margin: 0; }
.user-role { color: rgba(255,255,255,0.85); font-size: 12px; margin: 2px 0 0; }
.header-right { display: flex; align-items: center; padding-top: 4px; gap: 12px; }

/* ===== 快捷统计卡片 ===== */
.stat-cards {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr 1fr;
  gap: 8px;
  margin-top: -12px;
  margin-bottom: 14px;
  position: relative;
  z-index: 2;
}
.stat-item {
  background: #fff;
  border-radius: 10px;
  padding: 10px 4px;
  text-align: center;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
  cursor: pointer;
}
.stat-item:active { transform: scale(0.95); }
.stat-num { display: block; font-size: 20px; font-weight: bold; color: #3677ef; }
.stat-label { font-size: 11px; color: #888; margin-top: 2px; }

/* ===== 九宫格功能 ===== */
.func-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
  background: #fff;
  border-radius: 14px;
  padding: 14px 10px;
  margin-bottom: 14px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}
.func-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 5px;
  cursor: pointer;
  padding: 4px 0;
}
.func-item:active { transform: scale(0.92); }
.icon-wrap { width: 44px; height: 44px; border-radius: 12px; display: flex; align-items: center; justify-content: center; }
.func-text { font-size: 11px; color: #333; }
.approval-badge { position: absolute; top: -2px; right: -2px; }

/* ===== 底部导航栏 ===== */
:deep(.van-tabbar) {
  max-width: 430px;
  left: 50%;
  transform: translateX(-50%);
  border-top: 1px solid #f0f0f0;
  background: #fff;
  height: 60px;
}
:deep(.van-tabbar-item) {
  font-size: 11px;
}
:deep(.van-tabbar-item--active) {
  color: #3677ef;
}
:deep(.van-tabbar-item .van-icon) {
  font-size: 22px;
}

.safe-bottom { height: 10px; }
</style>