<template>
  <div class="page-container">
    <div class="page-header">
      <h2>📊 数据统计中心</h2>
      <el-button @click="refreshAll" :loading="loading">
        <el-icon><Refresh /></el-icon> 刷新数据
      </el-button>
    </div>

    <!-- 统计概览 -->
    <el-row :gutter="20" style="margin-bottom:20px;">
      <el-col :span="6" v-for="item in overview" :key="item.title">
        <el-card :body-style="{ padding: '16px' }">
          <div class="stat-item">
            <div class="stat-label">{{ item.title }}</div>
            <div class="stat-number" :style="{ color: item.color }">{{ item.value }}</div>
            <div class="stat-sub">{{ item.sub }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 考勤统计 -->
    <el-card style="margin-bottom:20px;">
      <template #header>
        <div class="card-header">
          <span>📋 考勤统计</span>
          <div class="header-actions">
            <el-date-picker
              v-model="attendanceParams.date"
              type="month"
              placeholder="选择月份"
              format="YYYY年MM月"
              value-format="YYYY-MM"
              @change="loadAttendanceStats"
              style="width:160px;"
            />
            <el-select v-model="attendanceParams.employeeId" placeholder="选择员工" clearable filterable @change="loadAttendanceStats" style="width:160px;">
              <el-option label="全部员工" :value="null" />
              <el-option 
                v-for="emp in employeeOptions" 
                :key="emp.id" 
                :label="emp.name" 
                :value="emp.id"
              />
            </el-select>
          </div>
        </div>
      </template>
      <el-table :data="attendanceData" style="width:100%;" v-loading="attendanceLoading" stripe>
        <el-table-column prop="employeeName" label="姓名" width="120" />
        <el-table-column prop="totalDays" label="应出勤" width="100" />
        <el-table-column prop="workDays" label="实际出勤" width="100" />
        <el-table-column prop="lateDays" label="迟到" width="100" />
        <el-table-column prop="earlyDays" label="早退" width="100" />
        <el-table-column prop="absentDays" label="缺勤" width="100" />
        <el-table-column prop="overtimeHours" label="加班(小时)" width="120" />
        <el-table-column label="出勤率" width="120">
          <template #default="{ row }">
            <el-progress 
              :percentage="row.attendanceRate || 0" 
              :color="getRateColor(row.attendanceRate)"
              :stroke-width="8"
            />
          </template>
        </el-table-column>
      </el-table>
      <div class="empty-tip" v-if="!attendanceLoading && attendanceData.length === 0">
        暂无数据
      </div>
    </el-card>

    <!-- 审批统计 -->
    <el-card>
      <template #header>
        <div class="card-header">
          <span>📋 审批统计报表</span>
          <div class="header-actions">
            <el-date-picker
              v-model="approvalParams.dateRange"
              type="daterange"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              format="YYYY-MM-DD"
              value-format="YYYY-MM-DD"
              @change="loadApprovalStats"
              style="width:280px;"
            />
          </div>
        </div>
      </template>
      <el-table :data="approvalData" style="width:100%;" v-loading="approvalLoading" stripe>
        <el-table-column prop="departmentName" label="部门" width="150" />
        <el-table-column prop="totalCount" label="总申请" width="100" />
        <el-table-column prop="pendingCount" label="待审批" width="100" />
        <el-table-column prop="approvedCount" label="已通过" width="100" />
        <el-table-column prop="rejectedCount" label="已拒绝" width="100" />
        <el-table-column prop="withdrawnCount" label="已撤回" width="100" />
        <el-table-column label="通过率" width="120">
          <template #default="{ row }">
            <el-progress 
              :percentage="row.approvalRate || 0" 
              :color="row.approvalRate > 70 ? '#67c23a' : row.approvalRate > 40 ? '#e6a23c' : '#f56c6c'"
              :stroke-width="8"
            />
          </template>
        </el-table-column>
        <el-table-column prop="avgProcessTime" label="平均处理时长" width="140">
          <template #default="{ row }">
            {{ row.avgProcessTime ? row.avgProcessTime + ' 小时' : '-' }}
          </template>
        </el-table-column>
      </el-table>
      <div class="empty-tip" v-if="!approvalLoading && approvalData.length === 0">
        暂无数据
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import {
  getAttendanceStatistics,
  getApprovalStatistics
} from '@/api/database'
import { getAllEmployees } from '@/api/employee'

// ==================== 加载状态 ====================
const loading = ref(false)
const attendanceLoading = ref(false)
const approvalLoading = ref(false)

// ==================== 统计概览 ====================
const overview = ref([
  { title: '📅 本月出勤率', value: '0%', color: '#67c23a', sub: '加载中...' },
  { title: '📋 待审批总数', value: '0', color: '#e6a23c', sub: '加载中...' },
  { title: '✅ 审批通过率', value: '0%', color: '#409eff', sub: '加载中...' },
  { title: '⏱️ 平均处理时长', value: '0h', color: '#f56c6c', sub: '加载中...' }
])

// ==================== 考勤统计 ====================
const attendanceParams = reactive({
  employeeId: null,
  date: new Date().toISOString().slice(0, 7) // YYYY-MM
})
const attendanceData = ref([])
const employeeOptions = ref([])

// ==================== 审批统计 ====================
const approvalParams = reactive({
  dateRange: []
})
const approvalData = ref([])

// ==================== 工具方法 ====================
const getRateColor = (rate) => {
  if (rate >= 90) return '#67c23a'
  if (rate >= 70) return '#e6a23c'
  return '#f56c6c'
}

// ==================== 加载员工列表 ====================
const loadEmployeeOptions = async () => {
  try {
    const res = await getAllEmployees()
    if (res.code === 0) {
      employeeOptions.value = Array.isArray(res.data) ? res.data : []
    }
  } catch (error) {
    console.error('加载员工列表失败:', error)
  }
}

// ==================== 加载考勤统计 ====================
const loadAttendanceStats = async () => {
  attendanceLoading.value = true
  try {
    let year = new Date().getFullYear()
    let month = new Date().getMonth() + 1
    
    if (attendanceParams.date) {
      const parts = attendanceParams.date.split('-')
      year = parseInt(parts[0])
      month = parseInt(parts[1])
    }

    // 调用 API
    const res = await getAttendanceStatistics(
      attendanceParams.employeeId || '',
      year,
      month
    )

    if (res.code === 0) {
      const data = res.data || []
      attendanceData.value = Array.isArray(data) ? data : []
      
      // 更新概览数据
      if (attendanceData.value.length > 0) {
        updateOverviewFromAttendance(attendanceData.value)
      }
    } else {
      ElMessage.warning(res.message || '获取考勤数据失败')
      attendanceData.value = []
    }
  } catch (error) {
    console.error('加载考勤统计失败:', error)
    ElMessage.error('加载考勤统计失败')
    attendanceData.value = []
  } finally {
    attendanceLoading.value = false
  }
}

// ==================== 从考勤数据更新概览 ====================
const updateOverviewFromAttendance = (data) => {
  try {
    let totalRate = 0
    let count = 0
    
    data.forEach(item => {
      if (item.attendanceRate !== undefined && item.attendanceRate !== null) {
        totalRate += item.attendanceRate
        count++
      }
    })
    
    const avgRate = count > 0 ? (totalRate / count) : 0
    
    overview.value = overview.value.map(item => {
      if (item.title.includes('出勤率')) {
        return { ...item, value: avgRate.toFixed(1) + '%', sub: '平均出勤率' }
      }
      return item
    })
  } catch (error) {
    console.error('更新概览数据失败:', error)
  }
}

// ==================== 加载审批统计 ====================
const loadApprovalStats = async () => {
  approvalLoading.value = true
  try {
    let startDate = new Date().toISOString().slice(0, 10)
    let endDate = new Date().toISOString().slice(0, 10)
    
    if (approvalParams.dateRange && approvalParams.dateRange.length === 2) {
      startDate = approvalParams.dateRange[0]
      endDate = approvalParams.dateRange[1]
    } else {
      // 默认最近30天
      const end = new Date()
      const start = new Date()
      start.setDate(start.getDate() - 30)
      startDate = start.toISOString().slice(0, 10)
      endDate = end.toISOString().slice(0, 10)
    }

    const res = await getApprovalStatistics(startDate, endDate)

    if (res.code === 0) {
      const data = res.data || []
      approvalData.value = Array.isArray(data) ? data : []
      
      // 更新概览数据
      if (approvalData.value.length > 0) {
        updateOverviewFromApproval(approvalData.value)
      }
    } else {
      ElMessage.warning(res.message || '获取审批数据失败')
      approvalData.value = []
    }
  } catch (error) {
    console.error('加载审批统计失败:', error)
    ElMessage.error('加载审批统计失败')
    approvalData.value = []
  } finally {
    approvalLoading.value = false
  }
}

// ==================== 从审批数据更新概览 ====================
const updateOverviewFromApproval = (data) => {
  try {
    let totalPending = 0
    let totalApproved = 0
    let totalRejected = 0
    let totalProcessTime = 0
    let count = 0
    
    data.forEach(item => {
      totalPending += (item.pendingCount || 0)
      totalApproved += (item.approvedCount || 0)
      totalRejected += (item.rejectedCount || 0)
      if (item.avgProcessTime !== undefined && item.avgProcessTime !== null) {
        totalProcessTime += item.avgProcessTime
        count++
      }
    })
    
    const totalCount = totalPending + totalApproved + totalRejected
    const approvalRate = totalCount > 0 ? (totalApproved / totalCount * 100) : 0
    const avgProcessTime = count > 0 ? (totalProcessTime / count) : 0
    
    overview.value = overview.value.map(item => {
      if (item.title.includes('待审批')) {
        return { ...item, value: String(totalPending), sub: '待审批总数' }
      }
      if (item.title.includes('通过率')) {
        return { ...item, value: approvalRate.toFixed(1) + '%', sub: '审批通过率' }
      }
      if (item.title.includes('平均处理时长')) {
        return { ...item, value: avgProcessTime.toFixed(1) + 'h', sub: '平均处理时长' }
      }
      return item
    })
  } catch (error) {
    console.error('更新概览数据失败:', error)
  }
}

// ==================== 刷新全部 ====================
const refreshAll = async () => {
  loading.value = true
  try {
    await Promise.all([
      loadAttendanceStats(),
      loadApprovalStats()
    ])
    ElMessage.success('数据已刷新')
  } catch (error) {
    console.error('刷新失败:', error)
    ElMessage.error('刷新失败')
  } finally {
    loading.value = false
  }
}

// ==================== 生命周期 ====================
onMounted(() => {
  loadEmployeeOptions()
  loadAttendanceStats()
  loadApprovalStats()
})
</script>

<style scoped>
.page-container {
  max-width: 1400px;
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0;
}

.stat-item {
  text-align: center;
}

.stat-label {
  font-size: 14px;
  color: #999;
}

.stat-number {
  font-size: 28px;
  font-weight: bold;
  margin-top: 4px;
}

.stat-sub {
  font-size: 12px;
  color: #bbb;
  margin-top: 4px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.empty-tip {
  text-align: center;
  padding: 30px 0;
  color: #999;
  font-size: 14px;
}

/* 响应式 */
@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
  
  .card-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
  
  .header-actions {
    width: 100%;
    flex-direction: column;
  }
  
  .header-actions .el-date-picker,
  .header-actions .el-select {
    width: 100% !important;
  }
}
</style>