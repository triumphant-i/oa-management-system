<template>
  <div class="meeting-control">
    <div class="page-header">
      <van-icon name="arrow-left" size="22" @click="$router.back()" />
      <h1 class="header-title">会议控制</h1>
      <div class="header-right"></div>
    </div>

    <div class="loading-wrap" v-if="loading">
      <van-loading size="40" text="加载中..." vertical />
    </div>

    <template v-else-if="meeting">
      <div class="meeting-card">
        <div class="status-badge" :class="meeting.status === '进行中' ? 'status-active' : 'status-pending'">
          {{ meeting.status === '进行中' ? '⏳ 进行中' : '📅 已预约' }}
        </div>
        
        <h2 class="meeting-title">{{ meeting.title }}</h2>
        
        <div class="meeting-info">
          <div class="info-item">
            <van-icon name="location-o" size="16" color="#6c5ce7" />
            <span>{{ meeting.roomName }}</span>
          </div>
          <div class="info-item">
            <van-icon name="user-o" size="16" color="#6c5ce7" />
            <span>{{ meeting.participants }}</span>
          </div>
        </div>

        <div class="time-section">
          <div class="time-item">
            <span class="time-label">开始时间</span>
            <span class="time-value">{{ formatDateTime(meeting.startTime) }}</span>
          </div>
          <div class="time-item">
            <span class="time-label">结束时间</span>
            <span class="time-value">{{ formatDateTime(meeting.endTime) }}</span>
          </div>
        </div>

        <div class="countdown-section" v-if="meeting.status === '进行中'">
          <div class="countdown-label">剩余时间</div>
          <div class="countdown">
            <span class="countdown-num">{{ countdown.hours }}</span>
            <span class="countdown-sep">:</span>
            <span class="countdown-num">{{ countdown.minutes }}</span>
            <span class="countdown-sep">:</span>
            <span class="countdown-num">{{ countdown.seconds }}</span>
          </div>
          <div v-if="isNearEnd" class="warning-text">
            ⚠️ 会议即将结束，请及时处理
          </div>
        </div>
      </div>

      <div class="action-section">
        <van-button 
          v-if="meeting.status === '已预约'"
          type="primary" 
          block 
          size="large" 
          class="action-btn"
          @click="handleStart"
          :loading="loadingAction"
        >
          🎬 开始会议
        </van-button>

        <template v-else-if="meeting.status === '进行中'">
          <van-button 
            type="danger" 
            block 
            size="large" 
            class="action-btn"
            @click="handleEnd"
            :loading="loadingAction"
          >
            🔚 结束会议
          </van-button>
          
          <van-button 
            type="primary" 
            block 
            size="large" 
            class="action-btn"
            @click="showExtendModal = true"
            :disabled="!canExtend"
            :loading="loadingAction"
          >
            ⏰ 延长时间
          </van-button>
          <div v-if="!canExtend" class="extend-tip">
            延长后将与其他会议冲突
          </div>
        </template>

        <van-button 
          v-else-if="meeting.status === '已结束'"
          type="default" 
          block 
          size="large" 
          class="action-btn disabled-btn"
          disabled
        >
          ✅ 会议已结束
        </van-button>
      </div>

      <div class="tips-section">
        <div class="tip-item">
          <van-icon name="info-o" size="16" color="#6c5ce7" />
          <span>点击"开始会议"后，会议室将被锁定</span>
        </div>
        <div class="tip-item">
          <van-icon name="info-o" size="16" color="#6c5ce7" />
          <span>会议结束后自动释放会议室</span>
        </div>
      </div>
    </template>

    <div class="empty-state" v-else>
      <van-icon name="calendar-o" size="48" color="#ccc" />
      <p>暂无进行中的会议</p>
      <van-button type="primary" size="large" @click="$router.push('/meeting')">
        去预约会议室
      </van-button>
    </div>

    <van-popup v-model:show="showExtendModal" position="bottom" round style="padding:20px;">
      <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:16px;">
        <h3 style="margin:0;">延长会议时间</h3>
        <van-icon name="close" size="22" @click="showExtendModal = false" />
      </div>
      
      <div class="extend-options">
        <div 
          v-for="option in extendOptions" 
          :key="option.value"
          class="extend-option"
          :class="{ active: extendMinutes === option.value }"
          @click="extendMinutes = option.value"
        >
          {{ option.label }}
        </div>
      </div>

      <div style="display:flex;gap:12px;margin-top:16px;">
        <van-button plain block @click="showExtendModal = false">取消</van-button>
        <van-button type="primary" block @click="handleExtend" :loading="loadingAction">
          确认延长
        </van-button>
      </div>
    </van-popup>

    <div class="safe-bottom"></div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { showToast, showConfirmDialog } from 'vant'
