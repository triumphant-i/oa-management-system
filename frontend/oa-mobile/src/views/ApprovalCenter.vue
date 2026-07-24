<template>
  <div class="oa-approval" ref="scrollContainer">
    <div class="banner">
      <div class="banner-text">
        <h2 class="banner-title">审批中心</h2>
        <p class="banner-desc">高效流转 · 智能审批</p>
      </div>
      <div class="banner-icon">
        <van-icon name="records" size="48" color="#3677ef" />
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="stat-row">
      <div class="stat-card" style="background: linear-gradient(135deg, #fff, #fff8ef);" @click="filterStatus = '待审批'">
        <p class="stat-label">待审批</p>
        <span class="stat-num" style="color: #ff6b35;">{{ getCount('待审批') }}</span>
        <van-icon class="stat-icon" name="todo-list-o" size="32" color="#ffb088" />
      </div>
      <div class="stat-card" style="background: linear-gradient(135deg, #fff, #f0fff4);" @click="filterStatus = '已通过'">
        <p class="stat-label">已通过</p>
        <span class="stat-num" style="color: #00b894;">{{ getCount('已通过') }}</span>
        <van-icon class="stat-icon" name="success" size="32" color="#88d8b8" />
      </div>
      <div class="stat-card" style="background: linear-gradient(135deg, #fff, #fff0f0);" @click="filterStatus = '已拒绝'">
        <p class="stat-label">已拒绝</p>
        <span class="stat-num" style="color: #e17055;">{{ getCount('已拒绝') }}</span>
        <van-icon class="stat-icon" name="close" size="32" color="#ffb088" />
      </div>
    </div>

    <!-- 筛选标签 -->
    <div class="filter-tabs">
      <span class="filter-tab" :class="{ active: filterStatus === 'all' }" @click="filterStatus = 'all'">全部</span>
      <span class="filter-tab" :class="{ active: filterStatus === '待审批' }" @click="filterStatus = '待审批'">
        待审批 
        <span class="badge-small" v-if="getCount('待审批') > 0">{{ getCount('待审批') }}</span>
      </span>
      <span class="filter-tab" :class="{ active: filterStatus === '已通过' }" @click="filterStatus = '已通过'">
        已通过
      </span>
      <span class="filter-tab" :class="{ active: filterStatus === '已拒绝' }" @click="filterStatus = '已拒绝'">
        已拒绝
      </span>
    </div>

    <!-- ===== 加载状态 ===== -->
    <div class="loading-wrap" v-if="loading">
      <van-loading size="40" text="加载中..." vertical />
    </div>

    <!-- ===== 审批列表（显示全部） ===== -->
    <div class="todo-section" v-else>
      <div class="section-header">
        <h3>审批记录 <span class="record-count">共 {{ filteredList.length }} 条</span></h3>
      </div>
      <div class="todo-list">
        <div class="todo-item" v-for="(item, index) in filteredList" :key="item.id || index" @click="goDetail(item.id)">
          <div class="todo-top">
            <div class="todo-left">
              <span class="todo-type">{{ getTypeLabel(item.approvalType) }}</span>
              <span class="todo-title">{{ item.title }}</span>
            </div>
            <span class="todo-time">{{ formatTime(item.createTime) }}</span>
          </div>
          <div class="todo-desc">{{ item.content || item.desc || '暂无描述' }}</div>
          <div class="todo-footer">
            <span class="todo-applicant">申请人：{{ item.applicantName || '未知' }}</span>
            <span class="todo-status" :class="getStatusClass(item.status)">
              {{ item.status }}
            </span>
          </div>
        </div>
        <div class="empty-state" v-if="filteredList.length === 0">
          <van-icon name="smile-o" size="40" color="#ccc" />
          <p>暂无审批记录</p>
        </div>
      </div>
    </div>

    <!-- ===== 底部返回键 ===== -->
    <div class="bottom-bar">
      <van-button plain block size="large" @click="$router.back()" class="back-btn">
        <van-icon name="arrow-left" size="18" /> 返回
      </van-button>
    </div>

    <div class="safe-bottom"></div>

    <!-- ============================================= -->
    <!-- ===== 一键到顶 / 一键到底 浮动按钮 ===== -->
    <!-- ============================================= -->
    <div class="scroll-buttons" v-show="showScrollButtons">
      <div class="scroll-btn" @click="scrollToTop" v-show="showTopBtn">
        <van-icon name="back-top" size="22" color="#3677ef" />
        <span>顶部</span>
      </div>
      <div class="scroll-btn" @click="scrollToBottom" v-show="showBottomBtn">
        <van-icon name="down" size="22" color="#3677ef" />
        <span>底部</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { getPendingList, getMyApplications, getApprovalByStatus, getPendingCount } from '@/api/approval'
