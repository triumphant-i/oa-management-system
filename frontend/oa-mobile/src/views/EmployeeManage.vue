<template>
  <div class="oa-employee-manage">
    <div class="page-header">
      <van-icon name="arrow-left" size="22" @click="$router.back()" />
      <h1 class="header-title">{{ pageTitle }}</h1>
      <div class="header-right">
        <van-icon name="ellipsis" size="22" />
      </div>
    </div>

    <div class="stat-row">
      <div class="stat-card" @click="filterStatus = 'all'">
        <span class="stat-num">{{ employees.length }}</span>
        <span class="stat-label">总人数</span>
      </div>
      <div class="stat-card" @click="filterStatus = '在职'">
        <span class="stat-num" style="color: #00b894;">{{ getCountByStatus('在职') }}</span>
        <span class="stat-label">在职</span>
      </div>
      <div class="stat-card" @click="filterStatus = '试用期'">
        <span class="stat-num" style="color: #fdcb6e;">{{ getCountByStatus('试用期') }}</span>
        <span class="stat-label">试用期</span>
      </div>
      <div class="stat-card" @click="filterStatus = '已离职'">
        <span class="stat-num" style="color: #e17055;">{{ getCountByStatus('已离职') }}</span>
        <span class="stat-label">已离职</span>
      </div>
    </div>

    <div class="toolbar">
      <div class="search-wrap">
        <van-field 
          v-model="searchKeyword" 
          placeholder="搜索姓名/部门" 
          left-icon="search" 
          class="search-field"
          @input="onSearch"
        />
      </div>
      <div class="toolbar-right" v-if="role === 'SYSTEM_ADMIN'">
        <van-button type="primary" size="small" @click="openAddModal">+ 添加</van-button>
      </div>
      <div class="toolbar-right" v-else-if="role === 'DEPARTMENT_MANAGER'">
        <van-button type="primary" size="small" @click="openAddModal">+ 添加</van-button>
      </div>
    </div>

    <div class="filter-tabs">
      <span class="filter-tab" :class="{ active: filterStatus === 'all' }" @click="filterStatus = 'all'">全部</span>
      <span class="filter-tab" :class="{ active: filterStatus === '在职' }" @click="filterStatus = '在职'">在职</span>
      <span class="filter-tab" :class="{ active: filterStatus === '试用期' }" @click="filterStatus = '试用期'">试用期</span>
      <span class="filter-tab" :class="{ active: filterStatus === '已离职' }" @click="filterStatus = '已离职'">已离职</span>
    </div>

    <div v-if="loading" class="loading-wrap">
      <van-loading size="24px">加载中...</van-loading>
    </div>

    <div v-else class="employee-list">
      <div class="employee-item" v-for="item in filteredList" :key="item.id">
        <van-image 
          round 
          width="48" 
          height="48" 
          :src="item.avatar || '/default-avatar.png'" 
          v-if="role === 'SYSTEM_ADMIN'"
          @click="openAvatarUpload(item)"
        />
        <van-image 
          round 
          width="48" 
          height="48" 
          :src="item.avatar || '/default-avatar.png'" 
          v-else
        />
        <div class="emp-info">
          <div class="emp-name-row">
            <span class="emp-name">{{ item.name }}</span>
            <van-tag :type="item.status === '在职' ? 'success' : item.status === '试用期' ? 'warning' : 'danger'" size="small">
              {{ item.status }}
            </van-tag>
          </div>
          <span class="emp-dept">{{ getDeptName(item.departmentId) }} · {{ item.position }}</span>
          <span class="emp-phone">{{ item.phone }}</span>
        </div>
        <div class="emp-actions" v-if="role === 'SYSTEM_ADMIN' || role === 'DEPARTMENT_MANAGER'">
          <van-icon name="edit" size="18" color="#3677ef" @click="openEditModal(item)" />
          <van-icon name="delete" size="18" color="#e17055" @click="handleDelete(item)" />
        </div>
        <div class="emp-actions" v-else>
          <span class="readonly-tag">只读</span>
        </div>
      </div>
      <div class="empty-state" v-if="filteredList.length === 0">
        <van-icon name="friends-o" size="48" color="#ccc" />
        <p>{{ role === 'DEPARTMENT_MANAGER' ? '本部门暂无员工' : '暂无员工' }}</p>
      </div>
    </div>

    <div class="safe-bottom"></div>

    <van-popup 
      v-if="role === 'SYSTEM_ADMIN' || role === 'DEPARTMENT_MANAGER'"
      v-model:show="showModal" 
      position="bottom" 
      round 
      style="padding:20px 16px 30px;max-height:85vh;overflow-y:auto;"
    >
      <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:16px;">
        <h3 style="margin:0;">{{ isEdit ? '编辑员工' : '添加员工' }}</h3>
        <van-icon name="close" size="22" @click="showModal = false" />
      </div>

      <div class="avatar-upload-wrap" @click="triggerFileUpload">
        <van-image 
          round 
          width="72" 
          height="72" 
          :src="formData.avatar || '/default-avatar.png'" 
        />
        <div class="avatar-upload-hint">
          <van-icon name="photograph" size="18" color="#3677ef" />
          <span>点击上传头像</span>
        </div>
        <input 
          type="file" 
          ref="fileInput" 
          accept="image/*" 
          style="display:none" 
          @change="handleFileChange"
        />
      </div>

      <van-form @submit="onSubmit">
        <van-cell-group inset>
          <van-field 
            v-model="formData.name" 
            label="姓名" 
            placeholder="请输入姓名"
            :rules="[{ required: true, message: '请输入姓名' }]"
          />
          <van-field 
            v-model="formData.gender" 
            label="性别" 
            placeholder="请选择"
            is-link
            @click="showGenderPicker = true"
            :rules="[{ required: true, message: '请选择性别' }]"
          />
          <van-field 
            v-model="formData.departmentName" 
            label="部门" 
            placeholder="请选择"
            is-link
            @click="showDeptPicker = true"
            :rules="[{ required: true, message: '请选择部门' }]"
          />
          <van-field 
            v-model="formData.position" 
            label="职位" 
            placeholder="请输入职位"
            :rules="[{ required: true, message: '请输入职位' }]"
          />
          <van-field 
            v-model="formData.username" 
            label="账号名" 
            placeholder="请输入登录账号"
            :rules="[
              { required: true, message: '请输入账号名' },
              { minLength: 3, message: '账号名至少3个字符' }
            ]"
          />
          <van-field 
            v-model="formData.phone" 
            label="电话" 
            placeholder="请输入电话"
            :rules="[
              { required: true, message: '请输入电话' },
              { validator: validatePhone, message: '请输入正确的11位手机号' }
            ]"
          />
          <van-field 
            v-model="formData.email" 
            label="邮箱" 
            placeholder="请输入邮箱"
            :rules="[
              { required: true, message: '请输入邮箱' },
              { validator: validateEmail, message: '请输入正确的邮箱格式' }
            ]"
          />
          <van-field 
            v-model="formData.roleName" 
            label="角色" 
            placeholder="请选择"
            is-link
            @click="showRolePicker = true"
            :rules="[{ required: true, message: '请选择角色' }]"
          />
          <van-field 
            v-model="formData.status" 
            label="状态" 
            placeholder="请选择"
            is-link
            @click="showStatusPicker = true"
            :rules="[{ required: true, message: '请选择状态' }]"
          />
          <van-field 
            v-model="formData.joinDate" 
            label="入职日期" 
            placeholder="请选择"
            is-link
            @click="showDatePicker = true"
            :rules="[{ required: true, message: '请选择入职日期' }]"
          />
          <van-field 
            v-model="formData.shiftName" 
            label="班次" 
            placeholder="请选择"
            is-link
            @click="showShiftPicker = true"
          />
        </van-cell-group>

        <div style="display:flex;gap:12px;margin-top:16px;">
          <van-button plain block @click="showModal = false">取消</van-button>
          <van-button type="primary" block native-type="submit" :loading="submitting">
            {{ isEdit ? '保存修改' : '确认添加' }}
          </van-button>
        </div>
      </van-form>
    </van-popup>

    <van-action-sheet v-model:show="showGenderPicker" title="选择性别">
      <div class="picker-list">
        <div class="picker-item" v-for="item in genderOptions" :key="item" @click="formData.gender = item; showGenderPicker = false">
          <span>{{ item }}</span>
          <van-icon v-if="formData.gender === item" name="success" color="#3677ef" />
        </div>
      </div>
    </van-action-sheet>

    <van-action-sheet v-model:show="showDeptPicker" title="选择部门">
      <div class="picker-list">
        <div class="picker-item" v-for="item in availableDepartments" :key="item.id" @click="selectDepartment(item); showDeptPicker = false">
          <span>{{ item.name }}</span>
          <van-icon v-if="formData.departmentId === item.id" name="success" color="#3677ef" />
        </div>
      </div>
    </van-action-sheet>

    <van-action-sheet v-model:show="showStatusPicker" title="选择状态">
      <div class="picker-list">
        <div class="picker-item" v-for="item in statusOptions" :key="item" @click="formData.status = item; showStatusPicker = false">
          <span>{{ item }}</span>
          <van-icon v-if="formData.status === item" name="success" color="#3677ef" />
        </div>
      </div>
    </van-action-sheet>

    <van-action-sheet v-model:show="showRolePicker" title="选择角色">
      <div class="picker-list">
        <div class="picker-item" v-for="item in roleOptions" :key="item.value" @click="selectRole(item); showRolePicker = false">
          <span>{{ item.label }}</span>
          <van-icon v-if="formData.role === item.value" name="success" color="#3677ef" />
        </div>
      </div>
    </van-action-sheet>

    <van-action-sheet v-model:show="showShiftPicker" title="选择班次">
      <div class="picker-list">
        <div class="picker-item" @click="selectShift(null); showShiftPicker = false">
          <span>无班次</span>
          <van-icon v-if="formData.shiftId === null" name="success" color="#3677ef" />
        </div>
        <div class="picker-item" v-for="item in shiftList" :key="item.id" @click="selectShift(item); showShiftPicker = false">
          <div>
            <span>{{ item.name }}</span>
            <span style="font-size:12px;color:#888;margin-left:8px;">{{ formatShiftTime(item.workStart) }} - {{ formatShiftTime(item.workEnd) }}</span>
          </div>
          <van-icon v-if="formData.shiftId === item.id" name="success" color="#3677ef" />
        </div>
      </div>
    </van-action-sheet>

    <van-calendar 
      v-model="dateValue"
      type="date" 
      :show="showDatePicker"
      @confirm="onConfirmDate" 
      @close="showDatePicker = false"
      title="选择入职日期"
    />

    <div class="bottom-bar">
      <van-button plain block size="large" @click="$router.back()" class="back-btn">
        <van-icon name="arrow-left" size="18" /> 返回
      </van-button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showToast, showConfirmDialog, showLoadingToast } from 'vant'
