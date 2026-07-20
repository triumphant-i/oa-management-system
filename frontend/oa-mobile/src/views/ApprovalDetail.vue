<template>
  <div class="oa-approval-detail">
    <!-- 顶部导航 -->
    <div class="page-header">
      <van-icon name="arrow-left" size="22" @click="$router.back()" />
      <h1 class="header-title">审批详情</h1>
      <div class="header-right">
        <van-icon name="ellipsis" size="22" />
      </div>
    </div>

    <!-- 加载状态 -->
    <div class="loading-wrap" v-if="loading">
      <van-loading size="40" text="加载中..." vertical />
    </div>

    <!-- 详情内容 -->
    <template v-else-if="detail.id">
      <!-- 审批状态 -->
      <div class="status-banner" :class="statusClass">
        <van-icon :name="statusIcon" size="28" color="#fff" />
        <div class="status-text">
          <span class="status-label">{{ detail.status }}</span>
          <span class="status-time">{{ formatTime(detail.updateTime || detail.createTime) }}</span>
        </div>
      </div>

      <!-- 申请信息 -->
      <div class="info-section">
        <div class="section-title">申请信息</div>
        <van-cell-group inset>
          <van-cell title="申请类型" :value="getTypeLabel(detail.approvalType)" />
          <van-cell title="申请标题" :value="detail.title" />
          <van-cell title="申请人" :value="detail.applicantName || '未知'" />
          <van-cell title="申请时间" :value="formatTime(detail.createTime)" />
          <van-cell title="所属部门" :value="detail.departmentName || '—'" />
        </van-cell-group>
      </div>

      <!-- 申请详情 -->
      <div class="info-section">
        <div class="section-title">申请详情</div>
        <van-cell-group inset>
          <van-cell v-for="(value, key) in detailFields" :key="key" :title="key" :value="value" />
          <van-cell title="申请理由" :value="detail.content || '—'" />
        </van-cell-group>
      </div>

      <!-- 审批意见（如果已审批） -->
      <div class="info-section" v-if="detail.approveReason">
        <div class="section-title">审批意见</div>
        <van-cell-group inset>
          <van-cell title="审批人" :value="detail.approverName || '—'" />
          <van-cell title="意见" :value="detail.approveReason" />
          <van-cell title="审批时间" :value="formatTime(detail.approveTime)" />
        </van-cell-group>
      </div>

      <!-- 审批流程 -->
      <div class="info-section">
        <div class="section-title">审批流程</div>
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
      </div>
    </template>

    <!-- 空状态 -->
    <div class="empty-state" v-else>
      <van-icon name="notes-o" size="48" color="#ccc" />
      <p>申请不存在</p>
    </div>

    <!-- 底部操作按钮（仅待审批状态显示） -->
    <div class="action-bar" v-if="detail.status === '待审批'">
      <van-button plain type="danger" size="large" @click="showRejectModal = true" :loading="submitting">拒绝</van-button>
      <van-button type="primary" size="large" @click="showApproveModal = true" :loading="submitting">同意</van-button>
    </div>

    <!-- 同意弹窗 -->
    <van-popup v-model:show="showApproveModal" position="bottom" round style="padding:20px 16px 30px;">
      <h3 style="margin:0 0 16px;">审批意见</h3>
      <van-field v-model="approveReason" rows="3" type="textarea" placeholder="请输入审批意见（选填）" />
      <div style="display:flex;gap:12px;margin-top:16px;">
        <van-button plain block @click="showApproveModal = false">取消</van-button>
        <van-button type="primary" block @click="submitApprove" :loading="submitting">同意</van-button>
      </div>
    </van-popup>

    <!-- 拒绝弹窗 -->
    <van-popup v-model:show="showRejectModal" position="bottom" round style="padding:20px 16px 30px;">
      <h3 style="margin:0 0 16px;">拒绝理由</h3>
      <van-field v-model="rejectReason" rows="3" type="textarea" placeholder="请输入拒绝理由（必填）" required />
      <div style="display:flex;gap:12px;margin-top:16px;">
        <van-button plain block @click="showRejectModal = false">取消</van-button>
        <van-button type="danger" block @click="submitReject" :loading="submitting">拒绝</van-button>
      </div>
    </van-popup>

    <div class="safe-bottom"></div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { showToast, showConfirmDialog } from 'vant'
import { getApprovalDetail, handleApproval } from '@/api/approval'

const router = useRouter()
const route = useRoute()

