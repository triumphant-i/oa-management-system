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