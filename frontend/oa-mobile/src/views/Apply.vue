<template>
  <div class="oa-apply">
    <div class="banner">
      <div class="banner-text">
        <h2 class="banner-title">发起申请</h2>
        <p class="banner-desc">快捷提交 · 流程清晰</p>
      </div>
      <div class="banner-icon">
        <van-icon name="edit" size="48" color="#fdcb6e" />
      </div>
    </div>

    <div class="type-grid">
      <div class="type-item" @click="selectType('leave')" :class="{ active: currentType === 'leave' }">
        <van-icon name="calendar-o" size="32" color="#00b894" />
        <span>请假申请</span>
      </div>
      <div class="type-item" @click="selectType('business')" :class="{ active: currentType === 'business' }">
        <van-icon name="location" size="32" color="#3677ef" />
        <span>出差申请</span>
      </div>
      <div class="type-item" @click="selectType('overtime')" :class="{ active: currentType === 'overtime' }">
        <van-icon name="clock" size="32" color="#fdcb6e" />
        <span>加班申请</span>
      </div>
      <div class="type-item" @click="selectType('reimburse')" :class="{ active: currentType === 'reimburse' }">
        <van-icon name="credit-pay" size="32" color="#e17055" />
        <span>报销申请</span>
      </div>
      <div class="type-item" @click="selectType('purchase')" :class="{ active: currentType === 'purchase' }">
        <van-icon name="shopping-cart-o" size="32" color="#6c5ce7" />
        <span>采购申请</span>
      </div>
      <div class="type-item" @click="selectType('card')" :class="{ active: currentType === 'card' }">
        <van-icon name="card" size="32" color="#00cec9" />
        <span>补卡申请</span>
      </div>
    </div>

    <div class="form-wrap" v-if="currentType">
      <van-form @submit="onSubmit">
        <van-cell-group inset>
          <van-field
            v-model="formData.title"
            label="申请标题"
            placeholder="请输入申请标题"
            :rules="[{ required: true, message: '请输入申请标题' }]"
          />

          <template v-if="currentType === 'leave'">
            <van-field
              v-model="formData.leaveType"
              label="请假类型"
              placeholder="请选择"
              is-link
              @click="showLeaveType = true"
              :rules="[{ required: true, message: '请选择请假类型' }]"
            />
            <van-field
              v-model="formData.startTime"
              label="开始日期"
              placeholder="请选择"
              is-link
              @click="showStartDate = true"
              :rules="[{ required: true, message: '请选择开始日期' }]"
            />
            <van-field
              v-model="formData.endTime"
              label="结束日期"
              placeholder="请选择"
              is-link
              @click="showEndDate = true"
              :rules="[{ required: true, message: '请选择结束日期' }]"
            />
          </template>

          <template v-else-if="currentType === 'business'">
            <van-field
              v-model="formData.destCity"
              label="出差城市"
              placeholder="请输入出差城市"
              :rules="[{ required: true, message: '请输入出差城市' }]"
            />
            <van-field
              v-model="formData.startTime"
              label="出发日期"
              placeholder="请选择"
              is-link
              @click="showStartDate = true"
              :rules="[{ required: true, message: '请选择出发日期' }]"
            />
            <van-field
              v-model="formData.endTime"
              label="返回日期"
              placeholder="请选择"
              is-link
              @click="showEndDate = true"
              :rules="[{ required: true, message: '请选择返回日期' }]"
            />
          </template>

          <template v-else-if="currentType === 'overtime'">
            <van-field
              v-model="formData.workDate"
              label="加班日期"
              placeholder="请选择"
              is-link
              @click="showStartDate = true"
              :rules="[{ required: true, message: '请选择加班日期' }]"
            />
            <van-field
              v-model="formData.startTimeOnly"
              label="开始时间"
              placeholder="请选择"
              is-link
              @click="showStartTimePicker = true"
              :rules="[{ required: true, message: '请选择开始时间' }]"
            />
            <van-field
              v-model="formData.endTimeOnly"
              label="结束时间"
              placeholder="请选择"
              is-link
              @click="showEndTimePicker = true"
              :rules="[{ required: true, message: '请选择结束时间' }]"
            />
          </template>

          <template v-else-if="currentType === 'reimburse'">
            <van-field
              v-model="formData.expenseType"
              label="报销类型"
              placeholder="请输入报销类型"
              :rules="[{ required: true, message: '请输入报销类型' }]"
            />
            <van-field
              v-model="formData.amount"
              label="报销金额"
              placeholder="请输入金额"
              type="digit"
              :rules="[{ required: true, message: '请输入金额' }]"
            />
            <van-field
              v-model="formData.expenseDate"
              label="费用日期"
              placeholder="请选择"
              is-link
              @click="showStartDate = true"
              :rules="[{ required: true, message: '请选择日期' }]"
            />
          </template>

          <template v-else-if="currentType === 'purchase'">
            <van-field
              v-model="formData.goodsName"
              label="物品名称"
              placeholder="请输入物品名称"
              :rules="[{ required: true, message: '请输入物品名称' }]"
            />
            <van-field
              v-model="formData.quantity"
              label="采购数量"
              placeholder="请输入数量"
              type="number"
              :rules="[{ required: true, message: '请输入数量' }]"
            />
            <van-field
              v-model="formData.unitPrice"
              label="单价"
              placeholder="请输入单价"
              type="digit"
              :rules="[{ required: true, message: '请输入单价' }]"
            />
          </template>

          <template v-else-if="currentType === 'card'">
            <van-field
              v-model="formData.cardDate"
              label="补卡日期"
              placeholder="请选择"
              is-link
              @click="showStartDate = true"
              :rules="[{ required: true, message: '请选择补卡日期' }]"
            />
            <van-field
              v-model="formData.cardTime"
              label="补卡时间"
              placeholder="请选择"
              is-link
              @click="showStartTimePicker = true"
              :rules="[{ required: true, message: '请选择补卡时间' }]"
            />
            <van-field
              v-model="formData.cardType"
              label="补卡类型"
              placeholder="请选择"
              is-link
              @click="showCardType = true"
              :rules="[{ required: true, message: '请选择补卡类型' }]"
            />
          </template>

          <van-field
            v-model="formData.content"
            label="申请理由"
            placeholder="请输入申请理由"
            type="textarea"
            rows="3"
            :rules="[{ required: true, message: '请输入申请理由' }]"
          />
          
          <!-- 附件上传 -->
          <div class="attachment-section">
            <div class="attachment-header">
              <span class="attachment-label">附件</span>
              <span class="attachment-count">{{ attachments.length }}个</span>
            </div>
            <div class="attachment-list">
              <div 
                v-for="(file, index) in attachments" 
                :key="file.id || index" 
                class="attachment-item"
              >
                <div class="attachment-icon" :class="getFileIconClass(file.name)">
                  <van-icon :name="getFileIcon(file.name)" size="24" />
                </div>
                <div class="attachment-info">
                  <span class="attachment-name">{{ file.name }}</span>
                  <span class="attachment-size">{{ formatFileSize(file.size) }}</span>
                </div>
                <van-icon name="delete" size="18" color="#ee0a24" @click="removeAttachment(index)" />
              </div>
              <div 
                class="attachment-item upload-btn" 
                @click="triggerFileUpload"
                v-if="attachments.length < 10"
              >
                <van-icon name="plus" size="24" color="#999" />
                <span class="upload-text">添加附件</span>
              </div>
            </div>
            <input 
              ref="fileInput" 
              type="file" 
              multiple 
              class="hidden-input"
              @change="handleFileChange"
              accept=".pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.jpg,.jpeg,.png,.gif"
            />
          </div>
        </van-cell-group>

        <div class="submit-wrap">
          <van-button type="primary" block size="large" native-type="submit" :loading="submitting">
            提交申请
          </van-button>
          <van-button plain block size="large" @click="resetForm" style="margin-top:10px;">
            重置
          </van-button>
        </div>
      </van-form>
    </div>

    <div class="bottom-bar">
      <van-button plain block size="large" @click="$router.back()" class="back-btn">
        <van-icon name="arrow-left" size="18" /> 返回
      </van-button>
    </div>

    <div class="safe-bottom"></div>

    <van-popup v-model:show="showLeaveType" position="bottom" round>
      <van-picker :columns="leaveTypeColumns" @confirm="onConfirmLeaveType" @cancel="showLeaveType = false" title="选择请假类型" />
    </van-popup>

    <van-popup v-model:show="showCardType" position="bottom" round>
      <van-picker :columns="cardTypeColumns" @confirm="onConfirmCardType" @cancel="showCardType = false" title="选择补卡类型" />
    </van-popup>

    <van-calendar 
      v-model="startDateValue"
      type="date" 
      :show="showStartDate"
      @confirm="onConfirmStartDate" 
      @close="showStartDate = false"
      :min-date="minDate" 
      :max-date="maxDate"
      confirm-text="确认" 
      cancel-text="取消"
      title="选择日期"
    />
    <van-calendar 
      v-model="endDateValue"
      type="date" 
      :show="showEndDate"
      @confirm="onConfirmEndDate" 
      @close="showEndDate = false"
      :min-date="minDate" 
      :max-date="maxDate"
      confirm-text="确认" 
      cancel-text="取消"
      title="选择日期"
    />

    <van-popup v-model:show="showStartTimePicker" position="bottom" round>
      <van-picker :columns="timeColumns" @confirm="onConfirmStartTime" @cancel="showStartTimePicker = false" title="选择开始时间" />
    </van-popup>
    <van-popup v-model:show="showEndTimePicker" position="bottom" round>
      <van-picker :columns="timeColumns" @confirm="onConfirmEndTime" @cancel="showEndTimePicker = false" title="选择结束时间" />
    </van-popup>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { showToast } from 'vant'
