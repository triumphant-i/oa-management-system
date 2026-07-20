<template>
  <div class="oa-department">
    <div class="page-header">
      <van-icon name="arrow-left" size="22" @click="$router.back()" />
      <h1 class="header-title">组织架构</h1>
      <div class="header-right">
        <van-icon name="ellipsis" size="22" />
      </div>
    </div>

    <div class="stat-row">
      <div class="stat-card" style="background: linear-gradient(135deg, #fff, #f0f7ff);">
        <span class="stat-num" style="color: #3677ef;">{{ deptList.length }}</span>
        <span class="stat-label">部门总数</span>
      </div>
      <div class="stat-card" style="background: linear-gradient(135deg, #fff, #f0fff4);">
        <span class="stat-num" style="color: #00b894;">{{ totalMembers }}</span>
        <span class="stat-label">总人数</span>
      </div>
      <div class="stat-card" style="background: linear-gradient(135deg, #fff, #fff8ef);" @click="showLevelDialog = true">
        <span class="stat-num" style="color: #fdcb6e;">{{ maxLevel }}</span>
        <span class="stat-label">最大层级</span>
      </div>
    </div>

    <div class="toolbar" v-if="role === 'SYSTEM_ADMIN'">
      <van-button type="primary" size="small" @click="openAddDept">
        <van-icon name="plus" /> 添加部门
      </van-button>
      <van-button type="success" size="small" plain @click="toggleAllExpand">
        <van-icon :name="allExpanded ? 'fold' : 'unfold'" /> {{ allExpanded ? '全部收起' : '全部展开' }}
      </van-button>
    </div>
    <div class="toolbar" v-else>
      <van-button type="success" size="small" plain @click="toggleAllExpand">
        <van-icon :name="allExpanded ? 'fold' : 'unfold'" /> {{ allExpanded ? '全部收起' : '全部展开' }}
      </van-button>
    </div>

    <div class="dept-tree" v-if="!loading">
      <DeptNode 
        v-for="item in rootDepts" 
        :key="item.id"
        :dept="item"
        :level="item.level || 1"
        :children="getChildDepts(item.id)"
        :members="employeeList"
        :expanded="allExpanded"
        @edit="openEditDept"
        @delete="deleteDept"
        @addChild="openAddChild"
        @setManager="openSetManager"
        @viewMembers="viewMembers"
        @transfer="openTransfer"
      />
      <div class="empty-state" v-if="deptList.length === 0">
        <van-icon name="cluster-o" size="48" color="#ccc" />
        <p>暂无部门信息</p>
      </div>
    </div>

    <div class="loading-state" v-else>
      <van-loading type="spinner" color="#3677ef" />
      <p>加载中...</p>
    </div>

    <van-popup 
      v-if="role === 'SYSTEM_ADMIN'"
      v-model:show="showAddDept" 
      position="bottom" 
      round 
      style="padding:20px 16px 30px;"
    >
      <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:16px;">
        <h3 style="margin:0;">{{ isEditDept ? '编辑部门' : '添加部门' }}</h3>
        <van-icon name="close" size="22" @click="showAddDept = false" />
      </div>

      <van-form @submit="onSubmitDept">
        <van-cell-group inset>
          <van-field 
            v-model="deptForm.name" 
            label="部门名称" 
            placeholder="请输入部门名称"
            :rules="[{ required: true, message: '请输入部门名称' }]"
          />
          <van-field 
            v-model="deptForm.parentName" 
            label="上级部门" 
            placeholder="请选择上级部门（可选）"
            is-link
            @click="showParentPicker = true"
          />
          <van-field 
            v-model="deptForm.managerName" 
            label="负责人" 
            placeholder="请输入负责人姓名"
          />
          <van-field 
            v-model="deptForm.description" 
            label="部门职责" 
            placeholder="请输入部门职责描述"
            type="textarea"
            rows="2"
          />
        </van-cell-group>

        <div style="display:flex;gap:12px;margin-top:16px;">
          <van-button plain block @click="showAddDept = false">取消</van-button>
          <van-button type="primary" block native-type="submit" :loading="deptSubmitting">
            {{ isEditDept ? '保存修改' : '确认添加' }}
          </van-button>
        </div>
      </van-form>
    </van-popup>

    <van-action-sheet v-if="role === 'SYSTEM_ADMIN'" v-model:show="showParentPicker" title="选择上级部门">
      <div class="picker-list">
        <div class="picker-item" @click="deptForm.parentName = ''; deptForm.parentId = null; showParentPicker = false">
          <span style="color:#999;">无上级部门（顶级部门）</span>
        </div>
        <div class="picker-item" v-for="item in deptList" :key="item.id" @click="deptForm.parentName = item.name; deptForm.parentId = item.id; showParentPicker = false">
          <span>{{ '—'.repeat(item.level - 1) }} {{ item.name }}</span>
          <van-icon v-if="deptForm.parentName === item.name" name="success" color="#3677ef" />
        </div>
      </div>
    </van-action-sheet>

    <van-popup 
      v-if="role === 'SYSTEM_ADMIN'"
      v-model:show="showManagerDialog" 
      position="bottom" 
      round 
      style="padding:20px 16px 30px;"
    >
      <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:16px;">
        <h3 style="margin:0;">👤 指定负责人</h3>
        <van-icon name="close" size="22" @click="showManagerDialog = false" />
      </div>

      <div class="manager-info">
        <div class="dept-name-display">{{ managerTarget?.name || '' }}</div>
        <van-field 
          v-model="managerForm.managerName" 
          label="负责人姓名" 
          placeholder="请输入负责人姓名"
          :rules="[{ required: true, message: '请输入负责人姓名' }]"
        />
      </div>

      <div style="display:flex;gap:12px;margin-top:16px;">
        <van-button plain block @click="showManagerDialog = false">取消</van-button>
        <van-button type="primary" block @click="confirmSetManager" :loading="managerSubmitting">
          确认指定
        </van-button>
      </div>
    </van-popup>

    <van-popup 
      v-model:show="showMemberDialog" 
      position="bottom" 
      round 
      style="padding:20px 16px 30px;max-height:70vh;overflow-y:auto;"
    >
      <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:16px;">
        <h3 style="margin:0;">👥 {{ memberTarget?.name || '' }} 成员 ({{ memberTarget?.memberCount || 0 }}人)</h3>
        <van-icon name="close" size="22" @click="showMemberDialog = false" />
      </div>

      <div class="member-list">
        <div class="member-item" v-for="member in memberList" :key="member.id">
          <van-image round width="36" height="36" :src="member.avatar || '/default-avatar.png'" />
          <div class="member-info">
            <span class="member-name">{{ member.name }}</span>
            <span class="member-position">{{ member.position }}</span>
          </div>
          <van-tag size="small" :type="member.status === '在职' ? 'success' : 'danger'">
            {{ member.status || '在职' }}
          </van-tag>
        </div>
        <div class="empty-state" v-if="!memberLoading && memberList.length === 0">
          <span style="color:#bbb;font-size:14px;">该部门暂无成员</span>
        </div>
        <div v-if="memberLoading" class="loading-state">
          <van-loading type="spinner" color="#3677ef" />
        </div>
      </div>
    </van-popup>

    <van-popup 
      v-model:show="showLevelDialog" 
      position="bottom" 
      round 
      style="padding:20px 16px 30px;max-height:70vh;overflow-y:auto;"
    >
      <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:16px;">
        <h3 style="margin:0;">📊 部门层级结构</h3>
        <van-icon name="close" size="22" @click="showLevelDialog = false" />
      </div>

      <div class="level-tree">
        <div v-for="item in sortedDeptList" :key="item.id" class="level-item" :style="{ paddingLeft: (item.level - 1) * 24 + 'px' }">
          <div class="level-dot" :style="{ background: getLevelColor(item.level) }"></div>
          <span class="level-name">{{ item.name }}</span>
          <span class="level-tag">Lv.{{ item.level }}</span>
          <span class="level-count">{{ item.memberCount || 0 }}人</span>
        </div>
      </div>

      <div class="level-stats">
        <div class="level-stat-item">
          <span class="stat-label">最大层级</span>
          <span class="stat-value">{{ maxLevel }}</span>
        </div>
        <div class="level-stat-item">
          <span class="stat-label">部门总数</span>
          <span class="stat-value">{{ deptList.length }}</span>
        </div>
        <div class="level-stat-item">
          <span class="stat-label">总人数</span>
          <span class="stat-value">{{ totalMembers }}</span>
        </div>
      </div>
    </van-popup>

    <van-popup 
      v-model:show="showTransferDialog" 
      position="bottom" 
      round 
      style="padding:20px 16px 30px;max-height:80vh;overflow-y:auto;"
    >
      <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:16px;">
        <h3 style="margin:0;">📋 调岗申请</h3>
        <van-icon name="close" size="22" @click="showTransferDialog = false" />
      </div>

      <van-form @submit="submitTransfer">
        <van-cell-group inset>
          <van-field 
            v-model="transferForm.employeeName" 
            label="员工" 
            disabled
          />
          <van-field 
            v-model="transferForm.currentDept" 
            label="当前部门" 
            disabled
          />
          <van-field 
            v-model="transferForm.currentPosition" 
            label="当前职位" 
            disabled
          />
          <van-field 
            v-model="transferForm.targetDept" 
            label="目标部门" 
            placeholder="请选择目标部门"
            is-link
            @click="showTransferDeptPicker = true"
            :rules="[{ required: true, message: '请选择目标部门' }]"
          />
          <van-field 
            v-model="transferForm.targetPosition" 
            label="目标职位" 
            placeholder="请输入目标职位"
            :rules="[{ required: true, message: '请输入目标职位' }]"
          />
          <van-field 
            v-model="transferForm.reason" 
            label="调岗原因" 
            placeholder="请输入调岗原因"
            type="textarea"
            rows="3"
            :rules="[
              { required: true, message: '请输入调岗原因' },
              { validator: (val) => val && val.trim().length >= 5, message: '调岗原因至少5个字' }
            ]"
          />
        </van-cell-group>

        <div style="display:flex;gap:12px;margin-top:16px;">
          <van-button plain block @click="showTransferDialog = false">取消</van-button>
          <van-button type="primary" block native-type="submit" :loading="transferSubmitting">
            提交调岗申请
          </van-button>
        </div>
      </van-form>
    </van-popup>

    <van-action-sheet v-model:show="showTransferDeptPicker" title="选择目标部门">
      <div class="picker-list">
        <div class="picker-item" 
          v-for="item in deptList" 
          :key="item.id" 
          @click="selectTransferDept(item)"
          :class="{ disabled: item.id === transferForm.currentDeptId }"
        >
          <span>{{ '—'.repeat(item.level - 1) }} {{ item.name }}</span>
          <span v-if="item.id === transferForm.currentDeptId" style="font-size:11px;color:#e17055;">(当前部门)</span>
          <van-icon v-if="transferForm.targetDept === item.name" name="success" color="#3677ef" />
        </div>
      </div>
    </van-action-sheet>

    <div class="bottom-bar">
      <van-button plain block size="large" @click="$router.back()" class="back-btn">
        <van-icon name="arrow-left" size="18" /> 返回
      </van-button>
    </div>

    <div class="safe-bottom"></div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showToast, showConfirmDialog } from 'vant'
