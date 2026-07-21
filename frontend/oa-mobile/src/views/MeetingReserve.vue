<template>
  <div class="oa-meeting-reserve">
    <!-- 顶部导航 -->
    <div class="page-header">
      <van-icon name="arrow-left" size="22" @click="$router.back()" />
      <h1 class="header-title">预订会议室</h1>
      <div class="header-right">
        <van-icon name="ellipsis" size="22" />
      </div>
    </div>

    <!-- 加载状态 -->
    <div class="loading-wrap" v-if="loading">
      <van-loading size="40" text="加载中..." vertical />
    </div>

    <!-- 内容 -->
    <template v-else>
      <!-- 会议室信息 -->
      <div class="room-info-card">
        <div class="room-info-header">
          <span class="room-name">{{ roomName }}</span>
          <span class="room-status" :class="roomStatus === '可用' ? 'status-free' : 'status-busy'">
            {{ roomStatus === '可用' ? '空闲' : '使用中' }}
          </span>
        </div>
        <div class="room-info-body">
          <span><van-icon name="location-o" /> {{ roomLocation || floor || '未知位置' }}</span>
          <span><van-icon name="user-o" /> {{ capacity }}人</span>
        </div>
      </div>

      <!-- 错误提示汇总 -->
      <div class="error-summary" v-if="errors.length > 0">
        <div class="error-header">
          <van-icon name="warning-o" size="18" color="#ee0a24" />
          <span class="error-title">请修正以下问题：</span>
          <van-icon name="close" size="16" color="#999" @click="errors = []" />
        </div>
        <div class="error-list">
          <div class="error-item" v-for="(error, index) in errors" :key="index">
            <span class="error-dot">•</span>
            <span class="error-text">{{ error }}</span>
          </div>
        </div>
      </div>

      <!-- 预订表单 -->
      <div class="form-wrap">
        <van-form @submit="onSubmit" ref="formRef">
          <van-cell-group inset>
            <van-field
              v-model="formData.date"
              name="date"
              label="预订日期"
              placeholder="请选择日期"
              is-link
              @click="showDatePicker = true"
              :class="{ 'field-error': fieldErrors.date }"
            />
            <van-field
              v-model="formData.startTime"
              name="startTime"
              label="开始时间"
              placeholder="请选择"
              is-link
              @click="showStartPicker = true"
              :class="{ 'field-error': fieldErrors.startTime }"
            />
            <van-field
              v-model="formData.endTime"
              name="endTime"
              label="结束时间"
              placeholder="请选择"
              is-link
              @click="showEndPicker = true"
              :class="{ 'field-error': fieldErrors.endTime }"
            />
            <van-field
              v-model="formData.title"
              name="title"
              label="会议主题"
              placeholder="请输入会议主题"
              :class="{ 'field-error': fieldErrors.title }"
            />
            <van-field
              v-model="participantDisplay"
              name="participants"
              label="参会人"
              placeholder="请选择参会人"
              is-link
              @click="showParticipantPicker = true"
              :class="{ 'field-error': fieldErrors.participants }"
            />
            <van-field
              v-model="formData.description"
              name="description"
              label="备注说明"
              placeholder="请输入备注（选填）"
              type="textarea"
              rows="2"
            />
          </van-cell-group>

          <!-- 提交按钮 -->
          <div class="submit-wrap">
            <van-button type="primary" block size="large" native-type="submit" :loading="submitting">
              确认预订
            </van-button>
            <van-button plain block size="large" @click="$router.back()" style="margin-top:10px;">
              取消
            </van-button>
          </div>
        </van-form>
      </div>
    </template>

    <!-- 日期选择 -->
    <van-calendar 
      v-model="dateValue"
      type="date" 
      :show="showDatePicker"
      @confirm="onConfirmDate" 
      @close="showDatePicker = false"
      :min-date="minDate"
      title="选择日期"
    />

    <!-- 时间选择 -->
    <van-popup v-model:show="showStartPicker" position="bottom" round>
      <van-picker :columns="timeColumns" @confirm="onConfirmStart" @cancel="showStartPicker = false" title="选择开始时间" />
    </van-popup>
    <van-popup v-model:show="showEndPicker" position="bottom" round>
      <van-picker :columns="timeColumns" @confirm="onConfirmEnd" @cancel="showEndPicker = false" title="选择结束时间" />
    </van-popup>

    <!-- 参会人选择弹窗 -->
    <van-popup v-model:show="showParticipantPicker" position="bottom" round style="height: 80%;">
      <div style="padding:16px;border-bottom:1px solid #f0f0f0;display:flex;justify-content:space-between;align-items:center;">
        <h3 style="margin:0;">选择参会人</h3>
        <van-button text size="small" @click="confirmParticipants">确定</van-button>
      </div>
      <div style="padding:12px 16px;overflow-y:auto;height:calc(100% - 60px);">
        <!-- 搜索框 -->
        <van-search
          v-model="searchKeyword"
          placeholder="搜索员工姓名"
          shape="round"
          @update:model-value="onSearch"
        />

        <!-- 部门筛选 -->
        <div class="dept-filter-bar">
          <span
            class="dept-chip"
            :class="{ active: selectedDeptFilter === null }"
            @click="selectedDeptFilter = null"
          >全部</span>
          <span
            v-for="dept in departmentList"
            :key="dept.id"
            class="dept-chip"
            :class="{ active: selectedDeptFilter === dept.id }"
            @click="selectedDeptFilter = dept.id"
          >{{ dept.name }}</span>
        </div>

        <!-- 员工列表（多选） -->
        <van-checkbox-group v-model="selectedEmployeeIds">
          <van-cell
            v-for="emp in filteredEmployeeList"
            :key="emp.id"
            clickable
            @click="toggleCheckbox(emp.id)"
          >
            <template #left-icon>
              <van-checkbox :name="emp.id" :ref="el => checkboxRefs[emp.id] = el" />
            </template>
            <template #title>
              <div class="emp-name">{{ emp.name }}</div>
            </template>
            <template #label>
              <div class="emp-info">{{ getDeptName(emp.departmentId) }} · {{ emp.position || '员工' }}</div>
            </template>
          </van-cell>
        </van-checkbox-group>

        <div v-if="filteredEmployeeList.length === 0" class="search-empty">
          <van-icon name="search" size="40" color="#ccc" />
          <p>未找到匹配的员工</p>
        </div>

        <!-- 已选人员预览 -->
        <div v-if="selectedParticipants.length > 0" class="selected-preview">
          <div class="preview-title">
            已选择 ({{ selectedParticipants.length }}人)
            <span class="clear-all" @click="clearAllParticipants">清空</span>
          </div>
          <div class="preview-list">
            <span v-for="p in selectedParticipants" :key="p.id" class="preview-tag">
              {{ p.name }}
              <van-icon name="cross" size="12" @click="removeParticipant(p.id)" />
            </span>
          </div>
        </div>
      </div>
    </van-popup>

    <div class="safe-bottom"></div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { showToast } from 'vant'