import { showToast } from 'vant'

const router = useRouter()

// =============================================
// ===== 滚动控制 =====
// =============================================
const scrollContainer = ref(null)
const showScrollButtons = ref(true)
const showTopBtn = ref(false)
const showBottomBtn = ref(true)

const scrollToTop = () => {
  window.scrollTo({
    top: 0,
    behavior: 'smooth'
  })
  if (scrollContainer.value) {
    scrollContainer.value.scrollTo({
      top: 0,
      behavior: 'smooth'
    })
  }
}

const scrollToBottom = () => {
  const scrollHeight = Math.max(
    document.documentElement.scrollHeight,
    document.body.scrollHeight,
    document.documentElement.offsetHeight,
    document.body.offsetHeight
  )
  window.scrollTo({
    top: scrollHeight,
    behavior: 'smooth'
  })
  if (scrollContainer.value) {
    scrollContainer.value.scrollTo({
      top: scrollContainer.value.scrollHeight,
      behavior: 'smooth'
    })
  }
}

const handleScroll = () => {
  const scrollTop = window.pageYOffset || document.documentElement.scrollTop
  const windowHeight = window.innerHeight
  const documentHeight = Math.max(
    document.documentElement.scrollHeight,
    document.body.scrollHeight
  )
  
  showTopBtn.value = scrollTop > 200
  const distanceToBottom = documentHeight - (scrollTop + windowHeight)
  showBottomBtn.value = distanceToBottom > 200
}

// =============================================
// ===== 状态 =====
// =============================================
const filterStatus = ref('all')
const allList = ref([])
const loading = ref(false)
const employeeId = ref(1)
const role = ref('EMPLOYEE')
const pendingCount = ref(0)

// 角色映射：支持前后端各种角色名格式
const roleMap = {
  'admin': 'SYSTEM_ADMIN',
  'manager': 'DEPARTMENT_MANAGER',
  'processAdmin': 'PROCESS_ADMIN',
  'employee': 'EMPLOYEE',
  'SYSTEM_ADMIN': 'SYSTEM_ADMIN',
  'DEPARTMENT_MANAGER': 'DEPARTMENT_MANAGER',
  'PROCESS_ADMIN': 'PROCESS_ADMIN',
  'EMPLOYEE': 'EMPLOYEE'
}

// =============================================
// ===== 类型映射 =====
// =============================================
const typeMap = {
  leave: '请假申请',
  business: '出差申请',
  overtime: '加班申请',
  reimburse: '报销申请',
  purchase: '采购申请',
  card: '补卡申请'
}

const getTypeLabel = (type) => {
  if (!type) return '申请'
  if (type.includes('申请')) return type
  return typeMap[type] || type
}

// =============================================
// ===== 状态样式映射 =====
// =============================================
const getStatusClass = (status) => {
  if (status === '已通过') return 'status-approved'
  if (status === '待审批') return 'status-pending'
  if (status === '已拒绝') return 'status-rejected'
  if (status === '已撤回') return 'status-withdrawn'
  return 'status-pending'
}

// =============================================
// ===== 加载数据 =====
// =============================================
const loadData = async () => {
  loading.value = true
  try {
    // 使用角色映射
    const backendRole = roleMap[role.value] || role.value
    
    // 并行请求所有数据
    const [pendingRes, myRes, approvedRes, rejectedRes, countRes] = await Promise.all([
      getPendingList(employeeId.value, backendRole).catch(() => ({ code: -1, data: [] })),
      getMyApplications(employeeId.value).catch(() => ({ code: -1, data: [] })),
      getApprovalByStatus('已通过').catch(() => ({ code: -1, data: [] })),
      getApprovalByStatus('已拒绝').catch(() => ({ code: -1, data: [] })),
      getPendingCount(employeeId.value, backendRole).catch(() => ({ code: -1, data: 0 }))
    ])

    // 合并数据（使用 Map 去重）
    const map = new Map()

    // 优先使用 getPendingList 的数据（后端已改为 DB 优先 + Flowable 补充）
    if (pendingRes.code === 0 && pendingRes.data && pendingRes.data.length > 0) {
      pendingRes.data.forEach(item => map.set(item.id, item))
    }

    // 从 myRes 补充数据
    if (myRes.code === 0) {
      (myRes.data || []).forEach(item => {
        if (!map.has(item.id)) {
          map.set(item.id, item)
        }
      })
    }

    // 从按状态查询补充
    if (approvedRes.code === 0) {
      (approvedRes.data || []).forEach(item => {
        if (!map.has(item.id)) {
          map.set(item.id, item)
        }
      })
    }

    if (rejectedRes.code === 0) {
      (rejectedRes.data || []).forEach(item => {
        if (!map.has(item.id)) {
          map.set(item.id, item)
        }
      })
    }

    // 如果仍未拉取到任何数据，再拉取一次 findByStatus
    if (map.size === 0) {
      console.log('主请求未返回数据，尝试 findByStatus 兜底')
      try {
        const pendingByStatus = await getApprovalByStatus('待审批')
        if (pendingByStatus.code === 0) {
          (pendingByStatus.data || []).forEach(item => map.set(item.id, item))
        }
      } catch (e) {
        console.warn('findByStatus 兜底也失败', e)
      }
    }

    allList.value = Array.from(map.values()).sort((a, b) =>
      new Date(b.createTime) - new Date(a.createTime)
    )

    // 更新待审批数量：以实际列表为准
    const actualPendingCount = allList.value.filter(item => item.status === '待审批' || !item.status).length
    if (countRes.code === 0 && actualPendingCount === 0) {
      pendingCount.value = countRes.data
    }
    // pendingCount 不再单独使用，getCount() 改为基于 allList

    console.log('✅ 审批数据加载成功:', allList.value.length, '条, 实际待审批:', actualPendingCount, 'Flowable待审批:', (countRes.code === 0 ? countRes.data : 'N/A'))
    console.log('list items:', allList.value.map(i => ({ id: i.id, status: i.status, title: i.title })))

  } catch (error) {
    console.error('❌ 加载审批数据失败:', error)
    showToast('加载失败，请稍后重试')
    allList.value = []
  } finally {
    loading.value = false
  }
}

