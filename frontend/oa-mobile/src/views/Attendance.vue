<template>
  <div class="oa-attendance">
    <div class="banner">
      <div class="banner-text">
        <h2 class="banner-title">考勤打卡</h2>
        <p class="banner-desc">准时打卡 · 记录每一天</p>
      </div>
      <div class="banner-icon">
        <van-icon name="clock-o" size="48" color="#ff6b35" />
      </div>
    </div>

    <div class="location-bar" @click="getLocation">
      <van-icon name="location-o" size="18" :color="locationStatus === 'success' ? '#00b894' : '#ff6b35'" />
      <span class="location-text">{{ locationText }}</span>
      <van-icon 
        v-if="locationStatus === 'loading'" 
        name="replay" 
        size="16" 
        class="spin" 
        color="#3677ef" 
      />
      <van-icon 
        v-else-if="locationStatus === 'success'" 
        name="success" 
        size="16" 
        color="#00b894" 
      />
      <van-icon 
        v-else-if="locationStatus === 'error'" 
        name="fail" 
        size="16" 
        color="#e17055" 
      />
    </div>

    <div class="stat-row">
      <div class="stat-card" style="background: linear-gradient(135deg, #fff, #fff8ef);">
        <p class="stat-label">本月出勤</p>
        <span class="stat-num" style="color: #ff6b35;">{{ stats.attendance }}</span>
        <span class="stat-sub">天</span>
      </div>
      <div class="stat-card" style="background: linear-gradient(135deg, #fff, #f0f7ff);">
        <p class="stat-label">迟到</p>
        <span class="stat-num" style="color: #3677ef;">{{ stats.late }}</span>
        <span class="stat-sub">次</span>
      </div>
      <div class="stat-card" style="background: linear-gradient(135deg, #fff, #f0fff4);">
        <p class="stat-label">早退</p>
        <span class="stat-num" style="color: #00b894;">{{ stats.early }}</span>
        <span class="stat-sub">次</span>
      </div>
      <div class="stat-card" style="background: linear-gradient(135deg, #fff, #fef0f0);">
        <p class="stat-label">缺勤</p>
        <span class="stat-num" style="color: #e17055;">{{ stats.absent }}</span>
        <span class="stat-sub">天</span>
      </div>
    </div>

    <div class="clock-wrap">
      <div class="clock-circle">
        <div class="clock-time">{{ currentTime }}</div>
        <div class="clock-date">{{ currentDate }}</div>
        <div class="clock-status" :class="todayStatus === '已签到' ? 'checked-in' : 'not-checked'">
          {{ todayStatus }}
        </div>
      </div>
      <div class="clock-buttons">
        <van-button 
          type="primary" 
          size="large" 
          round 
          class="check-btn" 
          @click="handleCheckIn"
          :disabled="todayStatus === '已签到' || todayStatus === '已签到签退' || locationStatus !== 'success'"
          :loading="checkingIn"
        >
          {{ todayStatus === '已签到' || todayStatus === '已签到签退' ? '已签到' : '签到' }}
        </van-button>
        <van-button 
          plain 
          size="large" 
          round 
          class="check-btn" 
          @click="handleCheckOut" 
          :disabled="todayStatus === '未签到' || todayStatus === '已签退' || todayStatus === '已签到签退'"
        >
          签退
        </van-button>
      </div>
      <div class="location-tip" v-if="locationStatus === 'error'">
        <van-icon name="warning-o" size="14" color="#e17055" />
        <span>定位失败，请检查GPS权限后重试</span>
        <span class="retry-link" @click="getLocation">重新定位</span>
      </div>
      <div class="location-tip" v-if="locationStatus === 'success' && !isInRange">
        <van-icon name="warning-o" size="14" color="#e17055" />
        <span>您不在公司范围内，无法签到</span>
      </div>
    </div>

    <div class="section-wrap">
      <div class="section-header">
        <h3>今日考勤记录</h3>
        <span class="look-more" @click="goToHistory">查看全部 ></span>
      </div>
      <div class="record-list">
        <div class="record-item" v-for="(item, index) in todayRecords" :key="index">
          <van-icon :name="item.icon" size="18" :color="item.color" />
          <span class="record-time">{{ item.time }}</span>
          <span class="record-type">{{ item.type }}</span>
          <span class="record-location" v-if="item.location">{{ item.location }}</span>
          <span class="record-status" :class="item.status === '正常' ? 'normal' : 'abnormal'">
            {{ item.status }}
          </span>
        </div>
        <div class="empty-state" v-if="todayRecords.length === 0">
          <van-icon name="clock-o" size="40" color="#ccc" />
          <p>今日暂无打卡记录</p>
        </div>
      </div>
    </div>

    <van-popup v-model:show="showApplyCorrection" position="bottom" round style="padding:20px 16px 30px;max-height:80vh;overflow-y:auto;">
      <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:16px;">
        <h3 style="margin:0;">📋 补卡申请</h3>
        <van-icon name="close" size="22" @click="showApplyCorrection = false" />
      </div>

      <van-form @submit="submitCorrection">
        <van-cell-group inset>
          <van-field
            v-model="correctionData.date"
            label="补卡日期"
            placeholder="请选择日期"
            is-link
            @click="showCorrectionDatePicker = true"
            :rules="[{ required: true, message: '请选择补卡日期' }]"
          />
          <van-field
            v-model="correctionData.time"
            label="补卡时间"
            placeholder="请选择时间"
            is-link
            @click="showCorrectionTimePicker = true"
            :rules="[{ required: true, message: '请选择补卡时间' }]"
          />
          <van-field
            v-model="correctionData.type"
            label="补卡类型"
            placeholder="请选择"
            is-link
            @click="showCorrectionTypePicker = true"
            :rules="[{ required: true, message: '请选择补卡类型' }]"
          />
          <van-field
            v-model="correctionData.reason"
            label="补卡原因"
            placeholder="请简述补卡原因"
            type="textarea"
            rows="3"
            :rules="[
              { required: true, message: '请输入补卡原因' },
              { validator: (val) => val && val.trim().length >= 5, message: '补卡原因至少5个字' }
            ]"
          />
        </van-cell-group>

        <div style="display:flex;gap:12px;margin-top:16px;">
          <van-button plain block @click="showApplyCorrection = false">取消</van-button>
          <van-button type="primary" block native-type="submit" :loading="correctionSubmitting">
            提交申请
          </van-button>
        </div>
      </van-form>
    </van-popup>

    <van-calendar 
      :show="showCorrectionDatePicker"
      type="date" 
      @confirm="onConfirmCorrectionDate" 
      @close="showCorrectionDatePicker = false"
      :max-date="maxDate"
      title="选择补卡日期"
    />

    <van-popup v-model:show="showCorrectionTimePicker" position="bottom" round>
      <van-picker :columns="timeColumns" @confirm="onConfirmCorrectionTime" @cancel="showCorrectionTimePicker = false" title="选择时间" />
    </van-popup>

    <van-action-sheet v-model:show="showCorrectionTypePicker" title="选择补卡类型">
      <div class="picker-list">
        <div class="picker-item" v-for="item in correctionTypes" :key="item" @click="correctionData.type = item; showCorrectionTypePicker = false">
          <span>{{ item }}</span>
          <van-icon v-if="correctionData.type === item" name="success" color="#3677ef" />
        </div>
      </div>
    </van-action-sheet>

    <div class="bottom-bar">
      <van-button plain block size="large" @click="$router.back()" class="back-btn">
        <van-icon name="arrow-left" size="18" /> 返回
      </van-button>
      <van-button type="primary" size="large" @click="showApplyCorrection = true" class="correction-btn">
        补卡申请
      </van-button>
    </div>

    <div class="safe-bottom"></div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { checkIn, checkOut, getTodayStatus, getMyRecords, applyForCorrection, getCompanyLocation, calculateDistance } from '@/api/attendance'

