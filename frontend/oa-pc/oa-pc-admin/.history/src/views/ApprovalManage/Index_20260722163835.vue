<template>
  <div class="page-container">
    <div class="page-header">
      <h2>审批管理</h2>
    </div>

    <!-- 统计卡片 -->
    <el-row :gutter="20" style="margin-bottom:20px;">
      <el-col :span="6" v-for="item in stats" :key="item.title">
        <el-card :body-style="{ padding: '16px' }">
          <div class="stat-item">
            <div class="stat-label">{{ item.title }}</div>
            <div class="stat-number" :style="{ color: item.color }">{{ item.value }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 标签页 -->
    <el-tabs v-model="activeTab" @tab-change="handleTabChange">
      <!-- 待审批 -->
      <el-tab-pane label="待审批" name="pending">
        <el-card>
          <div class="table-toolbar">
            <el-select v-model="filterType" placeholder="筛选类型" clearable @change="loadPendingList" style="width:150px;">
              <el-option label="全部" value="" />
              <el-option label="请假" value="leave" />
              <el-option label="出差" value="business" />
              <el-option label="加班" value="overtime" />
              <el-option label="报销" value="reimburse" />
              <el-option label="采购" value="purchase" />
            </el-select>
            <span style="margin-left:12px;color:#999;font-size:13px;">共 {{ pendingTotal }} 条</span>
          </div>
          <el-table :data="pendingList" style="width:100%;" v-loading="pendingLoading" empty-text="暂无待审批申请">
            <el-table-column prop="title" label="标题" min-width="180">
              <template #default="{ row }">
                <el-link type="primary" @click="openDetailDialog(row.id)">{{ row.title }}</el-link>
              </template>
            </el-table-column>
            <el-table-column prop="approvalType" label="类型" width="100">
              <template #default="{ row }">
                <el-tag>{{ getTypeLabel(row.approvalType) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="applicantName" label="申请人" width="120" />
            <el-table-column prop="departmentName" label="部门" width="120" />
            <el-table-column prop="createTime" label="申请时间" width="180">
              <template #default="{ row }">
                {{ formatTime(row.createTime) }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="280" fixed="right">
              <template #default="{ row }">
                <el-button size="small" type="success" @click="handleApprove(row)">通过</el-button>
                <el-button size="small" type="danger" @click="handleReject(row)">拒绝</el-button>
                <el-button size="small" type="info" plain @click="openDetailDialog(row.id)">详情</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>

      <!-- 已通过 -->
      <el-tab-pane label="已通过" name="approved">
        <el-card>
          <el-table :data="approvedList" style="width:100%;" v-loading="approvedLoading" empty-text="暂无已通过的申请">
            <el-table-column prop="title" label="标题" min-width="180">
              <template #default="{ row }">
                <el-link type="primary" @click="openDetailDialog(row.id)">{{ row.title }}</el-link>
              </template>
            </el-table-column>
            <el-table-column prop="approvalType" label="类型" width="100">
              <template #default="{ row }">
                <el-tag type="success">{{ getTypeLabel(row.approvalType) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="applicantName" label="申请人" width="120" />
            <el-table-column prop="departmentName" label="部门" width="120" />
            <el-table-column prop="approveTime" label="审批时间" width="180">
              <template #default="{ row }">
                {{ formatTime(row.approveTime) }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120">
              <template #default="{ row }">
                <el-button size="small" type="info" plain @click="openDetailDialog(row.id)">详情</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>

      <!-- 已拒绝 -->
      <el-tab-pane label="已拒绝" name="rejected">
        <el-card>
          <el-table :data="rejectedList" style="width:100%;" v-loading="rejectedLoading" empty-text="暂无已拒绝的申请">
            <el-table-column prop="title" label="标题" min-width="180">
              <template #default="{ row }">
                <el-link type="primary" @click="openDetailDialog(row.id)">{{ row.title }}</el-link>
              </template>
            </el-table-column>
            <el-table-column prop="approvalType" label="类型" width="100">
              <template #default="{ row }">
                <el-tag type="danger">{{ getTypeLabel(row.approvalType) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="applicantName" label="申请人" width="120" />
            <el-table-column prop="departmentName" label="部门" width="120" />
            <el-table-column prop="approveTime" label="审批时间" width="180">
              <template #default="{ row }">
                {{ formatTime(row.approveTime) }}
              </template>
            </el-table-column>
            <el-table-column prop="approveReason" label="拒绝理由" min-width="150" />
            <el-table-column label="操作" width="120">
              <template #default="{ row }">
                <el-button size="small" type="info" plain @click="openDetailDialog(row.id)">详情</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>

      <!-- 全部申请 -->
      <el-tab-pane label="全部申请" name="all">
        <el-card>
          <div class="table-toolbar">
            <el-select v-model="allFilterType" placeholder="筛选类型" clearable @change="loadAllList" style="width:150px;margin-right:12px;">
              <el-option label="全部" value="" />
              <el-option label="请假" value="leave" />
              <el-option label="出差" value="business" />
              <el-option label="加班" value="overtime" />
              <el-option label="报销" value="reimburse" />
              <el-option label="采购" value="purchase" />
            </el-select>
            <el-select v-model="allFilterStatus" placeholder="筛选状态" clearable @change="loadAllList" style="width:150px;">
              <el-option label="全部" value="" />
              <el-option label="待审批" value="待审批" />
              <el-option label="已通过" value="已通过" />
              <el-option label="已拒绝" value="已拒绝" />
              <el-option label="已撤回" value="已撤回" />
            </el-select>
          </div>
          <el-table :data="allList" style="width:100%;" v-loading="allLoading" empty-text="暂无申请记录">
            <el-table-column prop="title" label="标题" min-width="180">
              <template #default="{ row }">
                <el-link type="primary" @click="openDetailDialog(row.id)">{{ row.title }}</el-link>
              </template>
            </el-table-column>
            <el-table-column prop="approvalType" label="类型" width="100">
              <template #default="{ row }">
                <el-tag>{{ getTypeLabel(row.approvalType) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="applicantName" label="申请人" width="120" />
            <el-table-column prop="departmentName" label="部门" width="120" />
            <el-table-column prop="createTime" label="申请时间" width="180">
              <template #default="{ row }">
                {{ formatTime(row.createTime) }}
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="getStatusType(row.status)">{{ row.status }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120">
              <template #default="{ row }">
                <el-button size="small" type="info" plain @click="openDetailDialog(row.id)">详情</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div class="pagination">
            <el-pagination
              v-model:page-size="allPageSize"
              v-model:current-page="allPage"
              :total="allTotal"
              layout="total, sizes, prev, pager, next, jumper"
              @size-change="loadAllList"
              @current-change="loadAllList"
            />
          </div>
        </el-card>
      </el-tab-pane>

      <!-- ===== 审批报表 ===== -->
      <el-tab-pane label="审批报表" name="report">
        <el-card>
          <div class="table-toolbar">
            <el-date-picker
              v-model="reportParams.dateRange"
              type="daterange"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              format="YYYY-MM-DD"
              value-format="YYYY-MM-DD"
              @change="loadReportData"
              style="width:280px;"
            />
            <el-button type="primary" @click="loadReportData" style="margin-left:12px;">
              <el-icon><Search /></el-icon> 查询
            </el-button>
            <el-button @click="resetReport">
              <el-icon><Refresh /></el-icon> 重置
            </el-button>
            <el-button type="success" @click="exportReport" :loading="exportLoading">
              <el-icon><Download /></el-icon> 导出
            </el-button>
          </div>

          <!-- 报表统计卡片 -->
          <el-row :gutter="20" style="margin-bottom:20px;">
            <el-col :span="6" v-for="item in reportStats" :key="item.title">
              <el-card :body-style="{ padding: '12px 16px' }">
                <div class="stat-item">
                  <div class="stat-label">{{ item.title }}</div>
                  <div class="stat-number" :style="{ color: item.color }">{{ item.value }}</div>
                </div>
              </el-card>
            </el-col>
          </el-row>

          <el-table :data="reportData" style="width:100%;" v-loading="reportLoading" stripe>
            <el-table-column prop="departmentName" label="部门" width="150" />
            <el-table-column prop="totalCount" label="总申请" width="100" />
            <el-table-column prop="pendingCount" label="待审批" width="100" />
            <el-table-column prop="approvedCount" label="已通过" width="100" />
            <el-table-column prop="rejectedCount" label="已拒绝" width="100" />
            <el-table-column prop="withdrawnCount" label="已撤回" width="100" />
            <el-table-column label="审批率" width="120">
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
          <div class="empty-tip" v-if="!reportLoading && reportData.length === 0">
            暂无数据，请选择日期范围查询
          </div>
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <!-- ===== 审批详情对话框 ===== -->
    <el-dialog v-model="detailVisible" title="审批详情" width="700px">
      <div v-loading="detailLoading">
        <el-descriptions v-if="detailData" :column="2" border>
          <el-descriptions-item label="标题">{{ detailData.title }}</el-descriptions-item>
          <el-descriptions-item label="类型">
            <el-tag>{{ getTypeLabel(detailData.approvalType) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getStatusType(detailData.status)">{{ detailData.status }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="申请人">{{ detailData.applicantName || '未知' }}</el-descriptions-item>
          <el-descriptions-item label="部门">{{ detailData.departmentName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="申请时间">{{ formatTime(detailData.createTime) }}</el-descriptions-item>
          <el-descriptions-item label="申请理由" :span="2">{{ detailData.content || '—' }}</el-descriptions-item>
          <el-descriptions-item v-if="detailData.approveReason" label="审批意见" :span="2">
            {{ detailData.approveReason }}
          </el-descriptions-item>
          <el-descriptions-item v-if="detailData.approverName" label="审批人">{{ detailData.approverName }}</el-descriptions-item>
          <el-descriptions-item v-if="detailData.approveTime" label="审批时间">{{ formatTime(detailData.approveTime) }}</el-descriptions-item>
          <el-descriptions-item v-if="detailData.status === '已撤回'" label="撤回时间" :span="2">
            {{ formatTime(detailData.updateTime) }}
          </el-descriptions-item>
        </el-descriptions>
        <el-empty v-else description="暂无数据" />
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Download } from '@element-plus/icons-vue'
import * as XLSX from 'xlsx'
import {
  getPendingList,
  getApprovalPage,
  getApprovalByStatus,
  handleApproval,
  getApprovalDetail
} from '@/api/approval'
import { getEmployeePage } from '@/api/employee'

// ==================== 当前用户 ====================
const currentUser = ref({
  id: 1,
  name: '系统管理员'
})

// ==================== 员工缓存（用于获取部门信息） ====================
const employeeMap = ref({})

// ==================== 统计 ====================
const stats = ref([
  { title: '待审批', value: 0, color: '#e6a23c' },
  { title: '已通过', value: 0, color: '#67c23a' },
  { title: '已拒绝', value: 0, color: '#f56c6c' },
  { title: '总计', value: 0, color: '#409eff' }
])

// ==================== 标签页 ====================
const activeTab = ref('pending')
const filterType = ref('')

// ==================== 待审批 ====================
const pendingLoading = ref(false)
const pendingList = ref([])
const pendingTotal = ref(0)

// ==================== 已通过 ====================
const approvedLoading = ref(false)
const approvedList = ref([])

// ==================== 已拒绝 ====================
const rejectedLoading = ref(false)
const rejectedList = ref([])

// ==================== 全部 ====================
const allLoading = ref(false)
const allList = ref([])
const allPage = ref(1)
const allPageSize = ref(10)
const allTotal = ref(0)
const allFilterType = ref('')
const allFilterStatus = ref('')

// ==================== 审批报表 ====================
const reportLoading = ref(false)
const exportLoading = ref(false)
const reportData = ref([])
const reportParams = reactive({
  dateRange: []
})
const reportStats = ref([
  { title: '总申请数', value: 0, color: '#409eff' },
  { title: '待审批', value: 0, color: '#e6a23c' },
  { title: '已审批', value: 0, color: '#67c23a' },
  { title: '审批率', value: '0%', color: '#67c23a' }
])

// ==================== 详情 ====================
const detailVisible = ref(false)
const detailLoading = ref(false)
const detailData = ref(null)

// ==================== 类型映射 ====================
const typeMap = {
  leave: '请假',
  business: '出差',
  overtime: '加班',
  reimburse: '报销',
  purchase: '采购',
  card: '补卡'
}

const getTypeLabel = (type) => {
  return typeMap[type] || type || '未知'
}

const getStatusType = (status) => {
  const map = {
    '待审批': 'warning',
    '已通过': 'success',
    '已拒绝': 'danger',
    '已撤回': 'info'
  }
  return map[status] || 'info'
}

// ==================== 工具方法 ====================
const formatTime = (time) => {
  if (!time) return ''
  return time.replace('T', ' ')
}

// ==================== 获取员工部门 ====================
const getEmployeeDepartment = (employeeId) => {
  if (!employeeId) return '未分配'
  const emp = employeeMap.value[employeeId]
  return emp?.departmentName || '未分配'
}

// ==================== 加载所有员工 ====================
const loadAllEmployees = async () => {
  try {
    const res = await getEmployeePage(1, 9999)
    if (res.code === 0) {
      const list = res.data?.data || res.data?.records || []
      const map = {}
      list.forEach(emp => {
        map[emp.id] = {
          name: emp.name,
          departmentName: emp.departmentName || emp.department?.name || '未分配',
          departmentId: emp.departmentId
        }
      })
      employeeMap.value = map
      console.log('员工缓存加载完成，共', Object.keys(map).length, '人')
    }
  } catch (error) {
    console.error('加载员工列表失败:', error)
  }
}

// ==================== 加载数据 ====================
const loadPendingList = async () => {
  pendingLoading.value = true
  try {
    const res = await getPendingList()
    if (res.code === 0) {
      let list = (res.data || []).map(item => ({
        ...item,
        departmentName: item.departmentName || 
                        getEmployeeDepartment(item.applicantId)
      }))
      if (filterType.value) {
        list = list.filter(item => item.approvalType === filterType.value)
      }
      pendingList.value = list
      pendingTotal.value = list.length
      stats.value[0].value = pendingTotal.value
    }
  } catch (error) {
    console.error('加载待审批列表失败:', error)
  } finally {
    pendingLoading.value = false
  }
}

const loadApprovedList = async () => {
  approvedLoading.value = true
  try {
    const res = await getApprovalByStatus('已通过')
    if (res.code === 0) {
      approvedList.value = (res.data || []).map(item => ({
        ...item,
        departmentName: item.departmentName || 
                        getEmployeeDepartment(item.applicantId)
      }))
      stats.value[1].value = approvedList.value.length
    }
  } catch (error) {
    console.error('加载已通过列表失败:', error)
  } finally {
    approvedLoading.value = false
  }
}

const loadRejectedList = async () => {
  rejectedLoading.value = true
  try {
    const res = await getApprovalByStatus('已拒绝')
    if (res.code === 0) {
      rejectedList.value = (res.data || []).map(item => ({
        ...item,
        departmentName: item.departmentName || 
                        getEmployeeDepartment(item.applicantId)
      }))
      stats.value[2].value = rejectedList.value.length
    }
  } catch (error) {
    console.error('加载已拒绝列表失败:', error)
  } finally {
    rejectedLoading.value = false
  }
}

const loadAllList = async () => {
  allLoading.value = true
  try {
    const res = await getApprovalPage(allPage.value, allPageSize.value)
    if (res.code === 0) {
      let list = (res.data.records || []).map(item => ({
        ...item,
        departmentName: item.departmentName || 
                        getEmployeeDepartment(item.applicantId)
      }))
      if (allFilterType.value) {
        list = list.filter(item => item.approvalType === allFilterType.value)
      }
      if (allFilterStatus.value) {
        list = list.filter(item => item.status === allFilterStatus.value)
      }
      allList.value = list
      allTotal.value = list.length
      stats.value[3].value = allTotal.value
    }
  } catch (error) {
    console.error('加载全部列表失败:', error)
  } finally {
    allLoading.value = false
  }
}

const loadAllStats = async () => {
  try {
    const res = await getApprovalPage(1, 1)
    if (res.code === 0) {
      stats.value[3].value = res.data.total || 0
    }
  } catch (error) {
    console.error('加载统计失败:', error)
  }
}

// ==================== 审批报表 ====================
const loadReportData = async () => {
  reportLoading.value = true
  try {
    const res = await getApprovalPage(1, 9999)
    if (res.code === 0) {
      let allData = (res.data.records || []).map(item => ({
        ...item,
        departmentName: item.departmentName || 
                        getEmployeeDepartment(item.applicantId)
      }))
      
      if (reportParams.dateRange && reportParams.dateRange.length === 2) {
        const start = reportParams.dateRange[0]
        const end = reportParams.dateRange[1]
        allData = allData.filter(item => {
          const createDate = item.createTime ? item.createTime.split('T')[0] : ''
          return createDate >= start && createDate <= end
        })
      }
      
      const deptMap = {}
      allData.forEach(item => {
        const dept = item.departmentName || '未分配'
        if (!deptMap[dept]) {
          deptMap[dept] = {
            departmentName: dept,
            totalCount: 0,
            pendingCount: 0,
            approvedCount: 0,
            rejectedCount: 0,
            withdrawnCount: 0,
            totalProcessTime: 0,
            processedCount: 0
          }
        }
        deptMap[dept].totalCount++
        
        if (item.status === '待审批') {
          deptMap[dept].pendingCount++
        } else if (item.status === '已通过') {
          deptMap[dept].approvedCount++
          if (item.createTime && item.approveTime) {
            try {
              const createDate = new Date(item.createTime)
              const approveDate = new Date(item.approveTime)
              if (approveDate > createDate) {
                const diff = (approveDate - createDate) / (1000 * 60 * 60)
                deptMap[dept].totalProcessTime += diff
                deptMap[dept].processedCount++
              }
            } catch (e) {
              console.warn('时间计算异常:', e)
            }
          }
        } else if (item.status === '已拒绝') {
          deptMap[dept].rejectedCount++
          if (item.createTime && item.approveTime) {
            try {
              const createDate = new Date(item.createTime)
              const approveDate = new Date(item.approveTime)
              if (approveDate > createDate) {
                const diff = (approveDate - createDate) / (1000 * 60 * 60)
                deptMap[dept].totalProcessTime += diff
                deptMap[dept].processedCount++
              }
            } catch (e) {
              console.warn('时间计算异常:', e)
            }
          }
        } else if (item.status === '已撤回') {
          deptMap[dept].withdrawnCount++
        }
      })
      
      const result = Object.values(deptMap)
        .filter(item => item.departmentName !== '未分配')
        .map(item => {
          const approvedAndRejected = item.approvedCount + item.rejectedCount
          const approvalRate = item.totalCount > 0 
            ? Math.round((approvedAndRejected / item.totalCount) * 100) 
            : 0
          const avgProcessTime = item.processedCount > 0 
            ? Math.round((item.totalProcessTime / item.processedCount) * 10) / 10 
            : 0
          return { ...item, approvalRate, avgProcessTime }
        })
      
      reportData.value = result
      updateReportStats(result)
      
      if (result.length === 0) {
        ElMessage.info('当前没有审批数据')
      }
    } else {
      ElMessage.error(res.msg || '加载报表失败')
    }
  } catch (error) {
    console.error('加载报表失败:', error)
    ElMessage.error('加载报表失败')
  } finally {
    reportLoading.value = false
  }
}

const updateReportStats = (data) => {
  let total = 0, pending = 0, approved = 0, rejected = 0
  data.forEach(item => {
    total += item.totalCount || 0
    pending += item.pendingCount || 0
    approved += item.approvedCount || 0
    rejected += item.rejectedCount || 0
  })
  const approvedAndRejected = approved + rejected
  reportStats.value[0].value = total
  reportStats.value[1].value = pending
  reportStats.value[2].value = approvedAndRejected
  reportStats.value[3].value = total > 0 
    ? Math.round((approvedAndRejected / total) * 100) + '%' 
    : '0%'
}

const resetReport = () => {
  reportParams.dateRange = []
  loadReportData()
}

// ==================== 导出报表 ====================
const exportReport = async () => {
  exportLoading.value = true
  try {
    const res = await getApprovalPage(1, 9999)
    let allData = (res.data?.records || []).map(item => ({
      ...item,
      departmentName: item.departmentName || 
                      getEmployeeDepartment(item.applicantId)
    }))
    
    if (reportParams.dateRange && reportParams.dateRange.length === 2) {
      const start = reportParams.dateRange[0]
      const end = reportParams.dateRange[1]
      allData = allData.filter(item => {
        const createDate = item.createTime ? item.createTime.split('T')[0] : ''
        return createDate >= start && createDate <= end
      })
    }
    
    if (allData.length === 0) {
      ElMessage.warning('当前日期范围内没有申请记录')
      exportLoading.value = false
      return
    }
    
    const formattedData = allData.map(item => ({
      '标题': item.title || '-',
      '申请人': item.applicantName || '-',
      '部门': item.departmentName || '-',
      '申请类型': getTypeLabel(item.approvalType) || '-',
      '申请时间': formatTime(item.createTime) || '-',
      '状态': item.status || '-',
      '申请内容': item.content || '-',
      '审批人': item.approverName || '-',
      '审批时间': formatTime(item.approveTime) || '-',
      '审批意见': item.approveReason || '-'
    }))

    const wb = XLSX.utils.book_new()
    const ws = XLSX.utils.json_to_sheet(formattedData)
    
    ws['!cols'] = [
      { wch: 25 },
      { wch: 12 },
      { wch: 15 },
      { wch: 12 },
      { wch: 20 },
      { wch: 12 },
      { wch: 40 },
      { wch: 12 },
      { wch: 20 },
      { wch: 30 }
    ]
    
    XLSX.utils.book_append_sheet(wb, ws, '审批记录')
    
    const now = new Date()
    const dateStr = `${now.getFullYear()}-${String(now.getMonth()+1).padStart(2,'0')}-${String(now.getDate()).padStart(2,'0')}`
    XLSX.writeFile(wb, `审批记录_${dateStr}.xlsx`)
    
    ElMessage.success(`导出成功！共导出 ${allData.length} 条记录`)
  } catch (error) {
    console.error('导出失败:', error)
    ElMessage.error('导出失败，请稍后重试')
  } finally {
    exportLoading.value = false
  }
}

// ==================== 标签页切换 ====================
const handleTabChange = (tab) => {
  if (tab === 'pending') loadPendingList()
  else if (tab === 'approved') loadApprovedList()
  else if (tab === 'rejected') loadRejectedList()
  else if (tab === 'all') loadAllList()
  else if (tab === 'report') loadReportData()
}

// ==================== 审批操作 ====================
const handleApprove = async (row) => {
  try {
    await ElMessageBox.confirm(`确定通过「${row.title}」申请吗？`, '提示', {
      confirmButtonText: '通过',
      type: 'success'
    })

    const res = await handleApproval({
      id: row.id,
      status: '已通过',
      approverId: currentUser.value.id,
      approverName: currentUser.value.name,
      approveReason: '同意'
    })

    if (res.code === 0) {
      ElMessage.success('审批通过')
      loadPendingList()
      loadAllList()
      loadAllStats()
      loadApprovedList()
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

    const res = await handleApproval({
      id: row.id,
      status: '已拒绝',
      approverId: currentUser.value.id,
      approverName: currentUser.value.name,
      approveReason: value.trim()
    })

    if (res.code === 0) {
      ElMessage.success('已拒绝')
      loadPendingList()
      loadAllList()
      loadAllStats()
      loadRejectedList()
    } else {
      ElMessage.error(res.msg || '操作失败')
    }
  } catch {
    // 用户取消
  }
}

// ==================== 查看详情 ====================
const openDetailDialog = async (id) => {
  detailVisible.value = true
  detailLoading.value = true
  detailData.value = null
  try {
    const res = await getApprovalDetail(id)
    if (res.code === 0) {
      detailData.value = {
        ...res.data,
        departmentName: res.data.departmentName || 
                        getEmployeeDepartment(res.data.applicantId)
      }
    } else {
      ElMessage.error(res.msg || '获取详情失败')
    }
  } catch (error) {
    console.error('获取详情失败:', error)
    ElMessage.error('获取详情失败，请稍后重试')
  } finally {
    detailLoading.value = false
  }
}

// ==================== 生命周期 ====================
onMounted(async () => {
  const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}')
  if (userInfo.id) {
    currentUser.value.id = userInfo.id
    currentUser.value.name = userInfo.name || '系统管理员'
  }

  // 先加载所有员工信息（获取部门）
  await loadAllEmployees()
  
  loadPendingList()
  loadApprovedList()
  loadRejectedList()
  loadAllList()
  loadAllStats()
})
</script>

<style scoped>
.page-container {
  max-width: 1400px;
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

.table-toolbar {
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.empty-tip {
  text-align: center;
  padding: 40px 0;
  color: #999;
  font-size: 14px;
}

:deep(.el-descriptions__label) {
  width: 120px;
}
</style>