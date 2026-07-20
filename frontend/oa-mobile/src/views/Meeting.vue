<template>
  <div class="oa-meeting" ref="scrollContainer">
    <div class="banner">
      <div class="banner-text">
        <h2 class="banner-title">会议室预订</h2>
        <p class="banner-desc">高效协作 · 空间共享</p>
      </div>
      <div class="banner-icon">
        <van-icon name="location-o" size="48" color="#6c5ce7" />
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="stat-row">
      <div class="stat-card" style="background: linear-gradient(135deg, #fff, #f0f7ff);">
        <p class="stat-label">总会议室</p>
        <span class="stat-num" style="color: #3677ef;">{{ roomList.length }}</span>
      </div>
      <div class="stat-card" style="background: linear-gradient(135deg, #fff, #f0fff4);">
        <p class="stat-label">空闲中</p>
        <span class="stat-num" style="color: #00b894;">{{ stats.free }}</span>
      </div>
      <div class="stat-card" style="background: linear-gradient(135deg, #fff, #fff8ef);">
        <p class="stat-label">使用中</p>
        <span class="stat-num" style="color: #ff6b35;">{{ stats.busy }}</span>
      </div>
      <div class="stat-card" style="background: linear-gradient(135deg, #fff, #fef0f0);" @click="showMyBookings = true">
        <p class="stat-label">我的预订</p>
        <span class="stat-num" style="color: #e17055;">{{ myBookings.length }}</span>
      </div>
    </div>

    <!-- 日期选择 -->
    <div class="date-wrap">
      <van-icon name="calendar-o" size="20" color="#6c5ce7" />
      <span class="date-text">{{ currentDate }}</span>
      <van-icon name="arrow-down" size="16" color="#6c5ce7" @click="showCalendar = true" />
    </div>

    <!-- 加载状态 -->
    <div class="loading-wrap" v-if="loading">
      <van-loading size="40" text="加载中..." vertical />
    </div>

    <!-- 会议室列表 -->
    <div class="room-list" v-else>
      <div class="room-item" v-for="item in roomList" :key="item.id" :id="'room-' + item.id">
        <div class="room-header" @click="goDetail(item.id)">
          <div class="room-info">
            <h3 class="room-name">{{ item.name }}</h3>
            <span class="room-status" :class="item.status === '可用' ? 'status-free' : 'status-busy'">
              {{ item.status === '可用' ? '空闲' : '使用中' }}
            </span>
          </div>
          <span class="room-capacity"><van-icon name="user-o" size="14" /> {{ item.capacity }}人</span>
        </div>
        <div class="room-body" @click="goDetail(item.id)">
          <span class="room-location">{{ item.location || item.floor || '未知位置' }}</span>
          <div class="room-equipment">
            <span class="equip-tag" v-for="equip in getEquipmentList(item.equipment)" :key="equip">{{ equip }}</span>
          </div>
        </div>
        <div class="room-footer">
          <div class="time-slots">
            <span class="slot" v-for="slot in getTimeSlots(item.id)" :key="slot.time" 
                  :class="slot.status === '空闲' ? 'slot-free' : 'slot-busy'"
                  @click="selectTimeSlot(item, slot)">
              {{ slot.time }}
            </span>
          </div>
          <van-button 
            type="primary" 
            size="small" 
            round 
            class="book-btn" 
            @click="goReserve(item)"
            :disabled="getAvailableSlots(item.id).length === 0 || item.status !== '可用'"
          >
            预订
          </van-button>
        </div>
      </div>
      <div class="empty-state" v-if="roomList.length === 0">
        <van-icon name="location-o" size="48" color="#ccc" />
        <p>暂无会议室</p>
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
        <van-icon name="back-top" size="22" color="#6c5ce7" />
        <span>顶部</span>
      </div>
      <div class="scroll-btn" @click="scrollToBottom" v-show="showBottomBtn">
        <van-icon name="down" size="22" color="#6c5ce7" />
        <span>底部</span>
      </div>
    </div>

    <!-- ============================================= -->
    <!-- ===== 日历选择 ===== -->
    <!-- ============================================= -->
    <van-calendar 
      v-model="calendarValue"
      type="date" 
      :show="showCalendar"
      @confirm="onConfirmDate" 
      @close="showCalendar = false"
      :min-date="minDate"
      title="选择日期"
    />

    

    <!-- ============================================= -->
    <!-- ===== 我的预订列表 ===== -->
    <!-- ============================================= -->
    <van-popup v-model:show="showMyBookings" position="bottom" round style="padding:20px 16px 30px;max-height:70vh;overflow-y:auto;">
      <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:16px;">
        <h3 style="margin:0;">📋 我的预订</h3>
        <van-icon name="close" size="22" @click="showMyBookings = false" />
      </div>

      <div class="booking-list">
        <div class="booking-item" v-for="item in myBookings" :key="item.id">
          <div class="booking-header">
            <span class="booking-room">{{ item.roomName || item.roomId }}</span>
            <span class="booking-status" :class="item.status === '已确认' || item.status === '已预约' ? 'confirmed' : 'cancelled'">
              {{ item.status }}
            </span>
          </div>
          <div class="booking-body">
            <span class="booking-title">{{ item.title }}</span>
            <span class="booking-time">{{ formatDate(item.startTime) }} - {{ formatDate(item.endTime) }}</span>
          </div>
          <div class="booking-footer">
            <span class="booking-participants">👥 {{ item.participants || 0 }}人</span>
            <div class="booking-actions">
              <van-button size="mini" plain type="primary" @click="editBooking(item)">修改</van-button>
              <van-button size="mini" plain type="danger" @click="cancelBooking(item.id)">取消</van-button>
            </div>
          </div>
        </div>
        <div class="empty-state" v-if="myBookings.length === 0">
          <van-icon name="location-o" size="48" color="#ccc" />
          <p>暂无预订记录</p>
        </div>
      </div>
    </van-popup>

    <!-- 编辑预订弹窗 -->
    <van-popup v-model:show="showEditBooking" position="bottom" round style="padding:20px 16px 30px;max-height:80vh;overflow-y:auto;">
      <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:16px;">
        <h3 style="margin:0;">✏️ 修改预订</h3>
        <van-icon name="close" size="22" @click="showEditBooking = false" />
      </div>

      <van-form @submit="submitEditBooking">
        <van-cell-group inset>
          <van-field
            v-model="editForm.title"
            label="会议主题"
            placeholder="请输入会议主题"
            :rules="[{ required: true, message: '请输入会议主题' }]"
          />
          <van-field
            v-model="editForm.startTime"
            label="开始时间"
            placeholder="请选择"
            is-link
            @click="showEditStartPicker = true"
            :rules="[{ required: true, message: '请选择开始时间' }]"
          />
          <van-field
            v-model="editForm.endTime"
            label="结束时间"
            placeholder="请选择"
            is-link
            @click="showEditEndPicker = true"
            :rules="[
              { required: true, message: '请选择结束时间' },
              { validator: (v) => {
                if (!editForm.startTime || !v) return true
                return v > editForm.startTime
              }, message: '结束时间必须晚于开始时间' }
            ]"
          />
          <van-field
            v-model="editForm.participants"
            label="参会人数"
            placeholder="请输入人数"
            type="number"
            :rules="[{ required: true, message: '请输入参会人数' }]"
          />
        </van-cell-group>

        <div class="conflict-warning" v-if="editConflictDetected">
          <van-icon name="warning-o" size="16" color="#e17055" />
          <span>该时段已被其他人预订，请选择其他时间</span>
        </div>

        <div style="display:flex;gap:12px;margin-top:16px;">
          <van-button plain block @click="showEditBooking = false">取消</van-button>
          <van-button type="primary" block native-type="submit" :loading="editSubmitting" :disabled="editConflictDetected">
            保存修改
          </van-button>
        </div>
      </van-form>
    </van-popup>

    <!-- 编辑时间选择 -->
    <van-popup v-model:show="showEditStartPicker" position="bottom" round>
      <van-picker :columns="timeColumns" @confirm="onConfirmEditStart" @cancel="showEditStartPicker = false" title="选择开始时间" />
    </van-popup>
    <van-popup v-model:show="showEditEndPicker" position="bottom" round>
      <van-picker :columns="timeColumns" @confirm="onConfirmEditEnd" @cancel="showEditEndPicker = false" title="选择结束时间" />
    </van-popup>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { showToast, showConfirmDialog } from 'vant'
