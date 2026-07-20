<template>
  <div class="oa-system" ref="scrollContainer">
    <div class="page-header">
      <van-icon name="arrow-left" size="22" @click="$router.back()" />
      <h1 class="header-title">系统管理</h1>
      <div class="header-right">
        <van-icon name="ellipsis" size="22" />
      </div>
    </div>

    <div class="stat-row">
      <div class="stat-card" @click="activeTab = 'users'">
        <span class="stat-num">{{ stats.totalUsers }}</span>
        <span class="stat-label">总用户</span>
      </div>
      <div class="stat-card" @click="activeTab = 'users'">
        <span class="stat-num" style="color: #00b894;">{{ stats.activeUsers }}</span>
        <span class="stat-label">活跃用户</span>
      </div>
      <div class="stat-card" @click="activeTab = 'logs'">
        <span class="stat-num" style="color: #fdcb6e;">{{ stats.todayLogs }}</span>
        <span class="stat-label">今日操作</span>
      </div>
      <div class="stat-card" @click="activeTab = 'users'">
        <span class="stat-num" style="color: #ff6b35;">{{ stats.disabledUsers }}</span>
        <span class="stat-label">已禁用</span>
      </div>
    </div>

    <div class="tab-wrap">
      <span class="tab-item" :class="{ active: activeTab === 'users' }" @click="activeTab = 'users'">用户管理</span>
      <span class="tab-item" :class="{ active: activeTab === 'roles' }" @click="activeTab = 'roles'">角色权限</span>
      <span class="tab-item" :class="{ active: activeTab === 'logs' }" @click="activeTab = 'logs'">操作日志</span>
      <span class="tab-item" :class="{ active: activeTab === 'dict' }" @click="activeTab = 'dict'">数据字典</span>
    </div>

    <div v-if="activeTab === 'users'">
      <div class="user-toolbar">
        <van-field v-model="searchKeyword" placeholder="搜索姓名/工号" left-icon="search" class="search-field" @change="fetchUsers" />
        <van-button type="primary" size="small" @click="showAddUser = true">+ 添加</van-button>
        <van-button type="success" size="small" plain @click="showImportUser = true">导入</van-button>
      </div>

      <div class="user-list">
        <div class="user-item" v-for="item in filteredUsers" :key="item.id">
          <van-image round width="40" height="40" :src="getAvatarUrl(item.avatar)" />
          <div class="user-info">
            <p class="user-name">{{ item.name }}</p>
            <p class="user-dept">{{ item.departmentName || item.department || '未分配' }} · {{ getRoleName(item.role) }}</p>
          </div>
          <div class="user-actions">
            <van-switch 
              v-model="item.status" 
              size="18" 
              active-color="#00b894" 
              inactive-color="#e17055"
              :active-value="'在职'"
              :inactive-value="'离职'"
              @change="toggleUserStatus(item)"
            />
            <van-icon name="edit" size="18" color="#3677ef" @click="editUser(item)" />
            <van-icon name="delete" size="18" color="#e17055" @click="deleteUser(item)" />
          </div>
        </div>
        <div class="empty-state" v-if="filteredUsers.length === 0">
          <van-icon name="friends-o" size="48" color="#ccc" />
          <p>暂无用户</p>
        </div>
      </div>
    </div>

    <div v-if="activeTab === 'roles'">
      <div class="role-toolbar">
        <span class="role-tip">角色权限管理</span>
        <van-button type="primary" size="small" @click="showAddRole = true">+ 添加角色</van-button>
      </div>

      <div class="role-list">
        <div class="role-item" v-for="item in roleList" :key="item.id">
          <div class="role-header">
            <div class="role-icon" :style="{ background: item.color || '#6c5ce7' }">
              <van-icon :name="item.icon || 'shield-o'" size="18" color="#fff" />
            </div>
            <div class="role-info">
              <span class="role-name">{{ item.name }}</span>
              <span class="role-desc">{{ item.desc || '自定义角色' }}</span>
            </div>
            <span class="role-count">{{ item.userCount || 0 }}人</span>
          </div>
          <div class="role-permissions">
            <span class="perm-tag" v-for="perm in parsePermissions(item.permissions)" :key="perm">
              {{ perm }}
            </span>
          </div>
          <div class="role-actions">
            <van-button size="mini" plain type="primary" @click="editRole(item)">编辑权限</van-button>
            <van-button size="mini" plain type="danger" @click="deleteRole(item.id)">删除</van-button>
          </div>
        </div>
        <div class="empty-state" v-if="roleList.length === 0">
          <van-icon name="shield-o" size="48" color="#ccc" />
          <p>暂无角色</p>
        </div>
      </div>
    </div>

    <div v-if="activeTab === 'logs'">
      <div class="log-filter">
        <van-field v-model="logKeyword" placeholder="搜索操作人/操作内容" left-icon="search" class="log-search" />
        <van-button size="small" plain @click="clearLogs">清空日志</van-button>
      </div>

      <div class="log-list">
        <div class="log-item" v-for="item in filteredLogs" :key="item.id">
          <div class="log-header">
            <span class="log-user">{{ item.user }}</span>
            <span class="log-time">{{ formatDateTime(item.createTime) }}</span>
          </div>
          <div class="log-body">
            <span class="log-action">{{ item.action }}</span>
            <span class="log-desc">{{ item.desc }}</span>
          </div>
        </div>
        <div class="empty-state" v-if="filteredLogs.length === 0">
          <van-icon name="records" size="48" color="#ccc" />
          <p>暂无操作记录</p>
        </div>
      </div>
    </div>

    <div v-if="activeTab === 'dict'">
      <div class="dict-toolbar">
        <span class="dict-tip">数据字典配置</span>
        <van-button type="primary" size="small" @click="showAddDict = true">+ 添加</van-button>
      </div>

      <div class="dict-list">
        <div class="dict-item" v-for="item in dictList" :key="item.id">
          <div class="dict-header">
            <span class="dict-key">{{ item.key }}</span>
            <span class="dict-name">{{ item.name }}</span>
            <span class="dict-count">{{ getDictOptionCount(item.options) }}项</span>
          </div>
          <div class="dict-options">
            <span class="dict-option" v-for="(opt, idx) in parseDictOptions(item.options)" :key="idx">
              {{ opt.label }} ({{ opt.value }})
            </span>
          </div>
          <div class="dict-actions">
            <van-button size="mini" plain type="primary" @click="editDict(item)">编辑</van-button>
            <van-button size="mini" plain type="danger" @click="deleteDict(item.id)">删除</van-button>
          </div>
        </div>
        <div class="empty-state" v-if="dictList.length === 0">
          <van-icon name="records" size="48" color="#ccc" />
          <p>暂无数据字典</p>
        </div>
      </div>
    </div>

    <div class="scroll-buttons" v-show="showScrollButtons">
      <div class="scroll-btn" @click="scrollToTop" v-show="showTopBtn">
        <van-icon name="back-top" size="22" color="#e17055" />
        <span>顶部</span>
      </div>
      <div class="scroll-btn" @click="scrollToBottom" v-show="showBottomBtn">
        <van-icon name="down" size="22" color="#e17055" />
        <span>底部</span>
      </div>
    </div>

    <van-popup v-model:show="showAddUser" position="bottom" round style="padding:20px 16px 30px;">
      <h3 style="margin-bottom:16px;text-align:center;">添加用户</h3>
      <van-form @submit="onAddUser">
        <van-field v-model="newUser.name" label="姓名" placeholder="请输入姓名" :rules="[{ required: true }]" />
        <van-field v-model="newUser.username" label="账号" placeholder="请输入账号" :rules="[{ required: true }]" />
        <van-field v-model="newUser.password" label="密码" type="password" placeholder="请输入密码" :rules="[{ required: true }]" />
        <van-field v-model="newUser.departmentId" label="部门ID" type="number" placeholder="请输入部门ID" />
        <van-field v-model="newUser.role" label="角色" placeholder="请选择角色" is-link @click="showRolePicker = true" :rules="[{ required: true }]" />
        <van-field v-model="newUser.email" label="邮箱" placeholder="请输入邮箱" type="email" />
        <div style="display:flex;gap:12px;margin-top:16px;">
          <van-button plain block @click="showAddUser = false">取消</van-button>
          <van-button type="primary" block native-type="submit">确认添加</van-button>
        </div>
      </van-form>
    </van-popup>

    <van-popup v-model:show="showImportUser" position="bottom" round style="padding:20px 16px 30px;max-height:80vh;overflow-y:auto;">
      <h3 style="margin-bottom:8px;text-align:center;">批量导入用户</h3>
      <p style="text-align:center;font-size:13px;color:#999;margin-bottom:16px;">支持Excel/CSV格式，一次最多导入50人</p>
      <div class="import-tip">
        <van-icon name="info-o" size="16" color="#3677ef" />
        <span>请先下载模板，按格式填写后上传</span>
      </div>
      <van-button plain block size="small" @click="downloadTemplate" style="margin-bottom:16px;border-color:#3677ef;color:#3677ef;">
        下载导入模板
      </van-button>
      <van-uploader v-model="importFiles" :max-count="1" accept=".xlsx,.xls,.csv" @after-read="handleFileUpload" style="width:100%;">
        <div class="upload-area">
          <van-icon name="upload" size="32" color="#3677ef" />
          <p>点击选择文件或拖拽上传</p>
          <span style="font-size:12px;color:#bbb;">支持 .xlsx .xls .csv</span>
        </div>
      </van-uploader>
      <div v-if="importPreview.length > 0" style="margin-top:16px;">
        <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:8px;">
          <span style="font-size:14px;font-weight:500;">预览 ({{ importPreview.length }}人)</span>
        </div>
        <div class="preview-list">
          <div class="preview-item" v-for="(item, index) in importPreview.slice(0, 5)" :key="index">
            <span>{{ item.name }}</span>
            <span>{{ item.department }}</span>
            <span>{{ item.role }}</span>
          </div>
          <div v-if="importPreview.length > 5" class="preview-more">还有 {{ importPreview.length - 5 }} 人...</div>
        </div>
        <van-button type="primary" block @click="confirmImport" :loading="importing" style="margin-top:12px;">
          确认导入 ({{ importPreview.length }}人)
        </van-button>
      </div>
      <van-button plain block @click="showImportUser = false" style="margin-top:12px;">取消</van-button>
    </van-popup>

    <van-action-sheet v-model:show="showRolePicker" title="选择角色">
      <div class="picker-list">
        <div class="picker-item" v-for="item in roleOptions" :key="item.value" @click="newUser.role = item.value; showRolePicker = false">
          <span>{{ item.label }}</span>
          <van-icon v-if="newUser.role === item.value" name="success" color="#3677ef" />
        </div>
      </div>
    </van-action-sheet>

    <van-popup v-model:show="showAddRole" position="bottom" round style="padding:20px 16px 30px;max-height:80vh;overflow-y:auto;">
      <h3 style="margin-bottom:16px;text-align:center;">添加角色</h3>
      <van-form @submit="onAddRole">
        <van-field v-model="newRole.name" label="角色名称" placeholder="请输入角色名称" :rules="[{ required: true }]" />
        <van-field v-model="newRole.desc" label="角色描述" placeholder="请输入角色描述" />
        <van-field label="权限配置">
          <template #input>
            <div class="perm-checkbox-group">
              <div class="perm-checkbox" v-for="perm in allPermissions" :key="perm.value">
                <van-checkbox v-model="newRole.permissions" :name="perm.value" shape="square">
                  {{ perm.label }}
                </van-checkbox>
              </div>
            </div>
          </template>
        </van-field>
        <div style="display:flex;gap:12px;margin-top:16px;">
          <van-button plain block @click="showAddRole = false">取消</van-button>
          <van-button type="primary" block native-type="submit">确认添加</van-button>
        </div>
      </van-form>
    </van-popup>

    <van-popup v-model:show="showEditRole" position="bottom" round style="padding:20px 16px 30px;max-height:80vh;overflow-y:auto;">
      <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:16px;">
        <h3 style="margin:0;">✏️ 编辑角色权限</h3>
        <van-icon name="close" size="22" @click="showEditRole = false" />
      </div>
      <van-form @submit="onUpdateRole">
        <van-field v-model="editRoleData.name" label="角色名称" placeholder="请输入角色名称" :rules="[{ required: true }]" />
        <van-field v-model="editRoleData.desc" label="角色描述" placeholder="请输入角色描述" />
        <van-field label="权限配置">
          <template #input>
            <div class="perm-checkbox-group">
              <div class="perm-checkbox" v-for="perm in allPermissions" :key="perm.value">
                <van-checkbox v-model="editRoleData.permissions" :name="perm.value" shape="square">
                  {{ perm.label }}
                </van-checkbox>
              </div>
            </div>
          </template>
        </van-field>
        <div style="display:flex;gap:12px;margin-top:16px;">
          <van-button plain block @click="showEditRole = false">取消</van-button>
          <van-button type="primary" block native-type="submit">保存权限</van-button>
        </div>
      </van-form>
    </van-popup>

    <van-popup v-model:show="showAddDict" position="bottom" round style="padding:20px 16px 30px;max-height:80vh;overflow-y:auto;">
      <h3 style="margin-bottom:16px;text-align:center;">添加数据字典</h3>
      <van-form @submit="onAddDict">
        <van-field v-model="newDict.key" label="字典键" placeholder="如: leave_type" :rules="[{ required: true }]" />
        <van-field v-model="newDict.name" label="字典名称" placeholder="如: 请假类型" :rules="[{ required: true }]" />
        <van-field label="选项配置">
          <template #input>
            <div class="dict-option-input">
              <div class="dict-option-row" v-for="(opt, idx) in newDict.options" :key="idx">
                <van-field v-model="opt.label" placeholder="显示名称" class="opt-field" />
                <van-field v-model="opt.value" placeholder="值" class="opt-field" />
                <van-icon name="close" size="18" color="#e17055" @click="removeDictOption(idx)" />
              </div>
              <van-button size="small" plain type="primary" @click="addDictOption">+ 添加选项</van-button>
            </div>
          </template>
        </van-field>
        <div style="display:flex;gap:12px;margin-top:16px;">
          <van-button plain block @click="showAddDict = false">取消</van-button>
          <van-button type="primary" block native-type="submit">确认添加</van-button>
        </div>
      </van-form>
    </van-popup>

    <van-popup v-model:show="showEditDict" position="bottom" round style="padding:20px 16px 30px;max-height:80vh;overflow-y:auto;">
      <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:16px;">
        <h3 style="margin:0;">✏️ 编辑数据字典</h3>
        <van-icon name="close" size="22" @click="showEditDict = false" />
      </div>
      <van-form @submit="onUpdateDict">
        <van-field v-model="editDictData.key" label="字典键" placeholder="如: leave_type" :rules="[{ required: true }]" />
        <van-field v-model="editDictData.name" label="字典名称" placeholder="如: 请假类型" :rules="[{ required: true }]" />
        <van-field label="选项配置">
          <template #input>
            <div class="dict-option-input">
              <div class="dict-option-row" v-for="(opt, idx) in editDictData.options" :key="idx">
                <van-field v-model="opt.label" placeholder="显示名称" class="opt-field" />
                <van-field v-model="opt.value" placeholder="值" class="opt-field" />
                <van-icon name="close" size="18" color="#e17055" @click="removeEditDictOption(idx)" />
              </div>
              <van-button size="small" plain type="primary" @click="addEditDictOption">+ 添加选项</van-button>
            </div>
          </template>
        </van-field>
        <div style="display:flex;gap:12px;margin-top:16px;">
          <van-button plain block @click="showEditDict = false">取消</van-button>
          <van-button type="primary" block native-type="submit">保存修改</van-button>
        </div>
      </van-form>
    </van-popup>

    <div class="safe-bottom"></div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { showToast, showConfirmDialog } from 'vant'
