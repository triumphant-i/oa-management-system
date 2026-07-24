<template>
  <div class="page-container">
    <div class="page-header">
      <h2>员工管理</h2>
      <div class="header-actions">
        <el-button type="primary" @click="openAddDialog">
          <el-icon><Plus /></el-icon> 添加员工
        </el-button>
        <el-button type="success" @click="openImportDialog">
          <el-icon><Upload /></el-icon> 一键导入
        </el-button>
        <el-button type="warning" plain @click="handleBatchTransfer" :disabled="selectedIds.length === 0">
          <el-icon><Right /></el-icon> 批量调岗
        </el-button>
        <el-button type="danger" plain @click="handleBatchOffline" :disabled="selectedIds.length === 0">
          <el-icon><Remove /></el-icon> 批量离职
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
      <el-form :inline="true" :model="searchForm" @submit.prevent="handleSearch">
        <el-form-item label="关键词">
          <el-input 
            v-model="searchForm.key" 
            placeholder="姓名/用户名" 
            clearable 
            style="width:180px;"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="部门">
          <el-select v-model="searchForm.departmentId" placeholder="全部部门" clearable style="width:150px;" @change="handleSearch">
            <el-option label="全部" :value="null" />
            <el-option 
              v-for="dept in departmentOptions" 
              :key="dept.id" 
              :label="dept.name" 
              :value="dept.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="全部状态" clearable style="width:120px;" @change="handleSearch">
            <el-option label="全部" :value="null" />
            <el-option label="在职" value="在职" />
            <el-option label="离职" value="离职" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 员工表格 -->
    <el-card>
      <el-table 
        :data="tableData" 
        style="width:100%;" 
        v-loading="loading" 
        stripe
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column prop="avatar" label="头像" width="60">
          <template #default="{ row }">
            <el-avatar 
              :size="40" 
              :src="row.avatar ? 'http://localhost:8080' + row.avatar : ''"
            >
              {{ row.name?.charAt(0) || '员' }}
            </el-avatar>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="姓名" width="100" />
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="departmentName" label="部门" width="150" />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column prop="email" label="邮箱" min-width="160" />
        <el-table-column prop="role" label="角色" width="120">
          <template #default="{ row }">
            <el-tag :type="getRoleType(row.role)">
              {{ getRoleLabel(row.role) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === '在职' ? 'success' : 'danger'">
              {{ row.status || '在职' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" plain @click="openEditDialog(row)">
              <el-icon><Edit /></el-icon> 编辑
            </el-button>
            <el-button size="small" type="warning" plain @click="openRoleDialog(row)">
              <el-icon><User /></el-icon> 角色
            </el-button>
            <el-button 
              size="small" 
              :type="row.status === '在职' ? 'danger' : 'success'" 
              plain 
              @click="toggleStatus(row)"
            >
              <el-icon><Switch /></el-icon>
              {{ row.status === '在职' ? '离职' : '复职' }}
            </el-button>
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

    <!-- ===== 添加/编辑员工对话框 ===== -->
    <el-dialog 
      v-model="dialogVisible" 
      :title="dialogType === 'add' ? '添加员工' : '编辑员工'" 
      width="550px" 
      @close="resetForm"
    >
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" :disabled="dialogType === 'edit'" />
        </el-form-item>
        <el-form-item label="姓名" prop="name">
          <el-input v-model="form.name" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item v-if="dialogType === 'add'" label="密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password />
        </el-form-item>
        <el-form-item label="部门" prop="departmentId">
          <el-select v-model="form.departmentId" placeholder="请选择部门" style="width:100%;">
            <el-option 
              v-for="dept in departmentOptions" 
              :key="dept.id" 
              :label="dept.name" 
              :value="dept.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="form.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="form.email" placeholder="请输入邮箱" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitLoading">
          {{ dialogType === 'add' ? '确认添加' : '保存修改' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- ===== 修改角色对话框 ===== -->
    <el-dialog v-model="roleDialogVisible" title="修改员工角色" width="450px">
      <el-form label-width="100px">
        <el-form-item label="员工">
          <span>{{ roleForm.employeeName }}</span>
        </el-form-item>
        <el-form-item label="当前角色">
          <el-tag :type="getRoleType(roleForm.currentRole)">
            {{ getRoleLabel(roleForm.currentRole) }}
          </el-tag>
        </el-form-item>
        <el-form-item label="新角色">
          <el-select v-model="roleForm.newRole" placeholder="请选择新角色" style="width:100%;">
            <el-option label="系统管理员" value="SYSTEM_ADMIN" />
            <el-option label="部门主管" value="DEPARTMENT_MANAGER" />
            <el-option label="流程管理员" value="PROCESS_ADMIN" />
            <el-option label="普通员工" value="EMPLOYEE" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="roleDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleRoleUpdate" :loading="roleLoading">确认修改</el-button>
      </template>
    </el-dialog>

    <!-- ===== 一键导入对话框 ===== -->
    <el-dialog v-model="importDialogVisible" title="一键导入员工" width="550px">
      <div class="import-tips">
        <el-alert
          title="导入说明"
          type="info"
          :closable="false"
          show-icon
        >
          <template #default>
            <ul style="margin:8px 0;padding-left:20px;line-height:1.8;">
              <li>支持 <strong>.xlsx</strong> 和 <strong>.xls</strong> 格式</li>
              <li>必填列：<strong>姓名</strong>、<strong>用户名</strong>、<strong>密码</strong>、<strong>部门</strong></li>
              <li>可选列：手机号、邮箱、角色</li>
              <li>角色默认：普通员工；状态默认：在职</li>
              <li>文件大小不超过 5MB</li>
            </ul>
          </template>
        </el-alert>
      </div>

      <div class="import-upload-area" @dragover.prevent @drop.prevent="handleDrop">
        <el-upload
          ref="uploadRef"
          drag
          :auto-upload="false"
          :on-change="handleFileChange"
          :on-remove="handleRemove"
          :limit="1"
          :on-exceed="handleExceed"
          accept=".xlsx,.xls"
          style="width:100%;"
        >
          <el-icon class="upload-icon"><Upload /></el-icon>
          <div class="upload-text">
            将 Excel 文件拖到此处，或 <em>点击选择文件</em>
          </div>
          <div class="upload-hint">支持 .xlsx / .xls 格式</div>
        </el-upload>
      </div>

      <!-- 导入预览 -->
      <div v-if="importPreview.length > 0" style="margin-top:16px;">
        <el-divider>导入预览（共 {{ importPreview.length }} 条）</el-divider>
        <el-table :data="importPreview" border max-height="250" size="small">
          <el-table-column prop="name" label="姓名" width="100" />
          <el-table-column prop="username" label="用户名" width="120" />
          <el-table-column prop="departmentName" label="部门" width="120" />
          <el-table-column prop="phone" label="手机号" width="120" />
          <el-table-column prop="email" label="邮箱" min-width="150" />
          <el-table-column prop="role" label="角色" width="100">
            <template #default="{ row }">
              <el-tag size="small" :type="getRoleType(row.role)">
                {{ getRoleLabel(row.role) }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
        <div style="margin-top:8px;color:#999;font-size:13px;">
          共 {{ importPreview.length }} 条数据，将全部导入
        </div>
      </div>

      <template #footer>
        <el-button @click="importDialogVisible = false">取消</el-button>
        <el-button 
          type="primary" 
          @click="handleImport" 
          :loading="importLoading"
          :disabled="importPreview.length === 0"
        >
          确认导入（{{ importPreview.length }} 条）
        </el-button>
      </template>
    </el-dialog>

    <!-- ===== 批量调岗对话框 ===== -->
    <el-dialog v-model="transferDialogVisible" title="批量调岗" width="500px">
      <div class="transfer-tips">
        <el-alert
          title="调岗说明"
          type="info"
          :closable="false"
          show-icon
        >
          <template #default>
            <ul style="margin:8px 0;padding-left:20px;line-height:1.8;">
              <li>本次将调岗 <strong>{{ selectedIds.length }}</strong> 名员工</li>
              <li>已离职的员工不会被调岗</li>
              <li>请选择目标部门</li>
            </ul>
          </template>
        </el-alert>
      </div>

      <el-form label-width="100px" style="margin-top:16px;">
        <el-form-item label="目标部门">
          <el-select 
            v-model="transferTargetDeptId" 
            placeholder="请选择目标部门" 
            style="width:100%;"
            filterable
          >
            <el-option 
              v-for="dept in departmentOptions" 
              :key="dept.id" 
              :label="dept.name" 
              :value="dept.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="调岗原因">
          <el-input 
            v-model="transferReason" 
            type="textarea" 
            rows="2" 
            placeholder="请输入调岗原因（选填）"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="transferDialogVisible = false">取消</el-button>
        <el-button 
          type="primary" 
          @click="confirmBatchTransfer" 
          :loading="transferLoading"
          :disabled="!transferTargetDeptId"
        >
          确认调岗（{{ selectedIds.length }} 人）
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Edit, User, Switch, Upload, Remove, Right } from '@element-plus/icons-vue'
import * as XLSX from 'xlsx'
import {
  getEmployeePage,
  getEmployeeById,
  saveEmployee,
  updateEmployee,
  updateEmployeeStatus,
  updateEmployeeRole,
  searchEmployee
} from '@/api/employee'
import { getDepartmentList } from '@/api/department'

// ==================== 数据 ====================
const loading = ref(false)
const submitLoading = ref(false)
const roleLoading = ref(false)
const importLoading = ref(false)
const importDialogVisible = ref(false)
const importFile = ref(null)
const importPreview = ref([])

// 选中的员工ID列表
const selectedIds = ref([])

const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const tableData = ref([])
const departmentOptions = ref([])

// ==================== 批量调岗 ====================
const transferDialogVisible = ref(false)
const transferLoading = ref(false)
const transferTargetDeptId = ref(null)
const transferReason = ref('')

// ==================== 统计 ====================
const stats = ref([
  { title: '员工总数', value: 0, color: '#409eff' },
  { title: '在职', value: 0, color: '#67c23a' },
  { title: '离职', value: 0, color: '#f56c6c' }
])

// ==================== 搜索 ====================
const searchForm = reactive({
  key: '',
  departmentId: null,
  status: null
})

// ==================== 对话框 ====================
const dialogVisible = ref(false)
const dialogType = ref('add')
const formRef = ref()
const form = reactive({
  id: null,
  username: '',
  name: '',
  password: '',
  departmentId: null,
  phone: '',
  email: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名' }],
  name: [{ required: true, message: '请输入姓名' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  departmentId: [{ required: true, message: '请选择部门' }]
}

// ==================== 角色对话框 ====================
const roleDialogVisible = ref(false)
const roleForm = reactive({
  employeeId: null,
  employeeName: '',
  currentRole: '',
  newRole: ''
})

// ==================== 工具方法 ====================
const getRoleLabel = (role) => {
  const map = {
    'SYSTEM_ADMIN': '系统管理员',
    'DEPARTMENT_MANAGER': '部门主管',
    'PROCESS_ADMIN': '流程管理员',
    'EMPLOYEE': '普通员工'
  }
  return map[role] || role || '未知'
}

const getRoleType = (role) => {
  const map = {
    'SYSTEM_ADMIN': 'danger',
    'DEPARTMENT_MANAGER': 'warning',
    'PROCESS_ADMIN': 'info',
    'EMPLOYEE': 'success'
  }
  return map[role] || 'info'
}

// ==================== 填充部门名称 ====================
const fillDepartmentName = (list) => {
  if (!list || list.length === 0) return list
  return list.map(item => {
    if (item.departmentId && departmentOptions.value.length > 0) {
      const dept = departmentOptions.value.find(d => d.id === item.departmentId)
      if (dept) {
        item.departmentName = dept.name
      }
    }
    return item
  })
}

// ==================== 加载数据 ====================
const loadData = async () => {
  loading.value = true
  try {
    const res = await getEmployeePage(currentPage.value, pageSize.value, searchForm.departmentId)
    if (res.code === 0) {
      let list = []
      if (res.data && res.data.data) {
        list = res.data.data || []
      } else if (Array.isArray(res.data)) {
        list = res.data
      }
      
      // 前端过滤状态
      if (searchForm.status) {
        list = list.filter(item => item.status === searchForm.status)
      }
      
      list = fillDepartmentName(list)
      
      tableData.value = list
      total.value = res.data?.total || list.length
      updateStats()
      selectedIds.value = []
    } else {
      ElMessage.error(res.msg || '加载失败')
    }
  } catch (error) {
    console.error('加载员工数据失败:', error)
    ElMessage.error('加载员工数据失败')
  } finally {
    loading.value = false
  }
}

const loadDepartmentOptions = async () => {
  try {
    const res = await getDepartmentList()
    if (res.code === 0) {
      departmentOptions.value = res.data || []
    }
  } catch (error) {
    console.error('加载部门选项失败:', error)
  }
}

const updateStats = () => {
  const active = tableData.value.filter(item => item.status === '在职').length
  const inactive = tableData.value.filter(item => item.status === '离职').length
  stats.value[0].value = total.value
  stats.value[1].value = active
  stats.value[2].value = inactive
}

// ==================== 搜索 ====================
const handleSearch = async () => {
  // 如果有关键词，使用搜索接口
  if (searchForm.key && searchForm.key.trim()) {
    loading.value = true
    try {
      const res = await searchEmployee('name', searchForm.key.trim(), 1, 100)
      if (res.code === 0) {
        let list = []
        if (res.data && res.data.data) {
          list = res.data.data || []
        } else if (Array.isArray(res.data)) {
          list = res.data
        }
        
        // 前端过滤部门和状态
        if (searchForm.departmentId) {
          list = list.filter(item => item.departmentId === searchForm.departmentId)
        }
        if (searchForm.status) {
          list = list.filter(item => item.status === searchForm.status)
        }
        
        list = fillDepartmentName(list)
        
        tableData.value = list
        total.value = list.length
        updateStats()
      }
    } catch (error) {
      console.error('搜索失败:', error)
      ElMessage.error('搜索失败')
    } finally {
      loading.value = false
    }
  } else {
    // 没有关键词，走普通分页查询
    currentPage.value = 1
    await loadData()
  }
}

const resetSearch = () => {
  searchForm.key = ''
  searchForm.departmentId = null
  searchForm.status = null
  currentPage.value = 1
  loadData()
}

// ==================== 表格选中 ====================
const handleSelectionChange = (selection) => {
  selectedIds.value = selection.map(item => item.id)
}

// ==================== 添加/编辑 ====================
const openAddDialog = () => {
  dialogType.value = 'add'
  form.id = null
  form.username = ''
  form.name = ''
  form.password = ''
  form.departmentId = null
  form.phone = ''
  form.email = ''
  dialogVisible.value = true
}

const openEditDialog = async (row) => {
  dialogType.value = 'edit'
  try {
    const res = await getEmployeeById(row.id)
    if (res.code === 0) {
      const data = res.data
      form.id = data.id
      form.username = data.username
      form.name = data.name
      form.password = ''
      form.departmentId = data.departmentId
      form.phone = data.phone || ''
      form.email = data.email || ''
      dialogVisible.value = true
    }
  } catch (error) {
    console.error('获取员工信息失败:', error)
    ElMessage.error('获取员工信息失败')
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
      let res
      if (dialogType.value === 'add') {
        res = await saveEmployee(form)
      } else {
        res = await updateEmployee(form)
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
const toggleStatus = async (row) => {
  const newStatus = row.status === '在职' ? '离职' : '在职'
  try {
    await ElMessageBox.confirm(
      `确定将「${row.name}」状态改为「${newStatus}」吗？`,
      '提示',
      { confirmButtonText: '确定', cancelButtonText: '取消' }
    )
    
    const res = await updateEmployeeStatus({
      employeeId: row.id,
      status: newStatus
    })
    
    if (res.code === 0) {
      ElMessage.success(`状态已更新为「${newStatus}」`)
      loadData()
    } else {
      ElMessage.error(res.msg || '操作失败')
    }
  } catch {
    // 用户取消
  }
}

// ==================== 批量调岗 ====================
const handleBatchTransfer = () => {
  if (selectedIds.value.length === 0) {
    ElMessage.warning('请先选择要调岗的员工')
    return
  }

  const selectedEmployees = tableData.value.filter(item => selectedIds.value.includes(item.id))
  const alreadyOffline = selectedEmployees.filter(item => item.status === '离职')
  
  if (alreadyOffline.length > 0) {
    ElMessage.warning(`选中的员工中有 ${alreadyOffline.length} 人已离职，不能调岗，请重新选择`)
    return
  }

  transferTargetDeptId.value = null
  transferReason.value = ''
  transferDialogVisible.value = true
}

const confirmBatchTransfer = async () => {
  if (!transferTargetDeptId.value) {
    ElMessage.warning('请选择目标部门')
    return
  }

  const selectedEmployees = tableData.value.filter(item => selectedIds.value.includes(item.id))
  const targetDept = departmentOptions.value.find(d => d.id === transferTargetDeptId.value)
  
  const alreadyInTarget = selectedEmployees.filter(item => item.departmentId === transferTargetDeptId.value)
  if (alreadyInTarget.length > 0) {
    try {
      await ElMessageBox.confirm(
        `选中的员工中有 ${alreadyInTarget.length} 人已经在「${targetDept?.name || '目标部门'}」，是否继续调岗？`,
        '提示',
        { confirmButtonText: '继续', cancelButtonText: '取消', type: 'warning' }
      )
    } catch {
      return
    }
  }

  try {
    await ElMessageBox.confirm(
      `确定要将选中的 ${selectedIds.value.length} 名员工调入「${targetDept?.name || '目标部门'}」吗？`,
      '批量调岗确认',
      { 
        confirmButtonText: '确定调岗', 
        cancelButtonText: '取消', 
        type: 'info' 
      }
    )

    transferLoading.value = true
    let successCount = 0
    let failCount = 0

    for (const employee of selectedEmployees) {
      try {
        const res = await updateEmployee({
          id: employee.id,
          departmentId: transferTargetDeptId.value
        })
        if (res.code === 0) {
          successCount++
        } else {
          failCount++
          console.error('调岗失败:', res.msg, employee)
        }
      } catch (error) {
        failCount++
        console.error('调岗失败:', error, employee)
      }
    }

    ElMessage.success(`批量调岗完成：成功 ${successCount} 人，失败 ${failCount} 人`)
    transferDialogVisible.value = false
    loadData()
  } catch {
    // 用户取消
  } finally {
    transferLoading.value = false
  }
}

// ==================== 批量离职 ====================
const handleBatchOffline = async () => {
  if (selectedIds.value.length === 0) {
    ElMessage.warning('请先选择要离职的员工')
    return
  }

  const selectedEmployees = tableData.value.filter(item => selectedIds.value.includes(item.id))
  const alreadyOffline = selectedEmployees.filter(item => item.status === '离职')
  
  if (alreadyOffline.length > 0) {
    ElMessage.warning(`选中的员工中有 ${alreadyOffline.length} 人已离职，请重新选择`)
    return
  }

  try {
    await ElMessageBox.confirm(
      `确定要将选中的 ${selectedIds.value.length} 名员工全部设为「离职」状态吗？`,
      '批量离职确认',
      { 
        confirmButtonText: '确定离职', 
        cancelButtonText: '取消', 
        type: 'warning' 
      }
    )

    let successCount = 0
    let failCount = 0

    for (const id of selectedIds.value) {
      try {
        const res = await updateEmployeeStatus({
          employeeId: id,
          status: '离职'
        })
        if (res.code === 0) {
          successCount++
        } else {
          failCount++
          console.error('离职失败:', res.msg)
        }
      } catch (error) {
        failCount++
        console.error('离职失败:', error)
      }
    }

    ElMessage.success(`批量离职完成：成功 ${successCount} 人，失败 ${failCount} 人`)
    loadData()
  } catch {
    // 用户取消
  }
}

// ==================== 修改角色 ====================
const openRoleDialog = (row) => {
  roleForm.employeeId = row.id
  roleForm.employeeName = row.name
  roleForm.currentRole = row.role
  roleForm.newRole = row.role
  roleDialogVisible.value = true
}

const handleRoleUpdate = async () => {
  if (!roleForm.newRole) {
    ElMessage.warning('请选择新角色')
    return
  }
  
  if (roleForm.newRole === roleForm.currentRole) {
    ElMessage.warning('新角色与当前角色相同')
    return
  }
  
  roleLoading.value = true
  try {
    const res = await updateEmployeeRole({
      employeeId: roleForm.employeeId,
      role: roleForm.newRole
    })
    
    if (res.code === 0) {
      ElMessage.success('角色修改成功')
      roleDialogVisible.value = false
      loadData()
    } else {
      ElMessage.error(res.msg || '操作失败')
    }
  } catch (error) {
    console.error('修改角色失败:', error)
    ElMessage.error('操作失败，请稍后重试')
  } finally {
    roleLoading.value = false
  }
}

// ==================== 一键导入 ====================
const openImportDialog = () => {
  importFile.value = null
  importPreview.value = []
  importDialogVisible.value = true
}

const handleFileChange = (file) => {
  importFile.value = file
  parseExcel(file.raw)
}

const handleRemove = () => {
  importFile.value = null
  importPreview.value = []
}

const handleExceed = () => {
  ElMessage.warning('只支持上传一个文件，请先移除当前文件')
}

const handleDrop = (e) => {
  const files = e.dataTransfer.files
  if (files.length > 0) {
    const file = files[0]
    const validTypes = [
      'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
      'application/vnd.ms-excel'
    ]
    if (!validTypes.includes(file.type) && !file.name.match(/\.(xlsx|xls)$/)) {
      ElMessage.error('请上传 Excel 文件 (.xlsx / .xls)')
      return
    }
    importFile.value = file
    parseExcel(file)
  }
}

const parseExcel = (file) => {
  const reader = new FileReader()
  reader.onload = (e) => {
    try {
      const data = new Uint8Array(e.target.result)
      const workbook = XLSX.read(data, { type: 'array' })
      const firstSheet = workbook.Sheets[workbook.SheetNames[0]]
      const jsonData = XLSX.utils.sheet_to_json(firstSheet)

      if (!jsonData || jsonData.length === 0) {
        ElMessage.warning('Excel 文件为空或格式不正确')
        return
      }

      const mappedData = jsonData.map((row) => {
        const name = row['姓名'] || row['name'] || row['Name'] || ''
        const username = row['用户名'] || row['username'] || row['Username'] || ''
        const password = row['密码'] || row['password'] || row['Password'] || ''
        const departmentName = row['部门'] || row['department'] || row['Department'] || ''
        const phone = row['手机号'] || row['phone'] || row['Phone'] || ''
        const email = row['邮箱'] || row['email'] || row['Email'] || ''
        const role = row['角色'] || row['role'] || row['Role'] || 'EMPLOYEE'

        const dept = departmentOptions.value.find(d => d.name === departmentName)
        
        return {
          name: name || '',
          username: username || '',
          password: password || '123456',
          departmentId: dept ? dept.id : null,
          departmentName: departmentName || '',
          phone: phone || '',
          email: email || '',
          role: role || 'EMPLOYEE',
          status: '在职'
        }
      })

      const validData = mappedData.filter(item => 
        item.name && item.username && item.departmentId !== null
      )

      if (validData.length === 0) {
        ElMessage.error('没有有效数据，请检查必填列（姓名、用户名、部门）')
        return
      }

      if (validData.length < mappedData.length) {
        ElMessage.warning(`共 ${mappedData.length} 条数据，其中 ${mappedData.length - validData.length} 条无效（缺少必填字段）`)
      }

      importPreview.value = validData
      ElMessage.success(`成功解析 ${validData.length} 条数据`)
    } catch (error) {
      console.error('解析 Excel 失败:', error)
      ElMessage.error('解析 Excel 失败，请检查文件格式')
    }
  }
  reader.readAsArrayBuffer(file)
}

const handleImport = async () => {
  if (importPreview.value.length === 0) {
    ElMessage.warning('没有可导入的数据')
    return
  }

  try {
    await ElMessageBox.confirm(
      `确定要导入 ${importPreview.value.length} 名员工吗？`,
      '确认导入',
      { confirmButtonText: '确定导入', cancelButtonText: '取消', type: 'warning' }
    )

    importLoading.value = true
    let successCount = 0
    let failCount = 0

    for (const item of importPreview.value) {
      try {
        const res = await saveEmployee({
          username: item.username,
          name: item.name,
          password: item.password,
          departmentId: item.departmentId,
          phone: item.phone,
          email: item.email,
          role: item.role,
          status: item.status
        })
        if (res.code === 0) {
          successCount++
        } else {
          failCount++
          console.error('导入失败:', res.msg, item)
        }
      } catch (error) {
        failCount++
        console.error('导入失败:', error, item)
      }
    }

    ElMessage.success(`导入完成：成功 ${successCount} 条，失败 ${failCount} 条`)
    importDialogVisible.value = false
    importFile.value = null
    importPreview.value = []
    loadData()
  } catch {
    // 用户取消
  } finally {
    importLoading.value = false
  }
}

// ==================== 生命周期 ====================
onMounted(() => {
  loadDepartmentOptions()
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

.import-tips {
  margin-bottom: 16px;
}

.import-tips ul {
  margin: 4px 0;
  padding-left: 20px;
}

.import-upload-area {
  width: 100%;
}

.upload-icon {
  font-size: 48px;
  color: #409eff;
}

.upload-text {
  font-size: 14px;
  color: #606266;
}

.upload-text em {
  color: #409eff;
  font-style: normal;
  cursor: pointer;
}

.upload-hint {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.transfer-tips {
  margin-bottom: 8px;
}

.transfer-tips ul {
  margin: 4px 0;
  padding-left: 20px;
}

:deep(.el-upload-dragger) {
  padding: 30px 20px;
}
</style>