const router = useRouter()
let timer = null

const COMPANY_LOCATION = ref({
  lat: 26.02552,
  lng: 119.40617,
  range: 500
})

const employeeId = ref(null)
const employeeName = ref('')

const stats = ref({ attendance: 0, late: 0, early: 0, absent: 0 })

const todayStatus = ref('未签到')
const checkingIn = ref(false)

const currentTime = ref('')
const currentDate = ref('')

const locationStatus = ref('loading')
const locationText = ref('正在获取位置...')
const isInRange = ref(false)
const currentLatitude = ref(null)
const currentLongitude = ref(null)

const todayRecords = ref([])

const showApplyCorrection = ref(false)
const showCorrectionDatePicker = ref(false)
const showCorrectionTimePicker = ref(false)
const showCorrectionTypePicker = ref(false)
const correctionSubmitting = ref(false)

const correctionData = ref({
  date: '',
  time: '',
  type: '',
  reason: ''
})

const correctionTypes = ['上班签到', '下班签退', '忘记打卡']

const maxDate = computed(() => {
  const now = new Date()
  return now
})

const timeColumns = [
  { text: '08:00', value: '08:00' },
  { text: '08:30', value: '08:30' },
  { text: '09:00', value: '09:00' },
  { text: '09:30', value: '09:30' },
  { text: '10:00', value: '10:00' },
  { text: '10:30', value: '10:30' },
  { text: '11:00', value: '11:00' },
  { text: '11:30', value: '11:30' },
  { text: '12:00', value: '12:00' },
  { text: '12:30', value: '12:30' },
  { text: '13:00', value: '13:00' },
  { text: '13:30', value: '13:30' },
  { text: '14:00', value: '14:00' },
  { text: '14:30', value: '14:30' },
  { text: '15:00', value: '15:00' },
  { text: '15:30', value: '15:30' },
  { text: '16:00', value: '16:00' },
  { text: '16:30', value: '16:30' },
  { text: '17:00', value: '17:00' },
  { text: '17:30', value: '17:30' },
  { text: '18:00', value: '18:00' }
]