import { 
  getEmployeeList, 
  addEmployee, 
  updateEmployee, 
  deleteEmployee as deleteEmpApi,
  getDepartmentList,
  getAllShiftList
} from '@/api/employee'

const router = useRouter()

const role = ref('EMPLOYEE')
const pageTitle = ref('员工管理')
const loading = ref(false)
const allEmployees = ref([])
const departmentList = ref([])
const userDepartment = ref('研发部')
const userDepartmentId = ref(null)
const shiftList = ref([])

const deptMap = {
  1: '技术部',
  2: '产品部',
  3: '市场部',
  4: '人事部',
  5: '财务部'
}

const getDeptName = (deptId) => {
  return deptMap[deptId] || '未知部门'
}

const employees = computed(() => {
  if (role.value === 'SYSTEM_ADMIN') {
    return allEmployees.value
  }
  if (role.value === 'DEPARTMENT_MANAGER') {
    return allEmployees.value.filter(item => item.departmentId === userDepartmentId.value)
  }
  if (role.value === 'EMPLOYEE') {
    return allEmployees.value.filter(item => item.departmentId === userDepartmentId.value)
  }
  return allEmployees.value
})

const availableDepartments = computed(() => {
  if (role.value === 'SYSTEM_ADMIN') {
    return departmentList.value
  }
  if (role.value === 'DEPARTMENT_MANAGER') {
    return departmentList.value.filter(d => d.id === userDepartmentId.value)
  }
  return []
})

