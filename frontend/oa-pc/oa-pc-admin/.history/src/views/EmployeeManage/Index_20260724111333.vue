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
        <el-button type="danger" plain @click="handleBatchDelete" :disabled="selectedIds.length === 0">
          <el-icon><Delete /></el-icon> 批量删除
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
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button size="small" type="primary" plain @click="openEditDialog(row)">
                <el-icon><Edit /></el-icon>
              </el-button>
              <el-button size="small" type="warning" plain @click="openRoleDialog(row)">
                <el-icon><User /></el-icon>
              </el-button>
              <el-button 
                size="small" 
                type="danger" 
                plain 
                @click="handleDelete(row)"
              >
                <el-icon><Delete /></el-icon>
              </el-button>
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
    <el-dialog 
      v-model="importDialogVisible" 
      title="一键导入员工" 
      width="750px"
      :close-on-click-modal="false"
      :close-on-press-escape="!importing"
    >
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

      <!-- 导入进度 -->
      <div v-if="importing" style="margin: 16px 0;">
        <el-progress 
          :percentage="importProgress" 
          :status="importProgressStatus"
          :text-inside="true"
          stroke-width="24"
        />
        <div style="margin-top:8px;font-size:13px;color:#666;display:flex;justify-content:space-between;">
          <span>正在导入：{{ importSuccessCount }} 成功，{{ importFailCount }} 失败</span>
          <span>共 {{ importTotalCount }} 条</span>
        </div>
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
          :disabled="importing"
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
        <el-divider>
          <span>导入预览</span>
          <el-tag size="small" type="info" style="margin-left:8px;">
            共 {{ importPreview.length }} 条
          </el-tag>
          <el-tag v-if="validCount > 0" size="small" type="success" style="margin-left:4px;">
            ✅ {{ validCount }} 有效
          </el-tag>
          <el-tag v-if="invalidCount > 0" size="small" type="danger" style="margin-left:4px;">
            ❌ {{ invalidCount }} 无效
          </el-tag>
        </el-divider>
        
        <el-table 
          :data="importPreview" 
          border 
          max-height="350" 
          size="small"
          row-class-name="import-preview-row"
        >
          <el-table-column type="index" label="#" width="50" />
          <el-table-column prop="_index" label="行号" width="60" />
          <el-table-column prop="name" label="姓名" width="90">
            <template #default="{ row }">
              <span :class="{ 'error-text': !row._valid }">
                {{ row.name || '(空)' }}
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="username" label="用户名" width="110">
            <template #default="{ row }">
              <span :class="{ 'error-text': !row._valid }">
                {{ row.username || '(空)' }}
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="departmentName" label="部门" width="110">
            <template #default="{ row }">
              <span :class="{ 'error-text': !row._valid && !row.departmentName }">
                {{ row.departmentName || '(空)' }}
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="phone" label="手机号" width="110" />
          <el-table-column prop="email" label="邮箱" min-width="120" />
          <el-table-column prop="role" label="角色" width="90">
            <template #default="{ row }">
              <el-tag size="small" :type="getRoleType(row.role)">
                {{ getRoleLabel(row.role) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="_status" label="状态" width="80">
            <template #default="{ row }">
              <el-tag 
                v-if="row._status === 'success'" 
                size="small" 
                type="success"
              >
                成功
              </el-tag>
              <el-tag 
                v-else-if="row._status === 'fail'" 
                size="small" 
                type="danger"
              >
                失败
              </el-tag>
              <el-tag 
                v-else-if="!row._valid" 
                size="small" 
                type="info"
              >
                无效
              </el-tag>
              <span v-else style="color:#999;">待导入</span>
            </template>
          </el-table-column>
          <el-table-column prop="_errorMsg" label="错误信息" min-width="150">
            <template #default="{ row }">
              <span v-if="row._errorMsg" style="color:#f56c6c;font-size:12px;white-space:pre-wrap;">
                {{ row._errorMsg }}
              </span>
              <span v-else-if="!row._valid" style="color:#f56c6c;font-size:12px;">
                数据无效，请检查必填字段
              </span>
              <span v-else style="color:#999;font-size:12px;">-</span>
            </template>
          </el-table-column>
        </el-table>
        
        <div style="margin-top:12px;display:flex;justify-content:space-between;align-items:center;flex-wrap:wrap;gap:8px;">
          <div style="display:flex;align-items:center;gap:12px;flex-wrap:wrap;">
            <span style="color:#999;font-size:13px;">
              共 {{ importPreview.length }} 条数据
            </span>
            <span v-if="importSuccessCount > 0" style="color:#67c23a;font-size:13px;">
              ✅ 成功 {{ importSuccessCount }}
            </span>
            <span v-if="importFailCount > 0" style="color:#f56c6c;font-size:13px;">
              ❌ 失败 {{ importFailCount }}
            </span>
            <span v-if="validCount > 0 && importSuccessCount === 0 && importFailCount === 0" style="color:#409eff;font-size:13px;">
              📋 待导入 {{ validCount }} 条
            </span>
          </div>
          <div style="display:flex;gap:8px;">
            <el-button 
              v-if="importFailCount > 0" 
              size="small" 
              type="warning" 
              plain
              @click="exportFailedRecords"
            >
              <el-icon><Download /></el-icon> 导出失败记录
            </el-button>
            <el-button 
              v-if="importSuccessCount > 0 && importFailCount === 0" 
              size="small" 
              type="success" 
              plain
              @click="closeImportDialog"
            >
              导入完成，关闭
            </el-button>
          </div>
        </div>
      </div>

      <template #footer>
        <el-button @click="closeImportDialog" :disabled="importing">取消</el-button>
        <el-button 
          type="primary" 
          @click="handleImport" 
          :loading="importing"
          :disabled="validCount === 0 || importing"
        >
          {{ importing ? '导入中...' : `确认导入（${validCount} 条）` }}
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
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Edit, User, Upload, Right, Delete, Download } from '@element-plus/icons-vue'
import * as XLSX from 'xlsx'
import {
  getEmployeePage,
  getEmployeeById,
  saveEmployee,
  updateEmployee,
  updateEmployeeRole,
  searchEmployee,
  deleteEmployee
} from '@/api/employee'
import { getDepartmentList } from '@/api/department'

// ==================== 数据 ====================
const loading = ref(false)
const submitLoading = ref(false)
const roleLoading = ref(false)

// 导入相关
const importDialogVisible = ref(false)
const importFile = ref(null)
const importPreview = ref([])
const importing = ref(false)
const importProgress = ref(0)
const importProgressStatus = ref('')
const importSuccessCount = ref(0)
const importFailCount = ref(0)
const importTotalCount = ref(0)

// 选中的员工ID列表
const selectedIds = ref([])

const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const tableData = ref([])
const departmentOptions = ref([])

// 批量调岗
const transferDialogVisible = ref(false)
const transferLoading = ref(false)
const transferTargetDeptId = ref(null)
const transferReason = ref('')

// 统计
const stats = ref([
  { title: '员工总数', value: 0, color: '#409eff' },
  { title: '在职', value: 0, color: '#67c23a' }
])

// 搜索
const searchForm = reactive({
  key: '',
  departmentId: null
})

// 对话框
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

// 角色对话框
const roleDialogVisible = ref(false)
const roleForm = reactive({
  employeeId: null,
  employeeName: '',
  currentRole: '',
  newRole: ''
})

// ==================== 计算属性 ====================
const validCount = computed(() => {
  return importPreview.value.filter(item => item._valid).length
})

const invalidCount = computed(() => {
  return importPreview.value.filter(item => !item._valid).length
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
  stats.value[0].value = total.value
  stats.value[1].value = active
}

// ==================== 搜索 ====================
const handleSearch = async () => {
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
        
        if (searchForm.departmentId) {
          list = list.filter(item => item.departmentId === searchForm.departmentId)
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
    currentPage.value = 1
    await loadData()
  }
}

const resetSearch = () => {
  searchForm.key = ''
  searchForm.departmentId = null
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

// ==================== 删除单个员工 ====================
const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要永久删除员工「${row.name}」吗？\n此操作不可恢复！`,
      '警告',
      { 
        confirmButtonText: '确定删除', 
        cancelButtonText: '取消', 
        type: 'warning' 
      }
    )
    
    const res = await deleteEmployee(row.id)
    if (res.code === 0) {
      ElMessage.success('员工已永久删除')
      loadData()
    } else {
      ElMessage.error(res.msg || '删除失败')
    }
  } catch {
    // 用户取消
  }
}

// ==================== 批量删除 ====================
const handleBatchDelete = async () => {
  if (selectedIds.value.length === 0) {
    ElMessage.warning('请先选择要删除的员工')
    return
  }

  try {
    await ElMessageBox.confirm(
      `确定要永久删除选中的 ${selectedIds.value.length} 名员工吗？\n此操作不可恢复！`,
      '警告',
      { 
        confirmButtonText: '确定删除', 
        cancelButtonText: '取消', 
        type: 'warning' 
      }
    )

    let successCount = 0
    let failCount = 0

    for (const id of selectedIds.value) {
      try {
        const res = await deleteEmployee(id)
        if (res.code === 0) {
          successCount++
        } else {
          failCount++
          console.error('删除失败:', res.msg)
        }
      } catch (error) {
        failCount++
        console.error('删除失败:', error)
      }
    }

    ElMessage.success(`批量删除完成：成功 ${successCount} 人，失败 ${failCount} 人`)
    loadData()
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
  importSuccessCount.value = 0
  importFailCount.value = 0
  importProgress.value = 0
  importProgressStatus.value = ''
  importDialogVisible.value = true
}

const closeImportDialog = () => {
  if (importing.value) {
    ElMessage.warning('导入进行中，请等待完成')
    return
  }
  importDialogVisible.value = false
  importFile.value = null
  importPreview.value = []
  importSuccessCount.value = 0
  importFailCount.value = 0
  importProgress.value = 0
  importProgressStatus.value = ''
}

const handleFileChange = (file) => {
  importFile.value = file
  parseExcel(file.raw)
}

const handleRemove = () => {
  importFile.value = null
  importPreview.value = []
  importSuccessCount.value = 0
  importFailCount.value = 0
  importProgress.value = 0
  importProgressStatus.value = ''
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

      const mappedData = jsonData.map((row, index) => {
        const name = row['姓名'] || row['name'] || row['Name'] || ''
        const username = row['用户名'] || row['username'] || row['Username'] || ''
        const password = row['密码'] || row['password'] || row['Password'] || '123456'
        const departmentName = row['部门'] || row['department'] || row['Department'] || ''
        const phone = row['手机号'] || row['phone'] || row['Phone'] || ''
        const email = row['邮箱'] || row['email'] || row['Email'] || ''
        const role = row['角色'] || row['role'] || row['Role'] || 'EMPLOYEE'

        const dept = departmentOptions.value.find(d => d.name === departmentName)
        
        // 验证数据有效性
        const errors = []
        if (!name) errors.push('姓名不能为空')
        if (!username) errors.push('用户名不能为空')
        if (dept === null && departmentName) errors.push(`部门"${departmentName}"不存在`)
        if (!departmentName) errors.push('部门不能为空')
        
        // 检查用户名是否已存在
        const existingUser = tableData.value.find(e => e.username === username)
        if (existingUser && username) {
          errors.push(`用户名"${username}"已存在`)
        }

        return {
          _index: index + 2, // Excel 行号（从2开始，因为第1行是表头）
          _errors: errors,
          _valid: errors.length === 0,
          _status: 'pending',
          _errorMsg: errors.join('；'),
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

      // 更新预览数据
      importPreview.value = mappedData
      
      const validCount_ = mappedData.filter(item => item._valid).length
      const invalidCount_ = mappedData.length - validCount_
      
      if (validCount_ === 0) {
        ElMessage.error(`没有有效数据，共 ${mappedData.length} 条数据全部无效`)
      } else if (invalidCount_ > 0) {
        ElMessage.warning(`共 ${mappedData.length} 条数据，其中 ${validCount_} 条有效，${invalidCount_} 条无效（请检查红色标记的行）`)
      } else {
        ElMessage.success(`成功解析 ${validCount_} 条数据`)
      }
    } catch (error) {
      console.error('解析 Excel 失败:', error)
      ElMessage.error('解析 Excel 失败，请检查文件格式')
    }
  }
  reader.readAsArrayBuffer(file)
}

const handleImport = async () => {
  const validData = importPreview.value.filter(item => item._valid)
  
  if (validData.length === 0) {
    ElMessage.warning('没有可导入的有效数据')
    return
  }

  try {
    await ElMessageBox.confirm(
      `确定要导入 ${validData.length} 名员工吗？\n${importPreview.value.length - validData.length} 条无效数据将被跳过。`,
      '确认导入',
      { 
        confirmButtonText: '确定导入', 
        cancelButtonText: '取消', 
        type: 'warning' 
      }
    )

    importing.value = true
    importProgress.value = 0
    importProgressStatus.value = ''
    importSuccessCount.value = 0
    importFailCount.value = 0
    importTotalCount.value = validData.length

    // 重置状态
    importPreview.value.forEach(item => {
      if (item._valid) {
        item._status = 'pending'
        item._errorMsg = ''
      }
    })

    // 逐条导入
    for (let i = 0; i < validData.length; i++) {
      const item = validData[i]
      
      // 更新进度
      importProgress.value = Math.round(((i + 1) / validData.length) * 100)
      
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
          importSuccessCount.value++
          item._status = 'success'
          item._errorMsg = ''
        } else {
          importFailCount.value++
          item._status = 'fail'
          item._errorMsg = res.msg || '导入失败'
          console.error(`第 ${item._index} 行导入失败:`, res.msg)
        }
      } catch (error) {
        importFailCount.value++
        item._status = 'fail'
        item._errorMsg = error.message || '网络错误'
        console.error(`第 ${item._index} 行导入失败:`, error)
      }
    }

    // 更新进度状态
    if (importFailCount.value === 0) {
      importProgressStatus.value = 'success'
      ElMessage.success(`🎉 全部导入成功！共 ${importSuccessCount.value} 条`)
    } else if (importSuccessCount.value === 0) {
      importProgressStatus.value = 'exception'
      ElMessage.error(`❌ 全部导入失败！共 ${importFailCount.value} 条`)
    } else {
      importProgressStatus.value = 'warning'
      ElMessage.warning(`⚠️ 导入完成：成功 ${importSuccessCount.value} 条，失败 ${importFailCount.value} 条`)
    }

    // 如果全部成功，自动关闭对话框并刷新列表
    if (importFailCount.value === 0) {
      setTimeout(() => {
        closeImportDialog()
        loadData()
      }, 1500)
    }

  } catch {
    // 用户取消
  } finally {
    importing.value = false
  }
}

/**
 * 导出失败记录为 Excel
 */
const exportFailedRecords = () => {
  const failedRecords = importPreview.value.filter(item => item._status === 'fail')
  
  if (failedRecords.length === 0) {
    ElMessage.info('没有失败记录')
    return
  }

  try {
    // 构建导出数据
    const exportData = failedRecords.map(item => ({
      '行号': item._index,
      '姓名': item.name,
      '用户名': item.username,
      '部门': item.departmentName,
      '手机号': item.phone,
      '邮箱': item.email,
      '角色': getRoleLabel(item.role),
      '错误信息': item._errorMsg || '未知错误'
    }))

    const wb = XLSX.utils.book_new()
    const ws = XLSX.utils.json_to_sheet(exportData)
    XLSX.utils.book_append_sheet(wb, ws, '失败记录')
    
    // 生成文件
    const wbout = XLSX.write(wb, { bookType: 'xlsx', type: 'array' })
    const blob = new Blob([wbout], { type: 'application/octet-stream' })
    const link = document.createElement('a')
    link.href = URL.createObjectURL(blob)
    link.download = `导入失败记录_${new Date().toISOString().slice(0,10)}.xlsx`
    link.click()
    URL.revokeObjectURL(link.href)
    
    ElMessage.success(`已导出 ${failedRecords.length} 条失败记录`)
  } catch (error) {
    console.error('导出失败:', error)
    ElMessage.error('导出失败')
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

/* ===== 操作按钮样式 ===== */
.action-buttons {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
}

.action-buttons .el-button {
  margin: 0;
  padding: 5px 8px;
  min-width: 32px;
}

.action-buttons .el-button .el-icon {
  font-size: 14px;
  margin: 0;
}

:deep(.el-table .cell) {
  padding: 6px 4px;
}

/* ===== 导入预览错误样式 ===== */
.error-text {
  color: #f56c6c;
  font-weight: bold;
}

:deep(.import-preview-row) {
  transition: background-color 0.3s;
}

:deep(.import-preview-row td) {
  padding: 4px 6px;
}

/* 导入进度样式 */
:deep(.el-progress-bar__innerText) {
  font-size: 14px;
  font-weight: bold;
}
</style>