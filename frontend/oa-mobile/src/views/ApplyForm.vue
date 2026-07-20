<template>
  <div class="oa-apply-form">
    <!-- 顶部导航 -->
    <div class="page-header">
      <van-icon name="arrow-left" size="22" @click="handleBack" />
      <h1 class="header-title">{{ pageTitle }}</h1>
      <div class="header-right">
        <van-icon name="ellipsis" size="22" @click="showActionSheet = true" />
      </div>
    </div>

    <!-- 申请表单 -->
    <div class="form-wrap">
      <van-form @submit="onSubmit" ref="formRef">
        <!-- 申请类型 -->
        <div class="form-section">
          <div class="section-title">基本信息</div>
          <van-cell-group inset>
            <van-field
              v-model="pageTitle"
              name="type"
              label="申请类型"
              disabled
            />
            <van-field
              v-model="formData.title"
              name="title"
              label="申请标题"
              placeholder="请输入申请标题"
              :rules="[{ required: true, message: '请输入申请标题' }]"
            />
          </van-cell-group>
        </div>

        <!-- ===== 请假申请 ===== -->
        <div class="form-section" v-if="applyType === 'leave'">
          <div class="section-title">请假详情</div>
          <van-cell-group inset>
            <van-field
              v-model="formData.leaveType"
              name="leaveType"
              label="请假类型"
              placeholder="请选择"
              is-link
              @click="showLeaveTypePicker = true"
              :rules="[{ required: true, message: '请选择请假类型' }]"
            />
            <van-field
              v-model="formData.startDate"
              name="startDate"
              label="开始日期"
              placeholder="请选择"
              is-link
              @click="showStartPicker = true"
              :rules="[
                { required: true, message: '请选择开始日期' },
                { validator: validateStartDate, message: '开始日期不能早于今天' }
              ]"
            />
            <van-field
              v-model="formData.endDate"
              name="endDate"
              label="结束日期"
              placeholder="请选择"
              is-link
              @click="showEndPicker = true"
              :rules="[
                { required: true, message: '请选择结束日期' },
                { validator: validateEndDate, message: '结束日期不能早于开始日期' }
              ]"
            />
            <van-field
              v-model="formData.totalDays"
              name="totalDays"
              label="请假天数"
              placeholder="自动计算"
              disabled
            />
            <van-field
              v-model="formData.reason"
              name="reason"
              label="请假理由"
              placeholder="请输入请假理由（至少5个字）"
              type="textarea"
              rows="3"
              :rules="[
                { required: true, message: '请输入请假理由' },
                { validator: validateReason, message: '请假理由至少5个字' }
              ]"
            />
          </van-cell-group>
        </div>

        <!-- ===== 出差申请 ===== -->
        <div class="form-section" v-if="applyType === 'business'">
          <div class="section-title">出差详情</div>
          <van-cell-group inset>
            <van-field
              v-model="formData.destCity"
              name="destCity"
              label="出差城市"
              placeholder="请输入出差城市"
              :rules="[
                { required: true, message: '请输入出差城市' },
                { validator: validateCity, message: '请输入有效城市名称' }
              ]"
            />
            <van-field
              v-model="formData.startDate"
              name="startDate"
              label="出发日期"
              placeholder="请选择"
              is-link
              @click="showStartPicker = true"
              :rules="[
                { required: true, message: '请选择出发日期' },
                { validator: validateStartDate, message: '出发日期不能早于今天' }
              ]"
            />
            <van-field
              v-model="formData.endDate"
              name="endDate"
              label="返回日期"
              placeholder="请选择"
              is-link
              @click="showEndPicker = true"
              :rules="[
                { required: true, message: '请选择返回日期' },
                { validator: validateEndDate, message: '返回日期不能早于出发日期' }
              ]"
            />
            <van-field
              v-model="formData.totalDays"
              name="totalDays"
              label="出差天数"
              placeholder="自动计算"
              disabled
            />
            <van-field
              v-model="formData.reason"
              name="reason"
              label="出差事由"
              placeholder="请输入出差事由（至少5个字）"
              type="textarea"
              rows="3"
              :rules="[
                { required: true, message: '请输入出差事由' },
                { validator: validateReason, message: '出差事由至少5个字' }
              ]"
            />
          </van-cell-group>
        </div>

        <!-- ===== 加班申请 ===== -->
        <div class="form-section" v-if="applyType === 'overtime'">
          <div class="section-title">加班详情</div>
          <van-cell-group inset>
            <van-field
              v-model="formData.workDate"
              name="workDate"
              label="加班日期"
              placeholder="请选择"
              is-link
              @click="showDatePicker = true"
              :rules="[
                { required: true, message: '请选择加班日期' },
                { validator: validateStartDate, message: '加班日期不能早于今天' }
              ]"
            />
            <van-field
              v-model="formData.startTime"
              name="startTime"
              label="开始时间"
              placeholder="请选择"
              is-link
              @click="showStartTimePicker = true"
              :rules="[{ required: true, message: '请选择开始时间' }]"
            />
            <van-field
              v-model="formData.endTime"
              name="endTime"
              label="结束时间"
              placeholder="请选择"
              is-link
              @click="showEndTimePicker = true"
              :rules="[
                { required: true, message: '请选择结束时间' },
                { validator: validateTime, message: '结束时间不能早于开始时间' }
              ]"
            />
            <van-field
              v-model="formData.totalHours"
              name="totalHours"
              label="加班时长"
              placeholder="自动计算"
              disabled
            />
            <van-field
              v-model="formData.reason"
              name="reason"
              label="加班原因"
              placeholder="请输入加班原因（至少5个字）"
              type="textarea"
              rows="3"
              :rules="[
                { required: true, message: '请输入加班原因' },
                { validator: validateReason, message: '加班原因至少5个字' }
              ]"
            />
          </van-cell-group>
        </div>

        <!-- ===== 报销申请 ===== -->
        <div class="form-section" v-if="applyType === 'reimburse'">
          <div class="section-title">报销详情</div>
          <van-cell-group inset>
            <van-field
              v-model="formData.expenseType"
              name="expenseType"
              label="报销类型"
              placeholder="请选择"
              is-link
              @click="showExpenseTypePicker = true"
              :rules="[{ required: true, message: '请选择报销类型' }]"
            />
            <van-field
              v-model="formData.amount"
              name="amount"
              label="报销金额"
              placeholder="请输入金额（元）"
              type="number"
              :rules="[
                { required: true, message: '请输入报销金额' },
                { validator: validateAmount, message: '报销金额必须大于0且不超过100万' }
              ]"
            />
            <van-field
              v-model="formData.expenseDate"
              name="expenseDate"
              label="费用日期"
              placeholder="请选择"
              is-link
              @click="showDatePicker = true"
              :rules="[
                { required: true, message: '请选择费用日期' },
                { validator: validateStartDate, message: '费用日期不能晚于今天' }
              ]"
            />
            <van-field
              v-model="formData.reason"
              name="reason"
              label="报销事由"
              placeholder="请简述报销事由（至少5个字）"
              type="textarea"
              rows="3"
              :rules="[
                { required: true, message: '请输入报销事由' },
                { validator: validateReason, message: '报销事由至少5个字' }
              ]"
            />
          </van-cell-group>
        </div>

        <!-- ===== 采购申请 ===== -->
        <div class="form-section" v-if="applyType === 'purchase'">
          <div class="section-title">采购详情</div>
          <van-cell-group inset>
            <van-field
              v-model="formData.goodsName"
              name="goodsName"
              label="物品名称"
              placeholder="请输入采购物品名称"
              :rules="[
                { required: true, message: '请输入采购物品名称' },
                { validator: validateGoods, message: '物品名称至少2个字' }
              ]"
            />
            <van-field
              v-model="formData.quantity"
              name="quantity"
              label="采购数量"
              placeholder="请输入数量"
              type="number"
              :rules="[
                { required: true, message: '请输入采购数量' },
                { validator: validateQuantity, message: '采购数量必须大于0且不超过9999' }
              ]"
            />
            <van-field
              v-model="formData.unitPrice"
              name="unitPrice"
              label="单价（元）"
              placeholder="请输入单价"
              type="number"
              :rules="[
                { required: true, message: '请输入单价' },
                { validator: validateUnitPrice, message: '单价必须大于0且不超过100万' }
              ]"
            />
            <van-field
              v-model="formData.totalAmount"
              name="totalAmount"
              label="总金额"
              placeholder="自动计算"
              disabled
            />
            <van-field
              v-model="formData.reason"
              name="reason"
              label="采购事由"
              placeholder="请简述采购事由（至少5个字）"
              type="textarea"
              rows="3"
              :rules="[
                { required: true, message: '请输入采购事由' },
                { validator: validateReason, message: '采购事由至少5个字' }
              ]"
            />
          </van-cell-group>
        </div>

        <!-- ===== 补卡申请 ===== -->
        <div class="form-section" v-if="applyType === 'card'">
          <div class="section-title">补卡详情</div>
          <van-cell-group inset>
            <van-field
              v-model="formData.cardDate"
              name="cardDate"
              label="补卡日期"
              placeholder="请选择"
              is-link
              @click="showDatePicker = true"
              :rules="[
                { required: true, message: '请选择补卡日期' },
                { validator: validateStartDate, message: '补卡日期不能早于今天' }
              ]"
            />
            <van-field
              v-model="formData.cardTime"
              name="cardTime"
              label="补卡时间"
              placeholder="请选择"
              is-link
              @click="showTimePicker = true"
              :rules="[{ required: true, message: '请选择补卡时间' }]"
            />
            <van-field
              v-model="formData.cardType"
              name="cardType"
              label="补卡类型"
              placeholder="请选择"
              is-link
              @click="showCardTypePicker = true"
              :rules="[{ required: true, message: '请选择补卡类型' }]"
            />
            <van-field
              v-model="formData.reason"
              name="reason"
              label="补卡原因"
              placeholder="请简述补卡原因（至少5个字）"
              type="textarea"
              rows="3"
              :rules="[
                { required: true, message: '请输入补卡原因' },
                { validator: validateReason, message: '补卡原因至少5个字' }
              ]"
            />
          </van-cell-group>
        </div>

        <!-- 附件上传 -->
        <div class="form-section">
          <div class="section-title">附件（选填）</div>
          <van-cell-group inset>
            <van-field name="uploader" label="附件">
              <template #input>
                <van-uploader v-model="formData.files" :max-count="3" accept="image/*" />
              </template>
            </van-field>
          </van-cell-group>
        </div>

        <!-- 提交按钮 -->
        <div class="submit-wrap">
          <van-button type="primary" block size="large" native-type="submit" :loading="submitting">
            提交申请
          </van-button>
          <van-button plain block size="large" @click="$router.back()" style="margin-top:10px;">
            取消
          </van-button>
        </div>
      </van-form>
    </div>

    <!-- ===== 选择器（保持不变） ===== -->
    <van-action-sheet v-model:show="showLeaveTypePicker" title="选择请假类型">
      <div class="picker-list">
        <div class="picker-item" v-for="item in leaveTypes" :key="item" @click="formData.leaveType = item; showLeaveTypePicker = false">
          <span>{{ item }}</span>
          <van-icon v-if="formData.leaveType === item" name="success" color="#3677ef" />
        </div>
      </div>
    </van-action-sheet>

    <van-action-sheet v-model:show="showExpenseTypePicker" title="选择报销类型">
      <div class="picker-list">
        <div class="picker-item" v-for="item in expenseTypes" :key="item" @click="formData.expenseType = item; showExpenseTypePicker = false">
          <span>{{ item }}</span>
          <van-icon v-if="formData.expenseType === item" name="success" color="#3677ef" />
        </div>
      </div>
    </van-action-sheet>

    <van-action-sheet v-model:show="showCardTypePicker" title="选择补卡类型">
      <div class="picker-list">
        <div class="picker-item" v-for="item in cardTypes" :key="item" @click="formData.cardType = item; showCardTypePicker = false">
          <span>{{ item }}</span>
          <van-icon v-if="formData.cardType === item" name="success" color="#3677ef" />
        </div>
      </div>
    </van-action-sheet>

    <van-popup v-model:show="showStartPicker" position="bottom" round style="height: 80%;">
      <van-calendar type="date" @confirm="onConfirmStartDate" @cancel="showStartPicker = false" title="选择开始日期" />
    </van-popup>
    <van-popup v-model:show="showEndPicker" position="bottom" round style="height: 80%;">
      <van-calendar type="date" @confirm="onConfirmEndDate" @cancel="showEndPicker = false" title="选择结束日期" />
    </van-popup>
    <van-popup v-model:show="showDatePicker" position="bottom" round style="height: 80%;">
      <van-calendar type="date" @confirm="onConfirmDate" @cancel="showDatePicker = false" :title="datePickerTitle" />
    </van-popup>

    <van-popup v-model:show="showStartTimePicker" position="bottom" round>
      <van-picker :columns="timeColumnData" @confirm="onConfirmStartTime" @cancel="showStartTimePicker = false" title="选择开始时间" />
    </van-popup>
    <van-popup v-model:show="showEndTimePicker" position="bottom" round>
      <van-picker :columns="timeColumnData" @confirm="onConfirmEndTime" @cancel="showEndTimePicker = false" title="选择结束时间" />
    </van-popup>
    <van-popup v-model:show="showTimePicker" position="bottom" round>
      <van-picker :columns="timeColumnData" @confirm="onConfirmTime" @cancel="showTimePicker = false" title="选择时间" />
    </van-popup>

    <!-- ===== 更多操作 ===== -->
    <van-action-sheet v-model:show="showActionSheet" :actions="actions" @select="onSelect" cancel-text="取消" />

    <!-- ===== 审批流程弹窗 ===== -->
    <van-popup v-model:show="showFlowPopup" position="bottom" round style="padding:20px 16px 30px;max-height:70vh;overflow-y:auto;">
      <h3 style="text-align:center;margin-bottom:16px;">审批流程</h3>
      <div class="flow-timeline">
        <div class="flow-step" v-for="(step, index) in flowSteps" :key="index">
          <div class="step-left">
            <div class="step-dot" :class="step.status"></div>
            <div class="step-line" v-if="index < flowSteps.length - 1"></div>
          </div>
          <div class="step-right">
            <div class="step-title">{{ step.title }}</div>
            <div class="step-desc">{{ step.desc }}</div>
            <div class="step-time" v-if="step.time">{{ step.time }}</div>
          </div>
        </div>
      </div>
      <van-button block plain @click="showFlowPopup = false" style="margin-top:16px;">关闭</van-button>
    </van-popup>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { showToast, showConfirmDialog } from 'vant'
