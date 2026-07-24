<template>
  <div class="oa-my-approvals" ref="scrollContainer">
    <div class="banner">
      <div class="banner-text">
        <h2 class="banner-title">我的申请</h2>
        <p class="banner-desc">记录你的每一次申请</p>
      </div>
      <div class="banner-icon">
        <van-icon name="edit" size="48" color="#6c5ce7" />
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="stat-row">
      <div class="stat-card" style="background: linear-gradient(135deg, #fff, #fff8e8);" @click="filterStatus = ''">
        <p class="stat-label">全部</p>
        <span class="stat-num" style="color: #fdcb6e;">{{ getCount('') }}</span>
        <van-icon class="stat-icon" name="ordered-list-o" size="32" color="#fdcb6e" />
      </div>
      <div class="stat-card" style="background: linear-gradient(135deg, #fff, #fff8ef);" @click="filterStatus = '待审批'">
        <p class="stat-label">待审批</p>
        <span class="stat-num" style="color: #ff6b35;">{{ getCount('待审批') }}</span>
        <van-icon class="stat-icon" name="todo-list-o" size="32" color="#ffb088" />
      </div>
      <div class="stat-card" style="background: linear-gradient(135deg, #fff, #f0fff4);" @click="filterStatus = '已通过'">
        <p class="stat-label">已通过</p>
        <span class="stat-num" style="color: #00b894;">{{ getCount('已通过') }}</span>
        <van-icon class="stat-icon" name="success" size="32" color="#88d8b0" />
      </div>
    </div>

    <!-- 筛选标签 -->
    <div class="filter-tabs">
      <span class="filter-tab" :class="{ active: filterStatus === '' }" @click="filterStatus = ''">
        全部
      </span>
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
      <span class="filter-tab" :class="{ active: filterStatus === '已撤回' }" @click="filterStatus = '已撤回'">
        已撤回
      </span>
    </div>

    <!-- 加载状态 -->
    <div class="loading-wrap" v-if="loading">
      <van-loading size="40" text="加载中..." vertical />
    </div>

    <!-- 申请列表 -->
    <div class="list-section" v-else>
      <div class="list-header">
        <h3>申请记录 <span class="record-count">共 {{ filteredList.length }} 条</span></h3>
      </div>
      <div class="approval-list">
        <div class="approval-item" v-for="(item, index) in filteredList" :key="item.id || index">
          <div class="item-top" @click="goDetail(item.id)">
            <div class="item-left">
              <span class="item-type">{{ getTypeLabel(item.approvalType) }}</span>
              <span class="item-title">{{ item.title }}</span>
            </div>
            <div class="item-right">
              <span class="item-status" :class="getStatusClass(item.status)">
                {{ item.status }}
              </span>
              <span class="item-time">{{ formatTime(item.createTime) }}</span>
            </div>
          </div>
          <div class="item-desc" @click="goDetail(item.id)">
            {{ item.content || getTypeLabel(item.approvalType) }}
          </div>
          <div class="item-footer" v-if="item.status === '待审批'">
            <van-button size="small" type="default" class="withdraw-btn" @click.stop="handleWithdraw(item)">
              撤回申请
            </van-button>
          </div>
        </div>
        <div class="empty-state" v-if="filteredList.length === 0">
          <van-icon name="smile-o" size="40" color="#ccc" />
          <p>暂无申请记录</p>
        </div>
      </div>
    </div>

    <!-- 底部返回 -->
    <div class="bottom-bar">
      <van-button plain block size="large" @click="$router.back()" class="back-btn">
        <van-icon name="arrow-left" size="18" /> 返回
      </van-button>
    </div>

    <div class="safe-bottom"></div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getMyApplications, withdrawApproval } from '@/api/approval'
import { showToast, showConfirmDialog } from 'vant'

const router = useRouter()

const scrollContainer = ref(null)
const loading = ref(false)
const filterStatus = ref('')
const allList = ref([])
const employeeId = ref(1)

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

const getStatusClass = (status) => {
  if (status === '已通过') return 'status-approved'
  if (status === '待审批') return 'status-pending'
  if (status === '已拒绝') return 'status-rejected'
  if (status === '已撤回') return 'status-withdrawn'
  return 'status-pending'
}