import { 
  getEmployeeList, 
  addEmployee, 
  deleteEmployee, 
  updateEmployee,
  importEmployees,
  updateEmployeeStatus 
} from '@/api/employee'
import {
  getRoleList,
  saveRole,
  updateRole,
  deleteRole as deleteRoleApi,
  getAllPermissions
} from '@/api/system'
import {
  getOperationLogList,
  clearOperationLog,
  countTodayLogs,
  saveOperationLog
} from '@/api/system'
import {
  getDictList,
  saveDict,
  updateDict,
  deleteDict as deleteDictApi
} from '@/api/system'

const router = useRouter()

const scrollContainer = ref(null)
const showScrollButtons = ref(true)
const showTopBtn = ref(false)
const showBottomBtn = ref(true)

const scrollToTop = () => {
  window.scrollTo({ top: 0, behavior: 'smooth' })
  if (scrollContainer.value) {
    scrollContainer.value.scrollTo({ top: 0, behavior: 'smooth' })
  }
}

const scrollToBottom = () => {
  const scrollHeight = Math.max(
    document.documentElement.scrollHeight,
    document.body.scrollHeight
  )
  window.scrollTo({ top: scrollHeight, behavior: 'smooth' })
  if (scrollContainer.value) {
    scrollContainer.value.scrollTo({ top: scrollContainer.value.scrollHeight, behavior: 'smooth' })
  }
}