const searchKeyword = ref('')
const filterStatus = ref('all')

const getCountByStatus = (status) => {
  return employees.value.filter(item => item.status === status).length
}

const filteredList = computed(() => {
  let list = [...employees.value]
  
  if (filterStatus.value !== 'all') {
    list = list.filter(item => item.status === filterStatus.value)
  }
  
  if (searchKeyword.value.trim()) {
    const kw = searchKeyword.value.toLowerCase()
    list = list.filter(item => 
      item.name.includes(kw) || 
      getDeptName(item.departmentId).includes(kw) ||
      item.position.includes(kw)
    )
  }
  
  return list
})

const genderOptions = ['男', '女']
const statusOptions = ['在职', '试用期', '已离职']
const roleOptions = [
  { label: '系统管理员', value: 'SYSTEM_ADMIN' },
  { label: '部门主管', value: 'DEPARTMENT_MANAGER' },
  { label: '普通员工', value: 'EMPLOYEE' }
]

// 手机号验证函数
const validatePhone = (val) => {
  if (!val) return false
  // 验证11位手机号，第一位必须是1，第二位必须是3-9
  const phoneRegex = /^1[3-9]\d{9}$/
  return phoneRegex.test(val)
}

// 邮箱验证函数
const validateEmail = (val) => {
  if (!val) return false
  // 严格的邮箱验证规则
  const emailRegex = /^[a-zA-Z0-9](?:[a-zA-Z0-9._%+-]{0,61}[a-zA-Z0-9])?@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z]{2,})+$/
  return emailRegex.test(val)
}

