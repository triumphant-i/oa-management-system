<template>
  <div class="page-container">
    <div class="page-header">
      <h2>会议室管理</h2>
      <div class="header-actions">
        <el-button 
          v-if="hasPermission(PERMISSIONS.MEETING_MANAGE)" 
          type="primary" 
          @click="openAddDialog"
        >
          <el-icon><Plus /></el-icon> 添加会议室
        </el-button>
        <el-button @click="loadData">
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

    <!-- 搜索栏 -->
    <el-card class="search-card">
      <el-form :inline="true" :model="searchForm" @keyup.enter="handleSearch">
        <el-form-item label="会议室名称">
          <el-input 
            v-model="searchForm.name" 
            placeholder="请输入会议室名称" 
            clearable 
            style="width:200px;"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select 
            v-model="searchForm.status" 
            placeholder="请选择状态" 
            clearable 
            @change="handleSearch"
            style="width:140px;"
          >
            <el-option label="全部" value="" />
            <el-option label="可用" value="可用" />
            <el-option label="使用中" value="使用中" />
            <el-option label="维护中" value="维护中" />
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

    <!-- 会议室表格 -->
    <el-card>
      <el-table :data="tableData" style="width:100%;" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="会议室名称" min-width="150" />
        <el-table-column prop="location" label="位置" width="150" />
        <el-table-column prop="capacity" label="容纳人数" width="100" />
        <el-table-column label="设备" min-width="150">
          <template #default="{ row }">
            <span>{{ getEquipmentDisplay(row.equipment) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ row.status || '可用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="150" />
        <el-table-column label="操作" min-width="320" fixed="right">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button size="small" type="info" plain @click="viewRoomDetail(row)">
                <el-icon><Search /></el-icon> 详情
              </el-button>
              
              <template v-if="hasPermission(PERMISSIONS.MEETING_MANAGE)">
                <el-button size="small" type="primary" plain @click="openEditDialog(row)">
                  <el-icon><Edit /></el-icon> 编辑
                </el-button>
                <el-button 
                  size="small" 
                  :type="row.status === '可用' ? 'warning' : 'success'" 
                  plain 
                  @click="toggleRoomStatus(row)"
                >
                  <el-icon><Switch /></el-icon>
                  {{ row.status === '可用' ? '维护' : '启用' }}
                </el-button>
                <el-button size="small" type="danger" plain @click="handleDelete(row)">
                  <el-icon><Delete /></el-icon>
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
          @size-change="loadData"
          @current-change="loadData"
        />
      </div>
    </el-card>

    <!-- ===== 添加/编辑会议室对话框 ===== -->
    <el-dialog 
      v-model="dialogVisible" 
      :title="dialogType === 'add' ? '添加会议室' : '编辑会议室'" 
      width="550px" 
      @close="resetForm"
    >
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="会议室名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入会议室名称" />
        </el-form-item>
        <el-form-item label="位置" prop="location">
          <el-input v-model="form.location" placeholder="请输入位置，如：A栋3楼" />
        </el-form-item>
        <el-form-item label="容纳人数" prop="capacity">
          <el-input-number v-model="form.capacity" :min="1" :max="200" style="width:100%;" />
        </el-form-item>
        <el-form-item label="设备">
          <el-checkbox-group v-model="form.equipment">
            <el-checkbox label="投影仪" />
            <el-checkbox label="视频会议" />
            <el-checkbox label="白板" />
            <el-checkbox label="音响" />
            <el-checkbox label="空调" />
          </el-checkbox-group>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" placeholder="请选择状态" style="width:100%;">
            <el-option label="可用" value="可用" />
            <el-option label="使用中" value="使用中" />
            <el-option label="维护中" value="维护中" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" rows="2" placeholder="请输入备注信息" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitLoading">
          {{ dialogType === 'add' ? '确认添加' : '保存修改' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- ===== 会议室详情对话框 ===== -->
    <el-dialog v-model="detailVisible" title="会议室详情" width="550px">
      <el-descriptions v-if="detailData" :column="2" border>
        <el-descriptions-item label="名称">{{ detailData.name }}</el-descriptions-item>
        <el-descriptions-item label="位置">{{ detailData.location || '未设置' }}</el-descriptions-item>
        <el-descriptions-item label="容纳人数">{{ detailData.capacity }}人</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getStatusType(detailData.status)">{{ detailData.status || '可用' }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="设备" :span="2">
          {{ getEquipmentDisplay(detailData.equipment) }}
        </el-descriptions-item>
        <el-descriptions-item label="描述" :span="2">
          {{ detailData.description || '无' }}
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Edit, Search, Switch, Delete } from '@element-plus/icons-vue'
import { hasPermission, PERMISSIONS } from '@/utils/permission'

import {
  getMeetingRoomPage,
  getMeetingRoomById,
  saveMeetingRoom,
  updateMeetingRoom,
  deleteMeetingRoom,
  updateRoomStatus
} from '@/api/meetingRoom'

// ==================== 数据 ====================
const loading = ref(false)
const submitLoading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const tableData = ref([])
const allData = ref([])  // 存储所有数据用于前端过滤

// ==================== 搜索表单 ====================
const searchForm = reactive({
  name: '',
  status: ''
})

// ==================== 统计 ====================
const stats = ref([
  { title: '会议室总数', value: 0, color: '#409eff' },
  { title: '可用', value: 0, color: '#67c23a' },
  { title: '使用中', value: 0, color: '#f56c6c' },
  { title: '维护中', value: 0, color: '#e6a23c' }
])

// ==================== 对话框 ====================
const dialogVisible = ref(false)
const dialogType = ref('add')
const formRef = ref()
const form = reactive({
  id: null,
  name: '',
  location: '',
  capacity: 10,
  equipment: [],
  status: '可用',
  description: ''
})

const rules = {
  name: [{ required: true, message: '请输入会议室名称' }],
  location: [{ required: true, message: '请输入位置' }],
  capacity: [{ required: true, message: '请输入容纳人数' }]
}

// ==================== 详情 ====================
const detailVisible = ref(false)
const detailData = ref(null)

// ==================== 工具方法 ====================
const getStatusType = (status) => {
  const map = {
    '可用': 'success',
    '使用中': 'danger',
    '维护中': 'warning'
  }
  return map[status] || 'info'
}

const getEquipmentDisplay = (equipment) => {
  if (!equipment) return '无设备'
  if (Array.isArray(equipment)) {
    return equipment.length > 0 ? equipment.join('、') : '无设备'
  }
  if (typeof equipment === 'string') {
    try {
      const parsed = JSON.parse(equipment)
      if (Array.isArray(parsed)) {
        return parsed.length > 0 ? parsed.join('、') : '无设备'
      }
      return equipment
    } catch {
      const arr = equipment.split(',').filter(Boolean).map(s => s.trim())
      return arr.length > 0 ? arr.join('、') : '无设备'
    }
  }
  return String(equipment)
}

// ==================== 前端过滤 ====================
const filterRooms = (list) => {
  let result = [...list]
  
  if (searchForm.name && searchForm.name.trim()) {
    const keyword = searchForm.name.trim().toLowerCase()
    result = result.filter(item => 
      item.name.toLowerCase().includes(keyword)
    )
  }
  
  if (searchForm.status) {
    result = result.filter(item => item.status === searchForm.status)
  }
  
  return result
}

// ==================== 加载数据 ====================
const loadData = async () => {
  loading.value = true
  try {
    const res = await getMeetingRoomPage(currentPage.value, pageSize.value)
    if (res.code === 0) {
      const list = (res.data?.data || []).map(item => {
        if (item.equipment && typeof item.equipment === 'string') {
          try {
            item.equipment = JSON.parse(item.equipment)
          } catch {
            item.equipment = item.equipment.split(',').filter(Boolean).map(s => s.trim())
          }
        }
        return item
      })
      
      allData.value = list
      
      // 前端过滤
      const filtered = filterRooms(list)
      
      // 前端分页
      const start = (currentPage.value - 1) * pageSize.value
      const end = start + pageSize.value
      tableData.value = filtered.slice(start, end)
      total.value = filtered.length
      
      updateStats(filtered)
    }
  } catch (error) {
    console.error('加载会议室失败:', error)
    ElMessage.error('加载会议室失败')
  } finally {
    loading.value = false
  }
}

const updateStats = (data) => {
  const list = data || tableData.value
  const available = list.filter(r => r.status === '可用').length
  const occupied = list.filter(r => r.status === '使用中').length
  const maintenance = list.filter(r => r.status === '维护中').length

  stats.value[0].value = list.length
  stats.value[1].value = available
  stats.value[2].value = occupied
  stats.value[3].value = maintenance
}

// ==================== 搜索 ====================
const handleSearch = () => {
  currentPage.value = 1
  loadData()
}

const resetSearch = () => {
  searchForm.name = ''
  searchForm.status = ''
  currentPage.value = 1
  loadData()
}

// ==================== 添加/编辑会议室 ====================
const openAddDialog = () => {
  dialogType.value = 'add'
  form.id = null
  form.name = ''
  form.location = ''
  form.capacity = 10
  form.equipment = []
  form.status = '可用'
  form.description = ''
  dialogVisible.value = true
}

const openEditDialog = async (row) => {
  dialogType.value = 'edit'
  try {
    const res = await getMeetingRoomById(row.id)
    if (res.code === 0) {
      const data = res.data
      form.id = data.id
      form.name = data.name
      form.location = data.location || ''
      form.capacity = data.capacity || 10
      let equipment = []
      if (data.equipment) {
        if (typeof data.equipment === 'string') {
          try {
            equipment = JSON.parse(data.equipment)
          } catch {
            equipment = data.equipment.split(',').filter(Boolean).map(s => s.trim())
          }
        } else if (Array.isArray(data.equipment)) {
          equipment = data.equipment
        }
      }
      form.equipment = equipment
      form.status = data.status || '可用'
      form.description = data.description || ''
      dialogVisible.value = true
    } else {
      ElMessage.error(res.msg || '获取详情失败')
    }
  } catch (error) {
    console.error('获取会议室详情失败:', error)
    ElMessage.error('获取详情失败')
  }
}

const resetForm = () => {
  formRef.value?.resetFields()
}

const handleSubmit = async () => {
  await formRef.value.validate(async (valid) => {
    if (!valid) return

    submitLoading.value = true
    try {
      const submitData = {
        ...form,
        equipment: JSON.stringify(form.equipment)
      }

      let res
      if (dialogType.value === 'add') {
        res = await saveMeetingRoom(submitData)
      } else {
        res = await updateMeetingRoom(submitData)
      }

      if (res.code === 0) {
        ElMessage.success(dialogType.value === 'add' ? '添加成功' : '更新成功')
        dialogVisible.value = false
        loadData()
      } else {
        ElMessage.error(res.msg || '操作失败')
      }
    } catch (error) {
      console.error('提交失败:', error)
      ElMessage.error('操作失败，请稍后重试')
    } finally {
      submitLoading.value = false
    }
  })
}

// ==================== 切换状态 ====================
const toggleRoomStatus = async (row) => {
  const newStatus = row.status === '可用' ? '维护中' : '可用'
  const action = row.status === '可用' ? '维护' : '启用'

  try {
    await ElMessageBox.confirm(
      `确定将「${row.name}」设为「${newStatus}」状态吗？`,
      '提示',
      { confirmButtonText: '确定', cancelButtonText: '取消' }
    )

    const res = await updateRoomStatus(row.id, newStatus)
    if (res.code === 0) {
      ElMessage.success(`已${action}`)
      loadData()
    } else {
      ElMessage.error(res.msg || '操作失败')
    }
  } catch {
    // 用户取消
  }
}

// ==================== 删除 ====================
const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除「${row.name}」吗？此操作不可恢复！`,
      '警告',
      { confirmButtonText: '确定删除', cancelButtonText: '取消', type: 'warning' }
    )

    const res = await deleteMeetingRoom(row.id)
    if (res.code === 0) {
      ElMessage.success('删除成功')
      loadData()
    } else {
      ElMessage.error(res.msg || '删除失败')
    }
  } catch {
    // 用户取消
  }
}

// ==================== 查看详情 ====================
const viewRoomDetail = async (row) => {
  try {
    const res = await getMeetingRoomById(row.id)
    if (res.code === 0) {
      detailData.value = res.data
      detailVisible.value = true
    } else {
      ElMessage.error(res.msg || '获取详情失败')
    }
  } catch (error) {
    console.error('获取详情失败:', error)
    ElMessage.error('获取详情失败')
  }
}

// ==================== 生命周期 ====================
onMounted(() => {
  loadData()
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

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

:deep(.el-table) {
  font-size: 14px;
}

/* ===== 操作按钮样式优化 ===== */
.action-buttons {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 4px;
}

.action-buttons .el-button {
  margin: 0;
  padding: 6px 10px;
  font-size: 12px;
}

.action-buttons .el-button .el-icon {
  font-size: 14px;
  margin-right: 2px;
}

:deep(.el-table .cell) {
  padding: 8px 6px;
}
</style>