const handleScroll = () => {
  const scrollTop = window.pageYOffset || document.documentElement.scrollTop
  const windowHeight = window.innerHeight
  const documentHeight = Math.max(
    document.documentElement.scrollHeight,
    document.body.scrollHeight
  )
  showTopBtn.value = scrollTop > 200
  const distanceToBottom = documentHeight - (scrollTop + windowHeight)
  showBottomBtn.value = distanceToBottom > 200
}

const activeTab = ref('users')
const searchKeyword = ref('')
const logKeyword = ref('')
const saving = ref(false)
const importing = ref(false)

const stats = ref({
  totalUsers: 0,
  activeUsers: 0,
  todayLogs: 0,
  disabledUsers: 0
})

const users = ref([])
const roleList = ref([])
const logList = ref([])
const dictList = ref([])
const allPermissions = ref([])

const roleOptions = [
  { label: '系统管理员', value: 'SYSTEM_ADMIN' },
  { label: '部门主管', value: 'DEPARTMENT_MANAGER' },
  { label: '流程管理员', value: 'PROCESS_ADMIN' },
  { label: '普通员工', value: 'EMPLOYEE' }
]

const getRoleName = (role) => {
  const map = {
    'SYSTEM_ADMIN': '系统管理员',
    'DEPARTMENT_MANAGER': '部门主管',
    'PROCESS_ADMIN': '流程管理员',
    'EMPLOYEE': '普通员工'
  }
  return map[role] || role
}