import { submitApproval } from '@/api/approval'

const router = useRouter()
const route = useRoute()

// ===== 当前用户信息 =====
const employeeId = ref(1)
const employeeName = ref('张工')

// ===== 从路由获取申请类型 =====
const applyType = ref(route.params.type || 'leave')

// ===== 页面标题映射 =====
const typeMap = {
  leave: '请假申请',
  business: '出差申请',
  overtime: '加班申请',
  reimburse: '报销申请',
  purchase: '采购申请',
  card: '补卡申请'
}

const pageTitle = computed(() => {
  return typeMap[applyType.value] || '发起申请'
})

// ===== 日期选择器标题 =====
const datePickerTitle = computed(() => {
  if (applyType.value === 'overtime') return '选择加班日期'
  if (applyType.value === 'reimburse') return '选择费用日期'
  if (applyType.value === 'card') return '选择补卡日期'
  return '选择日期'
})

// ===== 表单数据 =====
const formData = ref({
  title: '',
  leaveType: '',
  startDate: '',
  endDate: '',
  totalDays: '',
  destCity: '',
  workDate: '',
  startTime: '',
  endTime: '',
  totalHours: '',
  expenseType: '',
  amount: '',
  expenseDate: '',
  goodsName: '',
  quantity: '',
  unitPrice: '',
  totalAmount: '',
  cardDate: '',
  cardTime: '',
  cardType: '',
  reason: '',
  files: []
})

