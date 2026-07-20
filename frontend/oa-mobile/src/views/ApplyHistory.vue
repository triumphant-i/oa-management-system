<template>
  <div class="oa-apply-history" ref="scrollContainer">
    <!-- 顶部导航 -->
    <div class="page-header">
      <h1 class="header-title">我的申请</h1>
      <div class="header-right">
        <van-icon name="search" size="22" @click="showSearch = true" />
      </div>
    </div>

    <!-- 加载状态 / 内容 -->
    <div v-if="loading" class="loading-wrap">
      <van-loading size="40" text="加载中..." vertical />
    </div>
    <div v-else>
      <!-- 统计卡片 -->
      <div class="stat-row">
        <div class="stat-card" @click="filterStatus = 'all'">
          <span class="stat-num" style="color: #3677ef;">{{ historyList.length }}</span>
          <span class="stat-label">全部</span>
        </div>
        <div class="stat-card" @click="filterStatus = '待审批'">
          <span class="stat-num" style="color: #fdcb6e;">{{ getCountByStatus('待审批') }}</span>
          <span class="stat-label">待审批</span>
        </div>
        <div class="stat-card" @click="filterStatus = '已通过'">
          <span class="stat-num" style="color: #00b894;">{{ getCountByStatus('已通过') }}</span>
          <span class="stat-label">已通过</span>
        </div>
        <div class="stat-card" @click="filterStatus = '已拒绝'">
          <span class="stat-num" style="color: #ff6b35;">{{ getCountByStatus('已拒绝') }}</span>
          <span class="stat-label">已拒绝</span>
        </div>
      </div>

      <!-- 筛选标签 -->
      <div class="filter-tabs">
        <span class="filter-tab" :class="{ active: filterStatus === 'all' }" @click="filterStatus = 'all'">全部</span>
        <span class="filter-tab" :class="{ active: filterStatus === '待审批' }" @click="filterStatus = '待审批'">待审批</span>
        <span class="filter-tab" :class="{ active: filterStatus === '已通过' }" @click="filterStatus = '已通过'">已通过</span>
        <span class="filter-tab" :class="{ active: filterStatus === '已拒绝' }" @click="filterStatus = '已拒绝'">已拒绝</span>
      </div>

      <!-- 申请列表 -->
      <div class="history-list">
        <div class="history-item" v-for="(item, index) in filteredList" :key="index" @click="goDetail(item.id)">
          <div class="item-header">
            <span class="item-type">{{ getTypeLabel(item.approvalType) }}</span>
            <span class="item-status" :class="item.status === '已通过' ? 'approved' : item.status === '待审批' ? 'pending' : 'rejected'">
              {{ item.status }}
            </span>
          </div>
          <h3 class="item-title">{{ item.title }}</h3>
          <p class="item-desc">{{ item.content || item.desc || '暂无描述' }}</p>
          <div class="item-footer">
            <span class="item-time">{{ formatTime(item.createTime) }}</span>
            <span class="item-step">{{ item.status }}</span>
          </div>
        </div>

        <div class="empty-state" v-if="filteredList.length === 0">
          <van-icon name="notes-o" size="48" color="#ccc" />
          <p>暂无申请记录</p>
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

    <!-- 搜索弹窗 -->
    <van-search
      v-model="searchKeyword"
      show-action
      placeholder="搜索申请标题"
      @search="onSearch"
      @cancel="showSearch = false; searchKeyword = ''"
      v-if="showSearch"
      style="position: fixed; top: 0; left: 0; right: 0; z-index: 100; background: #fff;"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { getMyApplications } from '@/api/approval'

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

// ===== 当前用户ID =====
const employeeId = ref(1)

// ===== 状态 =====
const filterStatus = ref('all')
const searchKeyword = ref('')
const showSearch = ref(false)
const loading = ref(true)

// ===== 历史数据 =====
const historyList = ref([])

// ===== 类型映射 =====
const typeMap = {
  leave: '请假申请',
  business: '出差申请',
  overtime: '加班申请',
  reimburse: '报销申请',
  purchase: '采购申请',
  card: '补卡申请'
}

// ===== 获取类型显示名称 =====
const getTypeLabel = (type) => {
  return typeMap[type] || type || '申请'
}

// =============================================
// ===== 加载数据 =====
// =============================================
const loadData = async () => {
  loading.value = true
  try {
    const res = await getMyApplications(employeeId.value)
    if (res.code === 0) {
      const list = res.data || []
      // 按创建时间倒序排列
      historyList.value = [...list].sort((a, b) =>
        new Date(b.createTime) - new Date(a.createTime)
      )
      console.log('申请历史加载成功:', historyList.value.length, '条')
    } else {
      showToast(res.msg || '加载失败')
    }
  } catch (error) {
    console.error('加载申请历史失败:', error)
  } finally {
    loading.value = false
  }
}

// =============================================
// ===== 统计 =====
// =============================================
const getCountByStatus = (status) => {
  return historyList.value.filter(item => item.status === status).length
}