import { getMeetingRoomById, bookMeeting } from '@/api/meeting'
import { getDepartmentList } from '@/api/department'
import { getAllEmployees } from '@/api/employee'

const router = useRouter()
const route = useRoute()

// =============================================
// ===== 会议室信息 =====
// =============================================
const roomId = ref(null)
const roomName = ref('')
const roomStatus = ref('')
const roomLocation = ref('')
const floor = ref('')
const capacity = ref(0)
const loading = ref(true)

// =============================================
// ===== 用户信息 =====
// =============================================
const employeeId = ref(1)
const employeeName = ref('用户')

// =============================================
// ===== 表单数据 =====
// =============================================
const formData = ref({
  date: '',
  startTime: '',
  endTime: '',
  title: '',
  description: ''
})

// =============================================
// ===== 参会人选择 =====
// =============================================
const showParticipantPicker = ref(false)
const employeeList = ref([])
const departmentList = ref([])
const selectedEmployeeIds = ref([])
const selectedParticipants = ref([])
const participantDisplay = ref('')
const searchKeyword = ref('')
const selectedDeptFilter = ref(null)
const checkboxRefs = ref({})

// 按搜索关键词和部门筛选员工
const filteredEmployeeList = computed(() => {
  let list = employeeList.value
  if (selectedDeptFilter.value !== null) {
    list = list.filter(e => e.departmentId === selectedDeptFilter.value)
  }
  if (searchKeyword.value.trim()) {
    const kw = searchKeyword.value.trim().toLowerCase()
    list = list.filter(e => e.name.toLowerCase().includes(kw))
  }
  return list
})

