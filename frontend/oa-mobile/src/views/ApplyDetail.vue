<template>
  <div class="oa-apply-detail">
    <!-- 顶部导航 -->
    <div class="page-header">
      <van-icon name="arrow-left" size="22" @click="$router.back()" />
      <h1 class="header-title">申请详情</h1>
      <div class="header-right">
        <!-- 待审批的申请可撤回 -->
        <van-button 
          v-if="detail.status === '待审批'" 
          type="danger" 
          size="small" 
          plain 
          :loading="withdrawLoading"
          @click="handleWithdraw"
        >
          撤回
        </van-button>
      </div>
    </div>

    <!-- 加载状态 -->
    <div class="loading-wrap" v-if="loading">
      <van-loading size="40" text="加载中..." vertical />
    </div>

    <!-- 详情内容 -->
    <template v-else-if="detail.id">
      <!-- 状态横幅 -->
      <div class="status-banner" :class="detail.status === '已通过' ? 'approved' : detail.status === '待审批' ? 'pending' : 'rejected'">
        <van-icon :name="detail.status === '已通过' ? 'success' : detail.status === '待审批' ? 'clock-o' : 'close'" size="28" color="#fff" />
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
          <van-cell title="申请人" :value="detail.applicantName || '我'" />
          <van-cell title="申请时间" :value="formatTime(detail.createTime)" />
          <van-cell title="当前步骤" :value="detail.currentStep || detail.status || '—'" />
        </van-cell-group>
      </div>

      <!-- 申请详情 -->
      <div class="info-section">
        <div class="section-title">申请详情</div>
        <van-cell-group inset>
          <van-cell v-for="(value, key) in detailFields" :key="key" :title="key" :value="value" />
          <van-cell title="申请理由" :value="detail.content || detail.reason || '—'" />
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

      <!-- 审批意见（如果已审批） -->
      <div class="info-section" v-if="detail.approveOpinion">
        <div class="section-title">审批意见</div>
        <van-cell-group inset>
          <van-cell title="审批人" :value="detail.approverName || '—'" />
          <van-cell title="意见" :value="detail.approveOpinion" />
          <van-cell title="审批时间" :value="formatTime(detail.approveTime)" />
        </van-cell-group>
      </div>

      <!-- 附件列表 -->
      <div class="info-section">
        <div class="section-title">附件</div>
        <div class="attachment-list">
          <div 
            v-for="(file, index) in attachments" 
            :key="file.id || index" 
            class="attachment-item"
          >
            <div class="attachment-icon" :class="getFileIconClass(file.fileName)">
              <van-icon :name="getFileIcon(file.fileName)" size="24" />
            </div>
            <div class="attachment-info" @click="handleDownload(file.id)">
              <span class="attachment-name">{{ file.fileName }}</span>
              <span class="attachment-size">{{ formatFileSize(file.fileSize) }}</span>
            </div>
            <van-icon name="delete" size="18" color="#ee0a24" @click="handleDeleteAttachment(file.id, index)" v-if="detail.status === '待审批'" />
          </div>
          <div 
            class="attachment-item upload-btn" 
            @click="triggerFileUpload"
            v-if="detail.status === '待审批' && attachments.length < 10"
          >
            <van-icon name="plus" size="24" color="#999" />
            <span class="upload-text">添加附件</span>
          </div>
          <div class="empty-attachments" v-if="attachments.length === 0">
            <span style="color:#bbb;font-size:14px;">暂无附件</span>
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
    </template>

    <!-- 空状态 -->
    <div class="empty-state" v-else>
      <van-icon name="notes-o" size="48" color="#ccc" />
      <p>申请不存在</p>
    </div>

    <!-- 底部安全区 -->
    <div class="safe-bottom"></div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { showToast, showConfirmDialog } from 'vant'
import { getApprovalDetail, withdrawApproval } from '@/api/approval'
import { getAttachmentList, uploadAttachment, deleteAttachment, downloadAttachment } from '@/api/attachment'