import { submitApproval } from '@/api/approval'
import { uploadAttachment } from '@/api/attachment'

const currentType = ref('')
const submitting = ref(false)

const employeeId = ref(1)
const employeeName = ref('用户')

const formData = reactive({
  title: '',
  leaveType: '',
  startTime: '',
  endTime: '',
  destCity: '',
  workDate: '',
  startTimeOnly: '',
  endTimeOnly: '',
  expenseType: '',
  amount: '',
  expenseDate: '',
  goodsName: '',
  quantity: '',
  unitPrice: '',
  cardDate: '',
  cardTime: '',
  cardType: '',
  content: ''
})

const showLeaveType = ref(false)
const showCardType = ref(false)
const showStartDate = ref(false)
const showEndDate = ref(false)
const showStartTimePicker = ref(false)
const showEndTimePicker = ref(false)
const fileInput = ref(null)
const attachments = ref([])

const startDateValue = ref(new Date())
const endDateValue = ref(new Date())

const minDate = computed(() => {
  const d = new Date()
  d.setHours(0, 0, 0, 0)
  return d
})

const maxDate = computed(() => {
  const d = new Date()
  d.setFullYear(d.getFullYear() + 1)
  d.setHours(23, 59, 59, 999)
  return d
})

const leaveTypeColumns = [
  { text: '事假', value: '事假' },
  { text: '病假', value: '病假' },
  { text: '年假', value: '年假' },
  { text: '婚假', value: '婚假' },
  { text: '产假', value: '产假' },
  { text: '丧假', value: '丧假' }
]