// ===== 状态 =====
const loading = ref(false)
const submitting = ref(false)
const showApproveModal = ref(false)
const showRejectModal = ref(false)
const approveReason = ref('')
const rejectReason = ref('')
const detail = ref({
  id: null,
  approvalType: '',
  title: '',
  applicantName: '',
  createTime: '',
  status: '',
  content: '',
  approveReason: '',
  approverName: '',
  approveTime: '',
  updateTime: '',
  departmentName: ''
})

// ===== 当前用户 =====
const currentUser = ref({
  id: 1,
  name: '系统管理员'
})

// =============================================
// ===== 类型映射 =====
// =============================================
const typeMap = {
  leave: '请假申请',
  business: '出差申请',
  overtime: '加班申请',
  reimburse: '报销申请',
  purchase: '采购申请',
  card: '补卡申请'
}

const getTypeLabel = (type) => {
  return typeMap[type] || type || '申请'
}

// =============================================
// ===== 状态计算 =====
// =============================================
const statusClass = computed(() => {
  if (detail.value.status === '待审批') return 'status-pending'
  if (detail.value.status === '已通过') return 'status-approved'
  if (detail.value.status === '已拒绝') return 'status-rejected'
  return 'status-pending'
})

const statusIcon = computed(() => {
  if (detail.value.status === '待审批') return 'clock-o'
  if (detail.value.status === '已通过') return 'success'
  if (detail.value.status === '已拒绝') return 'close'
  return 'clock-o'
})

// =============================================
// ===== 详情字段 =====
// =============================================
const detailFields = computed(() => {
  const fields = {}
  const d = detail.value
  
  switch (d.approvalType) {
    case 'leave':
      if (d.leaveType) fields['请假类型'] = d.leaveType
      if (d.startTime) fields['开始日期'] = formatDate(d.startTime)
      if (d.endTime) fields['结束日期'] = formatDate(d.endTime)
      if (d.totalDays) fields['请假天数'] = d.totalDays + '天'
      break
    case 'business':
      if (d.destCity) fields['出差城市'] = d.destCity
      if (d.startTime) fields['出发日期'] = formatDate(d.startTime)
      if (d.endTime) fields['返回日期'] = formatDate(d.endTime)
      if (d.totalDays) fields['出差天数'] = d.totalDays + '天'
      break
    case 'overtime':
      if (d.workDate) fields['加班日期'] = formatDate(d.workDate)
      if (d.startTimeOnly) fields['开始时间'] = d.startTimeOnly
      if (d.endTimeOnly) fields['结束时间'] = d.endTimeOnly
      if (d.totalHours) fields['加班时长'] = d.totalHours + '小时'
      break
    case 'reimburse':
      if (d.expenseType) fields['报销类型'] = d.expenseType
      if (d.amount) fields['报销金额'] = '¥' + d.amount
      if (d.expenseDate) fields['费用日期'] = formatDate(d.expenseDate)
      break
    case 'purchase':
      if (d.goodsName) fields['物品名称'] = d.goodsName
      if (d.quantity) fields['采购数量'] = d.quantity
      if (d.unitPrice) fields['单价'] = '¥' + d.unitPrice
      if (d.totalAmount) fields['总金额'] = '¥' + d.totalAmount
      break
    case 'card':
      if (d.cardDate) fields['补卡日期'] = formatDate(d.cardDate)
      if (d.cardTime) fields['补卡时间'] = d.cardTime
      if (d.cardType) fields['补卡类型'] = d.cardType
      break
  }
  
  return fields
})

// =============================================
// ===== 审批流程 =====
// =============================================
const flowSteps = computed(() => {
  const steps = []
  const d = detail.value
  const status = d.status
  
  // 第一步：提交申请
  steps.push({
    title: '提交申请',
    desc: `${d.applicantName || '申请人'} 提交了申请`,
    time: formatTime(d.createTime),
    status: 'done'
  })
  
  // 第二步：根据状态显示
  if (status === '待审批') {
    steps.push({
      title: '部门主管审批',
      desc: '⏳ 待审批',
      time: '',
      status: 'active'
    })
  } else if (status === '已通过') {
    steps.push({
      title: '部门主管审批',
      desc: '已通过',
      time: formatTime(d.approveTime || d.updateTime),
      status: 'done'
    })
  } else if (status === '已拒绝') {
    steps.push({
      title: '部门主管审批',
      desc: '已拒绝',
      time: formatTime(d.approveTime || d.updateTime),
      status: 'done'
    })
  }
  
  return steps
})

// =============================================
// ===== 工具方法 =====
// =============================================
const formatTime = (timeStr) => {
  if (!timeStr) return ''
  try {
    const date = new Date(timeStr)
    return `${date.getFullYear()}-${String(date.getMonth()+1).padStart(2,'0')}-${String(date.getDate()).padStart(2,'0')} ${String(date.getHours()).padStart(2,'0')}:${String(date.getMinutes()).padStart(2,'0')}`
  } catch {
    return timeStr
  }
}