const router = useRouter()
const route = useRoute()

// ===== 状态 =====
const loading = ref(false)
const withdrawLoading = ref(false)
const fileInput = ref(null)
const attachments = ref([])
const detail = ref({
  id: null,
  approvalType: '',
  title: '',
  applicantName: '',
  createTime: '',
  status: '',
  currentStep: '',
  content: '',
  fields: {},
  approveOpinion: '',
  approverName: '',
  approveTime: ''
})

// ===== 类型映射 =====
const typeMap = {
  leave: '请假申请',
  business: '出差申请',
  overtime: '加班申请',
  reimburse: '报销申请',
  purchase: '采购申请',
  card: '补卡申请'
}

// ===== 获取类型显示名称 =====
const getTypeLabel = (type) => {
  return typeMap[type] || type || '申请'
}

// ===== 解析详情字段 =====
const detailFields = computed(() => {
  const fields = {}
  if (detail.value.fields) {
    Object.keys(detail.value.fields).forEach(key => {
      fields[key] = detail.value.fields[key]
    })
  }
  // 如果有额外字段可以在这里添加
  return fields
})

// ===== 审批流程（根据状态生成） =====
const flowSteps = computed(() => {
  const steps = []
  const status = detail.value.status
  
  // 第一步：提交申请
  steps.push({
    title: '提交申请',
    desc: `${detail.value.applicantName || '我'} 提交了申请`,
    time: formatTime(detail.value.createTime),
    status: 'done'
  })
  
  // 第二步：部门主管审批
  if (status === '待审批') {
    steps.push({
      title: '部门主管审批',
      desc: '待审批',
      time: '',
      status: 'active'
    })
  } else if (status === '已通过' || status === '已拒绝') {
    steps.push({
      title: '部门主管审批',
      desc: status === '已通过' ? '已通过' : '已拒绝',
      time: formatTime(detail.value.approveTime),
      status: 'done'
    })
  }
  
  // 第三步：HR/财务审批（根据类型）
  const type = detail.value.approvalType
  if (type === 'leave' || type === 'overtime') {
    if (status === '已通过') {
      steps.push({
        title: 'HR备案',
        desc: '已完成',
        time: formatTime(detail.value.updateTime),
        status: 'done'
      })
    } else if (status === '待审批') {
      steps.push({
        title: 'HR备案',
        desc: '待审批',
        time: '',
        status: 'pending'
      })
    }
  }
  
  if (type === 'reimburse' || type === 'purchase') {
    if (status === '已通过') {
      steps.push({
        title: '财务审批',
        desc: '已通过',
        time: formatTime(detail.value.updateTime),
        status: 'done'
      })
    } else if (status === '待审批') {
      steps.push({
        title: '财务审批',
        desc: '待审批',
        time: '',
        status: 'pending'
      })
    }
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

// =============================================
// ===== 加载详情 =====
// =============================================
const loadDetail = async (id) => {
  loading.value = true
  try {
    const [detailRes, attachmentRes] = await Promise.all([
      getApprovalDetail(id),
      getAttachmentList(id, 'approval')
    ])
    
    if (detailRes.code === 0 && detailRes.data) {
      detail.value = detailRes.data
    } else {
      showToast(detailRes.msg || '加载失败')
    }
    
    if (attachmentRes.code === 0 && attachmentRes.data) {
      attachments.value = Array.isArray(attachmentRes.data) ? attachmentRes.data : []
    }
  } catch (error) {
    console.error('加载申请详情失败:', error)
    showToast('加载失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

// =============================================
// ===== 附件相关方法 =====
// =============================================
const triggerFileUpload = () => {
  fileInput.value?.click()
}

const handleFileChange = async (event) => {
  const files = event.target.files
  if (!files || files.length === 0) return
  
  for (let i = 0; i < files.length; i++) {
    if (attachments.value.length >= 10) {
      showToast('最多只能上传10个附件')
      break
    }
    const file = files[i]
    try {
      const res = await uploadAttachment(file, detail.value.id, 'approval')
      if (res.code === 0 && res.data) {
        attachments.value.push(res.data)
        showToast('附件上传成功')
      } else {
        showToast('附件上传失败')
      }
    } catch (error) {
      console.error('上传附件失败:', error)
      showToast('附件上传失败')
    }
  }
  event.target.value = ''
}

const handleDeleteAttachment = async (id, index) => {
  try {
    const res = await deleteAttachment(id)
    if (res.code === 0) {
      attachments.value.splice(index, 1)
      showToast('附件删除成功')
    } else {
      showToast(res.msg || '删除失败')
    }
  } catch (error) {
    console.error('删除附件失败:', error)
    showToast('删除失败')
  }
}

const handleDownload = async (id) => {
  try {
    const res = await downloadAttachment(id)
    const blob = new Blob([res.data], { type: res.headers['content-type'] || 'application/octet-stream' })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = res.headers['content-disposition']?.split('filename=')[1]?.replace(/"/g, '') || 'attachment'
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
  } catch (error) {
    console.error('下载附件失败:', error)
    showToast('下载失败')
  }
}

const getFileIcon = (fileName) => {
  if (!fileName) return 'file'
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
  if (!fileName) return 'icon-other'
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
  if (!size) return '0 B'
  if (size < 1024) return size + ' B'
  if (size < 1024 * 1024) return (size / 1024).toFixed(1) + ' KB'
  return (size / (1024 * 1024)).toFixed(1) + ' MB'
}

// =============================================
// ===== 撤回申请 =====
// =============================================
const handleWithdraw = () => {
  if (detail.value.status !== '待审批') {
    showToast('只有待审批的申请可以撤回')
    return
  }
  
  showConfirmDialog({
    title: '确认撤回',
    message: '确定要撤回该申请吗？撤回后可以修改重新提交。',
    confirmButtonText: '确定撤回'
  }).then(async () => {
    withdrawLoading.value = true
    try {
      const res = await withdrawApproval(detail.value.id)
      if (res.code === 0) {
        showToast('✅ 已撤回申请')
        setTimeout(() => router.back(), 500)
      } else {
        showToast(res.msg || '撤回失败')
      }
    } catch (error) {
      showToast('撤回失败，请稍后重试')
    } finally {
      withdrawLoading.value = false
    }
  }).catch(() => {})
}

// =============================================
// ===== 生命周期 =====
// =============================================
onMounted(() => {
  const id = parseInt(route.params.id)
  if (id) {
    loadDetail(id)
  } else {
    showToast('申请不存在')
  }
})
</script>

<style scoped>
.oa-apply-detail {
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
.status-banner.approved { background: linear-gradient(135deg, #00b894, #00cec9); }
.status-banner.pending { background: linear-gradient(135deg, #fdcb6e, #f39c12); }
.status-banner.rejected { background: linear-gradient(135deg, #e17055, #d63031); }
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
.empty-state p {
  margin-top: 12px;
  font-size: 15px;
}

/* ===== 底部安全区 ===== */
.safe-bottom { height: 20px; }

/* ===== 附件样式 ===== */
.attachment-list {
  background: #fff;
  border-radius: 12px;
  padding: 12px;
}
.attachment-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px;
  border-radius: 8px;
  margin-bottom: 8px;
  background: #f9f9f9;
}
.attachment-item:last-child {
  margin-bottom: 0;
}
.attachment-item.upload-btn {
  background: #fff;
  border: 2px dashed #ddd;
  justify-content: center;
  cursor: pointer;
}
.attachment-icon {
  width: 40px;
  height: 40px;
  border-radius: 8px;
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
  cursor: pointer;
}
.attachment-name {
  display: block;
  font-size: 14px;
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
  font-size: 14px;
  color: #999;
}
.empty-attachments {
  text-align: center;
  padding: 20px;
}
.hidden-input {
  display: none;
}
</style>