const getDeptName = (deptId) => {
  const dept = departmentList.value.find(d => d.id === deptId)
  return dept ? dept.name : '无部门'
}

const onSearch = () => {
  // 计算属性自动响应
}

const toggleCheckbox = (empId) => {
  const idx = selectedEmployeeIds.value.indexOf(empId)
  if (idx > -1) {
    selectedEmployeeIds.value.splice(idx, 1)
  } else {
    selectedEmployeeIds.value.push(empId)
  }
}

const clearAllParticipants = () => {
  selectedParticipants.value = []
  selectedEmployeeIds.value = []
  updateParticipantDisplay()
}

const removeParticipant = (id) => {
  selectedParticipants.value = selectedParticipants.value.filter(p => p.id !== id)
  selectedEmployeeIds.value = selectedEmployeeIds.value.filter(eid => eid !== id)
  updateParticipantDisplay()
}

const updateParticipantDisplay = () => {
  const names = selectedParticipants.value.map(p => p.name)
  if (names.length === 0) {
    participantDisplay.value = ''
  } else if (names.length <= 3) {
    participantDisplay.value = names.join('、')
  } else {
    participantDisplay.value = names.slice(0, 3).join('、') + ` 等${names.length}人`
  }
}

const confirmParticipants = () => {
  // 根据选中的ID构建参会人列表
  selectedParticipants.value = selectedEmployeeIds.value.map(empId => {
    const emp = employeeList.value.find(e => e.id === empId)
    return emp ? { id: emp.id, name: emp.name } : null
  }).filter(Boolean)

  updateParticipantDisplay()
  showParticipantPicker.value = false
}

// =============================================
// ===== 选择器控制 =====
// =============================================
const showDatePicker = ref(false)
const showStartPicker = ref(false)
const showEndPicker = ref(false)
const submitting = ref(false)
const formRef = ref()
const dateValue = ref(new Date())

// =============================================
// ===== 错误收集 =====
// =============================================
const errors = ref([])
const fieldErrors = ref({
  date: false,
  startTime: false,
  endTime: false,
  title: false,
  participants: false
})

// =============================================
// ===== 参会人方法 =====
// =============================================
const loadEmployeeAndDeptData = async () => {
  try {
    const [deptRes, empRes] = await Promise.all([
      getDepartmentList(),
      getAllEmployees()
    ])
    if (deptRes.code === 0 && deptRes.data) {
      departmentList.value = Array.isArray(deptRes.data) ? deptRes.data : []
    }
    if (empRes.code === 0 && empRes.data) {
      employeeList.value = Array.isArray(empRes.data) ? empRes.data : []
    }
  } catch (error) {
    console.error('加载数据失败:', error)
  }
}

// =============================================
// ===== 最小日期（今天） =====
// =============================================
const minDate = computed(() => {
  const now = new Date()
  now.setHours(0, 0, 0, 0)
  return now
})

// =============================================
// ===== 时间选项 =====
// =============================================
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