const loadData = async () => {
  loading.value = true
  try {
    // 加载当前用户的申请数据
    const res = await getMyApplications(employeeId.value)
    if (res.code === 0) {
      let dataList = []
      if (res.data) {
        if (Array.isArray(res.data.records)) {
          dataList = res.data.records
        } else if (Array.isArray(res.data)) {
          dataList = res.data
        } else if (res.data.list) {
          dataList = res.data.list
        }
      }
      allList.value = dataList
      console.log('✅ 我的申请数据加载成功:', allList.value.length, '条')
    }
  } catch (error) {
    console.error('❌ 加载数据失败:', error)
    showToast('加载失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

const getCount = (status) => {
  if (!status) return allList.value.length
  return allList.value.filter(item => item.status === status).length
}

const filteredList = computed(() => {
  if (!filterStatus.value) {
    return allList.value
  }
  return allList.value.filter(item => item.status === filterStatus.value)
})

const formatTime = (timeStr) => {
  if (!timeStr) return ''
  try {
    const date = new Date(timeStr)
    return `${date.getMonth()+1}月${date.getDate()}日 ${String(date.getHours()).padStart(2,'0')}:${String(date.getMinutes()).padStart(2,'0')}`
  } catch {
    return timeStr
  }
}

const goDetail = (id) => {
  if (!id) {
    showToast('申请ID不存在')
    return
  }
  router.push(`/approval/detail/${id}`)
}

const handleWithdraw = (item) => {
  showConfirmDialog({
    title: '确认撤回',
    message: '确定要撤回此申请吗？此操作不可恢复。',
    confirmButtonText: '确认撤回',
    cancelButtonText: '再想想'
  }).then(async () => {
    try {
      const res = await withdrawApproval(item.id)
      if (res.code === 0) {
        showToast('✅ 撤回成功')
        await loadData()
      } else {
        showToast(res.message || '撤回失败')
      }
    } catch (error) {
      console.error('❌ 撤回失败:', error)
      showToast('撤回失败，请稍后重试')
    }
  }).catch(() => {})
}

onMounted(() => {
  const storedEmployeeId = localStorage.getItem('employeeId')
  if (storedEmployeeId) {
    employeeId.value = parseInt(storedEmployeeId)
  }
  loadData()
})
</script>

<style scoped>
.oa-my-approvals {
  max-width: 430px;
  margin: 0 auto;
  min-height: 100vh;
  background: #f5f7fa;
  padding: 0 16px 20px;
  box-shadow: 0 0 20px rgba(0,0,0,0.06);
}

.loading-wrap {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 60px 0;
}

.banner {
  background: linear-gradient(135deg, #6c5ce7 0%, #a29bfe 100%);
  margin: 0 -16px 16px;
  padding: 36px 20px 28px;
  border-radius: 0 0 24px 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.banner-title { font-size: 24px; font-weight: bold; color: #fff; margin: 0; }
.banner-desc { color: rgba(255,255,255,0.85); font-size: 14px; margin: 4px 0 0; }
.banner-icon {
  background: rgba(255,255,255,0.2);
  border-radius: 50%;
  width: 60px;
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.stat-row {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 8px;
  margin-bottom: 16px;
}
.stat-card {
  background: #fff;
  border-radius: 10px;
  padding: 10px 10px 8px;
  position: relative;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
  cursor: pointer;
  transition: transform 0.15s;
}
.stat-card:active { transform: scale(0.95); }
.stat-label { font-size: 11px; color: #888; margin: 0; }
.stat-num { font-size: 20px; font-weight: bold; display: block; margin-top: 2px; }
.stat-icon { position: absolute; right: 6px; bottom: 6px; opacity: 0.5; }

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
  padding: 8px 4px;
  border-radius: 10px;
  font-size: 13px;
  color: #888;
  cursor: pointer;
  transition: all 0.3s;
  white-space: nowrap;
  position: relative;
}
.filter-tab.active {
  background: #6c5ce7;
  color: #fff;
  font-weight: 500;
}

.badge-small {
  display: inline-block;
  min-width: 16px;
  height: 16px;
  line-height: 16px;
  text-align: center;
  font-size: 10px;
  border-radius: 50%;
  padding: 0 4px;
  margin-left: 4px;
  background: #ff6b35;
  color: #fff;
}

.filter-tab.active .badge-small {
  background: rgba(255,255,255,0.3);
}

.list-section {
  background: #fff;
  border-radius: 14px;
  padding: 16px;
  margin-bottom: 12px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}
.list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
.list-header h3 { 
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

.approval-item {
  padding: 12px 0;
  border-bottom: 1px solid #f5f5f5;
  cursor: pointer;
}
.approval-item:last-child { border-bottom: none; }
.item-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 4px;
}
.item-left {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
  min-width: 0;
}
.item-type {
  font-size: 12px;
  color: #6c5ce7;
  background: #f0e8ff;
  padding: 2px 10px;
  border-radius: 10px;
  white-space: nowrap;
  flex-shrink: 0;
}
.item-title { 
  font-size: 15px; 
  color: #333; 
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.item-right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 4px;
}
.item-status {
  font-size: 12px;
  padding: 2px 10px;
  border-radius: 12px;
  font-weight: 500;
}
.item-time { 
  font-size: 11px; 
  color: #bbb; 
}
.item-desc { 
  font-size: 13px; 
  color: #999; 
  margin-bottom: 6px;
  padding-left: 4px;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.item-footer {
  display: flex;
  justify-content: flex-end;
  padding-top: 4px;
}
.withdraw-btn {
  border-radius: 8px;
  height: 32px;
  font-size: 13px;
}

.status-approved { color: #00b894; background: #e8f8f0; }
.status-pending { color: #f39c12; background: #fff8e8; }
.status-rejected { color: #e17055; background: #fff0e8; }
.status-withdrawn { color: #636e72; background: #f0f0f0; }

.empty-state {
  text-align: center;
  padding: 40px 0;
  color: #ccc;
}
.empty-state p {
  margin: 10px 0 0;
  font-size: 14px;
}

.bottom-bar { padding: 16px 0 8px; }
.back-btn {
  border-radius: 12px !important;
  height: 48px !important;
  font-size: 16px !important;
  border-color: #6c5ce7 !important;
  color: #6c5ce7 !important;
}
.back-btn:active { background: #f0e8ff !important; }

.safe-bottom { height: 20px; }
</style>