const showModal = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const editId = ref(null)

const formData = ref({
  name: '',
  gender: '',
  departmentId: null,
  departmentName: '',
  position: '',
  phone: '',
  email: '',
  status: '',
  joinDate: '',
  avatar: '',
  shiftId: null,
  shiftName: ''
})

const showGenderPicker = ref(false)
const showDeptPicker = ref(false)
const showStatusPicker = ref(false)
const showDatePicker = ref(false)
const showRolePicker = ref(false)
const showShiftPicker = ref(false)
const dateValue = ref(new Date())

const selectShift = (item) => {
  if (item) {
    formData.value.shiftId = item.id
    formData.value.shiftName = item.name
  } else {
    formData.value.shiftId = null
    formData.value.shiftName = ''
  }
}

const formatShiftTime = (timeStr) => {
  if (!timeStr) return ''
  return timeStr.substring(0, 5)
}

const selectDepartment = (item) => {
  formData.value.departmentId = item.id
  formData.value.departmentName = item.name
}

const selectRole = (item) => {
  formData.value.role = item.value
  formData.value.roleName = item.label
}

const openAddModal = () => {
  if (role.value !== 'SYSTEM_ADMIN' && role.value !== 'DEPARTMENT_MANAGER') {
    showToast('您没有权限')
    return
  }
  isEdit.value = false
  editId.value = null
  const defaultDeptId = userDepartmentId.value || (departmentList.value.length > 0 ? departmentList.value[0].id : null)
  formData.value = {
    name: '',
    gender: '',
    departmentId: defaultDeptId,
    departmentName: getDeptName(defaultDeptId),
    position: '',
    username: '',
    phone: '',
    email: '',
    role: 'EMPLOYEE',
    roleName: '普通员工',
    status: '在职',
    joinDate: '',
    avatar: '',
    shiftId: null,
    shiftName: ''
  }
  showModal.value = true
}

    const openEditModal = (item) => {
  if (role.value !== 'SYSTEM_ADMIN' && role.value !== 'DEPARTMENT_MANAGER') {
    showToast('您没有权限')
    return
  }
  isEdit.value = true
  editId.value = item.id
  const roleObj = roleOptions.find(r => r.value === item.role)
  const shiftObj = shiftList.value.find(s => s.id === item.shiftId)
  formData.value = {
    name: item.name || '',
    gender: item.gender || '',
    departmentId: item.departmentId || null,
    departmentName: getDeptName(item.departmentId) || '',
    position: item.position || '',
    phone: item.phone || '',
    email: item.email || '',
    role: item.role || 'EMPLOYEE',
    roleName: roleObj ? roleObj.label : '普通员工',
    status: item.status || '',
    joinDate: item.joinDate || '',
    avatar: item.avatar || '',
    shiftId: item.shiftId || null,
    shiftName: shiftObj ? shiftObj.name : ''
  }
  showModal.value = true
}

    const onSubmit = async () => {
  if (role.value !== 'SYSTEM_ADMIN' && role.value !== 'DEPARTMENT_MANAGER') return

  if (!formData.value.departmentId) {
    showToast('请选择部门')
    return
  }

  submitting.value = true
  try {
    const { departmentName, roleName, shiftName, ...submitData } = formData.value
    // 使用用户输入的username，不再自动使用phone作为username
    submitData.departmentId = parseInt(formData.value.departmentId)
    
    if (formData.value.shiftId !== null && formData.value.shiftId !== undefined) {
      submitData.shiftId = parseInt(formData.value.shiftId)
    }
    
    if (isEdit.value) {
      submitData.id = editId.value
      const res = await updateEmployee(submitData)
      if (res.code === 0) {
        showToast('员工信息已更新')
      } else {
        showToast(res.message || '更新失败')
      }
    } else {
      submitData.password = '123456'
      const res = await addEmployee(submitData)
      if (res.code === 0) {
        showToast('员工添加成功')
      } else {
        showToast(res.message || '添加失败')
      }
    }
    showModal.value = false
    loadEmployees()
  } catch (error) {
    console.error('操作失败：', error)
    showToast('操作失败')
  } finally {
    submitting.value = false
  }
}