// =============================================
// ===== 加载会议室信息 =====
// =============================================
const loadRoomInfo = async (id) => {
  loading.value = true
  try {
    const res = await getMeetingRoomById(id)
    if (res.code === 0 && res.data) {
      const data = res.data
      roomName.value = data.name || '未知会议室'
      roomStatus.value = data.status || '可用'
      roomLocation.value = data.location || data.floor || ''
      floor.value = data.floor || data.location || ''
      capacity.value = data.capacity || 0
    } else {
      showToast(res.msg || '加载会议室信息失败')
    }
  } catch (error) {
    console.error('加载会议室信息失败:', error)
    showToast('加载失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

// =============================================
// ===== 日期/时间选择 =====
// =============================================
const onConfirmDate = (value) => {
  const date = new Date(value)
  formData.value.date = `${date.getFullYear()}-${String(date.getMonth()+1).padStart(2,'0')}-${String(date.getDate()).padStart(2,'0')}`
  showDatePicker.value = false
  fieldErrors.value.date = false
  clearErrors()
}

const onConfirmStart = ({ selectedValues }) => {
  formData.value.startTime = selectedValues[0]
  showStartPicker.value = false
  fieldErrors.value.startTime = false
  clearErrors()
}

const onConfirmEnd = ({ selectedValues }) => {
  formData.value.endTime = selectedValues[0]
  showEndPicker.value = false
  fieldErrors.value.endTime = false
  clearErrors()
}

// =============================================
// ===== 错误处理 =====
// =============================================
const clearErrors = () => {
  errors.value = []
}

const showAllErrors = () => {
  const errorList = []
  const fieldErrorMap = {
    date: false,
    startTime: false,
    endTime: false,
    title: false,
    participants: false
  }

  // 1. 检查日期
  if (!formData.value.date) {
    errorList.push('请选择预订日期')
    fieldErrorMap.date = true
  } else {
    const today = new Date()
    today.setHours(0, 0, 0, 0)
    const selected = new Date(formData.value.date)
    if (selected < today) {
      errorList.push('预订日期不能早于今天')
      fieldErrorMap.date = true
    }
  }

  // 2. 检查开始时间
  if (!formData.value.startTime) {
    errorList.push('请选择开始时间')
    fieldErrorMap.startTime = true
  }

  // 3. 检查结束时间
  if (!formData.value.endTime) {
    errorList.push('请选择结束时间')
    fieldErrorMap.endTime = true
  } else if (formData.value.startTime && formData.value.endTime) {
    const start = new Date(`2026-01-01 ${formData.value.startTime}`)
    const end = new Date(`2026-01-01 ${formData.value.endTime}`)
    if (end <= start) {
      errorList.push('结束时间必须晚于开始时间')
      fieldErrorMap.endTime = true
    }
  }

  // 4. 检查会议主题
  if (!formData.value.title) {
    errorList.push('请输入会议主题')
    fieldErrorMap.title = true
  } else if (formData.value.title.trim().length < 2) {
    errorList.push('会议主题至少2个字')
    fieldErrorMap.title = true
  }

  // 5. 检查参会人
  if (selectedParticipants.value.length === 0) {
    errorList.push('请选择参会人')
    fieldErrorMap.participants = true
  } else if (selectedParticipants.value.length > capacity.value) {
    errorList.push(`参会人数不能超过会议室容量（${capacity.value}人）`)
    fieldErrorMap.participants = true
  }

  errors.value = errorList
  fieldErrors.value = fieldErrorMap

  return errorList.length === 0
}

// =============================================
// ===== 提交预订 =====
// =============================================
const onSubmit = async () => {
  clearErrors()
  
  const isValid = showAllErrors()
  if (!isValid) {
    const errorEl = document.querySelector('.error-summary')
    if (errorEl) {
      errorEl.scrollIntoView({ behavior: 'smooth', block: 'center' })
    }
    return
  }

  // 检查会议室状态
  if (roomStatus.value !== '可用') {
    showToast('该会议室当前不可用')
    return
  }

  submitting.value = true
  try {
    const participantIds = selectedParticipants.value.map(p => p.id).join(',')
    const participantNames = selectedParticipants.value.map(p => p.name).join(',')
    
    const data = {
      roomId: parseInt(route.params.id),
      organizerId: employeeId.value,
      organizerName: employeeName.value,
      title: formData.value.title,
      startTime: `${formData.value.date}T${formData.value.startTime}:00`,
      endTime: `${formData.value.date}T${formData.value.endTime}:00`,
      participants: participantNames,
      participantIds: participantIds,
      remark: formData.value.description || ''
    }
    
    const res = await bookMeeting(data)
    if (res.code === 0) {
      showToast('🎉 会议室预订成功！')
      setTimeout(() => router.push('/meeting'), 500)
    } else {
      showToast(res.msg || '预订失败')
    }
  } catch (error) {
    console.error('预订失败:', error)
    showToast('预订失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}

// =============================================
// ===== 生命周期 =====
// =============================================
onMounted(() => {
  const id = parseInt(route.params.id) || parseInt(route.query.roomId)
  if (id) {
    roomId.value = id
    loadRoomInfo(id)
    loadEmployeeAndDeptData()
  } else {
    loading.value = false
    showToast('会议室不存在')
  }
  
  if (route.query.roomName) {
    roomName.value = route.query.roomName
  }
  
  if (route.query.date) {
    formData.value.date = route.query.date
  }

  if (route.query.startTime) {
    formData.value.startTime = route.query.startTime
  }
  
  // 获取用户信息
  const idFromStorage = localStorage.getItem('employeeId')
  if (idFromStorage) {
    employeeId.value = parseInt(idFromStorage)
  }
  employeeName.value = localStorage.getItem('name') || localStorage.getItem('username') || '用户'
})
</script>

<style scoped>
.oa-meeting-reserve {
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
  gap: 16px;
}

/* ===== 会议室信息卡片 ===== */
.room-info-card {
  background: #fff;
  border-radius: 14px;
  padding: 16px;
  margin-bottom: 16px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}
.room-info-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}
.room-name {
  font-size: 18px;
  font-weight: bold;
  color: #222;
}
.room-status {
  font-size: 13px;
  padding: 2px 14px;
  border-radius: 12px;
}
.room-status.status-free {
  color: #00b894;
  background: #e8f8f0;
}
.room-status.status-busy {
  color: #e17055;
  background: #ffe8e8;
}
.room-info-body {
  display: flex;
  gap: 20px;
  font-size: 14px;
  color: #888;
}
.room-info-body span {
  display: flex;
  align-items: center;
  gap: 4px;
}

/* ===== 错误汇总 ===== */
.error-summary {
  background: #fff0f0;
  border: 1px solid #ffcdd2;
  border-radius: 12px;
  padding: 14px 16px;
  margin-bottom: 16px;
}
.error-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}
.error-title {
  flex: 1;
  font-size: 14px;
  font-weight: 500;
  color: #d32f2f;
}
.error-list {
  padding-left: 4px;
}
.error-item {
  display: flex;
  align-items: flex-start;
  gap: 6px;
  padding: 3px 0;
  font-size: 13px;
  color: #c62828;
}
.error-dot {
  color: #c62828;
  font-weight: bold;
}
.error-text {
  line-height: 1.4;
}

/* ===== 表单错误高亮 ===== */
:deep(.field-error .van-field) {
  border: 2px solid #ee0a24 !important;
  border-radius: 8px;
}
:deep(.field-error .van-field__body) {
  background: #fff5f5 !important;
}

/* ===== 表单 ===== */
.form-wrap {
  padding-top: 4px;
}
:deep(.van-cell-group) {
  border-radius: 12px;
  overflow: hidden;
}
:deep(.van-field) {
  padding: 12px 16px;
}
:deep(.van-field__label) {
  color: #555;
  width: 80px;
}
:deep(.van-field__body input) {
  font-size: 14px;
}
:deep(.van-field__body textarea) {
  font-size: 14px;
}

/* ===== 提交按钮 ===== */
.submit-wrap {
  padding: 16px 0 20px;
}
.submit-wrap :deep(.van-button) {
  border-radius: 12px;
  height: 48px;
  font-size: 17px;
}
.submit-wrap :deep(.van-button--primary) {
  background: #6c5ce7;
  border: none;
}

/* ===== Picker 弹窗 ===== */
:deep(.van-popup) {
  border-radius: 16px 16px 0 0;
}

/* ===== 参会人选择样式 ===== */
.dept-filter-bar {
  display: flex;
  gap: 8px;
  overflow-x: auto;
  padding: 4px 0 12px;
  -webkit-overflow-scrolling: touch;
}
.dept-filter-bar::-webkit-scrollbar { display: none; }
.dept-chip {
  flex-shrink: 0;
  padding: 6px 16px;
  background: #f5f7fa;
  border-radius: 20px;
  font-size: 13px;
  color: #666;
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;
}
.dept-chip.active {
  background: #3677ef;
  color: #fff;
}

:deep(.van-cell) {
  padding: 10px 16px;
}
.emp-name {
  font-size: 15px;
  color: #222;
  font-weight: 500;
}
.emp-info {
  font-size: 12px;
  color: #999;
  margin-top: 2px;
}

.search-empty {
  text-align: center;
  padding: 40px 0;
  color: #ccc;
}
.search-empty p {
  margin-top: 8px;
  font-size: 14px;
}

.selected-preview {
  margin-top: 16px;
  padding: 12px;
  background: #f5f7fa;
  border-radius: 12px;
}
.preview-title {
  font-size: 14px;
  font-weight: 500;
  color: #333;
  margin-bottom: 10px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.clear-all {
  font-size: 12px;
  color: #ee0a24;
  font-weight: normal;
  cursor: pointer;
}
.preview-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.preview-tag {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 12px;
  background: #fff;
  border-radius: 20px;
  font-size: 13px;
  color: #333;
}

.safe-bottom { height: 20px; }
</style>