const cardTypeColumns = [
  { text: '迟到补卡', value: '迟到补卡' },
  { text: '早退补卡', value: '早退补卡' },
  { text: '漏签补卡', value: '漏签补卡' }
]

const timeColumns = [
  '08:00', '08:30', '09:00', '09:30', '10:00', '10:30', '11:00', '11:30',
  '12:00', '12:30', '13:00', '13:30', '14:00', '14:30', '15:00', '15:30',
  '16:00', '16:30', '17:00', '17:30', '18:00'
].map(t => ({ text: t, value: t }))

const selectType = (type) => {
  currentType.value = type
  resetForm()
}

const resetForm = () => {
  formData.title = ''
  formData.leaveType = ''
  formData.startTime = ''
  formData.endTime = ''
  formData.destCity = ''
  formData.workDate = ''
  formData.startTimeOnly = ''
  formData.endTimeOnly = ''
  formData.expenseType = ''
  formData.amount = ''
  formData.expenseDate = ''
  formData.goodsName = ''
  formData.quantity = ''
  formData.unitPrice = ''
  formData.cardDate = ''
  formData.cardTime = ''
  formData.cardType = ''
  formData.content = ''
  attachments.value = []
}

const triggerFileUpload = () => {
  fileInput.value?.click()
}

const handleFileChange = (event) => {
  const files = event.target.files
  if (!files || files.length === 0) return
  
  for (let i = 0; i < files.length; i++) {
    if (attachments.value.length >= 10) {
      showToast('最多只能上传10个附件')
      break
    }
    const file = files[i]
    attachments.value.push({
      file: file,
      name: file.name,
      size: file.size,
      id: null
    })
  }
  event.target.value = ''
}