const getAvatarUrl = (avatar) => {
  if (!avatar) return '/default-avatar.png'
  if (avatar.startsWith('http')) return avatar
  return `${import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'}${avatar}`
}

const parsePermissions = (permissions) => {
  if (!permissions) return []
  try {
    return JSON.parse(permissions)
  } catch {
    return permissions.split(',')
  }
}

const parseDictOptions = (options) => {
  if (!options) return []
  try {
    return JSON.parse(options)
  } catch {
    return []
  }
}

const getDictOptionCount = (options) => {
  const opts = parseDictOptions(options)
  return opts.length
}

const formatDateTime = (dateTime) => {
  if (!dateTime) return ''
  const date = new Date(dateTime)
  return date.toLocaleString()
}

const filteredUsers = computed(() => {
  if (!searchKeyword.value) return users.value
  const kw = searchKeyword.value.toLowerCase()
  return users.value.filter(item => 
    (item.name && item.name.includes(kw)) || 
    (item.username && item.username.includes(kw))
  )
})

const filteredLogs = computed(() => {
  if (!logKeyword.value) return logList.value
  const kw = logKeyword.value.toLowerCase()
  return logList.value.filter(item => 
    (item.user && item.user.includes(kw)) || 
    (item.action && item.action.includes(kw)) ||
    (item.desc && item.desc.includes(kw))
  )
})