// ===== 选择器控制 =====
const showLeaveTypePicker = ref(false)
const showExpenseTypePicker = ref(false)
const showCardTypePicker = ref(false)
const showStartPicker = ref(false)
const showEndPicker = ref(false)
const showDatePicker = ref(false)
const showStartTimePicker = ref(false)
const showEndTimePicker = ref(false)
const showTimePicker = ref(false)
const showActionSheet = ref(false)
const showFlowPopup = ref(false)

// ===== 提交状态 =====
const submitting = ref(false)

// ===== 选项数据 =====
const leaveTypes = ['年假', '事假', '病假', '婚假', '产假', '陪产假', '丧假', '调休']
const expenseTypes = ['交通费', '餐饮费', '住宿费', '办公用品', '通讯费', '其他']
const cardTypes = ['上班签到', '下班签退', '忘记打卡']

// ===== 审批流程配置 =====
const flowStepsMap = {
  leave: [
    { title: '提交申请', desc: '发起请假申请', status: 'done' },
    { title: '部门主管审批', desc: '待审批', status: 'active' },
    { title: 'HR备案', desc: '待审批', status: 'pending' }
  ],
  business: [
    { title: '提交申请', desc: '发起出差申请', status: 'done' },
    { title: '部门主管审批', desc: '待审批', status: 'active' },
    { title: '分管领导审批', desc: '待审批', status: 'pending' }
  ],
  overtime: [
    { title: '提交申请', desc: '发起加班申请', status: 'done' },
    { title: '部门主管审批', desc: '待审批', status: 'active' },
    { title: 'HR备案', desc: '待审批', status: 'pending' }
  ],
  reimburse: [
    { title: '提交申请', desc: '发起报销申请', status: 'done' },
    { title: '部门主管审批', desc: '待审批', status: 'active' },
    { title: '财务审批', desc: '待审批', status: 'pending' }
  ],
  purchase: [
    { title: '提交申请', desc: '发起采购申请', status: 'done' },
    { title: '部门主管审批', desc: '待审批', status: 'active' },
    { title: '财务审批', desc: '待审批', status: 'pending' }
  ],
  card: [
    { title: '提交申请', desc: '发起补卡申请', status: 'done' },
    { title: '部门主管审批', desc: '待审批', status: 'active' }
  ]
}

