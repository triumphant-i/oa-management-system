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
        <el-button type="info" plain @click="viewMode = viewMode === 'table' ? 'tree' : 'table'">
          <el-icon><component :is="viewMode === 'table' ? 'Grid' : 'List'" /></el-icon>
          {{ viewMode === 'table' ? '树形视图' : '表格视图' }}
        </el-button>
      </div>
    </div>

    <!-- 搜索栏 -->
    <el-card class="search-card">
      <el-form :inline="true" :model="searchForm" @keyup.enter="handleSearch">
        <el-form-item label="部门名称">
          <el-input 
            v-model="searchForm.name" 
            placeholder="请输入部门名称" 
            clearable 
            style="width:200px;"
          />
        </el-form-item>
        <el-form-item label="上级部门">
          <el-select 
            v-model="searchForm.parentId" 
            placeholder="请选择上级部门" 
            clearable 
            style="width:180px;"
          >
            <el-option label="总公司（顶级）" :value="0" />
            <el-option 
              v-for="dept in departmentOptions" 
              :key="dept.id" 
              :label="dept.name" 
              :value="dept.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="部门主管">
          <el-select 
            v-model="searchForm.managerId" 
            placeholder="请选择主管" 
            clearable 
            filterable
            style="width:180px;"
          >
            <el-option 
              v-for="emp in allEmployees" 
              :key="emp.id" 
              :label="emp.name" 
              :value="emp.id"
            />
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

    <!-- ===== 表格视图 ===== -->
    <el-card v-if="viewMode === 'table'">
      <el-table :data="tableData" style="width:100%;" v-loading="loading" stripe row-key="id">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="部门名称" min-width="150" />
        <el-table-column prop="parentName" label="上级部门" width="150">
          <template #default="{ row }">
            {{ row.parentId === 0 || !row.parentId ? '总公司' : row.parentName || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="level" label="层级" width="80">
          <template #default="{ row }">
            <el-tag size="small" :type="getLevelType(row.level)">
              {{ row.level || 0 }} 级
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="managerName" label="部门主管" width="120">
          <template #default="{ row }">
            <span v-if="row.managerName">{{ row.managerName }}</span>
            <span v-else style="color:#999;">未指定</span>
          </template>
        </el-table-column>
        <el-table-column label="员工数" width="100">
          <template #default="{ row }">
            <el-tag size="small" type="info">{{ row.employeeCount || 0 }}</el-tag>
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

    <!-- ===== 树形视图 ===== -->
    <el-card v-else>
      <div class="tree-toolbar">
        <el-button size="small" @click="expandAll">全部展开</el-button>
        <el-button size="small" @click="collapseAll">全部折叠</el-button>
        <span style="margin-left:12px;color:#999;font-size:13px;">
          共 {{ treeTotal }} 个部门
        </span>
      </div>
      <el-tree
        ref="treeRef"
        :data="treeData"
        :props="treeProps"
        node-key="id"
        default-expand-all
        highlight-current
        v-loading="loading"
        class="department-tree"
      >
        <template #default="{ data }">
          <div class="tree-node">
            <div class="tree-node-left">
              <span class="tree-node-icon" :style="{ color: getLevelColor(data.level) }">
                <el-icon><component :is="getLevelIcon(data.level)" /></el-icon>
              </span>
              <span class="tree-node-name">{{ data.name }}</span>
              <el-tag size="small" :type="getLevelType(data.level)" style="margin-left:8px;">
                {{ data.level || 0 }} 级
              </el-tag>
              <el-tag size="small" type="info" style="margin-left:8px;">
                👤 {{ data.employeeCount || 0 }} 人
              </el-tag>
              <span v-if="data.managerName" class="tree-node-manager">
                <el-icon><User /></el-icon> {{ data.managerName }}
              </span>
            </div>
            <div class="tree-node-right">
              <el-button size="small" type="primary" plain @click="openEditDialog(data)">
                <el-icon><Edit /></el-icon>
              </el-button>
              <el-button size="small" type="danger" plain @click="handleDelete(data)">
                <el-icon><Delete /></el-icon>
              </el-button>
            </div>
          </div>
        </template>
      </el-tree>
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
            :disabled="!form.id && dialogType === 'add'"
          >
            <el-option 
              v-for="emp in departmentEmployees" 
              :key="emp.id" 
              :label="emp.name + (emp.role === 'DEPARTMENT_MANAGER' ? ' ⭐主管' : '')" 
              :value="emp.id"
            />
            <el-option label="暂不指定" :value="null" />
          </el-select>
          <div style="font-size:12px;color:#999;margin-top:4px;">
            ⭐ 标记表示该员工已是部门主管
            <span v-if="!form.id && dialogType === 'add'" style="color:#e6a23c;">添加部门后，编辑时再指定主管</span>
            <span v-else-if="departmentEmployees.length === 0" style="color:#e6a23c;">该部门暂无员工</span>
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
import { ref, reactive, onMounted, computed, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Edit, Delete, Search, Grid, List, User, OfficeBuilding } from '@element-plus/icons-vue'
import {
  getDepartmentPage,
  getDepartmentList,
  saveDepartment,
  updateDepartment,
  deleteDepartment
} from '@/api/department'
import { getEmployeePage, updateEmployeeRole, updateEmployee } from '@/api/employee'

// ==================== 视图模式 ====================
const viewMode = ref('table') // table | tree
const treeRef = ref()

// ==================== 数据 ====================
const loading = ref(false)
const submitLoading = ref(false)

const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const tableData = ref([])
const departmentOptions = ref([])
const allEmployees = ref([])
const allDeptData = ref([])

// 记录旧的主管ID，用于对比
const oldManagerId = ref(null)

// ==================== 搜索表单 ====================
const searchForm = reactive({
  name: '',
  parentId: '',
  managerId: ''
})

// ==================== 树形数据 ====================
const treeProps = {
  children: 'children',
  label: 'name'
}

// 构建树形结构
const buildTree = (list, parentId = 0) => {
  const result = []
  for (const item of list) {
    if (item.parentId === parentId || (parentId === 0 && (item.parentId === 0 || item.parentId === null))) {
      const children = buildTree(list, item.id)
      const node = { ...item }
      if (children.length > 0) {
        node.children = children
      }
      result.push(node)
    }
  }
  return result
}

// 树形数据
const treeData = computed(() => {
  if (searchForm.name.trim() || searchForm.parentId !== '' || searchForm.managerId !== '') {
    const filtered = filterDepartments(allDeptData.value)
    return buildTree(filtered, 0)
  }
  return buildTree(allDeptData.value, 0)
})

const treeTotal = computed(() => {
  return allDeptData.value.length
})

// ==================== 计算属性 ====================
const departmentEmployees = computed(() => {
  if (!form.id) return []
  return allEmployees.value.filter(emp => 
    emp.departmentId === form.id && emp.status === '在职'
  )
})

// ==================== 统计 ====================
const stats = ref([
  { title: '部门总数', value: 0, color: '#409eff' },
  { title: '员工总数', value: 0, color: '#67c23a' },
  { title: '已指定主管', value: 0, color: '#e6a23c' },
  { title: '部门层级', value: 0, color: '#f56c6c' }
])

// ==================== 对话框 ====================
const dialogVisible = ref(false)
const dialogType = ref('add')
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

// ==================== 层级工具函数 ====================
const getLevelType = (level) => {
  if (level === 1) return 'danger'
  if (level === 2) return 'warning'
  if (level === 3) return 'primary'
  return ''
}

const getLevelColor = (level) => {
  const colors = ['', '#f56c6c', '#e6a23c', '#409eff', '#67c23a', '#909399']
  return colors[level] || '#409eff'
}

const getLevelIcon = (level) => {
  return 'OfficeBuilding'
}

// ==================== 前端过滤 ====================
const filterDepartments = (list) => {
  let result = [...list]
  
  if (searchForm.name.trim()) {
    const keyword = searchForm.name.trim().toLowerCase()
    result = result.filter(item => 
      item.name.toLowerCase().includes(keyword)
    )
  }
  
  if (searchForm.parentId !== '' && searchForm.parentId !== null) {
    result = result.filter(item => 
      item.parentId === searchForm.parentId
    )
  }
  
  if (searchForm.managerId !== '' && searchForm.managerId !== null) {
    result = result.filter(item => 
      item.managerId === searchForm.managerId
    )
  }
  
  return result
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
    if (item.parentId && item.parentId !== 0 && deptMap[item.parentId]) {
      item.parentName = deptMap[item.parentId].name
    } else {
      item.parentName = '总公司'
    }
    
    if (item.managerId) {
      const manager = allEmployees.value.find(emp => emp.id === item.managerId)
      item.managerName = manager ? manager.name : '已离职'
    } else {
      item.managerName = null
    }
    
    let level = 1
    let parentId = item.parentId
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
    const empRes = await getEmployeePage(1, 999)
    if (empRes.code === 0) {
      allEmployees.value = empRes.data?.data || []
    }
    
    const allDeptRes = await getDepartmentList()
    let allDepts = []
    if (allDeptRes.code === 0) {
      allDepts = allDeptRes.data || []
      departmentOptions.value = allDepts
    }
    
    const res = await getDepartmentPage(currentPage.value, pageSize.value)
    if (res.code === 0) {
      let list = res.data?.data || []
      list = enrichDepartmentData(list, allDepts)
      
      allDeptData.value = list
      
      const filtered = filterDepartments(list)
      
      total.value = filtered.length
      
      const start = (currentPage.value - 1) * pageSize.value
      const end = start + pageSize.value
      tableData.value = filtered.slice(start, end)
      
      stats.value[0].value = filtered.length
      
      let totalEmployees = 0
      let hasManagerCount = 0
      let maxLevel = 0
      
      for (const dept of filtered) {
        const empCount = allEmployees.value.filter(emp => 
          emp.departmentId === dept.id && emp.status === '在职'
        ).length
        dept.employeeCount = empCount
        totalEmployees += empCount
        
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
  } catch (error) {
    console.error('加载部门数据失败:', error)
    ElMessage.error('加载部门数据失败')
  } finally {
    loading.value = false
  }
}

// ==================== 搜索 ====================
const handleSearch = () => {
  currentPage.value = 1
  loadData()
}

const resetSearch = () => {
  searchForm.name = ''
  searchForm.parentId = ''
  searchForm.managerId = ''
  handleSearch()
}

// ==================== 树形操作 ====================
const expandAll = () => {
  if (treeRef.value) {
    try {
      // 方法1：使用 store 遍历所有节点
      const nodes = treeRef.value.store.nodesMap
      for (const key in nodes) {
        const node = nodes[key]
        if (node.childNodes && node.childNodes.length > 0) {
          node.expanded = true
        }
      }
      console.log('全部展开成功')
    } catch (error) {
      console.warn('展开失败，尝试备用方法:', error)
      // 备用方法：使用 expand 方法
      treeRef.value.expandAll()
    }
  } else {
    console.warn('treeRef 未准备好，请稍后重试')
    ElMessage.warning('树形组件未加载完成，请稍后重试')
  }
}

const collapseAll = () => {
  if (treeRef.value) {
    try {
      const nodes = treeRef.value.store.nodesMap
      for (const key in nodes) {
        const node = nodes[key]
        if (node.childNodes && node.childNodes.length > 0) {
          node.expanded = false
        }
      }
      console.log('全部折叠成功')
    } catch (error) {
      console.warn('折叠失败，尝试备用方法:', error)
      // 备用方法：遍历折叠
      const nodes = treeRef.value.store.nodesMap
      for (const key in nodes) {
        const node = nodes[key]
        if (node.level > 1) {
          node.expanded = false
        }
      }
    }
  } else {
    console.warn('treeRef 未准备好，请稍后重试')
    ElMessage.warning('树形组件未加载完成，请稍后重试')
  }
}

// ==================== 同步更新员工角色和部门主管信息 ====================
const syncManagerRole = async (departmentId, newManagerId, oldManagerId) => {
  try {
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
    
    if (newManagerId) {
      try {
        await updateEmployeeRole({
          employeeId: newManagerId,
          role: 'DEPARTMENT_MANAGER'
        })
        console.log(`已将员工 ${newManagerId} 升级为部门主管`)
        
        await updateEmployee({
          id: newManagerId,
          departmentId: departmentId
        })
        console.log(`已将员工 ${newManagerId} 分配到部门 ${departmentId}`)
        
        const manager = allEmployees.value.find(emp => emp.id === newManagerId)
        if (manager) {
          await updateDepartment({
            id: departmentId,
            managerId: newManagerId,
            managerName: manager.name,
            managerPhone: manager.phone || ''
          })
          console.log(`已更新部门 ${departmentId} 的主管信息: ${manager.name}`)
        }
      } catch (error) {
        console.warn('升级新主管失败:', error)
      }
    } else {
      await updateDepartment({
        id: departmentId,
        managerId: null,
        managerName: null,
        managerPhone: null
      })
      console.log(`已清空部门 ${departmentId} 的主管信息`)
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
      const submitData = {
        name: form.name,
        parentId: form.parentId,
        managerId: form.managerId,
        description: form.description
      }
      
      if (dialogType.value === 'add') {
        res = await saveDepartment(submitData)
        if (res.code === 0) {
          ElMessage.success('部门添加成功')
          dialogVisible.value = false
          await loadData()
        } else {
          ElMessage.error(res.msg || '添加失败')
        }
      } else {
        submitData.id = form.id
        res = await updateDepartment(submitData)
        
        if (res.code === 0) {
          await syncManagerRole(form.id, form.managerId, oldManagerId.value)
          ElMessage.success('部门更新成功')
          dialogVisible.value = false
          await loadData()
        } else {
          ElMessage.error(res.msg || '更新失败')
        }
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

:deep(.el-table) {
  font-size: 14px;
}

:deep(.el-card__body) {
  padding: 20px;
}

/* ===== 树形视图样式 ===== */
.tree-toolbar {
  margin-bottom: 16px;
  display: flex;
  align-items: center;
}

.department-tree {
  min-height: 400px;
}

.department-tree :deep(.el-tree-node__content) {
  height: 48px;
  padding: 0 8px;
  border-radius: 8px;
  transition: background 0.2s;
}

.department-tree :deep(.el-tree-node__content:hover) {
  background: #f0f7ff;
}

.tree-node {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  padding-right: 12px;
}

.tree-node-left {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.tree-node-icon {
  font-size: 18px;
  display: flex;
  align-items: center;
}

.tree-node-name {
  font-size: 15px;
  font-weight: 500;
  color: #1a1a2e;
}

.tree-node-manager {
  font-size: 13px;
  color: #67c23a;
  display: flex;
  align-items: center;
  gap: 4px;
  margin-left: 4px;
}

.tree-node-right {
  display: flex;
  gap: 4px;
  opacity: 0.6;
  transition: opacity 0.2s;
}

.tree-node-right:hover {
  opacity: 1;
}

.tree-node-right .el-button {
  padding: 4px 6px;
}

/* 树形展开/折叠动画优化 */
:deep(.el-tree-node__expand-icon) {
  font-size: 14px;
  color: #409eff;
}

:deep(.el-tree-node__expand-icon.expanded) {
  transform: rotate(90deg);
}

/* 树形节点层级缩进 */
:deep(.el-tree-node) {
  transition: all 0.2s;
}

:deep(.el-tree-node__content) {
  border-left: 3px solid transparent;
}

:deep(.el-tree-node__content:hover) {
  border-left-color: #409eff;
}
</style>