const fetchCompanyLocation = async () => {
  try {
    const res = await getCompanyLocation()
    if (res.code === 0 && res.data) {
      COMPANY_LOCATION.value = {
        lat: res.data.latitude,
        lng: res.data.longitude,
        range: res.data.range
      }
    }
  } catch (error) {
    console.error('获取公司位置失败', error)
  }
}

const fetchTodayStatus = async () => {
  if (!employeeId.value) return
  try {
    const res = await getTodayStatus(employeeId.value)
    if (res.code === 0 && res.data) {
      const data = res.data
      if (data.checkIn && data.checkOut) {
        todayStatus.value = '已签到签退'
      } else if (data.checkIn) {
        todayStatus.value = '已签到'
      } else {
        todayStatus.value = '未签到'
      }
    }
  } catch (error) {
    console.error('获取今日状态失败', error)
  }
}

const fetchMyRecords = async () => {
  if (!employeeId.value) return
  try {
    const res = await getMyRecords(employeeId.value)
    if (res.code === 0 && res.data) {
      const records = Array.isArray(res.data) ? res.data : []
      const today = new Date().toISOString().split('T')[0]
      const todayRecordsData = records.filter(r => r.date === today)
      todayRecords.value = todayRecordsData.map(r => ({
        icon: r.checkInTime ? 'clock-o' : 'clock-o',
        color: r.status === '正常' ? '#00b894' : '#ff6b35',
        time: r.checkInTime ? new Date(r.checkInTime).toLocaleTimeString('zh-CN') : (r.checkOutTime ? new Date(r.checkOutTime).toLocaleTimeString('zh-CN') : ''),
        type: r.checkInTime && !r.checkOutTime ? '签到' : (r.checkOutTime ? '签退' : '签到'),
        location: '公司',
        status: r.status || '正常'
      }))
      calculateStats(records)
    }
  } catch (error) {
    console.error('获取考勤记录失败', error)
  }
}

const calculateStats = (records) => {
  const now = new Date()
  const currentMonth = now.getMonth()
  const currentYear = now.getFullYear()
  
  const monthRecords = records.filter(r => {
    const d = new Date(r.date)
    return d.getMonth() === currentMonth && d.getFullYear() === currentYear
  })
  
  let attendance = 0
  let late = 0
  let early = 0
  
  monthRecords.forEach(r => {
    if (r.checkInTime) {
      attendance++
      if (r.status && r.status.includes('迟到')) late++
      if (r.status && r.status.includes('早退')) early++
    }
  })
  
  stats.value = {
    attendance,
    late,
    early,
    absent: Math.max(0, now.getDate() - attendance)
  }
}

const handleCheckIn = async () => {
  if (todayStatus.value === '已签到' || todayStatus.value === '已签到签退') {
    showToast('您已签到，无需重复签到')
    return
  }
  
  if (locationStatus.value !== 'success') {
    showToast('请先获取定位')
    return
  }
  
  if (!isInRange.value) {
    showToast('您不在公司范围内，无法签到')
    return
  }
  
  checkingIn.value = true
  try {
    const data = {
      employeeId: employeeId.value,
      employeeName: employeeName.value
    }
    if (currentLatitude.value && currentLongitude.value) {
      data.latitude = currentLatitude.value
      data.longitude = currentLongitude.value
    }
    
    const res = await checkIn(data)
    if (res.code === 0) {
      showToast('✅ 签到成功！')
      await fetchTodayStatus()
      await fetchMyRecords()
    } else {
      showToast(res.message || '签到失败')
    }
  } catch (error) {
    showToast('签到失败，请稍后重试')
  } finally {
    checkingIn.value = false
  }
}