import DeptNode from './DeptNode.vue'
import { getDepartmentList, addDepartment, updateDepartment, deleteDepartment, assignManager, transferEmployee } from '@/api/department'
import { getEmployeeList, getEmployeeByDepartment } from '@/api/employee'

const router = useRouter()

const role = ref('EMPLOYEE')
const userDeptId = ref(null)
const loading = ref(true)

const deptList = ref([])
const employeeList = ref([])

const totalMembers = computed(() => {
  return deptList.value.reduce((sum, dept) => sum + (dept.memberCount || 0), 0)
})

const maxLevel = computed(() => {
  return deptList.value.reduce((max, dept) => Math.max(max, dept.level || 1), 1)
})

const rootDepts = computed(() => {
  if (role.value === 'SYSTEM_ADMIN') {
    return deptList.value.filter(item => !item.parentId || item.parentId === 0)
  }
  if (role.value === 'DEPARTMENT_MANAGER') {
    return deptList.value.filter(item => item.id === userDeptId.value)
  }
  return deptList.value.filter(item => item.id === userDeptId.value)
})

const sortedDeptList = computed(() => {
  return [...deptList.value].sort((a, b) => a.level - b.level || a.name.localeCompare(b.name))
})

const allExpanded = ref(false)

const toggleAllExpand = () => {
  allExpanded.value = !allExpanded.value
}

