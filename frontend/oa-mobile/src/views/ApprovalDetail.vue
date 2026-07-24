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

      <!-- 审批流程（当前进度） -->
      <div class="info-section">
        <div class="section-title">审批流程</div>
        <!-- 会签进度提示 -->
        <div class="countersign-info" v-if="countersignSummary">
          <van-tag round type="warning" size="medium">
            {{ countersignSummary }}
          </van-tag>
        </div>
        <div class="flow-timeline">
          <div class="flow-step" v-for="(step, index) in flowSteps" :key="index">
            <div class="step-left">
              <div class="step-dot" :class="step.status"></div>
              <div class="step-line" v-if="index < flowSteps.length - 1"></div>
            </div>
            <div class="step-right">
              <div class="step-title">{{ step.title }}</div>
              <div class="step-desc" v-html="step.desc"></div>
              <div class="step-time" v-if="step.time">{{ step.time }}</div>
              <!-- 会签子节点详细信息 -->
              <div class="parallel-detail" v-if="step.parallelTasks && step.parallelTasks.length > 0">
                <div
                  class="parallel-item"
                  v-for="(pt, pi) in step.parallelTasks"
                  :key="pi"
                  :class="pt.outcome === 'REJECT' ? 'parallel-rejected' : pt.outcome ? 'parallel-approved' : ''"
                >
                  <span class="parallel-icon">{{ pt.outcome === 'REJECT' ? '✗' : pt.outcome === 'APPROVE' ? '✓' : '⏳' }}</span>
                  <span class="parallel-name">{{ pt.assigneeName || pt.assignee || '待审批' }}</span>
                  <span class="parallel-comment" v-if="pt.rejectReason || pt.approvalComment">（{{ pt.rejectReason || pt.approvalComment }}）</span>
                </div>
              </div>
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

    <!-- 拒绝弹窗（强制填写理由） -->
    <van-popup v-model:show="showRejectModal" position="bottom" round style="padding:20px 16px 30px;">
      <h3 style="margin:0 0 16px;">拒绝理由</h3>
      <van-field
        v-model="rejectReason"
        rows="3"
        type="textarea"
        placeholder="请填写拒绝理由（必填，申请人和其他已审批人将可见）"
        required
        :rules="[{ required: true, message: '请填写拒绝理由' }]"
      />
      <div class="reject-tip">
        <van-icon name="info-o" /> 拒绝理由将对申请人和已审批人可见
      </div>
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
import { getApprovalDetail, getApprovalProgress, handleApproval } from '@/api/approval'

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

