<template>
  <div class="page-container">
    <!-- ===== 页面标题 ===== -->
    <div class="page-header">
      <h2>📋 全部会议管理</h2>
      <div class="header-actions">
        <el-button type="primary" @click="loadData">
          <el-icon><Refresh /></el-icon> 刷新
        </el-button>
      </div>
    </div>

    <!-- ===== 统计卡片 ===== -->
    <el-row :gutter="16" style="margin-bottom:20px;">
      <el-col :span="4" v-for="stat in stats" :key="stat.label">
        <el-card :body-style="{ padding: '14px' }" @click="filterByStatus(stat.status)">
          <div class="stat-item" :style="{ cursor: stat.status !== undefined ? 'pointer' : 'default' }">
            <div class="stat-label">{{ stat.label }}</div>
            <div class="stat-number" :style="{ color: stat.color }">{{ stat.value }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- ===== 搜索栏 ===== -->
    <el-card class="search-card">
      <el-form :inline="true" :model="searchForm" @keyup.enter="handleSearch">
        <el-form-item label="关键词">
          <el-input 
            v-model="searchForm.keyword" 
            placeholder="搜索会议名称/组织者" 
            clearable 
            style="width:220px;"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select 
            v-model="searchForm.status" 
            placeholder="全部状态" 
            clearable 
            @change="handleSearch"
            style="width:140px;"
          >
            <el-option label="全部" value="" />
            <el-option label="已预约" value="已预约" />
            <el-option label="进行中" value="进行中" />
            <el-option label="已结束" value="已结束" />
            <el-option label="已取消" value="已取消" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon> 查询
          </el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- ===== 会议列表 ===== -->
    <el-card>
      <el-table :data="tableData" style="width:100%;" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="title" label="会议主题" min-width="150" />
        <el-table-column prop="roomName" label="会议室" width="120" />
        <el-table-column prop="organizerName" label="组织者" width="100" />
        <el-table-column label="会议时间" min-width="200">
          <template #default="{ row }">
            <div>{{ formatDateTime(row.startTime) }}</div>
            <div style="color:#999;font-size:12px;">至 {{ formatDateTime(row.endTime) }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button size="small" type="info" plain @click="viewDetail(row)">
                <el-icon><View /></el-icon> 详情
              </el-button>
              <template v-if="row.status !== '已结束' && row.status !== '已取消'">
                <el-button size="small" type="warning" plain @click="openEditDialog(row)">
                  <el-icon><Edit /></el-icon> 修改
                </el-button>
                <el-button size="small" type="danger" plain @click="handleCancel(row)">
                  <el-icon><CircleClose /></el-icon> 取消
                </el-button>
              </template>
            </div>
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
          @size-change="handlePageChange"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>

    <!-- ===== 会议详情对话框 ===== -->
    <el-dialog v-model="detailVisible" title="会议详情" width="600px">
      <el-descriptions v-if="detailData" :column="2" border>
        <el-descriptions-item label="会议主题" :span="2">{{ detailData.title }}</el-descriptions-item>
        <el-descriptions-item label="会议室">{{ detailData.roomName }}</el-descriptions-item>
        <el-descriptions-item label="组织者">{{ detailData.organizerName }}</el-descriptions-item>
        <el-descriptions-item label="参会人" :span="2">{{ detailData.participants || '无' }}</el-descriptions-item>
        <el-descriptions-item label="开始时间">{{ formatDateTime(detailData.startTime) }}</el-descriptions-item>
        <el-descriptions-item label="结束时间">{{ formatDateTime(detailData.endTime) }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getStatusType(detailData.status)">{{ detailData.status }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ detailData.remark || '无' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间" :span="2">{{ formatDateTime(detailData.createTime) }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- ===== 修改会议对话框 ===== -->
    <el-dialog 
      v-model="editVisible" 
      title="修改会议" 
      width="620px"
      @close="resetEditForm"
    >
      <el-form :model="editForm" :rules="editRules" ref="editFormRef" label-width="100px">
        <el-form-item label="会议主题" prop="title">
          <el-input v-model="editForm.title" placeholder="请输入会议主题" />
        </el-form-item>
        <el-form-item label="会议室">
          <el-input :value="editForm.roomName" disabled />
        </el-form-item>
        <el-form-item label="开始时间" prop="startTime">
          <el-date-picker 
            v-model="editForm.startTime" 
            type="datetime" 
            placeholder="选择开始时间"
            format="YYYY-MM-DD HH:mm"
            value-format="YYYY-MM-DDTHH:mm:ss"
            style="width:100%;"
          />
        </el-form-item>
        <el-form-item label="结束时间" prop="endTime">
          <el-date-picker 
            v-model="editForm.endTime" 
            type="datetime" 
            placeholder="选择结束时间"
            format="YYYY-MM-DD HH:mm"
            value-format="YYYY-MM-DDTHH:mm:ss"
            style="width:100%;"
          />
        </el-form-item>
        
        <!-- ===== 参会人选择（多选） ===== -->
        <el-form-item label="参会人" prop="participantIds">
          <el-select
            v-model="editForm.participantIds"
            multiple
            filterable
            remote
            reserve-keyword
            placeholder="请输入姓名搜索员工"
            :remote-method="searchEmployees"
            :loading="employeeLoading"
            style="width:100%;"
            @change="handleParticipantChange"
          >
            <el-option
              v-for="emp in employeeOptions"
              :key="emp.id"
              :label="emp.name + (emp.departmentName ? ' (' + emp.departmentName + ')' : '')"
              :value="String(emp.id)"
            />
          </el-select>
          <div style="margin-top:8px;font-size:13px;color:#999;">
            已选：<el-tag 
              v-for="id in editForm.participantIds" 
              :key="id" 
              size="small" 
              style="margin:2px;"
              closable
              @close="removeParticipant(id)"
            >
              {{ getEmployeeName(id) }}
            </el-tag>
            <span v-if="editForm.participantIds.length === 0" style="color:#ccc;">暂无参会人</span>
          </div>
        </el-form-item>

        <el-form-item label="备注">
          <el-input v-model="editForm.remark" type="textarea" :rows="2" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmitEdit" :loading="editLoading">
          保存修改
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Search, View, Edit, CircleClose } from '@element-plus/icons-vue'

// ==================== API 接口 ====================
import request from '@/api/request'

// 获取所有会议
const getAllMeetings = (params) => {
  return request.get('/meeting/all', { params })
}

// 取消会议
const cancelMeeting = (id) => {
  return request.put(`/meeting/cancel/${id}`)
}

// 更新会议
const updateMeeting = (data) => {
  return request.put('/meeting/update', data)
}

// 获取所有在职员工
const getAllEmployees = () => {
  return request.get('/employee/findAll')
}

// ==================== 数据 ====================
const loading = ref(false)
const editLoading = ref(false)
const employeeLoading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const tableData = ref([])
const allData = ref([])

// ==================== 员工数据 ====================
const allEmployees = ref([])
const employeeOptions = ref([])

// ==================== 搜索表单 ====================
const searchForm = reactive({
  keyword: '',
  status: ''
})

// ==================== 统计 ====================
const stats = reactive([
  { label: '📊 全部', value: 0, color: '#409eff', status: undefined },
  { label: '📅 已预约', value: 0, color: '#67c23a', status: '已预约' },
  { label: '⏳ 进行中', value: 0, color: '#e6a23c', status: '进行中' },
  { label: '✅ 已结束', value: 0, color: '#909399', status: '已结束' },
  { label: '❌ 已取消', value: 0, color: '#f56c6c', status: '已取消' }
])

// ==================== 详情 ====================
const detailVisible = ref(false)
const detailData = ref(null)

// ==================== 编辑 ====================
const editVisible = ref(false)
const editFormRef = ref()
const editForm = reactive({
  id: null,
  title: '',
  roomName: '',
  startTime: '',
  endTime: '',
  participants: '',
  participantIds: [],
  remark: ''
})

const editRules = {
  title: [{ required: true, message: '请输入会议主题' }],
  startTime: [{ required: true, message: '请选择开始时间' }],
  endTime: [
    { required: true, message: '请选择结束时间' },
    { 
      validator: (rule, value, callback) => {
        if (editForm.startTime && value && value <= editForm.startTime) {
          callback(new Error('结束时间必须晚于开始时间'))
        } else {
          callback()
        }
      }, 
      trigger: 'change' 
    }
  ]
}

// ==================== 工具方法 ====================
const formatDateTime = (dateStr) => {
  if (!dateStr) return ''
  try {
    const date = new Date(dateStr)
    const year = date.getFullYear()
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    const hours = String(date.getHours()).padStart(2, '0')
    const minutes = String(date.getMinutes()).padStart(2, '0')
    return `${year}-${month}-${day} ${hours}:${minutes}`
  } catch {
    return dateStr
  }
}

const getStatusType = (status) => {
  const map = {
    '已预约': 'success',
    '进行中': 'warning',
    '已结束': 'info',
    '已取消': 'danger'
  }
  return map[status] || 'info'
}

// ==================== 员工相关方法 ====================
const loadEmployees = async () => {
  if (allEmployees.value.length > 0) return
  
  try {
    console.log('正在加载员工列表...')
    const res = await getAllEmployees()
    console.log('员工列表响应:', res)
    
    if (res.code === 0) {
      const data = res.data || []
      allEmployees.value = data.map(emp => ({
        ...emp,
        id: emp.id,
        name: emp.name || emp.username || '未知',
        departmentName: emp.departmentName || ''
      }))
      
      employeeOptions.value = allEmployees.value.slice(0, 50).map(emp => ({
        id: emp.id,
        name: emp.name,
        departmentName: emp.departmentName || ''
      }))
      
      console.log('加载员工成功，共', allEmployees.value.length, '人')
    } else {
      ElMessage.warning(res.msg || '加载员工列表失败')
    }
  } catch (error) {
    console.error('加载员工列表失败:', error)
    ElMessage.warning('加载员工列表失败，请检查后端服务')
  }
}

const searchEmployees = (query) => {
  employeeLoading.value = true
  try {
    if (query) {
      const keyword = query.toLowerCase()
      employeeOptions.value = allEmployees.value
        .filter(emp => 
          emp.name.toLowerCase().includes(keyword) ||
          (emp.departmentName && emp.departmentName.toLowerCase().includes(keyword))
        )
        .slice(0, 50)
        .map(emp => ({
          id: emp.id,
          name: emp.name,
          departmentName: emp.departmentName || ''
        }))
    } else {
      employeeOptions.value = allEmployees.value.slice(0, 50).map(emp => ({
        id: emp.id,
        name: emp.name,
        departmentName: emp.departmentName || ''
      }))
    }
  } finally {
    employeeLoading.value = false
  }
}

const getEmployeeName = (id) => {
  const emp = allEmployees.value.find(e => String(e.id) === String(id))
  return emp ? emp.name : id
}

const removeParticipant = (id) => {
  editForm.participantIds = editForm.participantIds.filter(item => String(item) !== String(id))
  updateParticipantsText()
}

const handleParticipantChange = () => {
  updateParticipantsText()
}

const updateParticipantsText = () => {
  const names = editForm.participantIds
    .map(id => getEmployeeName(id))
    .filter(Boolean)
  editForm.participants = names.join(',')
}

// ==================== 加载数据 ====================
const loadData = async () => {
  loading.value = true
  try {
    const params = {}
    if (searchForm.status) params.status = searchForm.status
    if (searchForm.keyword) params.keyword = searchForm.keyword
    
    const res = await getAllMeetings(params)
    if (res.code === 0) {
      allData.value = res.data || []
      
      let filtered = [...allData.value]
      if (searchForm.keyword) {
        const keyword = searchForm.keyword.toLowerCase()
        filtered = filtered.filter(item => 
          item.title?.toLowerCase().includes(keyword) ||
          item.organizerName?.toLowerCase().includes(keyword)
        )
      }
      
      const start = (currentPage.value - 1) * pageSize.value
      const end = start + pageSize.value
      tableData.value = filtered.slice(start, end)
      total.value = filtered.length
      
      updateStats(allData.value)
    } else {
      ElMessage.error(res.msg || '加载失败')
    }
  } catch (error) {
    console.error('加载会议失败:', error)
    ElMessage.error('加载失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

const updateStats = (data) => {
  const list = data || []
  stats[0].value = list.length
  stats[1].value = list.filter(item => item.status === '已预约').length
  stats[2].value = list.filter(item => item.status === '进行中').length
  stats[3].value = list.filter(item => item.status === '已结束').length
  stats[4].value = list.filter(item => item.status === '已取消').length
}

// ==================== 筛选 ====================
const filterByStatus = (status) => {
  if (status !== undefined) {
    searchForm.status = status
    currentPage.value = 1
    loadData()
  }
}

// ==================== 搜索 ====================
const handleSearch = () => {
  currentPage.value = 1
  loadData()
}

const resetSearch = () => {
  searchForm.keyword = ''
  searchForm.status = ''
  currentPage.value = 1
  loadData()
}

// ==================== 分页 ====================
const handlePageChange = () => {
  loadData()
}

// ==================== 查看详情 ====================
const viewDetail = (row) => {
  detailData.value = row
  detailVisible.value = true
}

// ==================== 取消会议 ====================
const handleCancel = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要取消会议「${row.title}」吗？\n此操作将通知所有参会人。`,
      '确认取消',
      { 
        confirmButtonText: '确定取消', 
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    const res = await cancelMeeting(row.id)
    if (res.code === 0) {
      ElMessage.success('会议已取消')
      loadData()
    } else {
      ElMessage.error(res.msg || '取消失败')
    }
  } catch {
    // 用户取消
  }
}

// ==================== 修改会议 ====================
const openEditDialog = async (row) => {
  if (allEmployees.value.length === 0) {
    await loadEmployees()
  }
  
  editForm.id = row.id
  editForm.title = row.title || ''
  editForm.roomName = row.roomName || ''
  editForm.startTime = row.startTime || ''
  editForm.endTime = row.endTime || ''
  editForm.remark = row.remark || ''
  
  if (row.participantIds) {
    editForm.participantIds = row.participantIds.split(',').filter(Boolean).map(id => String(id).trim())
  } else {
    editForm.participantIds = []
  }
  editForm.participants = row.participants || ''
  
  employeeOptions.value = allEmployees.value.slice(0, 50).map(emp => ({
    id: emp.id,
    name: emp.name,
    departmentName: emp.departmentName || ''
  }))
  
  editVisible.value = true
}

const resetEditForm = () => {
  editFormRef.value?.resetFields()
  editForm.participantIds = []
  editForm.participants = ''
}

const handleSubmitEdit = async () => {
  await editFormRef.value.validate(async (valid) => {
    if (!valid) return

    updateParticipantsText()
    
    if (editForm.participantIds.length === 0) {
      ElMessage.warning('请选择至少一名参会人')
      return
    }

    editLoading.value = true
    try {
      const data = {
        id: editForm.id,
        title: editForm.title,
        startTime: editForm.startTime,
        endTime: editForm.endTime,
        participants: editForm.participants,
        participantIds: editForm.participantIds.join(','),
        remark: editForm.remark
      }

      const res = await updateMeeting(data)
      if (res.code === 0) {
        ElMessage.success('修改成功，已通知参会人')
        editVisible.value = false
        loadData()
      } else {
        ElMessage.error(res.msg || '修改失败')
      }
    } catch (error) {
      console.error('修改失败:', error)
      ElMessage.error('修改失败，请稍后重试')
    } finally {
      editLoading.value = false
    }
  })
}

// ==================== 生命周期 ====================
onMounted(() => {
  loadData()
  loadEmployees()
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

.header-actions {
  display: flex;
  gap: 10px;
}

.search-card {
  margin-bottom: 20px;
}

.search-card :deep(.el-form-item) {
  margin-bottom: 0;
}

.stat-item {
  text-align: center;
  cursor: pointer;
  transition: transform 0.2s;
}

.stat-item:hover {
  transform: scale(1.05);
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

:deep(.el-table) {
  font-size: 14px;
}

.action-buttons {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 4px;
}

.action-buttons .el-button {
  margin: 0;
  padding: 5px 10px;
  font-size: 12px;
}

.action-buttons .el-button .el-icon {
  font-size: 14px;
  margin-right: 2px;
}

:deep(.el-table .cell) {
  padding: 6px 8px;
}

/* 响应式 */
@media (max-width: 768px) {
  .page-container {
    padding: 10px;
  }
  
  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }
  
  .header-actions {
    width: 100%;
    flex-wrap: wrap;
  }
  
  .search-card :deep(.el-form-item) {
    display: block;
    margin-bottom: 10px;
  }
  
  .search-card :deep(.el-form-item__content) {
    margin-left: 0 !important;
  }
  
  .search-card :deep(.el-input) {
    width: 100% !important;
  }
  
  .search-card :deep(.el-select) {
    width: 100% !important;
  }
}
</style>