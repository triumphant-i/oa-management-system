<template>
  <div class="page-container">
    <div class="page-header">
      <h2>系统管理</h2>
    </div>

    <el-tabs v-model="activeTab">
      <!-- ===== 角色管理 ===== -->
      <el-tab-pane label="角色管理" name="role">
        <el-card>
          <div style="margin-bottom:16px;display:flex;gap:10px;flex-wrap:wrap;">
            <el-button type="primary" @click="openRoleDialog">
              <el-icon><Plus /></el-icon> 添加角色
            </el-button>
          </div>
          <el-table :data="roleList" v-loading="roleLoading" stripe>
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="code" label="角色代码" width="150" />
            <el-table-column prop="name" label="角色名称" width="150" />
            <el-table-column prop="description" label="描述" min-width="150" />
            <el-table-column prop="isSystem" label="系统角色" width="100">
              <template #default="{ row }">
                <el-tag :type="row.isSystem ? 'danger' : 'info'" size="small">
                  {{ row.isSystem ? '系统' : '自定义' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="250" fixed="right">
              <template #default="{ row }">
                <el-button size="small" type="primary" plain @click="openPermissionDialog(row)">分配权限</el-button>
                <el-button size="small" type="warning" plain @click="openEditRoleDialog(row)" :disabled="row.isSystem">编辑</el-button>
                <el-button size="small" type="danger" plain @click="handleDeleteRole(row)" :disabled="row.isSystem">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>

      <!-- ===== 权限管理 ===== -->
      <el-tab-pane label="权限管理" name="permission">
        <el-card>
          <div style="margin-bottom:16px;">
            <el-button type="primary" @click="openPermissionDialog(null)">
              <el-icon><Plus /></el-icon> 添加权限
            </el-button>
          </div>
          <el-table :data="permissionList" v-loading="permissionLoading" stripe>
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="code" label="权限代码" width="180" />
            <el-table-column prop="name" label="权限名称" width="150" />
            <el-table-column prop="module" label="所属模块" width="120">
              <template #default="{ row }">
                <el-tag size="small">{{ row.module || '-' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="type" label="类型" width="100">
              <template #default="{ row }">
                <el-tag :type="row.type === 'MENU' ? 'primary' : 'success'" size="small">
                  {{ row.type === 'MENU' ? '菜单' : '按钮' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="150">
              <template #default="{ row }">
                <el-button size="small" type="warning" plain>编辑</el-button>
                <el-button size="small" type="danger" plain>删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>

      <!-- ===== 操作日志 ===== -->
      <el-tab-pane label="操作日志" name="log">
        <el-card>
          <div class="table-toolbar">
            <el-date-picker
              v-model="logParams.dateRange"
              type="daterange"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              format="YYYY-MM-DD"
              value-format="YYYY-MM-DD"
              style="width:260px;"
            />
            <el-input v-model="logParams.keyword" placeholder="搜索操作人/操作内容" clearable style="width:200px;" @keyup.enter="loadLogs" />
            <el-button type="primary" @click="loadLogs">查询</el-button>
            <el-button @click="resetLogParams">重置</el-button>
          </div>
          <el-table :data="logList" v-loading="logLoading" stripe>
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="userName" label="操作人" width="120" />
            <el-table-column prop="action" label="操作" min-width="200" />
            <el-table-column prop="module" label="模块" width="120" />
            <el-table-column prop="createTime" label="操作时间" width="180">
              <template #default="{ row }">
                {{ formatTime(row.createTime) }}
              </template>
            </el-table-column>
          </el-table>
          <div class="pagination">
            <el-pagination
              v-model:page-size="logPageSize"
              v-model:current-page="logPage"
              :total="logTotal"
              layout="total, sizes, prev, pager, next, jumper"
              @size-change="loadLogs"
              @current-change="loadLogs"
            />
          </div>
        </el-card>
      </el-tab-pane>

      <!-- ===== 数据字典 ===== -->
      <el-tab-pane label="数据字典" name="dict">
        <el-card>
          <div style="margin-bottom:16px;">
            <el-button type="primary" @click="openDictDialog">
              <el-icon><Plus /></el-icon> 添加字典
            </el-button>
          </div>
          <el-table :data="dictList" v-loading="dictLoading" stripe>
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="type" label="字典类型" width="150" />
            <el-table-column prop="label" label="标签" width="150" />
            <el-table-column prop="value" label="值" width="150" />
            <el-table-column prop="sort" label="排序" width="80" />
            <el-table-column label="操作" width="150">
              <template #default="{ row }">
                <el-button size="small" type="warning" plain>编辑</el-button>
                <el-button size="small" type="danger" plain>删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <!-- ===== 添加/编辑角色对话框 ===== -->
    <el-dialog v-model="roleDialogVisible" :title="roleDialogType === 'add' ? '添加角色' : '编辑角色'" width="500px" @close="resetRoleForm">
      <el-form :model="roleForm" :rules="roleRules" ref="roleFormRef" label-width="100px">
        <el-form-item label="角色代码" prop="code">
          <el-input v-model="roleForm.code" placeholder="如：MANAGER" :disabled="roleDialogType === 'edit'" />
          <div style="font-size:12px;color:#999;margin-top:4px;">建议使用大写字母和下划线</div>
        </el-form-item>
        <el-form-item label="角色名称" prop="name">
          <el-input v-model="roleForm.name" placeholder="请输入角色名称" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="roleForm.description" type="textarea" rows="2" placeholder="请输入角色描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="roleDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSaveRole" :loading="roleSubmitLoading">确认</el-button>
      </template>
    </el-dialog>

    <!-- ===== 分配权限对话框 ===== -->
    <el-dialog v-model="permDialogVisible" title="分配权限" width="700px" @close="resetPermForm">
      <div style="margin-bottom:12px;">
        <span style="font-weight:bold;">角色：</span>
        <el-tag>{{ permForm.roleName || '未选择' }}</el-tag>
      </div>
      <div class="perm-tree-wrap">
        <el-tree
          ref="permTreeRef"
          :data="permTreeData"
          show-checkbox
          node-key="id"
          :default-checked-keys="permForm.checkedKeys"
          :props="{ label: 'name', children: 'children' }"
          check-strictly
        />
      </div>
      <template #footer>
        <el-button @click="permDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSavePermissions" :loading="permSubmitLoading">保存权限</el-button>
      </template>
    </el-dialog>

    <!-- ===== 添加字典对话框 ===== -->
    <el-dialog v-model="dictDialogVisible" title="添加字典" width="500px" @close="resetDictForm">
      <el-form :model="dictForm" :rules="dictRules" ref="dictFormRef" label-width="100px">
        <el-form-item label="字典类型" prop="type">
          <el-input v-model="dictForm.type" placeholder="如：approval_type" />
        </el-form-item>
        <el-form-item label="标签" prop="label">
          <el-input v-model="dictForm.label" placeholder="如：请假" />
        </el-form-item>
        <el-form-item label="值" prop="value">
          <el-input v-model="dictForm.value" placeholder="如：leave" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="dictForm.sort" :min="1" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dictDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSaveDict" :loading="dictSubmitLoading">确认添加</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'

// ==================== 当前标签页 ====================
const activeTab = ref('role')

// ==================== 角色管理 ====================
const roleLoading = ref(false)
const roleSubmitLoading = ref(false)
const roleDialogVisible = ref(false)
const roleDialogType = ref('add') // add / edit
const roleFormRef = ref()
const roleList = ref([
  { id: 1, code: 'SYSTEM_ADMIN', name: '系统管理员', description: '拥有系统全部权限', isSystem: true },
  { id: 2, code: 'GENERAL_MANAGER', name: '总经理', description: '公司最高管理者', isSystem: true },
  { id: 3, code: 'DEPARTMENT_MANAGER', name: '部门经理', description: '部门负责人', isSystem: true },
  { id: 4, code: 'PROCESS_ADMIN', name: '流程管理员', description: '管理审批流程模板', isSystem: true },
  { id: 5, code: 'EMPLOYEE', name: '普通员工', description: '普通员工', isSystem: true }
])

const roleForm = reactive({
  id: null,
  code: '',
  name: '',
  description: ''
})

const roleRules = {
  code: [{ required: true, message: '请输入角色代码' }],
  name: [{ required: true, message: '请输入角色名称' }]
}

// ==================== 权限管理 ====================
const permissionLoading = ref(false)
const permissionList = ref([
  { id: 1, code: 'employee:view', name: '查看员工', module: '员工管理', type: 'BUTTON' },
  { id: 2, code: 'employee:add', name: '添加员工', module: '员工管理', type: 'BUTTON' },
  { id: 3, code: 'approval:approve', name: '审批通过', module: '审批管理', type: 'BUTTON' },
  { id: 4, code: 'notice:publish', name: '发布公告', module: '公告管理', type: 'BUTTON' },
  { id: 5, code: 'meeting:book', name: '预约会议室', module: '会议室管理', type: 'BUTTON' }
])

// ==================== 分配权限 ====================
const permDialogVisible = ref(false)
const permSubmitLoading = ref(false)
const permTreeRef = ref()
const permForm = reactive({
  roleId: null,
  roleName: '',
  checkedKeys: []
})

// 权限树数据
const permTreeData = computed(() => {
  // 按模块分组构建树
  const moduleMap = {}
  permissionList.value.forEach(item => {
    const module = item.module || '其他'
    if (!moduleMap[module]) {
      moduleMap[module] = {
        id: 'module_' + module,
        name: module,
        children: []
      }
    }
    moduleMap[module].children.push({
      id: item.id,
      name: item.name + ' (' + item.code + ')',
      code: item.code
    })
  })
  return Object.values(moduleMap)
})

// ==================== 操作日志 ====================
const logLoading = ref(false)
const logPage = ref(1)
const logPageSize = ref(10)
const logTotal = ref(0)
const logList = ref([
  { id: 1, userName: '系统管理员', action: '添加员工：张三', module: '员工管理', createTime: '2026-07-20 09:00:00' },
  { id: 2, userName: '系统管理员', action: '删除部门：测试部', module: '部门管理', createTime: '2026-07-20 08:30:00' },
  { id: 3, userName: '张三', action: '审批通过：请假申请', module: '审批管理', createTime: '2026-07-19 14:00:00' }
])
const logParams = reactive({
  dateRange: [],
  keyword: ''
})

// ==================== 数据字典 ====================
const dictLoading = ref(false)
const dictSubmitLoading = ref(false)
const dictDialogVisible = ref(false)
const dictFormRef = ref()
const dictList = ref([
  { id: 1, type: 'approval_type', label: '请假', value: 'leave', sort: 1 },
  { id: 2, type: 'approval_type', label: '报销', value: 'reimburse', sort: 2 },
  { id: 3, type: 'approval_type', label: '出差', value: 'business', sort: 3 },
  { id: 4, type: 'approval_type', label: '加班', value: 'overtime', sort: 4 },
  { id: 5, type: 'approval_type', label: '采购', value: 'purchase', sort: 5 }
])

const dictForm = reactive({
  type: '',
  label: '',
  value: '',
  sort: 1
})

const dictRules = {
  type: [{ required: true, message: '请输入字典类型' }],
  label: [{ required: true, message: '请输入标签' }],
  value: [{ required: true, message: '请输入值' }]
}

// ==================== 工具方法 ====================
const formatTime = (time) => {
  if (!time) return ''
  return time.replace('T', ' ')
}

// ==================== 角色操作 ====================
const openRoleDialog = () => {
  roleDialogType.value = 'add'
  roleForm.id = null
  roleForm.code = ''
  roleForm.name = ''
  roleForm.description = ''
  roleDialogVisible.value = true
}

const openEditRoleDialog = (row) => {
  roleDialogType.value = 'edit'
  roleForm.id = row.id
  roleForm.code = row.code
  roleForm.name = row.name
  roleForm.description = row.description || ''
  roleDialogVisible.value = true
}

const resetRoleForm = () => {
  roleFormRef.value?.resetFields()
}

const handleSaveRole = async () => {
  await roleFormRef.value.validate(async (valid) => {
    if (!valid) return

    roleSubmitLoading.value = true
    try {
      await new Promise(resolve => setTimeout(resolve, 500))
      if (roleDialogType.value === 'add') {
        const newRole = {
          id: roleList.value.length + 1,
          code: roleForm.code.toUpperCase(),
          name: roleForm.name,
          description: roleForm.description,
          isSystem: false
        }
        roleList.value.push(newRole)
        ElMessage.success('添加成功')
      } else {
        const index = roleList.value.findIndex(r => r.id === roleForm.id)
        if (index !== -1) {
          roleList.value[index].name = roleForm.name
          roleList.value[index].description = roleForm.description
        }
        ElMessage.success('更新成功')
      }
      roleDialogVisible.value = false
    } catch (error) {
      console.error('保存角色失败:', error)
      ElMessage.error('操作失败')
    } finally {
      roleSubmitLoading.value = false
    }
  })
}

const handleDeleteRole = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除角色「${row.name}」吗？`,
      '警告',
      { confirmButtonText: '确定删除', cancelButtonText: '取消', type: 'warning' }
    )
    const index = roleList.value.findIndex(r => r.id === row.id)
    if (index !== -1) {
      roleList.value.splice(index, 1)
      ElMessage.success('删除成功')
    }
  } catch {
    // 用户取消
  }
}

// ==================== 权限分配 ====================
const openPermissionDialog = (row) => {
  permForm.roleId = row?.id || null
  permForm.roleName = row?.name || ''
  permForm.checkedKeys = row?.id === 1 ? [1, 2, 3, 4, 5] : []
  permDialogVisible.value = true
}

const resetPermForm = () => {
  permForm.roleId = null
  permForm.roleName = ''
  permForm.checkedKeys = []
}

const handleSavePermissions = async () => {
  const checkedNodes = permTreeRef.value?.getCheckedNodes() || []
  const checkedKeys = checkedNodes.map(node => node.id)
  
  permSubmitLoading.value = true
  try {
    await new Promise(resolve => setTimeout(resolve, 500))
    ElMessage.success(`权限保存成功！共分配 ${checkedKeys.length} 个权限`)
    permDialogVisible.value = false
  } catch (error) {
    console.error('保存权限失败:', error)
    ElMessage.error('操作失败')
  } finally {
    permSubmitLoading.value = false
  }
}

// ==================== 日志操作 ====================
const loadLogs = async () => {
  logLoading.value = true
  try {
    await new Promise(resolve => setTimeout(resolve, 500))
    // 模拟数据
    logTotal.value = 3
  } catch (error) {
    console.error('加载日志失败:', error)
  } finally {
    logLoading.value = false
  }
}

const resetLogParams = () => {
  logParams.dateRange = []
  logParams.keyword = ''
  logPage.value = 1
  loadLogs()
}

// ==================== 字典操作 ====================
const openDictDialog = () => {
  dictForm.type = ''
  dictForm.label = ''
  dictForm.value = ''
  dictForm.sort = 1
  dictDialogVisible.value = true
}

const resetDictForm = () => {
  dictFormRef.value?.resetFields()
}

const handleSaveDict = async () => {
  await dictFormRef.value.validate(async (valid) => {
    if (!valid) return

    dictSubmitLoading.value = true
    try {
      await new Promise(resolve => setTimeout(resolve, 500))
      const newDict = {
        id: dictList.value.length + 1,
        type: dictForm.type,
        label: dictForm.label,
        value: dictForm.value,
        sort: dictForm.sort
      }
      dictList.value.push(newDict)
      ElMessage.success('添加成功')
      dictDialogVisible.value = false
    } catch (error) {
      console.error('保存字典失败:', error)
      ElMessage.error('操作失败')
    } finally {
      dictSubmitLoading.value = false
    }
  })
}

// ==================== 生命周期 ====================
onMounted(() => {
  loadLogs()
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

.table-toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 16px;
  align-items: center;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.perm-tree-wrap {
  max-height: 400px;
  overflow-y: auto;
  border: 1px solid #eee;
  border-radius: 4px;
  padding: 12px;
}

:deep(.el-tree) {
  background: transparent;
}

:deep(.el-tree-node__content) {
  height: 34px;
}

:deep(.el-tree-node__content:hover) {
  background: #f5f7fa;
}
</style>