const flowSteps = computed(() => {
  return flowStepsMap[applyType.value] || flowStepsMap.leave
})

// ===== Vant Picker 格式 =====
const timeColumnData = [
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
  { text: '18:00', value: '18:00' },
  { text: '18:30', value: '18:30' },
  { text: '19:00', value: '19:00' },
  { text: '19:30', value: '19:30' },
  { text: '20:00', value: '20:00' },
  { text: '20:30', value: '20:30' },
  { text: '21:00', value: '21:00' },
  { text: '21:30', value: '21:30' },
  { text: '22:00', value: '22:00' }
]

const actions = [
  { name: '保存草稿', value: 'draft' },
  { name: '查看审批流程', value: 'flow' },
  { name: '清空表单', value: 'clear' }
]

// =============================================
// ===== 合理性校验函数 =====
// =============================================

const validateStartDate = (val) => {
  if (!val) return true
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  const selected = new Date(val)
  return selected >= today
}

const validateEndDate = (val) => {
  if (!val || !formData.value.startDate) return true
  const start = new Date(formData.value.startDate)
  const end = new Date(val)
  return end >= start
}

const validateTime = (val) => {
  if (!val || !formData.value.startTime) return true
  const start = new Date(`2026-01-01 ${formData.value.startTime}`)
  const end = new Date(`2026-01-01 ${val}`)
  return end > start
}

