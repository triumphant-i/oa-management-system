<template>
  <div class="oa-attendance-history">
    <div class="page-header">
      <van-icon name="arrow-left" size="22" @click="$router.back()" />
      <h1 class="header-title">考勤记录</h1>
      <div class="header-right">
        <van-icon name="search" size="22" @click="showSearch = true" />
      </div>
    </div>

    <div class="stat-row">
      <div class="stat-card" :class="{ active: filterType === 'all' }" @click="filterType = 'all'">
        <span class="stat-num">{{ historyList.length }}</span>
        <span class="stat-label">全部</span>
      </div>
      <div class="stat-card" :class="{ active: filterType === '签到' }" @click="filterType = '签到'">
        <span class="stat-num">{{ getCount('签到') }}</span>
        <span class="stat-label">签到</span>
      </div>
      <div class="stat-card" :class="{ active: filterType === '签退' }" @click="filterType = '签退'">
        <span class="stat-num">{{ getCount('签退') }}</span>
        <span class="stat-label">签退</span>
      </div>
      <div class="stat-card" :class="{ active: filterType === '补卡' }" @click="filterType = '补卡'">
        <span class="stat-num">{{ getCount('补卡') }}</span>
        <span class="stat-label">补卡</span>
      </div>
    </div>

    <div class="list-wrap">
      <div class="list-item" v-for="(item, index) in filteredList" :key="index">
        <div class="item-header">
          <span class="item-type">{{ item.type }}</span>
          <span class="item-status" :class="item.status === '正常' ? 'status-normal' : 'status-abnormal'">
            {{ item.status }}
          </span>
        </div>
        <div class="item-body">
          <span class="item-time">{{ item.time }}</span>
          <span class="item-date">{{ item.date }}</span>
        </div>
      </div>
      <div class="empty-state" v-if="filteredList.length === 0">
        <van-icon name="clock-o" size="48" color="#ccc" />
        <p>暂无考勤记录</p>
      </div>
    </div>

    <div class="safe-bottom"></div>

    <van-search
      v-model="keyword"
      show-action
      placeholder="搜索日期"
      @search="onSearch"
      @cancel="showSearch = false; keyword = ''"
      v-if="showSearch"
      style="position: fixed; top: 0; left: 0; right: 0; z-index: 100; background: #fff;"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { getMyRecords } from '@/api/attendance'

const router = useRouter()

const filterType = ref('all')
const keyword = ref('')
const showSearch = ref(false)

const employeeId = ref(null)
const historyList = ref([])

const fetchHistory = async () => {
  if (!employeeId.value) return
  try {
    const res = await getMyRecords(employeeId.value)
    if (res.code === 0 && res.data) {
      const records = Array.isArray(res.data) ? res.data : []
      const formattedRecords = []
      records.forEach(r => {
        if (r.checkInTime) {
          formattedRecords.push({
            id: r.id,
            type: '签到',
            time: new Date(r.checkInTime).toLocaleTimeString('zh-CN'),
            date: r.date,
            status: r.status || '正常'
          })
        }
        if (r.checkOutTime) {
          formattedRecords.push({
            id: r.id + 1000,
            type: '签退',
            time: new Date(r.checkOutTime).toLocaleTimeString('zh-CN'),
            date: r.date,
            status: r.status || '正常'
          })
        }
      })
      historyList.value = formattedRecords.sort((a, b) => new Date(b.date + ' ' + b.time) - new Date(a.date + ' ' + a.time))
    }
  } catch (error) {
    console.error('获取考勤记录失败', error)
  }
}

const getCount = (type) => {
  return historyList.value.filter(item => item.type === type).length
}

const filteredList = computed(() => {
  let list = historyList.value
  
  if (filterType.value !== 'all') {
    list = list.filter(item => item.type === filterType.value)
  }
  
  if (keyword.value.trim()) {
    list = list.filter(item => item.date.includes(keyword.value.trim()))
  }
  
  return list
})

const onSearch = () => {
  showSearch.value = false
  if (keyword.value.trim()) {
    showToast(`搜索日期：${keyword.value}`)
  }
}

onMounted(() => {
  const userInfoStr = localStorage.getItem('userInfo')
  if (userInfoStr) {
    const userInfo = JSON.parse(userInfoStr)
    employeeId.value = userInfo.userId || userInfo.id || null
  }
  fetchHistory()
})
</script>

<style scoped>
.oa-attendance-history {
  max-width: 430px;
  margin: 0 auto;
  min-height: 100vh;
  background: #f5f7fa;
  padding: 0 16px 20px;
  box-shadow: 0 0 20px rgba(0,0,0,0.06);
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 16px 12px;
  background: #fff;
  margin: 0 -16px 0;
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
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
  cursor: pointer;
  transition: all 0.2s;
  border: 2px solid transparent;
}
.stat-card:active { transform: scale(0.95); }
.stat-card.active { border-color: #ff6b35; }
.stat-num { display: block; font-size: 22px; font-weight: bold; color: #ff6b35; }
.stat-label { font-size: 11px; color: #888; margin-top: 2px; }

.list-wrap { display: flex; flex-direction: column; gap: 12px; }
.list-item {
  background: #fff;
  border-radius: 14px;
  padding: 16px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}
.item-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}
.item-type {
  font-size: 15px;
  font-weight: 500;
  color: #222;
}
.item-status {
  font-size: 12px;
  padding: 2px 12px;
  border-radius: 12px;
}
.status-normal { color: #00b894; background: #e8f8f0; }
.status-abnormal { color: #ff6b35; background: #fff0e8; }
.item-body {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.item-time { font-size: 14px; color: #333; }
.item-date { font-size: 13px; color: #999; }

.empty-state {
  text-align: center;
  padding: 60px 20px;
  color: #ccc;
}
.empty-state p { margin-top: 12px; font-size: 15px; }

.safe-bottom { height: 20px; }
</style>