const handleCheckOut = async () => {
  if (todayStatus.value === '未签到') {
    showToast('请先签到')
    return
  }
  if (todayStatus.value === '已签退' || todayStatus.value === '已签到签退') {
    showToast('您已签退，无需重复签退')
    return
  }
  
  try {
    const data = {
      employeeId: employeeId.value
    }
    if (currentLatitude.value && currentLongitude.value) {
      data.latitude = currentLatitude.value
      data.longitude = currentLongitude.value
    }
    
    const res = await checkOut(data)
    if (res.code === 0) {
      showToast('✅ 签退成功！')
      await fetchTodayStatus()
      await fetchMyRecords()
    } else {
      showToast(res.message || '签退失败')
    }
  } catch (error) {
    showToast('签退失败，请稍后重试')
  }
}

const onConfirmCorrectionDate = (value) => {
  const date = new Date(value)
  correctionData.value.date = `${date.getFullYear()}-${String(date.getMonth()+1).padStart(2,'0')}-${String(date.getDate()).padStart(2,'0')}`
  showCorrectionDatePicker.value = false
}

const onConfirmCorrectionTime = ({ selectedValues }) => {
  correctionData.value.time = selectedValues[0]
  showCorrectionTimePicker.value = false
}

const submitCorrection = async () => {
  correctionSubmitting.value = true
  try {
    const data = {
      employeeId: employeeId.value,
      employeeName: employeeName.value,
      date: correctionData.value.date,
      time: correctionData.value.time,
      type: correctionData.value.type,
      reason: correctionData.value.reason
    }
    const res = await applyForCorrection(data)
    if (res.code === 0) {
      showToast('✅ 补卡申请已提交')
      showApplyCorrection.value = false
      correctionData.value = { date: '', time: '', type: '', reason: '' }
    } else {
      showToast(res.message || '提交失败')
    }
  } catch (error) {
    showToast('提交失败，请稍后重试')
  } finally {
    correctionSubmitting.value = false
  }
}

const calcDistance = (lat1, lng1, lat2, lng2) => {
  const R = 6371000
  const dLat = (lat2 - lat1) * Math.PI / 180
  const dLng = (lng2 - lng1) * Math.PI / 180
  const a = Math.sin(dLat/2) * Math.sin(dLat/2) +
            Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) *
            Math.sin(dLng/2) * Math.sin(dLng/2)
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a))
  return R * c
}

const getLocation = async () => {
  locationStatus.value = 'loading'
  locationText.value = '正在获取位置...'
  
  if (!navigator.geolocation) {
    locationStatus.value = 'error'
    locationText.value = '浏览器不支持定位'
    return
  }

  navigator.geolocation.getCurrentPosition(
    async (position) => {
      const { latitude, longitude } = position.coords
      currentLatitude.value = latitude
      currentLongitude.value = longitude
      
      try {
        const res = await calculateDistance(latitude, longitude)
        if (res.code === 0 && res.data) {
          isInRange.value = res.data.inRange
          locationStatus.value = 'success'
          locationText.value = isInRange.value 
            ? `📍 公司范围内 (${res.data.distance}m)`
            : `📍 距公司 ${res.data.distance}m，超出范围`
        } else {
          throw new Error('计算距离失败')
        }
      } catch (error) {
        const distance = calcDistance(
          latitude, longitude,
          COMPANY_LOCATION.value.lat, COMPANY_LOCATION.value.lng
        )
        isInRange.value = distance <= COMPANY_LOCATION.value.range
        locationStatus.value = 'success'
        locationText.value = isInRange.value 
          ? `📍 公司范围内 (${Math.round(distance)}m)`
          : `📍 距公司 ${Math.round(distance)}m，超出范围`
      }
    },
    () => {
      locationStatus.value = 'error'
      locationText.value = '定位失败，请检查GPS权限'
    },
    { enableHighAccuracy: true, timeout: 10000 }
  )
}

const updateTime = () => {
  const now = new Date()
  currentTime.value = now.toLocaleTimeString('zh-CN', { hour12: false })
  const weekdays = ['日', '一', '二', '三', '四', '五', '六']
  currentDate.value = `${now.getFullYear()}年${String(now.getMonth()+1).padStart(2,'0')}月${String(now.getDate()).padStart(2,'0')}日 周${weekdays[now.getDay()]}`
}

const goToHistory = () => {
  router.push('/attendance/history')
}