const removeAttachment = (index) => {
  attachments.value.splice(index, 1)
}

const getFileIcon = (fileName) => {
  const ext = fileName.split('.').pop().toLowerCase()
  const iconMap = {
    'pdf': 'file-text',
    'doc': 'file-text',
    'docx': 'file-text',
    'xls': 'file',
    'xlsx': 'file',
    'ppt': 'file',
    'pptx': 'file',
    'jpg': 'image',
    'jpeg': 'image',
    'png': 'image',
    'gif': 'image'
  }
  return iconMap[ext] || 'file'
}

const getFileIconClass = (fileName) => {
  const ext = fileName.split('.').pop().toLowerCase()
  const classMap = {
    'pdf': 'icon-pdf',
    'doc': 'icon-doc',
    'docx': 'icon-doc',
    'xls': 'icon-xls',
    'xlsx': 'icon-xls',
    'ppt': 'icon-ppt',
    'pptx': 'icon-ppt',
    'jpg': 'icon-image',
    'jpeg': 'icon-image',
    'png': 'icon-image',
    'gif': 'icon-image'
  }
  return classMap[ext] || 'icon-other'
}

const formatFileSize = (size) => {
  if (size < 1024) return size + ' B'
  if (size < 1024 * 1024) return (size / 1024).toFixed(1) + ' KB'
  return (size / (1024 * 1024)).toFixed(1) + ' MB'
}

const onConfirmLeaveType = ({ selectedValues }) => {
  formData.leaveType = selectedValues[0]
  showLeaveType.value = false
}

const onConfirmCardType = ({ selectedValues }) => {
  formData.cardType = selectedValues[0]
  showCardType.value = false
}

const onConfirmStartDate = (value) => {
  const date = new Date(value)
  const dateStr = `${date.getFullYear()}-${String(date.getMonth()+1).padStart(2,'0')}-${String(date.getDate()).padStart(2,'0')}`
  
  if (currentType.value === 'overtime') {
    formData.workDate = dateStr
  } else if (currentType.value === 'reimburse') {
    formData.expenseDate = dateStr
  } else if (currentType.value === 'card') {
    formData.cardDate = dateStr
  } else {
    formData.startTime = dateStr
  }
  showStartDate.value = false
}

const onConfirmEndDate = (value) => {
  const date = new Date(value)
  formData.endTime = `${date.getFullYear()}-${String(date.getMonth()+1).padStart(2,'0')}-${String(date.getDate()).padStart(2,'0')}`
  showEndDate.value = false
}

const onConfirmStartTime = ({ selectedValues }) => {
  if (currentType.value === 'card') {
    formData.cardTime = selectedValues[0]
  } else {
    formData.startTimeOnly = selectedValues[0]
  }
  showStartTimePicker.value = false
}

const onConfirmEndTime = ({ selectedValues }) => {
  formData.endTimeOnly = selectedValues[0]
  showEndTimePicker.value = false
}