const onConfirmDate = (value) => {
  const date = new Date(value)
  formData.value.joinDate = `${date.getFullYear()}-${String(date.getMonth()+1).padStart(2,'0')}-${String(date.getDate()).padStart(2,'0')}`
  showDatePicker.value = false
}

const fileInput = ref(null)

const triggerFileUpload = () => {
  if (role.value !== 'SYSTEM_ADMIN') {
    showToast('您没有权限')
    return
  }
  fileInput.value.click()
}

const handleFileChange = (event) => {
  const file = event.target.files[0]
  if (file) {
    const reader = new FileReader()
    reader.onload = (e) => {
      formData.value.avatar = e.target.result
      showToast('头像上传成功')
    }
    reader.readAsDataURL(file)
  }
  event.target.value = ''
}

const openAvatarUpload = (item) => {
  if (role.value !== 'SYSTEM_ADMIN') {
    showToast('您没有权限')
    return
  }
  const input = document.createElement('input')
  input.type = 'file'
  input.accept = 'image/*'
  input.onchange = (e) => {
    const file = e.target.files[0]
    if (file) {
      const reader = new FileReader()
      reader.onload = async (ev) => {
        item.avatar = ev.target.result
        await updateEmployee({ id: item.id, avatar: ev.target.result })
        showToast('头像已更新')
      }
      reader.readAsDataURL(file)
    }
  }
  input.click()
}

const handleDelete = (item) => {
  if (role.value !== 'SYSTEM_ADMIN' && role.value !== 'DEPARTMENT_MANAGER') {
    showToast('您没有权限')
    return
  }
  showConfirmDialog({
    title: '确认删除',
    message: `确定要删除员工 "${item.name}" 吗？`,
    confirmButtonText: '确定删除',
    confirmButtonColor: '#ee0a24'
  }).then(async () => {
    try {
      const res = await deleteEmpApi(item.id)
      if (res.code === 0) {
        showToast('已删除员工')
        loadEmployees()
      } else {
        showToast(res.message || '删除失败')
      }
    } catch (error) {
      console.error('删除失败：', error)
      showToast('操作失败')
    }
  }).catch(() => {})
}

const loadEmployees = async () => {
  loading.value = true
  try {
    console.log('EmployeeManage - 开始加载员工列表')
    const res = await getEmployeeList(1, 100)
    console.log('EmployeeManage - 接口返回:', res)
    
    if (res.code === 0) {
      if (res.data && res.data.data) {
        allEmployees.value = res.data.data || []
        console.log('EmployeeManage - 加载成功，共', allEmployees.value.length, '条数据')
      } else if (Array.isArray(res.data)) {
        allEmployees.value = res.data
        console.log('EmployeeManage - 加载成功（数组格式），共', allEmployees.value.length, '条数据')
      } else {
        console.error('EmployeeManage - 数据格式错误:', res.data)
        showToast('数据格式错误')
      }
    } else {
      console.error('EmployeeManage - 接口返回错误:', res.message)
      showToast(res.message || '加载失败')
    }
  } catch (error) {
    console.error('加载员工列表失败：', error)
    showToast('加载失败，请检查网络')
  } finally {
    loading.value = false
  }
}