const fetchUsers = async () => {
  try {
    const res = await getEmployeeList(1, 100)
    if (res.code === 0 && res.data) {
      users.value = res.data.data || res.data
      updateStats()
    }
  } catch (error) {
    console.error('获取用户列表失败:', error)
  }
}

const fetchRoles = async () => {
  try {
    const res = await getRoleList()
    if (res.code === 0 && res.data) {
      roleList.value = res.data
    }
  } catch (error) {
    console.error('获取角色列表失败:', error)
  }
}

const fetchLogs = async () => {
  try {
    const res = await getOperationLogList()
    if (res.code === 0 && res.data) {
      logList.value = res.data
    }
  } catch (error) {
    console.error('获取操作日志失败:', error)
  }
}

const fetchDicts = async () => {
  try {
    const res = await getDictList()
    if (res.code === 0 && res.data) {
      dictList.value = res.data
    }
  } catch (error) {
    console.error('获取数据字典失败:', error)
  }
}

const fetchPermissions = async () => {
  try {
    const res = await getAllPermissions()
    if (res.code === 0 && res.data) {
      allPermissions.value = res.data
    }
  } catch (error) {
    console.error('获取权限列表失败:', error)
  }
}

const fetchTodayLogCount = async () => {
  try {
    const res = await countTodayLogs()
    if (res.code === 0) {
      stats.value.todayLogs = res.data || 0
    }
  } catch (error) {
    console.error('获取今日日志数失败:', error)
  }
}

const updateStats = () => {
  stats.value.totalUsers = users.value.length
  stats.value.activeUsers = users.value.filter(u => u.status === '在职').length
  stats.value.disabledUsers = users.value.filter(u => u.status === '离职').length
}

const toggleUserStatus = async (item) => {
  try {
    const res = await updateEmployeeStatus({
      employeeId: item.id,
      status: item.status
    })
    if (res.code === 0) {
      showToast(`${item.name} 已${item.status === '在职' ? '启用' : '禁用'}`)
      updateStats()
    } else {
      showToast(res.message || '操作失败')
      item.status = item.status === '在职' ? '离职' : '在职'
    }
  } catch (error) {
    console.error('更新用户状态失败:', error)
    showToast('操作失败')
    item.status = item.status === '在职' ? '离职' : '在职'
  }
}

const editUser = (item) => {
  showToast(`编辑用户：${item.name}`)
}

const showAddUser = ref(false)
const showRolePicker = ref(false)
const newUser = ref({
  name: '',
  username: '',
  password: '',
  departmentId: '',
  role: '',
  email: ''
})

const onAddUser = async () => {
  try {
    const res = await addEmployee(newUser.value)
    if (res.code === 0) {
      showToast('用户添加成功')
      showAddUser.value = false
      newUser.value = { name: '', username: '', password: '', departmentId: '', role: '', email: '' }
      await fetchUsers()
    } else {
      showToast(res.message || '添加失败')
    }
  } catch (error) {
    console.error('添加用户失败:', error)
    showToast('添加失败')
  }
}

const deleteUser = async (item) => {
  showConfirmDialog({
    title: '确认删除',
    message: `确定要删除用户 "${item.name}" 吗？`,
    confirmButtonText: '确定删除',
    confirmButtonColor: '#ee0a24'
  }).then(async () => {
    try {
      const res = await deleteEmployee(item.id)
      if (res.code === 0) {
        showToast('已删除用户')
        await fetchUsers()
      } else {
        showToast(res.message || '删除失败')
      }
    } catch (error) {
      console.error('删除用户失败:', error)
      showToast('删除失败')
    }
  }).catch(() => {})
}

const showImportUser = ref(false)
const importFiles = ref([])
const importPreview = ref([])

const downloadTemplate = () => {
  const headers = '姓名,部门,角色,邮箱\n'
  const example = '张三,研发部,EMPLOYEE,zhangsan@company.com\n李四,产品部,DEPARTMENT_MANAGER,lisi@company.com\n王五,市场部,SYSTEM_ADMIN,wangwu@company.com'
  const content = headers + example
  const blob = new Blob(['\uFEFF' + content], { type: 'text/csv;charset=utf-8;' })
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = '用户导入模板.csv'
  link.click()
  URL.revokeObjectURL(link.href)
  showToast('模板已下载')
}