import { 
  startMeeting, 
  endMeeting, 
  extendMeeting,
  getRoomMeetings
} from '@/api/meeting'

const router = useRouter()
const route = useRoute()

const loading = ref(true)
const loadingAction = ref(false)
const meeting = ref(null)
const showExtendModal = ref(false)
const extendMinutes = ref(30)
const canExtend = ref(true)
const isNearEnd = ref(false)

const countdown = ref({ hours: '00', minutes: '00', seconds: '00' })
let timer = null

const extendOptions = [
  { label: '延长15分钟', value: 15 },
  { label: '延长30分钟', value: 30 },
  { label: '延长45分钟', value: 45 },
  { label: '延长60分钟', value: 60 }
]

const formatDateTime = (dateStr) => {
  if (!dateStr) return ''
  try {
    const date = new Date(dateStr)
    return `${date.getFullYear()}-${String(date.getMonth()+1).padStart(2,'0')}-${String(date.getDate()).padStart(2,'0')} ${String(date.getHours()).padStart(2,'0')}:${String(date.getMinutes()).padStart(2,'0')}`
  } catch {
    return dateStr
  }
}

const loadMeeting = async () => {
  loading.value = true
  try {
    const id = parseInt(route.params.id)
    if (!id) {
      loading.value = false
      return
    }
    
    const res = await getRoomMeetings(route.query.roomId)
    if (res.code === 0 && res.data) {
      meeting.value = res.data.find(m => m.id === id)
      if (meeting.value) {
        checkCanExtend()
        if (meeting.value.status === '进行中') {
          startCountdown()
        }
      }
    }
  } catch (error) {
    console.error('加载会议信息失败:', error)
    showToast('加载失败')
  } finally {
    loading.value = false
  }
}

const checkCanExtend = async () => {
  if (!meeting.value || meeting.value.status !== '进行中') return
  
  try {
    const res = await getRoomMeetings(meeting.value.roomId)
    if (res.code === 0 && res.data) {
      const newEndTime = new Date(meeting.value.endTime)
      newEndTime.setMinutes(newEndTime.getMinutes() + 30)
      
      const hasConflict = res.data.some(m => {
        if (m.id === meeting.value.id || m.status === '已取消' || m.status === '已结束') return false
        const mStart = new Date(m.startTime)
        const mEnd = new Date(m.endTime)
        return newEndTime > mStart && newEndTime < mEnd
      })
      
      canExtend.value = !hasConflict
    }
  } catch (error) {
    console.error('检查延长冲突失败:', error)
  }
}

const startCountdown = () => {
  const updateCountdown = () => {
    if (!meeting.value) return
    
    const now = new Date()
    const end = new Date(meeting.value.endTime)
    const diff = end - now
    
    if (diff <= 0) {
      countdown.value = { hours: '00', minutes: '00', seconds: '00' }
      isNearEnd.value = true
      return
    }
    
    const hours = Math.floor(diff / (1000 * 60 * 60))
    const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60))
    const seconds = Math.floor((diff % (1000 * 60)) / 1000)
    
    countdown.value = {
      hours: String(hours).padStart(2, '0'),
      minutes: String(minutes).padStart(2, '0'),
      seconds: String(seconds).padStart(2, '0')
    }
    
    isNearEnd.value = minutes <= 10 && hours === 0
  }
  
  updateCountdown()
  timer = setInterval(updateCountdown, 1000)
}

const handleStart = async () => {
  showConfirmDialog({
    title: '确认开始',
    message: '确定要开始本次会议吗？',
    confirmButtonText: '确定开始',
    confirmButtonColor: '#6c5ce7'
  }).then(async () => {
    loadingAction.value = true
    try {
      const res = await startMeeting(meeting.value.id)
      if (res.code === 0) {
        showToast('会议已开始')
        meeting.value.status = '进行中'
        startCountdown()
      } else {
        showToast(res.msg || '操作失败')
      }
    } catch (error) {
      showToast('操作失败')
    } finally {
      loadingAction.value = false
    }
  }).catch(() => {})
}

