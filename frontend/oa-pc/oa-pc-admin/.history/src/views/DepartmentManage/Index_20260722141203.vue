<template>
  <div class="page-container">
    <div class="page-header">
      <h2>部门管理</h2>
      <div class="header-actions">
        <el-button type="primary" @click="openAddDialog">
          <el-icon><Plus /></el-icon> 添加部门
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

    <!-- 部门表格 -->
    <el-card>
      <el-table :data="tableData" style="width:100%;" v-loading="loading" stripe row-key="id" default-expand-all>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="部门名称" min-width="150" />
        <el-table-column prop="parentName" label="上级部门" width="150">
          <template #default="{ row }">
            {{ row.parentId === 0 || !row.parentId ? '总公司' : row.parentName || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="level" label="层级" width="80">
          <template #default="{ row }">
            <el-tag size="small" :type="row.level === 1 ? 'danger' : row.level === 2 ? 'warning' : ''">
              {{ row.level || 0 }} 级
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="managerName" label="部门主管" width="120">
          <template #default="{ row }">
            {{ row.managerName || '未指定' }}
          </template>
        </el-table-column>
        <el-table-column label="员工数" width="100">
          <template #default="{ row }">
            {{ row.employeeCount || 0 }}
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="150" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" plain @click="openEditDialog(row)">
              <el-icon><Edit /></el-icon> 编辑
            </el-button>
            <el-button size="small" type="danger" plain @click="handleDelete(row)">
              <el-icon><Delete /></el-icon> 删除
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

    <!-- ===== 添加/编辑部门对话框 ===== -->
    <el-dialog 
      v-model="dialogVisible" 
      :title="dialogType === 'add' ? '添加部门' : '编辑部门'" 
      width="550px" 
      @close="resetForm"
    >
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="部门名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入部门名称" />
        </el-form-item>
        <el-form-item label="上级部门" prop="parentId">
          <el-select v-model="form.parentId" placeholder="请选择上级部门" style="width:100%;">
            <el-option label="总公司（顶级部门）" :value="0" />
            <el-option 
              v-for="dept in departmentOptions" 
              :key="dept.id" 
              :label="dept.name" 
              :value="dept.id"
              :disabled="dept.id === form.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="部门主管">
          <el-select 
            v-model="form.managerId" 
            placeholder="请选择部门主管" 
            style="width:100%;" 
            filterable 
            clearable
            @change="handleManagerChange"
          >
            <el-option 
              v-for="emp in employeeOptions" 
              :key="emp.id" 
              :label="emp.name + ' (' + (emp.departmentName || '未分配') + ')' + (emp.role === 'DEPARTMENT_MANAGER' ? ' ⭐主管' : '')" 
              :value="emp.id"
            />
            <el-option label="暂不指定" :value="null" />
          </el-select>
          <div style="font-size:12px;color:#999;margin-top:4px;">
            ⭐ 标记表示该员工已是部门主管
          </div>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" rows="3" placeholder="请输入部门描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitLoading">
          {{ dialogType === 'add' ? '确认添加' : '保存修改' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Edit, Delete } from '@element-plus/icons-vue'
import {
  getDepartmentPage,
  getDepartmentList,
  saveDepartment,
  updateDepartment,
  deleteDepartment,
  countDepartmentEmployees
} from '@/api/department'
import { getEmployeePage, updateEmployeeRole } from '@/api/employee'

// ==================== 数据 ====================
const loading = ref(false)
const submitLoading = ref(false)

const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const tableData = ref([])
const departmentOptions = ref([])
const employeeOptions = ref([])

// 记录旧的主管ID，用于对比
const oldManagerId = ref(null)

// ==================== 统计 ====================
const stats = ref([
  { title: '部门总数', value: 0, color: '#409eff' },
  { title: '员工总数', value: 0, color: '#67c23a' },
  { title: '已指定主管', value: 0, color: '#e6a23c' },
  { title: '部门层级', value: 0, color: '#f56c6c' }
])

// ==================== 对话框 ====================
const dialogVisible = ref(false)
const dialogType = ref('add') // add / edit
const formRef = ref()
const form = reactive({
  id: null,
  name: '',
  parentId: 0,
  managerId: null,
  description: ''
})

const rules = {
  name: [{ required: true, message: '请输入部门名称' }],
  parentId: [{ required: true, message: '请选择上级部门' }]
}

// ==================== 构建部门映射 ====================
const buildDepartmentMap = (list) => {
  const map = {}
  list.forEach(item => {
    map[item.id] = item
  })
  return map
}

// ==================== 填充部门名称和层级 ====================
const enrichDepartmentData = (list, allDepts) => {
  const deptMap = buildDepartmentMap(allDepts)
  
  return list.map(item => {
    // 填充上级部门名称
    if (item.parentId && item.parentId !== 0 && deptMap[item.parentId]) {
      item.parentName = deptMap[item.parentId].name
    } else {
      item.parentName = '总公司'
    }
    
    // 计算层级
    let level = 1
    let currentId = item.id
    let parentId = item.parentId
    
    // 递归向上查找层级
    while (parentId && parentId !== 0) {
      const parent = deptMap[parentId]
      if (parent) {
        level++
        parentId = parent.parentId
      } else {
        break
      }
    }
    item.level = level
    
    return item
  })
}

// ==================== 加载数据 ====================
const loadData = async () => {
  loading.value = true
  try {
    // 获取所有部门（用于层级计算和名称填充）
    const allDeptRes = await getDepartmentList()
    let allDepts = []
    if (allDeptRes.code === 0) {
      allDepts = allDeptRes.data || []
      departmentOptions.value = allDepts
    }
    
    // 获取分页数据
    const res = await getDepartmentPage(currentPage.value, pageSize.value)
    if (res.code === 0) {
      let list = res.data?.data || []
      total.value = res.data?.total || 0
      
      // 🔧 填充上级部门名称和层级
      list = enrichDepartmentData(list, allDepts)
      
      tableData.value = list
      
      // 更新统计
      stats.value[0].value = total.value
      
      let totalEmployees = 0
      let hasManagerCount = 0
      let maxLevel = 0
      
      for (const dept of list) {
        const countRes = await countDepartmentEmployees(dept.id)
        if (countRes.code === 0) {
          dept.employeeCount = countRes.data || 0
          totalEmployees += countRes.data || 0
        }
        if (dept.managerId) {
          hasManagerCount++
        }
        if (dept.level > maxLevel) {
          maxLevel = dept.level
        }
      }
      stats.value[1].value = totalEmployees
      stats.value[2].value = hasManagerCount
      stats.value[3].value = maxLevel
    }
    
    // 加载员工列表（用于选择主管）
    await loadEmployeeOptions()
  } catch (error) {
    console.error('加载部门数据失败:', error)
    ElMessage.error('加载部门数据失败')
  } finally {
    loading.value = false
  }
}

// ==================== 加载员工选项 ====================
const loadEmployeeOptions = async () => {
  try {
    const empRes = await getEmployeePage(1, 999)
    if (empRes.code === 0) {
      let empList = empRes.data?.data || []
      
      // 只显示在职员工
      empList = empList.filter(emp => emp.status === '在职')
      
      // 填充部门名称
      const deptMap = {}
      departmentOptions.value.forEach(dept => {
        deptMap[dept.id] = dept.name
      })
      
      empList = empList.map(emp => ({
        ...emp,
        departmentName: deptMap[emp.departmentId] || '未分配部门'
      }))
      
      employeeOptions.value = empList
    }
  } catch (error) {
    console.error('加载员工列表失败:', error)
  }
}

// ==================== 主管变更处理 ====================
const handleManagerChange = (newManagerId) => {
  // 记录新旧主管ID，用于后续更新角色
}

// ==================== 同步更新员工角色 ====================
const syncManagerRole = async (departmentId, newManagerId, oldManagerId) => {
  try {
    // 1. 如果旧主管存在且不等于新主管，将旧主管降级为普通员工
    if (oldManagerId && oldManagerId !== newManagerId) {
      try {
        await updateEmployeeRole({
          employeeId: oldManagerId,
          role: 'EMPLOYEE'
        })
        console.log(`已将员工 ${oldManagerId} 降级为普通员工`)
      } catch (error) {
        console.warn('降级旧主管失败:', error)
      }
    }
    
    // 2. 如果新主管存在，升级为部门主管
    if (newManagerId) {
      try {
        await updateEmployeeRole({
          employeeId: newManagerId,
          role: 'DEPARTMENT_MANAGER'
        })
        console.log(`已将员工 ${newManagerId} 升级为部门主管`)
      } catch (error) {
        console.warn('升级新主管失败:', error)
      }
    }
  } catch (error) {
    console.error('同步主管角色失败:', error)
  }
}

// ==================== 添加/编辑 ====================
const openAddDialog = () => {
  dialogType.value = 'add'
  form.id = null
  form.name = ''
  form.parentId = 0
  form.managerId = null
  form.description = ''
  oldManagerId.value = null
  dialogVisible.value = true
}

const openEditDialog = (row) => {
  dialogType.value = 'edit'
  form.id = row.id
  form.name = row.name
  form.parentId = row.parentId || 0
  form.managerId = row.managerId || null
  form.description = row.description || ''
  oldManagerId.value = row.managerId || null
  dialogVisible.value = true
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
        res = await saveDepartment(form)
      } else {
        res = await updateDepartment(form)
      }
      
      if (res.code === 0) {
        // 🔧 同步更新主管角色
        await syncManagerRole(form.id, form.managerId, oldManagerId.value)
        
        ElMessage.success(dialogType.value === 'add' ? '添加成功' : '更新成功')
        dialogVisible.value = false
        await loadData()
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

// ==================== 删除 ====================
const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除「${row.name}」吗？该部门下有 ${row.employeeCount || 0} 名员工，删除前请先转移员工！`,
      '警告',
      { confirmButtonText: '确定删除', cancelButtonText: '取消', type: 'warning' }
    )
    
    const res = await deleteDepartment(row.id)
    if (res.code === 0) {
      // 删除部门后，如果有主管，降级为普通员工
      if (row.managerId) {
        try {
          await updateEmployeeRole({
            employeeId: row.managerId,
            role: 'EMPLOYEE'
          })
        } catch (error) {
          console.warn('降级主管失败:', error)
        }
      }
      
      ElMessage.success('删除成功')
      loadData()
    } else {
      ElMessage.error(res.msg || '删除失败')
    }
  } catch {
    // 用户取消
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
</style>