const formatDate = (timeStr) => {
  if (!timeStr) return ''
  try {
    const date = new Date(timeStr)
    return `${date.getFullYear()}-${String(date.getMonth()+1).padStart(2,'0')}-${String(date.getDate()).padStart(2,'0')}`
  } catch {
    return timeStr
  }
}

// =============================================
// ===== 加载详情 =====
// =============================================
const loadDetail = async (id) => {
  loading.value = true
  try {
    const res = await getApprovalDetail(id)
    if (res.code === 0 && res.data) {
      detail.value = res.data
    } else {
      showToast(res.msg || '加载失败')
    }
  } catch (error) {
    console.error('加载申请详情失败:', error)
  } finally {
    loading.value = false
  }
}

// =============================================
// ===== 同意 =====
// =============================================
const submitApprove = async () => {
  submitting.value = true
  try {
    const res = await handleApproval({
      id: detail.value.id,
      status: '已通过',
      approverId: currentUser.value.id,
      approverName: currentUser.value.name,
      approveReason: approveReason.value || '同意'
    })
    if (res.code === 0) {
      showToast('审批通过！')
      showApproveModal.value = false
      approveReason.value = ''
      setTimeout(() => router.back(), 500)
    } else {
      showToast(res.msg || '操作失败')
    }
  } catch (error) {
    showToast('操作失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}

// =============================================
// ===== 拒绝 =====
// =============================================
const submitReject = async () => {
  if (!rejectReason.value || rejectReason.value.trim() === '') {
    showToast('请输入拒绝理由')
    return
  }
  submitting.value = true
  try {
    const res = await handleApproval({
      id: detail.value.id,
      status: '已拒绝',
      approverId: currentUser.value.id,
      approverName: currentUser.value.name,
      approveReason: rejectReason.value
    })
    if (res.code === 0) {
      showToast('已拒绝')
      showRejectModal.value = false
      rejectReason.value = ''
      setTimeout(() => router.back(), 500)
    } else {
      showToast(res.msg || '操作失败')
    }
  } catch (error) {
    showToast('操作失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}

// =============================================
// ===== 生命周期 =====
// =============================================
onMounted(() => {
  // 获取当前用户信息
  const userId = localStorage.getItem('employeeId')
  if (userId) {
    currentUser.value.id = parseInt(userId)
  }
  const userName = localStorage.getItem('username')
  if (userName) {
    currentUser.value.name = userName
  }
  
  const id = route.params.id
  if (id) {
    loadDetail(id)
  } else {
    showToast('申请不存在')
  }
})
</script>

<style scoped>
.oa-approval-detail {
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

/* ===== 状态横幅 ===== */
.status-banner {
  border-radius: 12px;
  padding: 16px 20px;
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 16px;
  color: #fff;
}
.status-banner.status-pending { 
  background: linear-gradient(135deg, #fdcb6e, #f39c12); 
}
.status-banner.status-approved { 
  background: linear-gradient(135deg, #00b894, #00cec9); 
}
.status-banner.status-rejected { 
  background: linear-gradient(135deg, #e17055, #d63031); 
}
.status-text { display: flex; flex-direction: column; }
.status-label { font-size: 18px; font-weight: bold; }
.status-time { font-size: 13px; opacity: 0.85; }

/* ===== 信息区块 ===== */
.info-section {
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
:deep(.van-cell) {
  padding: 12px 16px;
}
:deep(.van-cell__title) {
  color: #888;
  font-size: 14px;
}
:deep(.van-cell__value) {
  color: #333;
  font-size: 14px;
  text-align: right;
}

/* ===== 审批流程时间线 ===== */
.flow-timeline {
  background: #fff;
  border-radius: 12px;
  padding: 16px 20px;
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

/* ===== 空状态 ===== */
.empty-state {
  text-align: center;
  padding: 60px 20px;
  color: #ccc;
}
.empty-state p { margin-top: 12px; font-size: 15px; }

/* ===== 底部操作按钮 ===== */
.action-bar {
  display: flex;
  gap: 12px;
  padding: 16px 0 20px;
  background: #fff;
  margin: 0 -16px;
  padding: 16px 16px 20px;
  border-top: 1px solid #f0f0f0;
  position: fixed;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 100%;
  max-width: 430px;
}
.action-bar :deep(.van-button) {
  flex: 1;
  height: 48px;
  border-radius: 12px;
  font-size: 16px;
}
.action-bar :deep(.van-button--danger) {
  border-color: #e17055;
  color: #e17055;
}
.safe-bottom { height: 80px; }
</style>