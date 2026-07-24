<template>
  <div class="page-container">
    <div class="page-header">
      <h2>审批报表</h2>
      <div class="header-actions">
        <el-button type="success" @click="exportReport" :loading="exportLoading">
          <el-icon><Download /></el-icon> 导出报表
        </el-button>
        <el-button @click="loadReportData">
          <el-icon><Refresh /></el-icon> 刷新
        </el-button>
      </div>
    </div>

    <!-- 统计卡片 -->
    <el-row :gutter="20" style="margin-bottom:20px;">
      <el-col :span="6" v-for="item in reportStats" :key="item.title">
        <el-card :body-style="{ padding: '16px' }">
          <div class="stat-item">
            <div class="stat-label">{{ item.title }}</div>
            <div class="stat-number" :style="{ color: item.color }">{{ item.value }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 搜索/筛选栏 -->
    <el-card class="search-card">
      <el-form :inline="true" :model="reportParams">
        <el-form-item label="日期范围">
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
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadReportData">
            <el-icon><Search /></el-icon> 查询
          </el-button>
          <el-button @click="resetReport">
            <el-icon><Refresh /></el-icon> 重置
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 审批报表表格 -->
    <el-card>
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
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Refresh, Download } from '@element-plus/icons-vue'
import * as XLSX from 'xlsx'
import { getApprovalPage } from '@/api/approval'

// ==================== 数据 ====================
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

const formatTime = (time) => {
  if (!time) return ''
  return time.replace('T', ' ')
}

// ==================== 加载审批报表数据 ====================
const loadReportData = async () => {
  reportLoading.value = true
  try {
    const res = await getApprovalPage(1, 9999)
    if (res.code === 0) {
      let allData = res.data.records || []
      
      // 日期范围过滤
      if (reportParams.dateRange && reportParams.dateRange.length === 2) {
        const start = reportParams.dateRange[0]
        const end = reportParams.dateRange[1]
        allData = allData.filter(item => {
          const createDate = item.createTime ? item.createTime.split('T')[0] : ''
          return createDate >= start && createDate <= end
        })
      }
      
      // 按部门聚合
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
            const diff = new Date(item.approveTime) - new Date(item.createTime)
            if (diff > 0) {
              deptMap[dept].totalProcessTime += diff / (1000 * 60 * 60)
              deptMap[dept].processedCount++
            }
          }
        } else if (item.status === '已拒绝') {
          deptMap[dept].rejectedCount++
          if (item.createTime && item.approveTime) {
            const diff = new Date(item.approveTime) - new Date(item.createTime)
            if (diff > 0) {
              deptMap[dept].totalProcessTime += diff / (1000 * 60 * 60)
              deptMap[dept].processedCount++
            }
          }
        } else if (item.status === '已撤回') {
          deptMap[dept].withdrawnCount++
        }
      })
      
      // 过滤掉"未分配"部门
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
    let allData = res.data?.records || []
    
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

// ==================== 生命周期 ====================
onMounted(() => {
  loadReportData()
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

.header-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.search-card {
  margin-bottom: 20px;
}

.search-card :deep(.el-form-item) {
  margin-bottom: 0;
}

.search-card :deep(.el-form-item__label) {
  font-weight: 500;
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

.empty-tip {
  text-align: center;
  padding: 40px 0;
  color: #999;
  font-size: 14px;
}
</style>