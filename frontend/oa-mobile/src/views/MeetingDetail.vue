<template>
  <div class="oa-meeting-detail">
    <!-- 顶部导航栏 -->
    <div class="page-header">
      <van-icon name="arrow-left" size="22" @click="$router.back()" />
      <h1 class="header-title">会议室详情</h1>
      <div class="header-right">
        <!-- 这里原本有分享和更多图标，已删除 -->
      </div>
    </div>

    <!-- 加载状态 -->
    <div class="loading-wrap" v-if="loading">
      <van-loading size="40" text="加载中..." vertical />
    </div>

    <!-- 内容 -->
    <template v-else-if="roomData">
      <!-- 会议室标题标签 -->
      <div class="meeting-title-wrap">
        <span class="tag-green" :class="roomData.status === '可用' ? 'tag-free' : 'tag-busy'">
          {{ roomData.status === '可用' ? '空闲' : '使用中' }}
        </span>
        <h2 class="meeting-name">{{ roomData.name }}</h2>
      </div>

      <!-- 信息列表 -->
      <div class="info-list">
        <div class="info-row">
          <span class="label">会议室</span>
          <div class="val-wrap">
            <span class="val">{{ roomData.name }}</span>
            <van-icon name="copy" size="20" color="#3677ef" @click="handleCopy" />
          </div>
        </div>
        <div class="info-row">
          <span class="label">楼层位置</span>
          <span class="val">{{ roomData.location || roomData.floor || '未知' }}</span>
        </div>
        <div class="info-row">
          <span class="label">容纳人数</span>
          <span class="val">{{ roomData.capacity }}人</span>
        </div>
        <div class="info-row">
          <span class="label">设备配置</span>
          <span class="val">
            <span v-for="(equip, idx) in equipmentList" :key="idx">
              {{ equip }}{{ idx < equipmentList.length - 1 ? ' · ' : '' }}
            </span>
            <span v-if="equipmentList.length === 0">未配置</span>
          </span>
        </div>
        <div class="info-row">
          <span class="label">状态</span>
          <span class="val" :style="{ color: roomData.status === '可用' ? '#00b894' : '#e17055' }">
            {{ roomData.status === '可用' ? '✅ 可预订' : '🔴 不可用' }}
          </span>
        </div>
      </div>

      <!-- 功能选项行 -->
      <div class="func-list">
        <div class="func-row" @click="handleReserve">
          <div class="func-left">
            <van-icon name="records" size="22" color="#3677ef" />
            <span class="func-text">预订会议室</span>
          </div>
          <div class="func-right">
            <span class="tip-text">{{ roomData.status === '可用' ? '可预订' : '不可用' }}</span>
            <van-icon name="arrow" size="18" color="#c8c9cc" />
          </div>
        </div>
        <div class="func-row" @click="handleCancel">
          <div class="func-left">
            <van-icon name="close" size="22" color="#e17055" />
            <span class="func-text">取消预订</span>
          </div>
          <div class="func-right">
            <span class="tip-text">{{ canCancel ? '可取消' : '不可取消' }}</span>
            <van-icon name="arrow" size="18" color="#c8c9cc" />
          </div>
        </div>
      </div>

      <!-- 时间轴（会议室使用情况） -->
      <div class="timeline-wrap">
        <h3 class="timeline-title">今日使用情况</h3>
        <div class="timeline-list">
          <div class="timeline-item" v-for="(item, index) in timelineList" :key="index">
            <div class="timeline-time">{{ item.time }}</div>
            <div class="timeline-dot" :class="item.status === '使用中' ? 'dot-active' : 'dot-free'"></div>
            <div class="timeline-content">
              <span class="tl-title">{{ item.title || '空闲' }}</span>
              <span class="tl-status" :class="item.status === '使用中' ? 'status-active' : 'status-free'">
                {{ item.status }}
              </span>
            </div>
          </div>
          <div class="empty-state" v-if="timelineList.length === 0">
            <span style="color:#bbb;font-size:14px;">今日暂无预订</span>
          </div>
        </div>
      </div>

      <!-- 底部蓝色提示栏 -->
      <div class="bottom-tip-bar" v-if="showTip">
        <span class="bar-text">💡 会议室预订需提前15分钟签到，超时将自动取消</span>
        <van-icon name="close" size="18" color="#fff" @click="showTip = false" />
      </div>

      <!-- 底部主按钮 -->
      <van-button 
        type="primary" 
        block 
        size="large" 
        class="enter-btn" 
        @click="handleEnter"
        :disabled="roomData.status !== '可用'"
      >
        进入会议室
      </van-button>
    </template>

    <!-- 空状态 -->
    <div class="empty-state" v-else>
      <van-icon name="location-o" size="48" color="#ccc" />
      <p>会议室不存在</p>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { showToast, showConfirmDialog } from 'vant'