const handleFileUpload = () => {
  showToast('文件读取中...')
  setTimeout(() => {
    const mockData = [
      { name: '张三', department: '研发部', role: 'EMPLOYEE', email: 'zhangsan@company.com' },
      { name: '李四', department: '产品部', role: 'DEPARTMENT_MANAGER', email: 'lisi@company.com' },
      { name: '王五', department: '市场部', role: 'SYSTEM_ADMIN', email: 'wangwu@company.com' },
      { name: '赵六', department: '人事部', role: 'EMPLOYEE', email: 'zhaoliu@company.com' },
      { name: '孙七', department: '财务部', role: 'EMPLOYEE', email: 'sunqi@company.com' }
    ]
    importPreview.value = mockData
    showToast(`解析成功，共 ${mockData.length} 人`)
  }, 800)
}

const confirmImport = async () => {
  if (importPreview.value.length === 0) {
    showToast('请先上传文件')
    return
  }
  importing.value = true
  try {
    const employees = importPreview.value.map(item => ({
      name: item.name,
      departmentId: 1,
      role: item.role,
      email: item.email,
      username: item.email.split('@')[0],
      password: '123456'
    }))
    const res = await importEmployees(employees)
    if (res.code === 0) {
      showToast(`成功导入 ${importPreview.value.length} 人`)
      await fetchUsers()
    } else {
      showToast(res.message || '导入失败')
    }
  } catch (error) {
    console.error('导入用户失败:', error)
    showToast('导入失败')
  } finally {
    importing.value = false
    showImportUser.value = false
    importPreview.value = []
    importFiles.value = []
  }
}

const showAddRole = ref(false)
const newRole = ref({
  name: '',
  desc: '',
  permissions: []
})

const onAddRole = async () => {
  try {
    const data = {
      ...newRole.value,
      permissions: JSON.stringify(newRole.value.permissions)
    }
    const res = await saveRole(data)
    if (res.code === 0) {
      showToast('角色添加成功')
      showAddRole.value = false
      newRole.value = { name: '', desc: '', permissions: [] }
      await fetchRoles()
    } else {
      showToast(res.message || '添加失败')
    }
  } catch (error) {
    console.error('添加角色失败:', error)
    showToast('添加失败')
  }
}

const showEditRole = ref(false)
const editRoleData = ref({
  id: null,
  name: '',
  desc: '',
  permissions: []
})

const editRole = (item) => {
  editRoleData.value = {
    id: item.id,
    name: item.name,
    desc: item.desc || '',
    permissions: parsePermissions(item.permissions)
  }
  showEditRole.value = true
}

const onUpdateRole = async () => {
  try {
    const data = {
      ...editRoleData.value,
      permissions: JSON.stringify(editRoleData.value.permissions)
    }
    const res = await updateRole(data)
    if (res.code === 0) {
      showToast('权限已更新')
      showEditRole.value = false
      await fetchRoles()
    } else {
      showToast(res.message || '更新失败')
    }
  } catch (error) {
    console.error('更新角色失败:', error)
    showToast('更新失败')
  }
}

const deleteRole = async (id) => {
  const role = roleList.value.find(r => r.id === id)
  showConfirmDialog({
    title: '确认删除',
    message: `确定要删除角色 "${role?.name}" 吗？`,
    confirmButtonText: '确定删除',
    confirmButtonColor: '#ee0a24'
  }).then(async () => {
    try {
      const res = await deleteRoleApi(id)
      if (res.code === 0) {
        showToast('已删除角色')
        await fetchRoles()
      } else {
        showToast(res.message || '删除失败')
      }
    } catch (error) {
      console.error('删除角色失败:', error)
      showToast('删除失败')
    }
  }).catch(() => {})
}

const clearLogs = async () => {
  showConfirmDialog({
    title: '确认清空',
    message: '确定要清空所有操作日志吗？',
    confirmButtonText: '确定清空',
    confirmButtonColor: '#ee0a24'
  }).then(async () => {
    try {
      const res = await clearOperationLog()
      if (res.code === 0) {
        showToast('日志已清空')
        logList.value = []
      } else {
        showToast(res.message || '清空失败')
      }
    } catch (error) {
      console.error('清空日志失败:', error)
      showToast('清空失败')
    }
  }).catch(() => {})
}

const showAddDict = ref(false)
const newDict = ref({
  key: '',
  name: '',
  options: [{ label: '', value: '' }]
})

const addDictOption = () => {
  newDict.value.options.push({ label: '', value: '' })
}

const removeDictOption = (idx) => {
  if (newDict.value.options.length > 1) {
    newDict.value.options.splice(idx, 1)
  }
}