const getChildDepts = (parentId) => {
  return deptList.value.filter(item => item.parentId === parentId)
}

const getLevelColor = (level) => {
  const colors = ['#3677ef', '#6c5ce7', '#00b894', '#fdcb6e', '#e17055', '#0984e3']
  return colors[(level - 1) % colors.length]
}

const showAddDept = ref(false)
const isEditDept = ref(false)
const deptSubmitting = ref(false)
const editDeptId = ref(null)
const showParentPicker = ref(false)

const deptForm = ref({
  name: '',
  parentName: '',
  parentId: null,
  managerName: '',
  description: ''
})

const openAddDept = () => {
  isEditDept.value = false
  editDeptId.value = null
  deptForm.value = { name: '', parentName: '', parentId: null, managerName: '', description: '' }
  showAddDept.value = true
}

const openAddChild = (parentId) => {
  const parent = deptList.value.find(d => d.id === parentId)
  isEditDept.value = false
  editDeptId.value = null
  deptForm.value = { 
    name: '', 
    parentName: parent?.name || '', 
    parentId: parentId, 
    managerName: '', 
    description: '' 
  }
  showAddDept.value = true
}

const openEditDept = (id) => {
  const dept = deptList.value.find(d => d.id === id)
  if (!dept) return
  isEditDept.value = true
  editDeptId.value = id
  const parent = deptList.value.find(d => d.id === dept.parentId)
  deptForm.value = {
    name: dept.name,
    parentName: parent?.name || '',
    parentId: dept.parentId,
    managerName: dept.managerName || '',
    description: dept.description || ''
  }
  showAddDept.value = true
}

