<template>
  <div class="oa-dept-members">
    <!-- 顶部导航 -->
    <div class="page-header">
      <van-icon name="arrow-left" size="22" @click="$router.back()" />
      <h1 class="header-title">{{ departmentName }}成员</h1>
      <van-icon name="search" size="22" @click="showSearch = !showSearch" />
    </div>

    <!-- 搜索栏 -->
    <div class="search-wrap" v-if="showSearch">
      <van-search 
        v-model="searchKeyword" 
        placeholder="搜索姓名/职位" 
        shape="round"
        @search="handleSearch"
        @clear="loadMembers"
      />
    </div>

    <!-- 统计信息 -->
    <div class="stat-row">
      <div class="stat-card">
        <span class="stat-num">{{ memberList.length }}</span>
        <span class="stat-label">总人数</span>
      </div>
      <div class="stat-card">
        <span class="stat-num" style="color: #00b894;">{{ activeCount }}</span>
        <span class="stat-label">在职</span>
      </div>
      <div class="stat-card">
        <span class="stat-num" style="color: #fdcb6e;">{{ managerCount }}</span>
        <span class="stat-label">管理层</span>
      </div>
    </div>

    <!-- 筛选标签 -->
    <div class="filter-row">
      <span 
        class="filter-item" 
        :class="{ active: filterRole === 'all' }"
        @click="filterRole = 'all'"
      >全部</span>
      <span 
        class="filter-item" 
        :class="{ active: filterRole === 'manager' }"
        @click="filterRole = 'manager'"
      >管理层</span>
      <span 
        class="filter-item" 
        :class="{ active: filterRole === 'employee' }"
        @click="filterRole = 'employee'"
      >普通员工</span>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading-wrap">
      <van-loading size="24px">加载中...</van-loading>
    </div>

    <!-- 成员列表 -->
    <div v-else class="member-list">
      <div 
        class="member-item" 
        v-for="item in filteredList" 
        :key="item.id"
        @click="goEmployeeDetail(item.id)"
      >
        <van-image round width="44" height="44" :src="item.avatar || '/default-avatar.png'" />
        <div class="member-info">
          <div class="member-name-row">
            <span class="member-name">{{ item.name }}</span>
            <van-tag :type="getRoleTagType(item.role)" size="small">
              {{ getRoleName(item.role) }}
            </van-tag>
          </div>
          <p class="member-position">{{ item.position || '员工' }}</p>
          <p class="member-contact" v-if="item.phone || item.email">
            <van-icon name="phone-o" size="12" v-if="item.phone" />
            <span v-if="item.phone">{{ item.phone }}</span>
            <van-icon name="envelop-o" size="12" v-if="item.email" style="margin-left:8px;" />
            <span v-if="item.email">{{ item.email }}</span>
          </p>
        </div>
        <van-icon name="arrow" size="16" color="#ccc" />
      </div>

      <!-- 空状态 -->
      <div v-if="filteredList.length === 0" class="empty-state">
        <van-icon name="friends-o" size="48" color="#ccc" />
        <p>{{ searchKeyword ? '未找到匹配的员工' : '该部门暂无成员' }}</p>
      </div>
    </div>

    <div class="safe-bottom"></div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast } from 'vant'
import { getEmployeeByDepartment } from '@/api/employee'
import { getDepartmentById } from '@/api/department'

const route = useRoute()
const router = useRouter()

// 状态
const loading = ref(false)
const showSearch = ref(false)
const searchKeyword = ref('')
const filterRole = ref('all')

// 数据
const departmentName = ref('')
const memberList = ref([])

// 角色映射
const roleMap = {
  'SYSTEM_ADMIN': '系统管理员',
  'DEPARTMENT_MANAGER': '部门主管',
  'FLOW_ADMIN': '流程管理员',
  'EMPLOYEE': '普通员工'
}

const getRoleName = (role) => roleMap[role] || '未知'

const getRoleTagType = (role) => {
  const types = {
    'SYSTEM_ADMIN': 'danger',
    'DEPARTMENT_MANAGER': 'warning',
    'FLOW_ADMIN': 'primary',
    'EMPLOYEE': 'success'
  }
  return types[role] || 'default'
}

// 计算属性
const activeCount = computed(() => {
  return memberList.value.filter(m => m.status === '在职').length
})

const managerCount = computed(() => {
  return memberList.value.filter(m => 
    m.role === 'SYSTEM_ADMIN' || m.role === 'DEPARTMENT_MANAGER'
  ).length
})