import { 
  getMeetingRoomList, 
  getMyMeetings, 
  bookMeeting, 
  cancelMeeting, 
  updateMeeting,
  getRoomMeetings 
} from '@/api/meeting'

const router = useRouter()

// =============================================
// ===== 当前时间 =====
// =============================================
const now = ref(new Date())
let timer = null

// =============================================
// ===== 滚动控制 =====
// =============================================
const scrollContainer = ref(null)
const showScrollButtons = ref(true)
const showTopBtn = ref(false)
const showBottomBtn = ref(true)

// =============================================
// ===== 状态 =====
// =============================================
const loading = ref(true)
  const stats = ref({ free: 0, busy: 0 })
  const currentDate = ref('')
  const showCalendar = ref(false)
  const calendarValue = ref(new Date())

// =============================================
// ===== 会议室数据 =====
// =============================================
const roomList = ref([])
const roomBookings = ref([])

// =============================================
// ===== 我的预订 =====
// =============================================
const myBookings = ref([])

// =============================================
// ===== 用户信息 =====
// =============================================
const employeeId = ref(1)
const employeeName = ref('用户')

// =============================================
// ===== 滚动控制方法 =====
// =============================================
const scrollToTop = () => {
  // 使用平滑滚动到顶部
  window.scrollTo({
    top: 0,
    behavior: 'smooth'
  })
  // 也尝试滚动容器
  if (scrollContainer.value) {
    scrollContainer.value.scrollTo({
      top: 0,
      behavior: 'smooth'
    })
  }
}