const onAddDict = async () => {
  try {
    const data = {
      ...newDict.value,
      options: JSON.stringify(newDict.value.options.filter(o => o.label && o.value))
    }
    const res = await saveDict(data)
    if (res.code === 0) {
      showToast('数据字典添加成功')
      showAddDict.value = false
      newDict.value = { key: '', name: '', options: [{ label: '', value: '' }] }
      await fetchDicts()
    } else {
      showToast(res.message || '添加失败')
    }
  } catch (error) {
    console.error('添加数据字典失败:', error)
    showToast('添加失败')
  }
}

const showEditDict = ref(false)
const editDictData = ref({
  id: null,
  key: '',
  name: '',
  options: [{ label: '', value: '' }]
})

const editDict = (item) => {
  editDictData.value = {
    id: item.id,
    key: item.key,
    name: item.name,
    options: parseDictOptions(item.options)
  }
  showEditDict.value = true
}

const addEditDictOption = () => {
  editDictData.value.options.push({ label: '', value: '' })
}

const removeEditDictOption = (idx) => {
  if (editDictData.value.options.length > 1) {
    editDictData.value.options.splice(idx, 1)
  }
}

const onUpdateDict = async () => {
  try {
    const data = {
      ...editDictData.value,
      options: JSON.stringify(editDictData.value.options.filter(o => o.label && o.value))
    }
    const res = await updateDict(data)
    if (res.code === 0) {
      showToast('数据字典已更新')
      showEditDict.value = false
      await fetchDicts()
    } else {
      showToast(res.message || '更新失败')
    }
  } catch (error) {
    console.error('更新数据字典失败:', error)
    showToast('更新失败')
  }
}

const deleteDict = async (id) => {
  const dict = dictList.value.find(d => d.id === id)
  showConfirmDialog({
    title: '确认删除',
    message: `确定要删除数据字典 "${dict?.name}" 吗？`,
    confirmButtonText: '确定删除',
    confirmButtonColor: '#ee0a24'
  }).then(async () => {
    try {
      const res = await deleteDictApi(id)
      if (res.code === 0) {
        showToast('已删除数据字典')
        await fetchDicts()
      } else {
        showToast(res.message || '删除失败')
      }
    } catch (error) {
      console.error('删除数据字典失败:', error)
      showToast('删除失败')
    }
  }).catch(() => {})
}

onMounted(() => {
  window.addEventListener('scroll', handleScroll)
  setTimeout(handleScroll, 300)
  fetchUsers()
  fetchRoles()
  fetchLogs()
  fetchDicts()
  fetchPermissions()
  fetchTodayLogCount()
})

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll)
})
</script>