const validateAmount = (val) => {
  if (!val) return true
  const num = parseFloat(val)
  return num > 0 && num <= 1000000
}

const validateQuantity = (val) => {
  if (!val) return true
  const num = parseFloat(val)
  return Number.isInteger(num) && num > 0 && num <= 9999
}

const validateUnitPrice = (val) => {
  if (!val) return true
  const num = parseFloat(val)
  return num > 0 && num <= 1000000
}

const validateReason = (val) => {
  if (!val) return true
  return val.trim().length >= 5
}

const validateCity = (val) => {
  if (!val) return true
  return val.trim().length >= 2
}

const validateGoods = (val) => {
  if (!val) return true
  return val.trim().length >= 2
}

// =============================================
// ===== 自动计算 =====
// =============================================

watch([() => formData.value.startDate, () => formData.value.endDate], () => {
  if (formData.value.startDate && formData.value.endDate) {
    const start = new Date(formData.value.startDate)
    const end = new Date(formData.value.endDate)
    const diff = Math.ceil((end - start) / (1000 * 60 * 60 * 24)) + 1
    formData.value.totalDays = diff > 0 ? `${diff}天` : ''
  }
})

watch([() => formData.value.startTime, () => formData.value.endTime], () => {
  if (formData.value.startTime && formData.value.endTime) {
    const start = new Date(`2026-01-01 ${formData.value.startTime}`)
    const end = new Date(`2026-01-01 ${formData.value.endTime}`)
    const diff = (end - start) / (1000 * 60 * 60)
    formData.value.totalHours = diff > 0 ? `${diff}小时` : ''
  }
})

watch([() => formData.value.quantity, () => formData.value.unitPrice], () => {
  if (formData.value.quantity && formData.value.unitPrice) {
    const qty = parseFloat(formData.value.quantity)
    const price = parseFloat(formData.value.unitPrice)
    if (qty > 0 && price > 0) {
      formData.value.totalAmount = (qty * price).toFixed(2)
    } else {
      formData.value.totalAmount = ''
    }
  }
})