const filteredList = computed(() => {
  let list = memberList.value

  // 角色筛选
  if (filterRole.value === 'manager') {
    list = list.filter(m => 
      m.role === 'SYSTEM_ADMIN' || m.role === 'DEPARTMENT_MANAGER'
    )
  } else if (filterRole.value === 'employee') {
    list = list.filter(m => m.role === 'EMPLOYEE')
  }

  // 搜索筛选
  if (searchKeyword.value) {
    const keyword = searchKeyword.value.toLowerCase()
    list = list.filter(m => 
      m.name?.toLowerCase().includes(keyword) ||
      m.position?.toLowerCase().includes(keyword)
    )
  }

  // 按角色排序：管理员 > 主管 > 员工
  const roleOrder = { 'SYSTEM_ADMIN': 1, 'DEPARTMENT_MANAGER': 2, 'FLOW_ADMIN': 3, 'EMPLOYEE': 4 }
  list = [...list].sort((a, b) => {
    const orderA = roleOrder[a.role] || 5
    const orderB = roleOrder[b.role] || 5
    return orderA - orderB
  })

  return list
})

// 加载部门成员
const loadMembers = async () => {
  loading.value = true
  const deptId = route.params.id
  try {
    const res = await getEmployeeByDepartment(deptId)
    console.log('部门成员列表：', res)
    if (res.code === 0) {
      memberList.value = Array.isArray(res.data) ? res.data : []
    } else {
      showToast(res.message || '获取成员列表失败')
    }
  } catch (error) {
    console.error('加载成员失败：', error)
    showToast('加载失败')
  } finally {
    loading.value = false
  }
}

// 加载部门信息
const loadDepartmentInfo = async () => {
  const deptId = route.params.id
  try {
    const res = await getDepartmentById(deptId)
    if (res.code === 0 && res.data) {
      departmentName.value = res.data.name || '部门'
    }
  } catch (error) {
    console.warn('获取部门信息失败：', error)
    departmentName.value = '部门'
  }
}

// 搜索
const handleSearch = () => {
  // 使用计算属性自动过滤，无需额外操作
  console.log('搜索关键词：', searchKeyword.value)
}

// 跳转员工详情
const goEmployeeDetail = (id) => {
  // 如果有员工详情页，跳转过去
  // router.push(`/employee/detail/${id}`)
  showToast(`查看员工${id}详情功能开发中`)
}

// 初始化
onMounted(() => {
  loadDepartmentInfo()
  loadMembers()
})
</script>

<style scoped>
.oa-dept-members {
  max-width: 430px;
  margin: 0 auto;
  min-height: 100vh;
  background: #f5f7fa;
  padding: 0 16px 20px;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 0 12px;
}
.header-title { font-size: 18px; font-weight: 600; margin: 0; color: #222; }

.search-wrap {
  margin-bottom: 12px;
}

/* 统计卡片 */
.stat-row {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 10px;
  margin-bottom: 16px;
}
.stat-card {
  background: #fff;
  border-radius: 12px;
  padding: 14px 8px;
  text-align: center;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}
.stat-num { display: block; font-size: 24px; font-weight: bold; color: #3677ef; }
.stat-label { font-size: 11px; color: #888; margin-top: 2px; }

/* 筛选标签 */
.filter-row {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
  overflow-x: auto;
  padding: 2px;
}
.filter-item {
  padding: 6px 16px;
  border-radius: 20px;
  font-size: 13px;
  background: #fff;
  color: #666;
  white-space: nowrap;
  cursor: pointer;
  box-shadow: 0 1px 4px rgba(0,0,0,0.04);
}
.filter-item.active {
  background: #3677ef;
  color: #fff;
  font-weight: 500;
}

.loading-wrap { text-align: center; padding: 40px 0; }

/* 成员列表 */
.member-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.member-item {
  background: #fff;
  border-radius: 12px;
  padding: 14px 16px;
  display: flex;
  align-items: center;
  gap: 12px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
  cursor: pointer;
}
.member-item:active { transform: scale(0.98); }
.member-info { flex: 1; min-width: 0; }
.member-name-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 2px;
}
.member-name { font-size: 16px; font-weight: 500; color: #222; }
.member-position { font-size: 13px; color: #666; margin: 0 0 4px; }
.member-contact {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #999;
  margin: 0;
}
.member-contact span { margin-left: 2px; }

.empty-state {
  text-align: center;
  padding: 40px 0;
  color: #ccc;
}
.empty-state p { margin-top: 8px; font-size: 14px; }

.safe-bottom { height: 20px; }
</style>