// =============================================
// ===== 统计数量 =====
// =============================================
const getCount = (status) => {
  // 基于实际列表数据的统计，不再依赖 Flowable 的 pendingCount
  return allList.value.filter(item => {
    // 对待审批的状态做宽松匹配
    if (status === '待审批') {
      return item.status === '待审批' || !item.status
    }
    return item.status === status
  }).length
}

// =============================================
// ===== 筛选列表（显示全部） =====
// =============================================
const filteredList = computed(() => {
  if (filterStatus.value === 'all') {
    return allList.value
  }
  return allList.value.filter(item => item.status === filterStatus.value)
})

// =============================================
// ===== 工具方法 =====
// =============================================
const formatTime = (timeStr) => {
  if (!timeStr) return ''
  try {
    const date = new Date(timeStr)
    return `${date.getMonth()+1}月${date.getDate()}日 ${String(date.getHours()).padStart(2,'0')}:${String(date.getMinutes()).padStart(2,'0')}`
  } catch {
    return timeStr
  }
}

// =============================================
// ===== 跳转方法 =====
// =============================================
const goDetail = (id) => {
  if (!id) {
    showToast('申请ID不存在')
    return
  }
  router.push(`/approval/detail/${id}`)
}

// =============================================
// ===== 生命周期 =====
// =============================================
onMounted(() => {
  const id = localStorage.getItem('employeeId')
  if (id) {
    employeeId.value = parseInt(id)
  }
  const userRole = localStorage.getItem('role')
  if (userRole) {
    role.value = userRole
  }
  console.log('👤 当前用户ID:', employeeId.value, '角色:', role.value)
  loadData()
  
  // 监听滚动事件
  window.addEventListener('scroll', handleScroll)
  setTimeout(handleScroll, 300)
})

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll)
})
</script>

<style scoped>
.oa-approval {
  max-width: 430px;
  margin: 0 auto;
  min-height: 100vh;
  background: #f5f7fa;
  padding: 0 16px 20px;
  box-shadow: 0 0 20px rgba(0,0,0,0.06);
}

/* ===== 加载状态 ===== */
.loading-wrap {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 60px 0;
}