const onSubmitDept = async () => {
  deptSubmitting.value = true
  try {
    const data = {
      id: isEditDept.value ? editDeptId.value : null,
      name: deptForm.value.name,
      parentId: deptForm.value.parentId || 0,
      managerName: deptForm.value.managerName,
      description: deptForm.value.description
    }
    
    let res
    if (isEditDept.value) {
      res = await updateDepartment(data)
    } else {
      res = await addDepartment(data)
    }
    
    if (res.code === 0) {
      showToast(isEditDept.value ? '部门已更新' : '部门添加成功')
      showAddDept.value = false
      loadDepartments()
    } else {
      showToast(res.message || '操作失败')
    }
  } catch (error) {
    console.error('操作失败：', error)
    showToast('操作失败')
  } finally {
    deptSubmitting.value = false
  }
}

const deleteDept = (id) => {
  const dept = deptList.value.find(d => d.id === id)
  const children = deptList.value.filter(d => d.parentId === id)
  if (children.length > 0) {
    showToast('该部门下有子部门，请先删除子部门')
    return
  }
  if (dept?.memberCount > 0) {
    showToast('该部门有成员，请先调走成员')
    return
  }
  showConfirmDialog({
    title: '确认删除',
    message: `确定要删除部门 "${dept?.name}" 吗？`,
    confirmButtonText: '确定删除',
    confirmButtonColor: '#ee0a24'
  }).then(async () => {
    try {
      const res = await deleteDepartment(id)
      if (res.code === 0) {
        showToast('已删除部门')
        loadDepartments()
      } else {
        showToast(res.message || '删除失败')
      }
    } catch (error) {
      console.error('删除失败：', error)
      showToast('删除失败')
    }
  }).catch(() => {})
}