onMounted(() => {
  updateTime()
  timer = setInterval(updateTime, 1000)
  
  const storedEmployeeId = localStorage.getItem('employeeId')
  if (storedEmployeeId) {
    employeeId.value = parseInt(storedEmployeeId)
  }
  
  const storedName = localStorage.getItem('name') || localStorage.getItem('username')
  if (storedName) {
    employeeName.value = storedName
  }
  
  fetchCompanyLocation()
  fetchTodayStatus()
  fetchMyRecords()
  
  setTimeout(getLocation, 500)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
.oa-attendance {
  max-width: 430px;
  margin: 0 auto;
  min-height: 100vh;
  background: #f5f7fa;
  padding: 0 16px 20px;
  box-shadow: 0 0 20px rgba(0,0,0,0.06);
}

.banner {
  background: linear-gradient(135deg, #ff6b35 0%, #ff8a65 100%);
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

.location-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  background: #fff;
  border-radius: 12px;
  padding: 12px 16px;
  margin-bottom: 14px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
  cursor: pointer;
}
.location-bar:active { opacity: 0.7; }
.location-text { flex: 1; font-size: 14px; color: #333; }
.spin { animation: spin 1s linear infinite; }
@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
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
}
.stat-label { font-size: 12px; color: #888; margin: 0; }
.stat-num { font-size: 28px; font-weight: bold; }
.stat-sub { font-size: 13px; color: #888; margin-left: 2px; }

.clock-wrap {
  background: #fff;
  border-radius: 14px;
  padding: 30px 20px;
  text-align: center;
  margin-bottom: 14px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}
.clock-circle {
  position: relative;
  width: 200px;
  height: 200px;
  border-radius: 50%;
  border: 4px solid #e8f0ff;
  margin: 0 auto 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}
.clock-time { font-size: 36px; font-weight: bold; color: #333; }
.clock-date { font-size: 14px; color: #888; margin-top: 4px; }
.clock-status {
  font-size: 13px;
  margin-top: 6px;
  padding: 2px 16px;
  border-radius: 12px;
}
.clock-status.checked-in { color: #00b894; background: #e8f8f0; }
.clock-status.not-checked { color: #e17055; background: #ffe8e8; }

.clock-buttons { display: flex; gap: 12px; }
.clock-buttons .check-btn { 
  flex: 1; 
  height: 48px;
  font-size: 16px;
}
.clock-buttons .check-btn:disabled { opacity: 0.5; }

.location-tip {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  margin-top: 12px;
  font-size: 13px;
  color: #e17055;
  flex-wrap: wrap;
}
.retry-link { color: #3677ef; cursor: pointer; text-decoration: underline; }

.section-wrap {
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
.section-header h3 { font-size: 17px; font-weight: bold; margin: 0; color: #222; }
.look-more { font-size: 13px; color: #999; cursor: pointer; }
.look-more:active { color: #ff6b35; }

.record-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 0;
  border-bottom: 1px solid #f5f5f5;
}
.record-item:last-child { border-bottom: none; }
.record-time { font-size: 14px; color: #333; flex: 1; }
.record-type { font-size: 14px; color: #666; }
.record-location { font-size: 11px; color: #999; background: #f0f0f0; padding: 2px 8px; border-radius: 8px; }
.record-status { font-size: 12px; padding: 2px 12px; border-radius: 12px; }
.record-status.normal { color: #00b894; background: #e8f8f0; }
.record-status.abnormal { color: #ff6b35; background: #fff0e8; }

.empty-state {
  text-align: center;
  padding: 20px 0;
  color: #ccc;
}
.empty-state p { margin-top: 8px; font-size: 14px; }

.bottom-bar {
  display: flex;
  gap: 10px;
  padding: 16px 0 8px;
}
.back-btn {
  flex: 1;
  border-radius: 12px !important;
  height: 48px !important;
  font-size: 16px !important;
  border-color: #ff6b35 !important;
  color: #ff6b35 !important;
}
.back-btn:active { background: #fff0e8 !important; }
.correction-btn {
  flex: 1;
  border-radius: 12px !important;
  height: 48px !important;
  font-size: 16px !important;
  background: #ff6b35 !important;
  border: none !important;
}
.correction-btn:active { opacity: 0.8; }

.picker-list { padding: 12px 16px 20px; }
.picker-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 12px;
  border-bottom: 1px solid #f5f5f5;
  cursor: pointer;
}
.picker-item:active { background: #f5f7fa; }
.picker-item span { font-size: 15px; color: #333; }

.safe-bottom { height: 20px; }
:deep(.van-popup) { border-radius: 16px 16px 0 0; }
</style>