const scrollToBottom = () => {
  // 滚动到底部
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

// =============================================
// ===== 滚动事件监听 =====
// =============================================
const handleScroll = () => {
  const scrollTop = window.pageYOffset || document.documentElement.scrollTop
  const windowHeight = window.innerHeight
  const documentHeight = Math.max(
    document.documentElement.scrollHeight,
    document.body.scrollHeight
  )
  
  // 显示/隐藏顶部按钮（滚动超过200px显示）
  showTopBtn.value = scrollTop > 200
  
  // 显示/隐藏底部按钮（距离底部超过200px显示）
  const distanceToBottom = documentHeight - (scrollTop + windowHeight)
  showBottomBtn.value = distanceToBottom > 200
}

// =============================================
// ===== 时间槽位 =====
// =============================================
const allTimeSlots = ['08:00', '08:30', '09:00', '09:30', '10:00', '10:30', '11:00', '11:30', '12:00', '12:30', '13:00', '13:30', '14:00', '14:30', '15:00', '15:30', '16:00', '16:30', '17:00', '17:30', '18:00', '18:30', '19:00', '19:30', '20:00', '20:30', '21:00']

const getTimeSlots = (roomId) => {
  const currentHour = now.value.getHours() + now.value.getMinutes() / 60
  const roomBookingsData = roomBookings.value.filter(b => b.roomId === roomId)
  
  return allTimeSlots
    .filter(time => {
      const hour = parseInt(time.split(':')[0]) + parseInt(time.split(':')[1]) / 60
      return hour > currentHour
    })
    .map(time => {
      const isBooked = roomBookingsData.some(b => {
        const start = new Date(b.startTime)
        const end = new Date(b.endTime)
        const slotTime = new Date(`${currentDate.value}T${time}:00`)
        return slotTime >= start && slotTime < end
      })
      return { time, status: isBooked ? '占用' : '空闲' }
    })
}

const getAvailableSlots = (roomId) => {
  return getTimeSlots(roomId).filter(s => s.status === '空闲')
}

// =============================================
// ===== 工具方法 =====
// =============================================
const formatDate = (dateStr) => {
  if (!dateStr) return ''
  try {
    const date = new Date(dateStr)
    return `${date.getFullYear()}-${String(date.getMonth()+1).padStart(2,'0')}-${String(date.getDate()).padStart(2,'0')} ${String(date.getHours()).padStart(2,'0')}:${String(date.getMinutes()).padStart(2,'0')}`
  } catch {
    return dateStr
  }
}

const getEquipmentList = (equipment) => {
  if (!equipment) return []
  if (typeof equipment === 'string') {
    return equipment.split(',').map(e => e.trim())
  }
  if (Array.isArray(equipment)) {
    return equipment
  }
  return []
}

// =============================================
// ===== 加载数据 =====
// =============================================
const loadRoomList = async () => {
  try {
    const res = await getMeetingRoomList()
    if (res.code === 0) {
      roomList.value = res.data || []
      updateRoomStatus()
    }
  } catch (error) {
    console.error('加载会议室列表失败:', error)
    showToast('加载会议室失败')
  }
}

const loadMyBookings = async () => {
  try {
    const id = localStorage.getItem('employeeId')
    if (!id) return
    const res = await getMyMeetings(parseInt(id))
    if (res.code === 0) {
      myBookings.value = res.data || []
    }
  } catch (error) {
    console.error('加载我的预订失败:', error)
  }
}

const loadRoomBookings = async (roomId) => {
  try {
    const res = await getRoomMeetings(roomId)
    if (res.code === 0) {
      roomBookings.value = res.data || []
    }
  } catch (error) {
    console.error('加载会议室预订失败:', error)
  }
}

// =============================================
// ===== 更新会议室状态 =====
// =============================================
const updateRoomStatus = () => {
  let free = 0
  let busy = 0
  roomList.value.forEach(room => {
    if (room.status === '可用') {
      free++
    } else {
      busy++
    }
  })
  stats.value.free = free
  stats.value.busy = busy
}

// =============================================
// ===== 日期 =====
// =============================================
const minDate = computed(() => {
  const d = new Date()
  d.setHours(0, 0, 0, 0)
  return d
})

const onConfirmDate = (value) => {
  const date = new Date(value)
  currentDate.value = `${date.getFullYear()}-${String(date.getMonth()+1).padStart(2,'0')}-${String(date.getDate()).padStart(2,'0')}`
  showCalendar.value = false
}

// =============================================
// ===== 预订 =====
// =============================================
const goReserve = (room) => {
  router.push({
    path: '/meeting/reserve',
    query: {
      roomId: room.id,
      roomName: room.name,
      date: currentDate.value || new Date().toISOString().split('T')[0]
    }
  })
}

// =============================================
// ===== 我的预订 - 取消/修改 =====
// =============================================
const showMyBookings = ref(false)
const showEditBooking = ref(false)
const editBookingId = ref(null)
const editSubmitting = ref(false)
const editConflictDetected = ref(false)
const showEditStartPicker = ref(false)
const showEditEndPicker = ref(false)

const editForm = ref({
  title: '',
  startTime: '',
  endTime: '',
  participants: ''
})

const editBooking = (item) => {
  editBookingId.value = item.id
  editForm.value = {
    title: item.title,
    startTime: formatTimeOnly(item.startTime),
    endTime: formatTimeOnly(item.endTime),
    participants: String(item.participants || 0)
  }
  editConflictDetected.value = false
  showEditBooking.value = true
}

const formatTimeOnly = (dateStr) => {
  if (!dateStr) return ''
  try {
    const date = new Date(dateStr)
    return `${String(date.getHours()).padStart(2,'0')}:${String(date.getMinutes()).padStart(2,'0')}`
  } catch {
    return ''
  }
}

const cancelBooking = async (id) => {
  showConfirmDialog({
    title: '确认取消',
    message: '确定要取消该预订吗？',
    confirmButtonText: '确定取消',
    confirmButtonColor: '#ee0a24'
  }).then(async () => {
    try {
      const res = await cancelMeeting(id)
      if (res.code === 0) {
        showToast('已取消预订')
        await loadMyBookings()
        await loadRoomList()
      } else {
        showToast(res.msg || '取消失败')
      }
    } catch (error) {
      showToast('取消失败，请稍后重试')
    }
  }).catch(() => {})
}

const onConfirmEditStart = ({ selectedValues }) => {
  editForm.value.startTime = selectedValues[0]
  showEditStartPicker.value = false
  setTimeout(checkEditConflict, 100)
}

const onConfirmEditEnd = ({ selectedValues }) => {
  editForm.value.endTime = selectedValues[0]
  showEditEndPicker.value = false
  setTimeout(checkEditConflict, 100)
}

const checkEditConflict = () => {
  if (!editBookingId.value || !editForm.value.startTime || !editForm.value.endTime) {
    editConflictDetected.value = false
    return
  }
  
  const booking = myBookings.value.find(b => b.id === editBookingId.value)
  if (!booking) return
  
  const date = new Date(booking.startTime).toISOString().split('T')[0]
  const start = new Date(`${date}T${editForm.value.startTime}:00`)
  const end = new Date(`${date}T${editForm.value.endTime}:00`)
  
  const existing = roomBookings.value.filter(b => 
    b.roomId === booking.roomId && b.id !== editBookingId.value
  )
  
  const hasConflict = existing.some(b => {
    const bStart = new Date(b.startTime)
    const bEnd = new Date(b.endTime)
    return start < bEnd && end > bStart
  })
  
  editConflictDetected.value = hasConflict
}

const submitEditBooking = async () => {
  if (editConflictDetected.value) {
    showToast('该时段已被其他人预订，请选择其他时间')
    return
  }
  
  editSubmitting.value = true
  try {
    const booking = myBookings.value.find(b => b.id === editBookingId.value)
    if (!booking) return
    
    const date = new Date(booking.startTime).toISOString().split('T')[0]
    const data = {
      id: editBookingId.value,
      title: editForm.value.title,
      startTime: `${date}T${editForm.value.startTime}:00`,
      endTime: `${date}T${editForm.value.endTime}:00`,
      participants: parseInt(editForm.value.participants)
    }
    const res = await updateMeeting(data)
    if (res.code === 0) {
      showToast('预订已修改')
      showEditBooking.value = false
      await loadMyBookings()
      await loadRoomBookings(booking.roomId)
    } else {
      showToast(res.msg || '修改失败')
    }
  } catch (error) {
    showToast('修改失败，请稍后重试')
  } finally {
    editSubmitting.value = false
  }
}

// =============================================
// ===== 跳转详情 =====
// =============================================
const goDetail = (id) => {
  router.push(`/meeting/detail/${id}`)
}

// =============================================
// ===== 生命周期 =====
// =============================================
const updateCurrentTime = () => {
  now.value = new Date()
}

onMounted(async () => {
  const d = new Date()
  currentDate.value = `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}`
  
  // 获取用户信息
  const id = localStorage.getItem('employeeId')
  if (id) {
    employeeId.value = parseInt(id)
  }
  employeeName.value = localStorage.getItem('name') || localStorage.getItem('username') || '用户'
  
  // 加载数据
  loading.value = true
  await loadRoomList()
  await loadMyBookings()
  if (roomList.value.length > 0) {
    await loadRoomBookings(roomList.value[0].id)
  }
  loading.value = false
  
  timer = setInterval(updateCurrentTime, 60000)
  
  // 监听滚动事件
  window.addEventListener('scroll', handleScroll)
  // 初始检查
  setTimeout(handleScroll, 300)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
  window.removeEventListener('scroll', handleScroll)
})
</script>

<style scoped>
.oa-meeting {
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
  padding: 40px 0;
}

.banner {
  background: linear-gradient(135deg, #6c5ce7 0%, #8c7cf7 100%);
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
  grid-template-columns: 1fr 1fr 1fr 1fr;
  gap: 10px;
  margin-bottom: 16px;
}
.stat-card {
  background: #fff;
  border-radius: 12px;
  padding: 14px 8px;
  text-align: center;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
  cursor: pointer;
}
.stat-card:active { transform: scale(0.95); }
.stat-label { font-size: 12px; color: #888; margin: 0; }
.stat-num { font-size: 28px; font-weight: bold; }

.date-wrap {
  background: #fff;
  border-radius: 12px;
  padding: 12px 16px;
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 14px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}
.date-text { flex: 1; font-size: 16px; color: #333; }

.room-list { display: flex; flex-direction: column; gap: 12px; }
.room-item {
  background: #fff;
  border-radius: 14px;
  padding: 16px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}
.room-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; cursor: pointer; }
.room-info { display: flex; align-items: center; gap: 10px; }
.room-name { font-size: 17px; font-weight: bold; margin: 0; color: #222; }
.room-status { font-size: 12px; padding: 2px 10px; border-radius: 12px; }
.room-status.status-free { color: #00b894; background: #e8f8f0; }
.room-status.status-busy { color: #e17055; background: #ffe8e8; }
.room-capacity { color: #999; font-size: 13px; display: flex; align-items: center; gap: 4px; }
.room-body { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; cursor: pointer; }
.room-location { color: #aaa; font-size: 13px; }
.room-equipment { display: flex; gap: 6px; }
.equip-tag { font-size: 11px; padding: 2px 8px; border-radius: 10px; background: #f0f0f0; color: #666; }
.room-footer { display: flex; justify-content: space-between; align-items: center; padding-top: 10px; border-top: 1px solid #f5f5f5; }
.time-slots { display: flex; gap: 6px; flex-wrap: wrap; }
.slot { font-size: 11px; padding: 3px 10px; border-radius: 12px; border: 1px solid #eee; cursor: pointer; }
.slot-free { color: #00b894; border-color: #b2dfdb; background: #f0faf8; }
.slot-busy { color: #e17055; border-color: #ffcdd2; background: #faf0f0; cursor: not-allowed; }
.book-btn { height: 30px; padding: 0 18px; font-size: 13px; background: #6c5ce7; border: none; }
.book-btn:disabled { opacity: 0.5; }

.empty-state {
  text-align: center;
  padding: 40px 0;
  color: #ccc;
}
.empty-state p { margin-top: 8px; font-size: 14px; }

.reserve-room-info {
  background: #f8f9fa;
  border-radius: 10px;
  padding: 12px 16px;
  margin-bottom: 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.reserve-room-name { font-size: 16px; font-weight: 500; color: #222; }
.reserve-room-location { font-size: 13px; color: #999; }

.conflict-warning {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  background: #fff0e8;
  border-radius: 8px;
  margin-top: 12px;
  font-size: 13px;
  color: #e17055;
}

.booking-list { display: flex; flex-direction: column; gap: 12px; }
.booking-item {
  background: #f8f9fa;
  border-radius: 12px;
  padding: 14px 16px;
}
.booking-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}
.booking-room { font-size: 15px; font-weight: 500; color: #222; }
.booking-status { font-size: 12px; padding: 2px 12px; border-radius: 12px; }
.booking-status.confirmed { color: #00b894; background: #e8f8f0; }
.booking-status.cancelled { color: #e17055; background: #fff0e8; }
.booking-body { display: flex; flex-direction: column; gap: 2px; margin-bottom: 10px; }
.booking-title { font-size: 14px; color: #333; }
.booking-time { font-size: 12px; color: #999; }
.booking-footer {
  display: flex; justify-content: space-between; align-items: center; padding-top: 10px; border-top: 1px solid #f0f0f0; }
.booking-participants { font-size: 13px; color: #888; }
.booking-actions { display: flex; gap: 6px; }

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
:deep(.van-popup) { border-radius: 16px 16px 0 0; }

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
  box-shadow: 0 4px 16px rgba(108, 92, 231, 0.25);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s ease;
  border: 2px solid rgba(108, 92, 231, 0.15);
}

.scroll-btn:hover {
  transform: scale(1.05);
  box-shadow: 0 6px 24px rgba(108, 92, 231, 0.35);
}

.scroll-btn:active {
  transform: scale(0.92);
}

.scroll-btn span {
  font-size: 10px;
  color: #6c5ce7;
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

/* 当滚动到顶部时，顶部按钮淡出 */
.scroll-btn[data-hidden="true"] {
  opacity: 0;
  pointer-events: none;
  transform: scale(0.8);
}
</style>