const showManagerDialog = ref(false)
const managerTarget = ref(null)
const managerSubmitting = ref(false)

const managerForm = ref({
  managerName: ''
})

const openSetManager = (id) => {
  const dept = deptList.value.find(d => d.id === id)
  if (!dept) return
  managerTarget.value = dept
  managerForm.value.managerName = dept.managerName || ''
  showManagerDialog.value = true
}

const confirmSetManager = async () => {
  if (!managerForm.value.managerName.trim()) {
    showToast('请输入负责人姓名')
    return
  }
  managerSubmitting.value = true
  try {
    const res = await assignManager({
      departmentId: managerTarget.value.id,
      managerName: managerForm.value.managerName
    })
    if (res.code === 0) {
      showToast(`已指定 ${managerForm.value.managerName} 为负责人`)
      showManagerDialog.value = false
      loadDepartments()
    } else {
      showToast(res.message || '设置失败')
    }
  } catch (error) {
    console.error('设置负责人失败：', error)
    showToast('设置失败')
  } finally {
    managerSubmitting.value = false
  }
}

const showMemberDialog = ref(false)
const memberTarget = ref(null)
const memberList = ref([])
const memberLoading = ref(false)

const viewMembers = async (id) => {
  const dept = deptList.value.find(d => d.id === id)
  if (!dept) return
  memberTarget.value = dept
  memberLoading.value = true
  try {
    const res = await getEmployeeByDepartment(id)
    if (res.code === 0 && res.data) {
      memberList.value = Array.isArray(res.data) ? res.data : []
    } else {
      memberList.value = []
    }
  } catch (error) {
    console.error('获取部门员工失败：', error)
    memberList.value = []
  } finally {
    memberLoading.value = false
  }
  showMemberDialog.value = true
}

const showLevelDialog = ref(false)

const showTransferDialog = ref(false)
const showTransferDeptPicker = ref(false)
const transferSubmitting = ref(false)

const transferForm = ref({
  employeeId: '',
  employeeName: '',
  currentDeptId: '',
  currentDept: '',
  currentPosition: '',
  targetDept: '',
  targetDeptId: '',
  targetPosition: '',
  reason: ''
})

const openTransfer = (deptId) => {
  const dept = deptList.value.find(d => d.id === deptId)
  if (!dept) return
  const members = employeeList.value.filter(e => e.departmentId === deptId)
  if (members.length === 0) {
    showToast('该部门没有成员可调岗')
    return
  }
  const member = members[0]
  transferForm.value = {
    employeeId: member.id,
    employeeName: member.name,
    currentDeptId: dept.id,
    currentDept: dept.name,
    currentPosition: member.position || '',
    targetDept: '',
    targetDeptId: '',
    targetPosition: '',
    reason: ''
  }
  showTransferDialog.value = true
}

const selectTransferDept = (dept) => {
  if (dept.id === transferForm.value.currentDeptId) {
    showToast('不能调岗到当前部门')
    return
  }
  transferForm.value.targetDept = dept.name
  transferForm.value.targetDeptId = dept.id
  showTransferDeptPicker.value = false
}

const submitTransfer = async () => {
  if (!transferForm.value.targetDept) {
    showToast('请选择目标部门')
    return
  }
  if (!transferForm.value.targetPosition) {
    showToast('请输入目标职位')
    return
  }
  if (!transferForm.value.reason || transferForm.value.reason.trim().length < 5) {
    showToast('调岗原因至少5个字')
    return
  }

  transferSubmitting.value = true
  try {
    const res = await transferEmployee({
      employeeId: transferForm.value.employeeId,
      newDepartmentId: transferForm.value.targetDeptId
    })
    if (res.code === 0) {
      showToast('调岗成功')
      showTransferDialog.value = false
      loadDepartments()
      loadEmployees()
    } else {
      showToast(res.message || '调岗失败')
    }
  } catch (error) {
    console.error('调岗失败：', error)
    showToast('调岗失败')
  } finally {
    transferSubmitting.value = false
  }
}

