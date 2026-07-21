<template>
  <div class="dashboard">
    <!-- 统计卡片 -->
    <el-row :gutter="20">
      <el-col :span="6" v-for="item in stats" :key="item.title">
        <el-card class="stat-card" :body-style="{ padding: '20px' }">
          <div class="stat-content">
            <div class="stat-info">
              <div class="stat-title">{{ item.title }}</div>
              <div class="stat-value">{{ item.value }}</div>
            </div>
            <div class="stat-icon" :style="{ background: item.color }">
              <el-icon :size="28"><component :is="item.icon" /></el-icon>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表和待办 -->
    <el-row :gutter="20" style="margin-top:20px;">
      <el-col :span="16">
        <el-card>
          <template #header>
            <span>📋 待办审批</span>
            <el-button text type="primary" style="float:right;" @click="$router.push('/approval')">
              查看全部 →
            </el-button>
          </template>
          <el-table :data="pendingList" style="width:100%;" v-loading="pendingLoading" empty-text="暂无待审批">
            <el-table-column prop="title" label="标题" min-width="200">
              <template #default="{ row }">
                <el-link type="primary" @click="$router.push(`/approval/detail/${row.id}`)">
                  {{ row.title }}
                </el-link>
              </template>
            </el-table-column>
            <el-table-column prop="applicant" label="申请人" width="120" />
            <el-table-column prop="time" label="申请时间" width="180" />
            <el-table-column label="操作" width="200">
              <template #default="{ row }">
                <el-button size="small" type="success" @click="handleApprove(row)">通过</el-button>
                <el-button size="small" type="danger" @click="handleReject(row)">拒绝</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <!-- 快捷操作（管理员专用） -->
      <el-col :span="8">
        <el-card>
          <template #header><span>⚡ 快捷操作</span></template>
          <div class="quick-actions">
            <el-button type="primary" plain style="width:100%;margin-bottom:10px;" @click="$router.push('/employee')">
              <el-icon><User /></el-icon> 添加员工
            </el-button>
            <el-button type="success" plain style="width:100%;margin-bottom:10px;" @click="$router.push('/notice')">
              <el-icon><Edit /></el-icon> 发布公告
            </el-button>
            <el-button type="warning" plain style="width:100%;margin-bottom:10px;" @click="$router.push('/department')">
              <el-icon><OfficeBuilding /></el-icon> 管理部门
            </el-button>
            <el-button type="info" plain style="width:100%;" @click="$router.push('/system')">
              <el-icon><Setting /></el-icon> 系统设置
            </el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 系统概览 -->
    <el-row :gutter="20" style="margin-top:20px;">
      <el-col :span="24">
        <el-card>
          <template #header><span>📊 系统概览</span></template>
          <el-row :gutter="20">
            <el-col :span="6" v-for="item in overview" :key="item.label">
              <div class="overview-item">
                <div class="overview-number">{{ item.value }}</div>
                <div class="overview-label">{{ item.label }}</div>
              </div>
            </el-col>
          </el-row>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  User, Document, Bell, Calendar,
  Plus, Edit, Setting, OfficeBuilding
} from '@element-plus/icons-vue'
import { getPendingList, handleApproval } from '@/api/approval'
import { getEmployeePage } from '@/api/employee'
import { getDepartmentList } from '@/api/department'

const router = useRouter()
const pendingLoading = ref(false)

// ==================== 统计卡片 ====================
const stats = ref([
  { title: '员工总数', value: 0, icon: User, color: '#409eff' },
  { title: '待审批', value: 0, icon: Document, color: '#e6a23c' },
  { title: '部门总数', value: 0, icon: OfficeBuilding, color: '#67c23a' },
  { title: '今日签到', value: 0, icon: Calendar, color: '#f56c6c' }
])

// ==================== 待办审批 ====================
const pendingList = ref([])

// ==================== 系统概览 ====================
const overview = ref([
  { label: '系统管理员', value: 0 },
  { label: '部门主管', value: 0 },
  { label: '普通员工', value: 0 },
  { label: '流程管理员', value: 0 }
])

// ==================== 加载数据 ====================
const loadStats = async () => {
  try {
    // 获取员工总数
    const empRes = await getEmployeePage(1, 1)
    if (empRes.code === 0) {
      stats.value[0].value = empRes.data?.total || 0
    }

    // 获取待审批数量
    const pendingRes = await getPendingList()
    if (pendingRes.code === 0) {
      const list = pendingRes.data || []
      stats.value[1].value = list.length
      pendingList.value = list.slice(0, 5).map(item => ({
        id: item.id,
        title: item.title || `${item.applicantName} - ${item.approvalType}`,
        applicant: item.applicantName || '未知',
        time: item.createTime?.replace('T', ' ') || ''
      }))
    }

    // 获取部门总数
    const deptRes = await getDepartmentList()
    if (deptRes.code === 0) {
      stats.value[2].value = deptRes.data?.length || 0
    }

    // 今日签到（模拟数据）
    stats.value[3].value = Math.floor(Math.random() * 50) + 50

    // 角色统计（模拟）
    overview.value[0].value = 1
    overview.value[1].value = 3
    overview.value[2].value = stats.value[0].value - 4
    overview.value[3].value = 1
  } catch (error) {
    console.error('加载统计数据失败:', error)
  }
}

// ==================== 审批操作 ====================
const handleApprove = async (row) => {
  try {
    await ElMessageBox.confirm(`确定通过「${row.title}」申请吗？`, '提示', {
      confirmButtonText: '通过',
      type: 'success'
    })

    const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}')
    const res = await handleApproval({
      id: row.id,
      status: '已通过',
      approverId: userInfo.id || 1,
      approverName: userInfo.name || '系统管理员',
      approveReason: '同意'
    })

    if (res.code === 0) {
      ElMessage.success('审批通过')
      loadStats()
    } else {
      ElMessage.error(res.msg || '操作失败')
    }
  } catch {
    // 用户取消
  }
}

const handleReject = async (row) => {
  try {
    const { value } = await ElMessageBox.prompt('请填写拒绝理由：', '拒绝申请', {
      confirmButtonText: '确认拒绝',
      cancelButtonText: '取消',
      inputType: 'textarea',
      inputPlaceholder: '请输入拒绝理由（必填）',
      inputValidator: (val) => {
        if (!val || val.trim() === '') return '请填写拒绝理由'
        return true
      }
    })

    const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}')
    const res = await handleApproval({
      id: row.id,
      status: '已拒绝',
      approverId: userInfo.id || 1,
      approverName: userInfo.name || '系统管理员',
      approveReason: value.trim()
    })

    if (res.code === 0) {
      ElMessage.success('已拒绝')
      loadStats()
    } else {
      ElMessage.error(res.msg || '操作失败')
    }
  } catch {
    // 用户取消
  }
}

// ==================== 生命周期 ====================
onMounted(() => {
  loadStats()
})
</script>

<style scoped>
.dashboard {
  max-width: 1400px;
}

.stat-card {
  border-radius: 12px;
}

.stat-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.stat-info {
  display: flex;
  flex-direction: column;
}

.stat-title {
  color: #999;
  font-size: 14px;
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
  color: #333;
  margin-top: 4px;
}

.stat-icon {
  width: 52px;
  height: 52px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
}

.quick-actions {
  display: flex;
  flex-direction: column;
}

.overview-item {
  text-align: center;
  padding: 12px 0;
}

.overview-number {
  font-size: 32px;
  font-weight: bold;
  color: #1a1a2e;
}

.overview-label {
  color: #999;
  font-size: 14px;
  margin-top: 4px;
}
</style>