const onSubmit = async () => {
  const userId = localStorage.getItem('employeeId')
  if (userId) {
    employeeId.value = parseInt(userId)
  }
  const name = localStorage.getItem('name') || localStorage.getItem('username')
  if (name) {
    employeeName.value = name
  }
  const departmentId = localStorage.getItem('departmentId')

  const data = {
    applicantId: employeeId.value,
    applicantName: employeeName.value,
    applicantDepartmentId: departmentId ? parseInt(departmentId) : null,
    approvalType: currentType.value,
    title: formData.title,
    content: formData.content,
    leaveType: formData.leaveType,
    startTime: formData.startTime,
    endTime: formData.endTime,
    destCity: formData.destCity,
    workDate: formData.workDate,
    startTimeOnly: formData.startTimeOnly,
    endTimeOnly: formData.endTimeOnly,
    expenseType: formData.expenseType,
    amount: formData.amount,
    expenseDate: formData.expenseDate,
    goodsName: formData.goodsName,
    quantity: formData.quantity,
    unitPrice: formData.unitPrice,
    cardDate: formData.cardDate,
    cardTime: formData.cardTime,
    cardType: formData.cardType
  }

  submitting.value = true
  try {
    const res = await submitApproval(data)
    if (res.code === 0) {
      const approvalId = res.data
      
      if (attachments.value.length > 0 && approvalId) {
        for (const item of attachments.value) {
          if (item.file) {
            await uploadAttachment(item.file, approvalId, 'approval')
          }
        }
      }
      
      showToast('✅ 申请提交成功！')
      resetForm()
      currentType.value = ''
    } else {
      showToast(res.message || '提交失败')
    }
  } catch (error) {
    showToast('提交失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.oa-apply {
  max-width: 430px;
  margin: 0 auto;
  min-height: 100vh;
  background: #f5f7fa;
  padding: 0 16px 20px;
}

.banner {
  background: linear-gradient(135deg, #fdcb6e 0%, #f39c12 100%);
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

/* ===== 附件样式 ===== */
.attachment-section {
  margin-top: 8px;
}
.attachment-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}
.attachment-label {
  font-size: 14px;
  font-weight: 500;
  color: #333;
}
.attachment-count {
  font-size: 13px;
  color: #999;
}
.attachment-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}
.attachment-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  background: #fff;
  border-radius: 8px;
  border: 1px solid #eee;
  width: calc(50% - 5px);
  box-sizing: border-box;
}
.attachment-item.upload-btn {
  border: 2px dashed #ddd;
  justify-content: center;
  cursor: pointer;
  min-height: 60px;
}
.attachment-icon {
  width: 36px;
  height: 36px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.icon-pdf { background: #e8f0fe; color: #3677ef; }
.icon-doc { background: #e8f8ff; color: #00b894; }
.icon-xls { background: #fff3cd; color: #fdcb6e; }
.icon-ppt { background: #f8d7da; color: #e17055; }
.icon-image { background: #d4edda; color: #28a745; }
.icon-other { background: #f5f5f5; color: #666; }
.attachment-info {
  flex: 1;
  min-width: 0;
}
.attachment-name {
  display: block;
  font-size: 13px;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.attachment-size {
  display: block;
  font-size: 12px;
  color: #999;
}
.upload-text {
  font-size: 13px;
  color: #999;
}
.hidden-input {
  display: none;
}

.type-grid {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 12px;
  margin-bottom: 16px;
}
.type-item {
  background: #fff;
  border-radius: 14px;
  padding: 20px 10px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
  transition: all 0.2s;
}
.type-item:active { transform: scale(0.95); }
.type-item.active {
  background: #fff8e8;
  border: 2px solid #f39c12;
}
.type-item span { font-size: 13px; color: #333; }

.form-wrap {
  background: #fff;
  border-radius: 14px;
  padding: 16px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}

.submit-wrap {
  padding: 16px 0 8px;
}
.submit-wrap :deep(.van-button) {
  border-radius: 12px;
  height: 48px;
  font-size: 16px;
}
.submit-wrap :deep(.van-button--primary) {
  background: #fdcb6e;
  border-color: #fdcb6e;
}

.bottom-bar { padding: 16px 0 8px; }
.back-btn {
  border-radius: 12px !important;
  height: 48px !important;
  font-size: 16px !important;
  border-color: #f39c12 !important;
  color: #f39c12 !important;
}
.back-btn:active { background: #fff8e8 !important; }

.safe-bottom { height: 20px; }
:deep(.van-popup) { border-radius: 16px 16px 0 0; }
</style>