const progress = ref({
  approval: null,
  currentActivities: null,
  historyTasks: [],
  pendingTasks: [],
  signType: '',
  requiredRoles: ''
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
      break
    case 'business':
      if (d.destCity) fields['出差城市'] = d.destCity
      if (d.startTime) fields['出发日期'] = formatDate(d.startTime)
      if (d.endTime) fields['返回日期'] = formatDate(d.endTime)
      break
    case 'overtime':
      if (d.workDate) fields['加班日期'] = formatDate(d.workDate)
      if (d.startTimeOnly) fields['开始时间'] = d.startTimeOnly
      if (d.endTimeOnly) fields['结束时间'] = d.endTimeOnly
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
// ===== 会签进度汇总 =====
// =============================================
const countersignSummary = computed(() => {
  const p = progress.value
  const pendingTasks = p.pendingTasks || []
  const historyTasks = p.historyTasks || []

  if (pendingTasks.length <= 1) return null

  // 按节点分组统计
  const nodeMap = {}
  pendingTasks.forEach(t => {
    const key = t.name || '未知节点'
    if (!nodeMap[key]) nodeMap[key] = { pending: 0, completed: 0, total: 0 }
    nodeMap[key].pending++
    nodeMap[key].total++
  })
  historyTasks.forEach(t => {
    if (t.completeTime) {
      const key = t.name || '未知节点'
      if (!nodeMap[key]) nodeMap[key] = { pending: 0, completed: 0, total: 0 }
      nodeMap[key].completed++
      nodeMap[key].total++
    }
  })

  const multiNodes = Object.entries(nodeMap).filter(([, info]) => info.total > 1)
  if (multiNodes.length === 0) return null

  const [name, info] = multiNodes[0]
  return `「${name}」会签中: ${info.completed}/${info.total} 已完成`
})

// =============================================
// ===== 审批流程（智能识别三种模式） =====
// =============================================
const flowSteps = computed(() => {
  const steps = []
  const d = detail.value
  const p = progress.value
  const signType = p.signType || ''
  const historyTasks = p.historyTasks || []
  const pendingTasks = p.pendingTasks || []

  // 第一步：提交申请
  steps.push({
    title: '提交申请',
    desc: `${d.applicantName || '申请人'} 提交了申请`,
    time: formatTime(d.createTime),
    status: 'done'
  })

  // 判断是否有真实流程数据
  if (historyTasks.length > 0 || pendingTasks.length > 0) {
    // 按节点分组（taskDefinitionKey 分组）
    const nodeGroups = groupTasksByNode(historyTasks, pendingTasks)

    nodeGroups.forEach((node, index) => {
      const allCompleted = node.tasks.every(t => t.completeTime)
      const hasReject = node.tasks.some(t => t.outcome === 'REJECT' || t.outcome === '已拒绝')
      const isActive = !allCompleted

      // 节点状态
      let status = 'pending'
      if (allCompleted) status = hasReject ? 'rejected' : 'done'
      else if (isActive) status = 'active'

      // 构建描述
      let desc = ''
      if (allCompleted && hasReject) {
        const rejectTask = node.tasks.find(t => t.outcome === 'REJECT')
        const rejectorName = rejectTask?.assigneeName || rejectTask?.assignee || '审批人'
        const reason = rejectTask?.rejectReason || ''
        desc = `<span style="color:#ee0a24;">✗ 被 ${rejectorName} 拒绝</span>`
        if (reason) desc += `<br/><span style="font-size:12px;color:#999;">理由：${reason}</span>`
      } else if (allCompleted) {
        if (node.tasks.length > 1) {
          const names = node.tasks.map(t => t.assigneeName || t.assignee || '未知').filter(Boolean)
          desc = `✓ 已全部通过（${names.join('、')}）`
        } else {
          const name = node.tasks[0]?.assigneeName || node.tasks[0]?.assignee || '审批人'
          desc = `✓ ${name} 已通过`
        }
      } else {
        // 当前活跃节点
        if (node.tasks.length > 1) {
          const completedCount = node.tasks.filter(t => t.completeTime).length
          const pendingNames = node.tasks
            .filter(t => !t.completeTime)
            .map(t => t.assigneeName || t.assignee || '待审批')
            .filter(Boolean)
          desc = `⏳ 会签中（${completedCount}/${node.tasks.length}）`
          if (pendingNames.length > 0) {
            desc += `<br/><span style="font-size:12px;color:#999;">待审批：${pendingNames.join('、')}</span>`
          }
        } else {
          const name = node.tasks[0]?.assigneeName || node.tasks[0]?.assignee || '审批人'
          desc = `⏳ 待 ${name} 审批`
        }
      }

      const stepInfo = {
        title: node.nodeName || '审批节点',
        desc: desc,
        time: node.completedTime ? formatTime(node.completedTime) : '',
        status: status,
        parallelTasks: node.tasks.length > 1 ? node.tasks : []
      }

      steps.push(stepInfo)
    })
  } else {
    // 没有真实进度数据时回退到简单展示
    if (d.status === '待审批') {
      steps.push({
        title: '等待审批',
        desc: '⏳ 待审批',
        time: '',
        status: 'active'
      })
    } else if (d.status === '已通过') {
      steps.push({
        title: '审批通过',
        desc: '✓ 申请已通过',
        time: formatTime(d.approveTime || d.updateTime),
        status: 'done'
      })
    } else if (d.status === '已拒绝') {
      steps.push({
        title: '审批拒绝',
        desc: `✗ 理由：${d.approveReason || '无'}`,
        time: formatTime(d.approveTime || d.updateTime),
        status: 'rejected'
      })
    }
  }

  return steps
})

/**
 * 将 Flowable 任务按节点分组
 * 同一节点上的多个并行任务（会签）会被合并到一个步骤中展示
 */
function groupTasksByNode(historyTasks, pendingTasks) {
  // 合并历史+待办，按 taskDefinitionKey 和 name 分组
  const allTasks = [
    ...historyTasks.map(t => ({ ...t, completeTime: t.completeTime || null })),
    ...pendingTasks.map(t => ({ ...t, completeTime: null }))
  ]

  // 按 taskDefinitionKey 或 name 分组
  const groups = {}
  allTasks.forEach(task => {
    const key = task.taskDefinitionKey || task.name || 'unknown'
    if (!groups[key]) {
      groups[key] = {
        nodeName: task.name || '审批节点',
        nodeKey: key,
        tasks: [],
        completedTime: null
      }
    }
    groups[key].tasks.push(task)
    // 取该节点最晚的完成时间
    if (task.completeTime && (!groups[key].completedTime || task.completeTime > groups[key].completedTime)) {
      groups[key].completedTime = task.completeTime
    }
  })

  // 按任务创建时间排序
  return Object.values(groups).sort((a, b) => {
    const aTime = a.tasks[0]?.createTime || ''
    const bTime = b.tasks[0]?.createTime || ''
    return aTime.localeCompare(bTime)
  })
}

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
    const [detailRes, progressRes] = await Promise.all([
      getApprovalDetail(id),
      getApprovalProgress(id).catch(() => ({ code: -1 }))
    ])

    if (detailRes.code === 0 && detailRes.data) {
      detail.value = detailRes.data
    } else {
      showToast(detailRes.msg || '加载失败')
    }

    if (progressRes.code === 0 && progressRes.data) {
      progress.value = progressRes.data
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
      setTimeout(() => {
        loadDetail(detail.value.id)
      }, 300)
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
// ===== 拒绝（强制填写理由） =====
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
      setTimeout(() => {
        loadDetail(detail.value.id)
      }, 300)
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

/* ===== 会签进度摘要 ===== */
.countersign-info {
  margin-bottom: 10px;
  text-align: center;
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
.step-dot.rejected { border-color: #ee0a24; background: #ee0a24; }
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

/* ===== 会签子节点详细信息 ===== */
.parallel-detail {
  margin-top: 6px;
  padding: 8px 10px;
  background: #f8f9fa;
  border-radius: 8px;
}
.parallel-item {
  font-size: 12px;
  padding: 3px 0;
  display: flex;
  align-items: center;
  gap: 6px;
}
.parallel-item.parallel-approved { color: #00b894; }
.parallel-item.parallel-rejected { color: #ee0a24; }
.parallel-icon { font-weight: bold; }
.parallel-name { font-weight: 500; }
.parallel-comment { color: #999; font-size: 11px; }

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

/* ===== 拒绝提示 ===== */
.reject-tip {
  font-size: 12px;
  color: #999;
  margin-top: 8px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.safe-bottom { height: 80px; }
</style>