// =============================================
// ===== 筛选列表 =====
// =============================================
const filteredList = computed(() => {
  let list = historyList.value

  // 状态筛选
  if (filterStatus.value !== 'all') {
    list = list.filter(item => item.status === filterStatus.value)
  }

  // 关键词搜索
  if (searchKeyword.value.trim()) {
    const keyword = searchKeyword.value.trim()
    list = list.filter(item =>
      (item.title || '').includes(keyword) ||
      (item.approvalType || '').includes(keyword)
    )
  }

  return list
})

// =============================================
// ===== 工具方法 =====
// =============================================
const formatTime = (timeStr) => {
  if (!timeStr) return ''
  try {
    const date = new Date(timeStr)
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    const hours = String(date.getHours()).padStart(2, '0')
    const minutes = String(date.getMinutes()).padStart(2, '0')
    return `${month}-${day} ${hours}:${minutes}`
  } catch {
    return timeStr
  }
}

// =============================================
// ===== 跳转 =====
// =============================================
const goDetail = (id) => {
  router.push(`/apply/detail/${id}`)
}

const onSearch = () => {
  showSearch.value = false
  if (searchKeyword.value.trim()) {
    showToast(`搜索：${searchKeyword.value}`)
  }
}

// =============================================
// ===== 生命周期 =====
// =============================================
onMounted(async () => {
  // 从 localStorage 获取用户ID
  const id = localStorage.getItem('employeeId')
  if (id) {
    employeeId.value = parseInt(id)
  } else {
    const username = localStorage.getItem('username')
    if (username === 'admin') {
      employeeId.value = 1
    }
  }
  console.log('当前用户ID:', employeeId.value)
  await loadData()
  
  // 监听滚动事件
  window.addEventListener('scroll', handleScroll)
  setTimeout(handleScroll, 300)
})

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll)
})
</script>

<style scoped>
.oa-apply-history {
  max-width: 430px;
  margin: 0 auto;
  min-height: 100vh;
  background: #f5f7fa;
  padding: 0 16px 20px;
  box-shadow: 0 0 20px rgba(0, 0, 0, 0.06);
}

/* 加载状态 */
.loading-wrap {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 60px 0;
}

/* 顶部导航（去掉返回按钮） */
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 0 12px;
  background: #fff;
  margin: 0 -16px 0;
  padding: 16px 16px 12px;
}
.header-title {
  font-size: 18px;
  font-weight: 600;
  margin: 0;
  color: #222;
}
.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

/* 统计卡片 */
.stat-row {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr 1fr;
  gap: 10px;
  margin-bottom: 16px;
}
.stat-card {
  background: #fff;
  border-radius: 12px;
  padding: 12px 8px;
  text-align: center;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  cursor: pointer;
  transition: all 0.2s;
}
.stat-card:active {
  transform: scale(0.95);
}
.stat-num {
  display: block;
  font-size: 24px;
  font-weight: bold;
}
.stat-label {
  font-size: 11px;
  color: #888;
  margin-top: 2px;
}

/* 筛选标签 */
.filter-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 14px;
  overflow-x: auto;
}
.filter-tabs::-webkit-scrollbar {
  display: none;
}
.filter-tab {
  padding: 6px 16px;
  border-radius: 20px;
  font-size: 13px;
  color: #888;
  background: #fff;
  white-space: nowrap;
  cursor: pointer;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.04);
  transition: all 0.2s;
}
.filter-tab.active {
  background: #3677ef;
  color: #fff;
}

/* 历史列表 */
.history-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.history-item {
  background: #fff;
  border-radius: 14px;
  padding: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  cursor: pointer;
  transition: transform 0.15s;
}
.history-item:active {
  transform: scale(0.98);
}
.item-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}
.item-type {
  font-size: 13px;
  color: #3677ef;
  background: #e8f0ff;
  padding: 2px 12px;
  border-radius: 10px;
}
.item-status {
  font-size: 12px;
  padding: 2px 12px;
  border-radius: 12px;
}
.item-status.approved {
  color: #00b894;
  background: #e8f8f0;
}
.item-status.pending {
  color: #fdcb6e;
  background: #fff8e8;
}
.item-status.rejected {
  color: #ff6b35;
  background: #fff0e8;
}
.item-title {
  font-size: 16px;
  font-weight: 500;
  margin: 0 0 4px;
  color: #222;
}
.item-desc {
  font-size: 14px;
  color: #888;
  margin: 0 0 10px;
}
.item-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 10px;
  border-top: 1px solid #f5f5f5;
}
.item-time {
  font-size: 12px;
  color: #bbb;
}
.item-step {
  font-size: 12px;
  color: #3677ef;
}

/* 空状态 */
.empty-state {
  text-align: center;
  padding: 60px 20px;
  color: #ccc;
}
.empty-state p {
  margin-top: 12px;
  font-size: 15px;
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

/* 底部安全区 */
.safe-bottom {
  height: 20px;
}

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