/* ===== Banner ===== */
.banner {
  background: linear-gradient(135deg, #3677ef 0%, #5b8def 100%);
  margin: 0 -16px 16px;
  padding: 36px 20px 28px;
  border-radius: 0 0 24px 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.banner-title { 
  font-size: 24px; 
  font-weight: bold; 
  color: #fff; 
  margin: 0; 
}
.banner-desc { 
  color: rgba(255,255,255,0.85); 
  font-size: 14px; 
  margin: 4px 0 0; 
}
.banner-icon {
  background: rgba(255,255,255,0.2);
  border-radius: 50%;
  width: 60px;
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* ===== 统计卡片 ===== */
.stat-row {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 10px;
  margin-bottom: 16px;
}
.stat-card {
  background: #fff;
  border-radius: 12px;
  padding: 14px 14px 12px;
  position: relative;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
  cursor: pointer;
  transition: transform 0.15s;
}
.stat-card:active { transform: scale(0.95); }
.stat-label { font-size: 13px; color: #888; margin: 0; }
.stat-num { font-size: 28px; font-weight: bold; display: block; margin-top: 4px; }
.stat-icon { position: absolute; right: 10px; bottom: 10px; opacity: 0.5; }

/* ===== 筛选标签 ===== */
.filter-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 14px;
  background: #fff;
  border-radius: 12px;
  padding: 6px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
  overflow-x: auto;
}
.filter-tabs::-webkit-scrollbar { display: none; }
.filter-tab {
  flex: 1;
  text-align: center;
  padding: 8px 0;
  border-radius: 10px;
  font-size: 14px;
  color: #888;
  cursor: pointer;
  transition: all 0.3s;
  white-space: nowrap;
  position: relative;
}
.filter-tab.active {
  background: #3677ef;
  color: #fff;
  font-weight: 500;
}

/* ===== 徽章样式 ===== */
.badge-small {
  display: inline-block;
  min-width: 18px;
  height: 18px;
  line-height: 18px;
  text-align: center;
  font-size: 11px;
  border-radius: 50%;
  padding: 0 6px;
  margin-left: 4px;
  background: #ff6b35;
  color: #fff;
}

.filter-tab.active .badge-small {
  background: rgba(255,255,255,0.3);
}

/* ===== 列表区域 ===== */
.todo-section {
  background: #fff;
  border-radius: 14px;
  padding: 16px;
  margin-bottom: 12px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}
.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
.section-header h3 { 
  font-size: 17px; 
  font-weight: bold; 
  margin: 0; 
  color: #222; 
}
.record-count {
  font-size: 13px;
  font-weight: normal;
  color: #999;
  margin-left: 8px;
}

/* ===== 待办项 ===== */
.todo-item {
  padding: 14px 0;
  border-bottom: 1px solid #f5f5f5;
  cursor: pointer;
  transition: background 0.15s;
  border-radius: 8px;
}
.todo-item:last-child { border-bottom: none; }
.todo-item:active { 
  background: #f5f7fa; 
  margin: 0 -8px; 
  padding: 14px 8px; 
}

.todo-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 4px;
}
.todo-left {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
  min-width: 0;
}
.todo-type {
  font-size: 12px;
  color: #3677ef;
  background: #e8f0ff;
  padding: 2px 10px;
  border-radius: 10px;
  white-space: nowrap;
  flex-shrink: 0;
}
.todo-title { 
  font-size: 15px; 
  color: #333; 
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.todo-time { 
  font-size: 12px; 
  color: #bbb; 
  flex-shrink: 0;
}
.todo-desc { 
  font-size: 13px; 
  color: #999; 
  padding-left: 4px;
  margin-bottom: 6px;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.todo-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.todo-applicant {
  font-size: 12px;
  color: #aaa;
}
.todo-status {
  font-size: 12px;
  padding: 2px 12px;
  border-radius: 12px;
  font-weight: 500;
}
.status-approved { 
  color: #00b894; 
  background: #e8f8f0; 
}
.status-pending { 
  color: #f39c12; 
  background: #fff8e8; 
}
.status-rejected { 
  color: #e17055; 
  background: #fff0e8; 
}
.status-withdrawn { 
  color: #636e72; 
  background: #f0f0f0; 
}

/* ===== 空状态 ===== */
.empty-state {
  text-align: center;
  padding: 30px 0;
  color: #ccc;
}
.empty-state p {
  margin: 8px 0 0;
  font-size: 14px;
}

/* ===== 底部返回键 ===== */
.bottom-bar {
  padding: 16px 0 8px;
}
.back-btn {
  border-radius: 12px !important;
  height: 48px !important;
  font-size: 16px !important;
  border-color: #3677ef !important;
  color: #3677ef !important;
}
.back-btn:active {
  background: #f0f7ff !important;
}

.safe-bottom { height: 20px; }

/* ============================================= */
/* ===== 一键到顶 / 一键到底 浮动按钮 ===== */
/* ============================================= */
.scroll-buttons {
  position: fixed;
  right: 16px;
  bottom: 100px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  z-index: 999;
  animation: fadeIn 0.3s ease;
}

.scroll-btn {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: #fff;
  box-shadow: 0 4px 16px rgba(54, 119, 239, 0.25);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s ease;
  border: 2px solid rgba(54, 119, 239, 0.15);
}

.scroll-btn:hover {
  transform: scale(1.05);
  box-shadow: 0 6px 24px rgba(54, 119, 239, 0.35);
}

.scroll-btn:active {
  transform: scale(0.92);
}

.scroll-btn span {
  font-size: 10px;
  color: #3677ef;
  margin-top: -2px;
  font-weight: 500;
}

.scroll-btn .van-icon {
  line-height: 1;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateX(20px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}
</style>