// =============================================
// ===== 保存草稿 =====
// =============================================

const saveDraft = () => {
  const key = `draft_${applyType.value}`
  localStorage.setItem(key, JSON.stringify(formData.value))
  showToast('草稿已保存')
}

// =============================================
// ===== 加载草稿 =====
// =============================================

const loadDraft = () => {
  const key = `draft_${applyType.value}`
  const data = localStorage.getItem(key)
  if (data) {
    try {
      const parsed = JSON.parse(data)
      Object.keys(parsed).forEach(key => {
        if (key in formData.value) {
          formData.value[key] = parsed[key]
        }
      })
      showToast('已加载草稿')
    } catch (e) {
      console.error('加载草稿失败', e)
    }
  }
}

// =============================================
// ===== 清空表单 =====
// =============================================

const clearForm = () => {
  showConfirmDialog({
    title: '确认清空',
    message: '确定要清空所有已填内容吗？',
    confirmButtonText: '确定清空'
  }).then(() => {
    const title = formData.value.title
    Object.keys(formData.value).forEach(key => {
      if (key === 'title') {
        formData.value.title = title
      } else if (key === 'files') {
        formData.value.files = []
      } else {
        formData.value[key] = ''
      }
    })
    const key = `draft_${applyType.value}`
    localStorage.removeItem(key)
    showToast('已清空表单')
  }).catch(() => {})
}

// =============================================
// ===== 查看审批流程 =====
// =============================================

const viewFlow = () => {
  showFlowPopup.value = true
}

// =============================================
// ===== 更多操作 =====
// =============================================

const onSelect = (action) => {
  if (action.value === 'draft') {
    saveDraft()
  } else if (action.value === 'flow') {
    viewFlow()
  } else if (action.value === 'clear') {
    clearForm()
  }
  showActionSheet.value = false
}

// =============================================
// ===== 日期选择回调 =====
// =============================================

const onConfirmStartDate = (value) => {
  formData.value.startDate = formatDate(value)
  showStartPicker.value = false
}

const onConfirmEndDate = (value) => {
  formData.value.endDate = formatDate(value)
  showEndPicker.value = false
}

const onConfirmDate = (value) => {
  const d = formatDate(value)
  if (applyType.value === 'overtime') {
    formData.value.workDate = d
  } else if (applyType.value === 'reimburse') {
    formData.value.expenseDate = d
  } else if (applyType.value === 'card') {
    formData.value.cardDate = d
  }
  showDatePicker.value = false
}

// =============================================
// ===== 时间选择回调 =====
// =============================================

const onConfirmStartTime = ({ selectedValues }) => {
  formData.value.startTime = selectedValues[0]
  showStartTimePicker.value = false
}

const onConfirmEndTime = ({ selectedValues }) => {
  formData.value.endTime = selectedValues[0]
  showEndTimePicker.value = false
}

const onConfirmTime = ({ selectedValues }) => {
  formData.value.cardTime = selectedValues[0]
  showTimePicker.value = false
}

// =============================================
// ===== 工具方法 =====
// =============================================