import { getMeetingRoomById, getRoomMeetings } from '@/api/meeting'

const router = useRouter()
const route = useRoute()

// ===== 状态 =====
const loading = ref(true)
const roomData = ref(null)
const roomMeetings = ref([])
const showTip = ref(true)
const showActionSheet = ref(false)
const canCancel = ref(false)

// ===== 设备列表 =====
const equipmentList = computed(() => {
  if (!roomData.value) return []
  const equip = roomData.value.equipment
  if (!equip) return []
  if (typeof equip === 'string') {
    return equip.split(',').map(e => e.trim()).filter(e => e)
  }
  if (Array.isArray(equip)) {
    return equip
  }
  return []
})

// ===== 时间轴数据 =====
const timelineList = computed(() => {
  if (!roomMeetings.value || roomMeetings.value.length === 0) {
    return []
  }
  return roomMeetings.value.map(item => {
    const start = new Date(item.startTime)
    const end = new Date(item.endTime)
    const timeStr = `${String(start.getHours()).padStart(2,'0')}:${String(start.getMinutes()).padStart(2,'0')}-${String(end.getHours()).padStart(2,'0')}:${String(end.getMinutes()).padStart(2,'0')}`
    return {
      time: timeStr,
      title: item.title || '会议',
      status: item.status === '已预约' || item.status === '已确认' ? '使用中' : '空闲'
    }
  })
})