const loadDepartments = async () => {
  loading.value = true
  try {
    const res = await getDepartmentList()
    if (res.code === 0 && res.data) {
      const depts = Array.isArray(res.data) ? res.data : []
      const deptsWithCount = depts.map(dept => {
        const memberCount = employeeList.value.filter(e => e.departmentId === dept.id).length
        return { ...dept, memberCount }
      })
      deptList.value = deptsWithCount
    }
  } catch (error) {
    console.error('加载部门列表失败：', error)
    showToast('加载部门列表失败')
  } finally {
    loading.value = false
  }
}

const loadEmployees = async () => {
  try {
    const res = await getEmployeeList(1, 100)
    if (res.code === 0 && res.data) {
      if (res.data.data) {
        employeeList.value = res.data.data || []
      } else if (Array.isArray(res.data)) {
        employeeList.value = res.data
      }
    }
  } catch (error) {
    console.error('加载员工列表失败：', error)
  }
}

onMounted(async () => {
  const storedRole = localStorage.getItem('role')
  const storedDeptId = localStorage.getItem('departmentId')
  
  if (storedRole) {
    role.value = storedRole
  }
  if (storedDeptId) {
    userDeptId.value = parseInt(storedDeptId)
  }
  
  console.log('Department - 用户角色:', role.value)
  
  await loadEmployees()
  await loadDepartments()
})
</script>

<style scoped>
.oa-department {
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
  grid-template-columns: 1fr 1fr 1fr;
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
.stat-num { display: block; font-size: 28px; font-weight: bold; }
.stat-label { font-size: 12px; color: #888; margin-top: 2px; }

.toolbar {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.dept-tree {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 0;
  color: #999;
}
.loading-state p { margin-top: 12px; }

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
.picker-item.disabled { opacity: 0.5; cursor: not-allowed; }

.manager-info { padding: 4px 0; }
.dept-name-display {
  font-size: 18px;
  font-weight: bold;
  color: #222;
  text-align: center;
  padding: 8px 0 16px;
}

.member-list { display: flex; flex-direction: column; gap: 8px; }
.member-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  background: #f8f9fa;
  border-radius: 10px;
}
.member-info { flex: 1; display: flex; flex-direction: column; }
.member-name { font-size: 14px; font-weight: 500; color: #222; }
.member-position { font-size: 12px; color: #999; }

.level-tree {
  display: flex;
  flex-direction: column;
  gap: 6px;
  max-height: 300px;
  overflow-y: auto;
  padding: 4px 0;
}
.level-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  background: #f8f9fa;
  border-radius: 8px;
}
.level-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  flex-shrink: 0;
}
.level-name { flex: 1; font-size: 14px; color: #333; }
.level-tag { font-size: 11px; color: #999; background: #f0f0f0; padding: 2px 8px; border-radius: 8px; }
.level-count { font-size: 12px; color: #3677ef; }

.level-stats {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 10px;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
}
.level-stat-item { text-align: center; }
.level-stat-item .stat-label { font-size: 12px; color: #999; display: block; }
.level-stat-item .stat-value { font-size: 20px; font-weight: bold; color: #3677ef; }

.empty-state {
  text-align: center;
  padding: 40px 0;
  color: #ccc;
}
.empty-state p { margin-top: 8px; font-size: 14px; }

.bottom-bar { padding: 16px 0 8px; }
.back-btn {
  border-radius: 12px !important;
  height: 48px !important;
  font-size: 16px !important;
  border-color: #fdcb6e !important;
  color: #fdcb6e !important;
}
.back-btn:active { background: #fff8e8 !important; }

.safe-bottom { height: 20px; }
</style>