const handleEnd = async () => {
  showConfirmDialog({
    title: '确认结束',
    message: '确定要提前结束本次会议吗？',
    confirmButtonText: '确定结束',
    confirmButtonColor: '#ee0a24'
  }).then(async () => {
    loadingAction.value = true
    try {
      const res = await endMeeting(meeting.value.id)
      if (res.code === 0) {
        showToast('会议已结束')
        meeting.value.status = '已结束'
        if (timer) clearInterval(timer)
      } else {
        showToast(res.msg || '操作失败')
      }
    } catch (error) {
      showToast('操作失败')
    } finally {
      loadingAction.value = false
    }
  }).catch(() => {})
}

const handleExtend = async () => {
  loadingAction.value = true
  try {
    const res = await extendMeeting(meeting.value.id, extendMinutes.value)
    if (res.code === 0) {
      showToast(res.msg || '延长成功')
      const newEnd = new Date(meeting.value.endTime)
      newEnd.setMinutes(newEnd.getMinutes() + extendMinutes.value)
      meeting.value.endTime = newEnd.toISOString()
      showExtendModal.value = false
      checkCanExtend()
    } else {
      showToast(res.msg || '延长失败')
    }
  } catch (error) {
    showToast('延长失败')
  } finally {
    loadingAction.value = false
  }
}

onMounted(() => {
  loadMeeting()
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
.meeting-control {
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
  padding: 16px;
  background: #fff;
  margin: 0 -16px;
}

.header-title {
  font-size: 18px;
  font-weight: 600;
  margin: 0;
  color: #222;
}

.loading-wrap {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 60px 0;
}

.meeting-card {
  background: #fff;
  border-radius: 16px;
  padding: 20px;
  margin: 16px 0;
  box-shadow: 0 2px 12px rgba(0,0,0,0.06);
}

.status-badge {
  display: inline-block;
  padding: 6px 16px;
  border-radius: 20px;
  font-size: 14px;
  font-weight: 500;
  margin-bottom: 12px;
}

.status-active {
  background: #e8f0ff;
  color: #3677ef;
}

.status-pending {
  background: #e8f8f0;
  color: #00b894;
}

.meeting-title {
  font-size: 20px;
  font-weight: bold;
  margin: 0 0 12px;
  color: #222;
}

.meeting-info {
  display: flex;
  gap: 20px;
  margin-bottom: 16px;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  color: #666;
}

.time-section {
  display: flex;
  gap: 20px;
  padding: 12px 0;
  border-top: 1px solid #f5f5f5;
  border-bottom: 1px solid #f5f5f5;
}

.time-item {
  flex: 1;
}

.time-label {
  display: block;
  font-size: 12px;
  color: #999;
  margin-bottom: 4px;
}

.time-value {
  font-size: 15px;
  color: #333;
  font-weight: 500;
}

.countdown-section {
  text-align: center;
  padding: 20px 0;
}

.countdown-label {
  font-size: 14px;
  color: #888;
  margin-bottom: 12px;
}

.countdown {
  font-size: 36px;
  font-weight: bold;
  color: #6c5ce7;
  font-family: 'Courier New', monospace;
}

.countdown-num {
  background: #f0e8ff;
  padding: 8px 12px;
  border-radius: 8px;
}

.countdown-sep {
  margin: 0 4px;
}

.warning-text {
  margin-top: 12px;
  font-size: 14px;
  color: #e17055;
  font-weight: 500;
}

.action-section {
  padding: 0 0 16px;
}

.action-btn {
  height: 52px;
  font-size: 17px;
  border-radius: 14px;
  margin-bottom: 12px;
}

.extend-tip {
  text-align: center;
  font-size: 13px;
  color: #999;
  margin-top: -8px;
}

.disabled-btn {
  background: #eee !important;
  color: #999 !important;
  border: none;
}

.tips-section {
  background: #fff;
  border-radius: 12px;
  padding: 16px;
}

.tip-item {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  font-size: 13px;
  color: #888;
  margin-bottom: 8px;
}

.tip-item:last-child {
  margin-bottom: 0;
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
  color: #ccc;
}

.empty-state p {
  margin: 12px 0 24px;
  font-size: 15px;
}

.extend-options {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.extend-option {
  padding: 14px;
  background: #f5f7fa;
  border-radius: 12px;
  text-align: center;
  font-size: 14px;
  color: #666;
  cursor: pointer;
  transition: all 0.2s;
}

.extend-option.active {
  background: #6c5ce7;
  color: #fff;
}

.safe-bottom { height: 20px; }
</style>