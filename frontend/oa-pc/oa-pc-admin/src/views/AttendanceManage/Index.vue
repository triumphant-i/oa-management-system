<template>
  <div class="page-container">
    <div class="page-header">
      <h2>考勤管理</h2>
      <div class="header-actions">
        <el-button type="primary" @click="exportData">
          <el-icon><Download /></el-icon> 导出报表
        </el-button>
        <el-button type="success" @click="loadAllRecords">
          <el-icon><Refresh /></el-icon> 刷新
        </el-button>
      </div>
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

    <!-- 筛选条件 -->
    <el-card class="search-card">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="日期">
          <el-date-picker
            v-model="searchForm.date"
            type="date"
            placeholder="选择日期"
            clearable
            value-format="YYYY-MM-DD"
            @change="handleSearch"
          />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="searchForm.name" placeholder="请输入姓名" clearable style="width:150px;" @clear="handleSearch" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="请选择状态" clearable @change="handleSearch" style="width:150px;">
            <el-option label="全部" value="" />
            <el-option label="正常" value="正常" />
            <el-option label="迟到" value="迟到" />
            <el-option label="早退" value="早退" />
            <el-option label="迟到/早退" value="迟到/早退" />
            <el-option label="待审核" value="待审核" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 考勤记录表格 -->
    <el-card>
      <el-table :data="filteredRecords" style="width:100%;" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="employeeName" label="姓名" width="120" />
        <el-table-column prop="date" label="日期" width="120">
          <template #default="{ row }">
            {{ row.date }}
          </template>
        </el-table-column>
        <el-table-column prop="checkInTime" label="签到时间" width="180">
          <template #default="{ row }">
            {{ formatTime(row.checkInTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="checkOutTime" label="签退时间" width="180">
          <template #default="{ row }">
            {{ formatTime(row.checkOutTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="getStatusTagType(row.status)">
              {{ row.status || '—' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="150">
          <template #default="{ row }">
            {{ row.remark || '—' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" plain @click="viewDetail(row)">详情</el-button>
            <el-button 
              v-if="row.status === '待审核'" 
              size="small" type="success" plain 
              @click="handleApprove(row)"
            >审核</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination">
        <el-pagination
          v-model:page-size="pageSize"
          v-model:current-page="currentPage"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadAllRecords"
          @current-change="loadAllRecords"
        />
      </div>
    </el-card>

    <!-- ===== 考勤详情对话框 ===== -->
    <el-dialog v-model="detailVisible" title="考勤详情" width="550px">
      <el-descriptions v-if="detailData" :column="2" border>
        <el-descriptions-item label="姓名">{{ detailData.employeeName }}</el-descriptions-item>
        <el-descriptions-item label="日期">{{ detailData.date }}</el-descriptions-item>
        <el-descriptions-item label="签到时间">{{ formatTime(detailData.checkInTime) || '未签到' }}</el-descriptions-item>
        <el-descriptions-item label="签退时间">{{ formatTime(detailData.checkOutTime) || '未签退' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getStatusTagType(detailData.status)">{{ detailData.status || '—' }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="备注">{{ detailData.remark || '—' }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- ===== 审核补卡对话框 ===== -->
    <el-dialog v-model="approveVisible" title="审核补卡申请" width="500px">
      <el-form :model="approveForm" label-width="100px">
        <el-form-item label="申请人">
          <span>{{ approveForm.employeeName }}</span>
        </el-form-item>
        <el-form-item label="补卡日期">
          <span>{{ approveForm.date }}</span>
        </el-form-item>
        <el-form-item label="补卡原因">
          <span>{{ approveForm.remark }}</span>
        </el-form-item>
        <el-form-item label="审核意见" prop="approveRemark">
          <el-input v-model="approveForm.approveRemark" type="textarea" rows="3" placeholder="请输入审核意见" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="approveVisible = false">取消</el-button>
        <el-button type="danger" @click="handleRejectApprove">拒绝</el-button>
        <el-button type="primary" @click="handleConfirmApprove">通过</el-button>
      </template>
    </el-dialog>

    <!-- ===== 导出对话框 ===== -->
    <el-dialog v-model="exportVisible" title="导出考勤报表" width="500px">
      <el-form label-width="100px">
        <el-form-item label="日期范围">
          <el-date-picker
            v-model="exportParams.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width:100%;"
          />
        </el-form-item>
        <el-form-item label="导出类型">
          <el-radio-group v-model="exportParams.type">
            <el-radio label="all">全部记录</el-radio>
            <el-radio label="filtered">当前筛选结果</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="文件格式">
          <el-radio-group v-model="exportParams.format">
            <el-radio label="xlsx">Excel (.xlsx)</el-radio>
            <el-radio label="csv">CSV (.csv)</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="exportVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmExport" :loading="exportLoading">
          <el-icon><Download /></el-icon> 确认导出
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Download, Refresh } from '@element-plus/icons-vue'
import * as XLSX from 'xlsx'
import {
  getAttendancePage,
  getAttendanceByDate,
  getMyAttendanceRecords
} from '@/api/attendance'
import { getEmployeePage } from '@/api/employee'

// ==================== 当前管理员 ====================
const currentUser = ref({
  id: 1,
  name: '超级管理员'
})

// ==================== 加载状态 ====================
const loading = ref(false)
const exportLoading = ref(false)

// ==================== 统计数据 ====================
const stats = ref([
  { title: '今日签到', value: 0, color: '#67c23a' },
  { title: '今日缺勤', value: 0, color: '#f56c6c' },
  { title: '待审核补卡', value: 0, color: '#e6a23c' },
  { title: '本月总记录', value: 0, color: '#409eff' }
])

// ==================== 列表数据 ====================
const allRecords = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

// ==================== 搜索 ====================
const searchForm = reactive({
  date: null,
  name: '',
  status: ''
})

// ==================== 筛选后的数据 ====================
const filteredRecords = computed(() => {
  let list = allRecords.value

  if (searchForm.name) {
    list = list.filter(item => 
      item.employeeName && item.employeeName.includes(searchForm.name)
    )
  }

  if (searchForm.status) {
    list = list.filter(item => item.status === searchForm.status)
  }

  return list
})

// ==================== 详情 ====================
const detailVisible = ref(false)
const detailData = ref(null)

// ==================== 审核 ====================
const approveVisible = ref(false)
const approveForm = reactive({
  id: null,
  employeeName: '',
  date: '',
  remark: '',
  approveRemark: ''
})

// ==================== 导出 ====================
const exportVisible = ref(false)
const exportParams = reactive({
  dateRange: null,
  type: 'all',
  format: 'xlsx'
})

// ==================== 工具方法 ====================
const formatTime = (time) => {
  if (!time) return ''
  return time.replace('T', ' ')
}

const getStatusTagType = (status) => {
  const map = {
    '正常': 'success',
    '迟到': 'warning',
    '早退': 'warning',
    '迟到/早退': 'danger',
    '待审核': 'info'
  }
  return map[status] || 'info'
}

// ==================== 加载数据 ====================
const loadAllRecords = async () => {
  loading.value = true
  try {
    const res = await getAttendancePage(currentPage.value, pageSize.value)
    if (res.code === 0) {
      allRecords.value = res.data?.data || []
      total.value = res.data?.total || 0
      await updateStats()
    }
  } catch (error) {
    console.error('加载考勤记录失败:', error)
    ElMessage.error('加载考勤记录失败')
  } finally {
    loading.value = false
  }
}

const updateStats = async () => {
  try {
    const today = new Date().toISOString().split('T')[0]
    
    // 🔧 获取今日考勤记录
    const res = await getAttendanceByDate(today)
    let checkedIn = 0
    if (res.code === 0) {
      const list = res.data || []
      checkedIn = list.filter(item => item.checkInTime).length
    }
    stats.value[0].value = checkedIn
    
    // 🔧 获取在职员工总数
    const empRes = await getEmployeePage(1, 9999)
    let totalEmployees = 0
    if (empRes.code === 0) {
      const empList = empRes.data?.data || []
      // 只统计在职员工
      totalEmployees = empList.filter(item => item.status === '在职').length
    }
    
    // 🔧 缺勤 = 在职员工总数 - 今日已签到人数
    stats.value[1].value = Math.max(0, totalEmployees - checkedIn)

    // 待审核补卡
    const pending = allRecords.value.filter(item => item.status === '待审核')
    stats.value[2].value = pending.length
    stats.value[3].value = total.value
  } catch (error) {
    console.error('更新统计失败:', error)
  }
}

// ==================== 搜索 ====================
const handleSearch = async () => {
  if (searchForm.date) {
    loading.value = true
    try {
      const res = await getAttendanceByDate(searchForm.date)
      if (res.code === 0) {
        allRecords.value = res.data || []
        total.value = allRecords.value.length
      }
    } catch (error) {
      console.error('查询失败:', error)
      ElMessage.error('查询失败')
    } finally {
      loading.value = false
    }
  } else {
    currentPage.value = 1
    await loadAllRecords()
  }
}

const resetSearch = () => {
  searchForm.date = null
  searchForm.name = ''
  searchForm.status = ''
  currentPage.value = 1
  loadAllRecords()
}

// ==================== 查看详情 ====================
const viewDetail = (row) => {
  detailData.value = row
  detailVisible.value = true
}

// ==================== 审核补卡 ====================
const handleApprove = (row) => {
  approveForm.id = row.id
  approveForm.employeeName = row.employeeName
  approveForm.date = row.date
  approveForm.remark = row.remark
  approveForm.approveRemark = ''
  approveVisible.value = true
}

const handleConfirmApprove = () => {
  ElMessage.success('补卡申请已通过')
  approveVisible.value = false
  loadAllRecords()
}

const handleRejectApprove = () => {
  ElMessage.warning('补卡申请已拒绝')
  approveVisible.value = false
  loadAllRecords()
}

// ==================== 导出报表 ====================
const exportData = () => {
  exportVisible.value = true
}

const confirmExport = async () => {
  let exportData = []
  
  if (exportParams.type === 'filtered') {
    exportData = filteredRecords.value
  } else {
    try {
      const res = await getAttendancePage(1, 9999)
      if (res.code === 0) {
        exportData = res.data?.data || []
      }
    } catch (error) {
      console.error('获取全部数据失败:', error)
      ElMessage.error('获取数据失败')
      return
    }
  }

  if (exportParams.dateRange && exportParams.dateRange.length === 2) {
    const start = exportParams.dateRange[0]
    const end = exportParams.dateRange[1]
    exportData = exportData.filter(item => {
      return item.date >= start && item.date <= end
    })
  }

  if (exportData.length === 0) {
    ElMessage.warning('没有数据可导出')
    return
  }

  exportLoading.value = true

  try {
    const formattedData = exportData.map(item => ({
      'ID': item.id,
      '姓名': item.employeeName,
      '日期': item.date,
      '签到时间': formatTime(item.checkInTime),
      '签退时间': formatTime(item.checkOutTime),
      '状态': item.status || '—',
      '备注': item.remark || '—'
    }))

    const wb = XLSX.utils.book_new()
    const ws = XLSX.utils.json_to_sheet(formattedData)

    ws['!cols'] = [
      { wch: 8 },
      { wch: 12 },
      { wch: 14 },
      { wch: 20 },
      { wch: 20 },
      { wch: 14 },
      { wch: 20 }
    ]

    XLSX.utils.book_append_sheet(wb, ws, '考勤记录')

    const now = new Date()
    const dateStr = `${now.getFullYear()}-${String(now.getMonth()+1).padStart(2,'0')}-${String(now.getDate()).padStart(2,'0')}`
    const fileName = `考勤报表_${dateStr}`

    if (exportParams.format === 'csv') {
      XLSX.writeFile(wb, `${fileName}.csv`)
    } else {
      XLSX.writeFile(wb, `${fileName}.xlsx`)
    }

    ElMessage.success(`导出成功！共导出 ${exportData.length} 条记录`)
    exportVisible.value = false
  } catch (error) {
    console.error('导出失败:', error)
    ElMessage.error('导出失败，请稍后重试')
  } finally {
    exportLoading.value = false
  }
}

// ==================== 生命周期 ====================
onMounted(() => {
  const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}')
  if (userInfo.id) {
    currentUser.value.id = userInfo.id
    currentUser.value.name = userInfo.name || '超级管理员'
  }
  loadAllRecords()
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
}

.search-card {
  margin-bottom: 20px;
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

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>