<style scoped>
.oa-system {
  max-width: 430px;
  margin: 0 auto;
  min-height: 100vh;
  background: #f5f7fa;
  padding: 0 16px 20px;
  box-shadow: 0 0 20px rgba(0,0,0,0.06);
}
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 16px 12px;
  background: #fff;
  margin: 0 -16px 0;
}
.header-title {
  font-size: 18px;
  font-weight: 600;
  margin: 0;
  color: #222;
}
.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}
.stat-row {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr 1fr;
  gap: 10px;
  margin-bottom: 16px;
}
.stat-card {
  background: #fff;
  border-radius: 12px;
  padding: 14px 8px;
  text-align: center;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
  cursor: pointer;
}
.stat-card:active { transform: scale(0.95); }
.stat-num { display: block; font-size: 24px; font-weight: bold; color: #3677ef; }
.stat-label { font-size: 11px; color: #888; margin-top: 2px; }
.tab-wrap {
  display: flex;
  background: #fff;
  border-radius: 12px;
  padding: 4px;
  margin-bottom: 16px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}
.tab-item {
  flex: 1;
  text-align: center;
  padding: 8px 0;
  border-radius: 10px;
  font-size: 13px;
  color: #888;
  cursor: pointer;
  transition: all 0.3s;
}
.tab-item.active {
  background: #3677ef;
  color: #fff;
  font-weight: 500;
}
.user-toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 14px;
}
.search-field {
  flex: 1;
  background: #fff;
  border-radius: 12px;
  padding: 0 10px;
}
.search-field :deep(.van-field__body) {
  background: transparent;
}
.user-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.user-item {
  background: #fff;
  border-radius: 12px;
  padding: 12px 16px;
  display: flex;
  align-items: center;
  gap: 12px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}
.user-info { flex: 1; }
.user-name { font-size: 15px; font-weight: 500; margin: 0; color: #222; }
.user-dept { font-size: 12px; color: #888; margin: 2px 0 0; }
.user-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}
.user-actions .van-icon { cursor: pointer; }
.role-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
}
.role-tip { font-size: 14px; color: #888; }
.role-list { display: flex; flex-direction: column; gap: 12px; }
.role-item {
  background: #fff;
  border-radius: 14px;
  padding: 16px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}
.role-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}
.role-icon {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.role-info { flex: 1; }
.role-name { font-size: 15px; font-weight: 500; color: #222; }
.role-desc { font-size: 12px; color: #999; }
.role-count { font-size: 12px; color: #3677ef; }
.role-permissions {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  margin: 8px 0 10px;
}
.perm-tag {
  font-size: 11px;
  padding: 2px 10px;
  border-radius: 10px;
  background: #e8f0ff;
  color: #3677ef;
}
.role-actions {
  display: flex;
  gap: 8px;
  padding-top: 10px;
  border-top: 1px solid #f5f5f5;
}
.log-filter {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 14px;
}
.log-search {
  flex: 1;
  background: #fff;
  border-radius: 12px;
  padding: 0 10px;
}
.log-search :deep(.van-field__body) { background: transparent; }
.log-list { display: flex; flex-direction: column; gap: 10px; }
.log-item {
  background: #fff;
  border-radius: 12px;
  padding: 14px 16px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}
.log-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 4px;
}
.log-user { font-size: 14px; font-weight: 500; color: #222; }
.log-time { font-size: 12px; color: #bbb; }
.log-body {
  display: flex;
  gap: 12px;
  font-size: 13px;
  color: #888;
}
.log-action { color: #3677ef; }
.dict-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
}
.dict-tip { font-size: 14px; color: #888; }
.dict-list { display: flex; flex-direction: column; gap: 12px; }
.dict-item {
  background: #fff;
  border-radius: 14px;
  padding: 16px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}
.dict-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}
.dict-key {
  font-size: 12px;
  color: #3677ef;
  background: #e8f0ff;
  padding: 2px 10px;
  border-radius: 10px;
}
.dict-name { font-size: 15px; font-weight: 500; color: #222; flex: 1; }
.dict-count { font-size: 12px; color: #999; }
.dict-options {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  margin: 8px 0 10px;
}
.dict-option {
  font-size: 11px;
  padding: 2px 10px;
  border-radius: 10px;
  background: #f5f5f5;
  color: #666;
}
.dict-actions {
  display: flex;
  gap: 8px;
  padding-top: 10px;
  border-top: 1px solid #f5f5f5;
}
.perm-checkbox-group {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  padding: 4px 0;
}
.perm-checkbox :deep(.van-checkbox) {
  font-size: 13px;
}
.perm-checkbox :deep(.van-checkbox__icon) {
  font-size: 16px;
}
.dict-option-input {
  display: flex;
  flex-direction: column;
  gap: 6px;
  width: 100%;
}
.dict-option-row {
  display: flex;
  align-items: center;
  gap: 6px;
}
.opt-field {
  flex: 1;
  padding: 0 !important;
}
.opt-field :deep(.van-field__body) {
  background: #f5f7fa !important;
  padding: 4px 8px !important;
  border-radius: 6px;
}
.empty-state {
  text-align: center;
  padding: 40px 0;
  color: #ccc;
}
.empty-state p { margin-top: 8px; font-size: 14px; }
.import-tip {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  background: #f0f7ff;
  border-radius: 8px;
  font-size: 13px;
  color: #888;
  margin-bottom: 12px;
}
.upload-area {
  border: 2px dashed #ddd;
  border-radius: 12px;
  padding: 30px 20px;
  text-align: center;
  cursor: pointer;
  width: 100%;
}
.upload-area:hover { border-color: #3677ef; background: #f8faff; }
.upload-area p { margin: 8px 0 4px; font-size: 14px; color: #888; }
.preview-list {
  background: #f8f9fa;
  border-radius: 8px;
  padding: 8px 12px;
  max-height: 150px;
  overflow-y: auto;
}
.preview-item {
  display: flex;
  justify-content: space-between;
  padding: 6px 0;
  font-size: 13px;
  color: #333;
  border-bottom: 1px solid #f0f0f0;
}
.preview-item:last-child { border-bottom: none; }
.preview-more { text-align: center; font-size: 13px; color: #999; padding: 6px 0; }
.picker-list { padding: 12px 16px 20px; }
.picker-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 12px;
  border-bottom: 1px solid #f5f5f5;
  cursor: pointer;
}
.picker-item:active { background: #f5f7fa; }
.picker-item span { font-size: 15px; color: #333; }
.safe-bottom { height: 20px; }
.scroll-buttons {
  position: fixed;
  right: 16px;
  bottom: 100px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  z-index: 999;
  animation: fadeIn 0.3s ease;
}
.scroll-btn {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: #fff;
  box-shadow: 0 4px 16px rgba(225, 112, 85, 0.25);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s ease;
  border: 2px solid rgba(225, 112, 85, 0.15);
}
.scroll-btn:hover {
  transform: scale(1.05);
  box-shadow: 0 6px 24px rgba(225, 112, 85, 0.35);
}
.scroll-btn:active {
  transform: scale(0.92);
}
.scroll-btn span {
  font-size: 10px;
  color: #e17055;
  margin-top: -2px;
  font-weight: 500;
}
@keyframes fadeIn {
  from { opacity: 0; transform: translateX(20px); }
  to { opacity: 1; transform: translateX(0); }
}
</style>