// =============================================
// ===== 加载数据 =====
// =============================================
const loadRoomDetail = async (id) => {
  loading.value = true
  try {
    const res = await getMeetingRoomById(id)
    if (res.code === 0 && res.data) {
      roomData.value = res.data
      // 加载会议室预订情况
      await loadRoomMeetings(id)
    } else {
      showToast(res.msg || '加载失败')
    }
  } catch (error) {
    console.error('加载会议室详情失败:', error)
    showToast('加载失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

const loadRoomMeetings = async (roomId) => {
  try {
    const res = await getRoomMeetings(roomId)
    if (res.code === 0) {
      roomMeetings.value = res.data || []
    }
  } catch (error) {
    console.error('加载会议室预订失败:', error)
  }
}

// =============================================
// ===== 方法 =====
// =============================================
const handleCopy = () => {
  showToast('已复制会议室信息')
}

const handleReserve = () => {
  if (roomData.value?.status === '可用') {
    router.push(`/meeting/reserve/${roomData.value.id}`)
  } else {
    showToast('该会议室当前不可用')
  }
}

const handleCancel = () => {
  if (canCancel.value) {
    showConfirmDialog({
      title: '确认取消',
      message: '确定要取消本次会议室预订吗？',
      confirmButtonText: '确定取消'
    }).then(res => {
      if (res) {
        showToast('已取消预订')
        canCancel.value = false
      }
    }).catch(() => {})
  } else {
    showToast('当前没有可取消的预订')
  }
}

const handleEnter = () => {
  if (roomData.value?.status === '可用') {
    showToast('正在进入会议室...')
  } else {
    showToast('会议室当前不可用')
  }
}

// =============================================
// ===== 生命周期 =====
// =============================================
onMounted(() => {
  const id = parseInt(route.params.id)
  if (id) {
    loadRoomDetail(id)
  } else {
    loading.value = false
    showToast('会议室不存在')
  }
})
</script>

<style scoped>
.oa-meeting-detail {
  background: #f5f7fa;
  min-height: 100vh;
  padding: 0 16px 20px;
  max-width: 430px;
  margin: 0 auto;
  box-shadow: 0 0 20px rgba(0,0,0,0.06);
}

/* ===== 加载状态 ===== */
.loading-wrap {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 60px 0;
}

/* ===== 空状态 ===== */
.empty-state {
  text-align: center;
  padding: 60px 20px;
  color: #ccc;
}
.empty-state p {
  margin-top: 12px;
  font-size: 15px;
}

/* ===== 顶部导航 ===== */
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
  min-width: 22px; /* 保持布局平衡 */
}

/* ===== 会议标题 ===== */
.meeting-title-wrap {
  display: flex;
  align-items: center;
  gap: 10px;
  background: #fff;
  padding: 16px;
  border-radius: 12px;
  margin-bottom: 14px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}
.tag-green {
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 13px;
  font-weight: 500;
  flex-shrink: 0;
}
.tag-free {
  background: #e8f8f0;
  color: #00b894;
}
.tag-busy {
  background: #ffe8e8;
  color: #e17055;
}
.meeting-name {
  font-size: 18px;
  font-weight: bold;
  margin: 0;
  color: #222;
}

/* ===== 信息列表 ===== */
.info-list {
  background: #fff;
  border-radius: 12px;
  padding: 4px 16px;
  margin-bottom: 12px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}
.info-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 0;
  border-bottom: 1px solid #f5f5f5;
}
.info-row:last-child {
  border-bottom: none;
}
.label {
  font-size: 15px;
  color: #888;
  flex-shrink: 0;
}
.val {
  font-size: 15px;
  color: #222;
}
.val-wrap {
  display: flex;
  align-items: center;
  gap: 10px;
}

/* ===== 功能列表 ===== */
.func-list {
  background: #fff;
  border-radius: 12px;
  margin-bottom: 14px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}
.func-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 16px;
  border-bottom: 1px solid #f5f5f5;
  cursor: pointer;
  transition: background 0.15s;
}
.func-row:active {
  background: #f8f8f8;
}
.func-row:last-child {
  border-bottom: none;
}
.func-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.func-text {
  font-size: 15px;
  color: #333;
}
.func-right {
  display: flex;
  align-items: center;
  gap: 6px;
}
.tip-text {
  color: #999;
  font-size: 14px;
}

/* ===== 时间轴 ===== */
.timeline-wrap {
  background: #fff;
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 14px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}
.timeline-title {
  font-size: 16px;
  font-weight: bold;
  margin: 0 0 14px;
  color: #222;
}
.timeline-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 0;
  border-bottom: 1px solid #f5f5f5;
}
.timeline-item:last-child {
  border-bottom: none;
}
.timeline-time {
  font-size: 13px;
  color: #888;
  flex-shrink: 0;
  min-width: 80px;
}
.timeline-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  flex-shrink: 0;
}
.timeline-dot.dot-active {
  background: #3677ef;
}
.timeline-dot.dot-free {
  background: #00b894;
}
.timeline-content {
  flex: 1;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.tl-title {
  font-size: 14px;
  color: #333;
}
.tl-status {
  font-size: 12px;
  padding: 2px 10px;
  border-radius: 10px;
}
.tl-status.status-active {
  color: #3677ef;
  background: #e8f0ff;
}
.tl-status.status-free {
  color: #00b894;
  background: #e8f8f0;
}

/* ===== 底部提示栏 ===== */
.bottom-tip-bar {
  background: #3677ef;
  border-radius: 10px;
  padding: 12px 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: #fff;
  margin-bottom: 14px;
}
.bar-text {
  font-size: 13px;
}

/* ===== 进入会议按钮 ===== */
.enter-btn {
  height: 48px;
  font-size: 17px;
  border-radius: 12px;
  background: #3677ef;
  border: none;
}
.enter-btn:active {
  opacity: 0.8;
}
.enter-btn:disabled {
  opacity: 0.5;
}
</style>