const loadDepartments = async () => {
  try {
    const res = await getDepartmentList()
    if (res.code === 0 && res.data) {
      departmentList.value = Array.isArray(res.data) ? res.data : []
    }
  } catch (error) {
    console.error('加载部门列表失败：', error)
  }
}

const loadShiftList = async () => {
  try {
    const res = await getAllShiftList()
    if (res.code === 0 && res.data) {
      shiftList.value = Array.isArray(res.data) ? res.data : []
    }
  } catch (error) {
    console.error('加载班次列表失败：', error)
  }
}

onMounted(() => {
  const storedRole = localStorage.getItem('role')
  const roleMap = {
    'SYSTEM_ADMIN': 'SYSTEM_ADMIN',
    'DEPARTMENT_MANAGER': 'DEPARTMENT_MANAGER',
    'PROCESS_ADMIN': 'PROCESS_ADMIN',
    'EMPLOYEE': 'EMPLOYEE'
  }
  role.value = roleMap[storedRole] || 'EMPLOYEE'
  
  const storedDeptId = localStorage.getItem('departmentId')
  userDepartmentId.value = storedDeptId ? parseInt(storedDeptId) : null
  
  console.log('EmployeeManage - 用户角色:', role.value)
  console.log('EmployeeManage - 用户部门ID:', userDepartmentId.value)
  
  if (role.value === 'SYSTEM_ADMIN') {
    pageTitle.value = '员工管理'
  } else if (role.value === 'DEPARTMENT_MANAGER') {
    pageTitle.value = '部门员工'
  } else {
    pageTitle.value = '员工管理'
  }
  
  loadEmployees()
  loadDepartments()
  loadShiftList()
})
</script>

<style scoped>
.oa-employee-manage {
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
  padding: 16px 0 12px;
  background: #fff;
  margin: 0 -16px 0;
  padding: 16px 16px 12px;
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
  margin-bottom: 14px;
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

.toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}
.search-wrap { flex: 1; }
.search-field {
  background: #fff;
  border-radius: 12px;
  padding: 0 10px;
}
.search-field :deep(.van-field__body) { background: transparent; }
.toolbar-right {
  display: flex;
  gap: 6px;
  flex-shrink: 0;
}

.filter-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 14px;
  overflow-x: auto;
}
.filter-tabs::-webkit-scrollbar { display: none; }
.filter-tab {
  padding: 6px 16px;
  border-radius: 20px;
  font-size: 13px;
  color: #888;
  background: #fff;
  white-space: nowrap;
  cursor: pointer;
  box-shadow: 0 2px 4px rgba(0,0,0,0.04);
}
.filter-tab.active { background: #3677ef; color: #fff; }

.loading-wrap { text-align: center; padding: 40px 0; }

.employee-list { display: flex; flex-direction: column; gap: 10px; }
.employee-item {
  background: #fff;
  border-radius: 14px;
  padding: 14px 16px;
  display: flex;
  align-items: center;
  gap: 12px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}
.emp-info { flex: 1; min-width: 0; }
.emp-name-row { display: flex; align-items: center; gap: 8px; }
.emp-name { font-size: 16px; font-weight: 500; color: #222; }
.emp-dept { display: block; font-size: 13px; color: #888; margin-top: 2px; }
.emp-phone { display: block; font-size: 12px; color: #bbb; margin-top: 1px; }
.emp-actions { display: flex; gap: 12px; flex-shrink: 0; align-items: center; }
.emp-actions .van-icon { cursor: pointer; }
.readonly-tag {
  font-size: 11px;
  color: #999;
  background: #f5f5f5;
  padding: 2px 10px;
  border-radius: 10px;
}

.empty-state { text-align: center; padding: 40px 0; color: #ccc; }
.empty-state p { margin-top: 8px; font-size: 14px; }

.avatar-upload-wrap {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 12px 0;
  cursor: pointer;
}
.avatar-upload-hint {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: #3677ef;
}

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

.bottom-bar { padding: 16px 0 8px; }
.back-btn {
  border-radius: 12px !important;
  height: 48px !important;
  font-size: 16px !important;
  border-color: #3677ef !important;
  color: #3677ef !important;
}
.back-btn:active { background: #f0f7ff !important; }

.safe-bottom { height: 20px; }
</style>