const formatDate = (value) => {
  const date = new Date(value)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

// =============================================
// ===== 返回（检查是否有未保存内容） =====
// =============================================

const handleBack = () => {
  const hasContent = formData.value.title || formData.value.reason || formData.value.leaveType
  if (hasContent) {
    showConfirmDialog({
      title: '确认离开',
      message: '表单有未提交的内容，确定要离开吗？',
      confirmButtonText: '确定离开'
    }).then(() => {
      router.back()
    }).catch(() => {})
  } else {
    router.back()
  }
}

// =============================================
// ===== 提交表单（对接后端接口） =====
// =============================================

const onSubmit = async () => {
  // ===== 前端校验 =====
  if (!formData.value.title) {
    showToast('请输入申请标题')
    return
  }
  
  if ((applyType.value === 'leave' || applyType.value === 'business') && !formData.value.totalDays) {
    showToast('请选择有效的日期范围')
    return
  }
  
  if (applyType.value === 'overtime' && !formData.value.totalHours) {
    showToast('请选择有效的时间范围')
    return
  }
  
  if (applyType.value === 'purchase' && !formData.value.totalAmount) {
    showToast('请填写有效的数量和单价')
    return
  }

  // ===== 构建提交数据 =====
  const submitData = {
    approvalType: applyType.value,
    title: formData.value.title,
    content: formData.value.reason || formData.value.destCity || '',
    applicantId: employeeId.value,
    applicantName: employeeName.value,
    // 请假
    leaveType: formData.value.leaveType,
    startDate: formData.value.startDate,
    endDate: formData.value.endDate,
    totalDays: formData.value.totalDays,
    // 出差
    destCity: formData.value.destCity,
    // 加班
    workDate: formData.value.workDate,
    startTime: formData.value.startTime,
    endTime: formData.value.endTime,
    totalHours: formData.value.totalHours,
    // 报销
    expenseType: formData.value.expenseType,
    amount: formData.value.amount,
    expenseDate: formData.value.expenseDate,
    // 采购
    goodsName: formData.value.goodsName,
    quantity: formData.value.quantity,
    unitPrice: formData.value.unitPrice,
    totalAmount: formData.value.totalAmount,
    // 补卡
    cardDate: formData.value.cardDate,
    cardTime: formData.value.cardTime,
    cardType: formData.value.cardType
  }

  // ===== 调用后端接口 =====
  submitting.value = true
  try {
    const res = await submitApproval(submitData)
    if (res.code === 0) {
      const key = `draft_${applyType.value}`
      localStorage.removeItem(key)
      showToast('✅ 申请提交成功！')
      setTimeout(() => router.push('/apply'), 500)
    } else {
      showToast(res.msg || '提交失败')
    }
  } catch (error) {
    console.error('提交申请失败:', error)
    showToast('提交失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}

// =============================================
// ===== 初始化 =====
// =============================================

onMounted(() => {
  employeeId.value = parseInt(localStorage.getItem('employeeId')) || 1
  employeeName.value = localStorage.getItem('username') || '张工'
  
  const now = new Date()
  formData.value.title = `${pageTitle.value} - ${now.getFullYear()}年${String(now.getMonth()+1).padStart(2,'0')}月${String(now.getDate()).padStart(2,'0')}日`
  
  setTimeout(() => {
    loadDraft()
  }, 300)
})
</script>

<style scoped>
.oa-apply-form {
  max-width: 430px;
  margin: 0 auto;
  min-height: 100vh;
  background: #f5f7fa;
  padding: 0 16px 20px;
  box-shadow: 0 0 20px rgba(0,0,0,0.06);
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

/* ===== 表单 ===== */
.form-wrap {
  padding-top: 4px;
}
.form-section {
  margin-bottom: 16px;
}
.section-title {
  font-size: 15px;
  font-weight: 500;
  color: #222;
  padding: 8px 4px 10px;
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
:deep(.van-uploader) {
  width: 100%;
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
  background: #3677ef;
  border: none;
}

/* ===== 选择器列表 ===== */
.picker-list {
  padding: 12px 16px 20px;
}
.picker-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 12px;
  border-bottom: 1px solid #f5f5f5;
  cursor: pointer;
}
.picker-item:active {
  background: #f5f7fa;
}
.picker-item span {
  font-size: 15px;
  color: #333;
}

/* ===== Picker 弹窗 ===== */
:deep(.van-popup) {
  border-radius: 16px 16px 0 0;
}

/* ===== 审批流程时间线 ===== */
.flow-timeline {
  padding: 8px 0;
}
.flow-step {
  display: flex;
  gap: 14px;
  position: relative;
}
.step-left {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: 4px;
}
.step-dot {
  width: 14px;
  height: 14px;
  border-radius: 50%;
  flex-shrink: 0;
  border: 2px solid #ddd;
  background: #fff;
  position: relative;
  z-index: 1;
}
.step-dot.done { border-color: #00b894; background: #00b894; }
.step-dot.active { border-color: #3677ef; background: #3677ef; box-shadow: 0 0 0 4px rgba(54,119,239,0.2); }
.step-dot.pending { border-color: #ddd; background: #fff; }
.step-line {
  width: 2px;
  flex: 1;
  background: #e0e0e0;
  margin: 4px 0;
  min-height: 20px;
}
.step-right {
  flex: 1;
  padding-bottom: 16px;
}
.step-title {
  font-size: 15px;
  font-weight: 500;
  color: #222;
}
.step-desc {
  font-size: 13px;
  color: #888;
  margin: 2px 0;
}
.step-time {
  font-size: 12px;
  color: #bbb;
}
</style>