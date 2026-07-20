<template>
  <div class="oa-dept-detail">
    <!-- 顶部导航 -->
    <div class="page-header">
      <van-icon name="arrow-left" size="22" @click="$router.back()" />
      <h1 class="header-title">部门详情</h1>
      <van-icon name="edit" size="22" v-if="canEdit" @click="showEdit = true" />
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading-wrap">
      <van-loading size="24px">加载中...</van-loading>
    </div>

    <div v-else-if="department">
      <!-- 部门信息卡片 -->
      <div class="dept-card">
        <div class="dept-banner">
          <div class="dept-icon" :style="{background: getDeptColor(department.id)}">
            <van-icon :name="getDeptIcon(department.id)" size="36" color="#fff" />
          </div>
          <div class="dept-title">
            <h2>{{ department.name }}</h2>
            <p>层级：{{ getLevelName(department.level) }}</p>
          </div>
        </div>

        <div class="dept-stats">
          <div class="stat-item">
            <span class="stat-num">{{ employeeCount }}</span>
            <span class="stat-label">员工数</span>
          </div>
          <div class="stat-divider"></div>
          <div class="stat-item">
            <span class="stat-num">{{ department.managerName ? 1 : 0 }}</span>
            <span class="stat-label">负责人</span>
          </div>
          <div class="stat-divider"></div>
          <div class="stat-item">
            <span class="stat-num">{{ department.level || 1 }}</span>
            <span class="stat-label">层级</span>
          </div>
        </div>

        <div class="dept-info-list">
          <div class="info-item">
            <span class="info-label">部门描述</span>
            <span class="info-value">{{ department.description || '暂无描述' }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">部门负责人</span>
            <span class="info-value">{{ department.managerName || '暂无' }}</span>
          </div>
          <div class="info-item" v-if="department.managerPhone">
            <span class="info-label">联系电话</span>
            <span class="info-value">{{ department.managerPhone }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">上级部门</span>
            <span class="info-value">{{ parentDeptName }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">创建时间</span>
            <span class="info-value">{{ formatTime(department.createTime) }}</span>
          </div>
        </div>
      </div>

      <!-- 快捷操作 -->
      <div class="action-row">
        <div class="action-item" @click="goMembers">
          <van-icon name="friends-o" size="28" color="#3677ef" />
          <span>查看成员</span>
        </div>
        <div class="action-item" v-if="canEdit" @click="showAssignManager = true">
          <van-icon name="manager-o" size="28" color="#00b894" />
          <span>指定负责人</span>
        </div>
        <div class="action-item" v-if="canEdit" @click="confirmDelete">
          <van-icon name="delete-o" size="28" color="#e17055" />
          <span>删除部门</span>
        </div>
      </div>

      <!-- 子部门列表（如果有） -->
      <div class="section" v-if="subDepartments.length > 0">
        <h3 class="section-title">子部门</h3>
        <div class="sub-dept-list">
          <div 
            class="sub-dept-item" 
            v-for="sub in subDepartments" 
            :key="sub.id"
            @click="goSubDetail(sub.id)"
          >
            <van-icon name="cluster-o" size="20" color="#3677ef" />
            <div class="sub-dept-info">
              <p class="sub-dept-name">{{ sub.name }}</p>
              <p class="sub-dept-manager">{{ sub.managerName || '暂无负责人' }}</p>
            </div>
            <van-icon name="arrow" size="16" color="#ccc" />
          </div>
        </div>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-else class="empty-state">
      <van-icon name="warning-o" size="48" color="#ccc" />
      <p>部门不存在或已被删除</p>
    </div>

    <!-- ===== 编辑部门弹窗 ===== -->
    <van-popup v-model:show="showEdit" position="bottom" round style="padding:20px 16px 30px;">
      <h3 style="margin-bottom:16px;text-align:center;">编辑部门</h3>
      <van-form @submit="onSubmitEdit">
        <van-field v-model="editForm.name" label="部门名称" placeholder="请输入部门名称" :rules="[{ required: true, message: '请输入部门名称' }]" />
        <van-field v-model="editForm.description" label="部门描述" type="textarea" placeholder="请输入部门描述" rows="2" />
        <div style="display:flex;gap:12px;margin-top:16px;">
          <van-button plain block @click="showEdit = false">取消</van-button>
          <van-button type="primary" block native-type="submit" :loading="submitting">保存修改</van-button>
        </div>
      </van-form>
    </van-popup>

    <!-- ===== 指定负责人弹窗 ===== -->
    <van-popup 
      v-model:show="showAssignManager" 
      position="bottom" 
      round 
      style="padding:20px 16px 30px;"
      @open="onShowAssignManager"
    >
      <h3 style="margin-bottom:16px;text-align:center;">指定部门负责人</h3>
      <div v-if="loadingMembers" class="loading-wrap">
        <van-loading size="24px">加载员工列表...</van-loading>
      </div>
      <div v-else>
        <div 
          class="manager-item" 
          v-for="emp in memberList" 
          :key="emp.id"
          @click="selectManager(emp)"
          :class="{ active: selectedManagerId === emp.id }"
        >
          <van-image round width="36" height="36" :src="emp.avatar || '/default-avatar.png'" />
          <div class="manager-info">
            <p class="manager-name">{{ emp.name }}</p>
            <p class="manager-position">{{ emp.position || '员工' }}</p>
          </div>
          <van-icon v-if="selectedManagerId === emp.id" name="success" color="#3677ef" size="20" />
        </div>
        <div v-if="memberList.length === 0" class="empty-state">
          <van-icon name="friends-o" size="36" color="#ccc" />
          <p>该部门暂无员工</p>
        </div>
        <div style="display:flex;gap:12px;margin-top:16px;" v-if="memberList.length > 0">
          <van-button plain block @click="showAssignManager = false">取消</van-button>
          <van-button type="primary" block @click="confirmAssignManager" :loading="submitting" :disabled="!selectedManagerId">
            确认指定
          </van-button>
        </div>
      </div>
    </van-popup>

    <div class="safe-bottom"></div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast, showConfirmDialog } from 'vant'
import { 
  getDepartmentById, 
  updateDepartment, 
  deleteDepartment, 
  countDepartmentEmployees,
  assignManager 
} from '@/api/department'
import { getEmployeeByDepartment } from '@/api/employee'

const route = useRoute()
const router = useRouter()

// 状态
const loading = ref(false)
const loadingMembers = ref(false)
const submitting = ref(false)
const department = ref(null)
const employeeCount = ref(0)
const memberList = ref([])
const subDepartments = ref([])

// 弹窗
const showEdit = ref(false)
const showAssignManager = ref(false)
const selectedManagerId = ref(null)

// 编辑表单
const editForm = ref({
  name: '',
  description: ''
})

// 权限判断
const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}')
const canEdit = computed(() => userInfo.role === 'SYSTEM_ADMIN')

// 部门图标和颜色
const deptIconMap = {
  1: 'desktop-o', 2: 'apps-o', 3: 'bullhorn-o', 4: 'friends-o', 5: 'gold-coin-o'
}
const deptColorMap = {
  1: '#3677ef', 2: '#6c5ce7', 3: '#00b894', 4: '#fdcb6e', 5: '#e17055'
}
const getDeptIcon = (id) => deptIconMap[id] || 'cluster-o'
const getDeptColor = (id) => deptColorMap[id] || '#3677ef'

const getLevelName = (level) => {
  const names = { 1: '一级（公司级）', 2: '二级（部门级）', 3: '三级（小组级）' }
  return names[level] || `${level}级`
}

const parentDeptName = computed(() => {
  if (!department.value || !department.value.parentId || department.value.parentId === 0) {
    return '无（顶级部门）'
  }
  // 这里简化处理，实际可以通过API获取父部门名称
  return `上级部门ID: ${department.value.parentId}`
})

const formatTime = (time) => {
  if (!time) return '未知'
  return time.replace('T', ' ').substring(0, 19)
}

// 加载部门详情
const loadDepartmentDetail = async () => {
  loading.value = true
  const id = route.params.id
  try {
    const res = await getDepartmentById(id)
    console.log('部门详情：', res)
    if (res.code === 0 && res.data) {
      department.value = res.data
      editForm.value.name = res.data.name || ''
      editForm.value.description = res.data.description || ''
      
      // 加载员工数量
      try {
        const countRes = await countDepartmentEmployees(id)
        employeeCount.value = countRes.data || 0
      } catch (error) {
        console.warn('获取员工数量失败：', error)
      }
    } else {
      showToast(res.message || '获取部门详情失败')
    }
  } catch (error) {
    console.error('加载部门详情失败：', error)
    showToast('加载失败')
  } finally {
    loading.value = false
  }
}

// 查看成员
const goMembers = () => {
  router.push(`/department/members/${route.params.id}`)
}

// 跳转子部门详情
const goSubDetail = (id) => {
  router.push(`/department/detail/${id}`)
}

// 加载部门成员（用于指定负责人）
const loadMembers = async () => {
  loadingMembers.value = true
  try {
    const res = await getEmployeeByDepartment(route.params.id)
    console.log('部门成员：', res)
    if (res.code === 0) {
      memberList.value = Array.isArray(res.data) ? res.data : []
    }
  } catch (error) {
    console.error('加载成员失败：', error)
    showToast('加载成员失败')
  } finally {
    loadingMembers.value = false
  }
}

// 选择负责人
const selectManager = (emp) => {
  selectedManagerId.value = emp.id
}

// 确认指定负责人
const confirmAssignManager = async () => {
  if (!selectedManagerId.value) {
    showToast('请选择负责人')
    return
  }
  
  submitting.value = true
  try {
    const res = await assignManager({
      departmentId: parseInt(route.params.id),
      managerId: selectedManagerId.value
    })
    console.log('指定负责人结果：', res)
    if (res.code === 0) {
      showToast('负责人设置成功')
      showAssignManager.value = false
      loadDepartmentDetail()
    } else {
      showToast(res.message || '设置失败')
    }
  } catch (error) {
    console.error('指定负责人失败：', error)
    showToast('操作失败')
  } finally {
    submitting.value = false
  }
}

// 提交编辑
const onSubmitEdit = async () => {
  submitting.value = true
  try {
    const data = {
      id: parseInt(route.params.id),
      name: editForm.value.name,
      description: editForm.value.description
    }
    const res = await updateDepartment(data)
    console.log('更新结果：', res)
    if (res.code === 0) {
      showToast('部门信息已更新')
      showEdit.value = false
      loadDepartmentDetail()
    } else {
      showToast(res.message || '更新失败')
    }
  } catch (error) {
    console.error('更新失败：', error)
    showToast('操作失败')
  } finally {
    submitting.value = false
  }
}

// 删除部门
const confirmDelete = () => {
  showConfirmDialog({
    title: '确认删除',
    message: `确定要删除部门"${department.value.name}"吗？此操作不可恢复！`,
    confirmButtonText: '确定删除',
    confirmButtonColor: '#ee0a24'
  }).then(async () => {
    try {
      const res = await deleteDepartment(route.params.id)
      if (res.code === 0) {
        showToast('部门已删除')
        router.back()
      } else {
        showToast(res.message || '删除失败')
      }
    } catch (error) {
      console.error('删除失败：', error)
      showToast('删除失败')
    }
  }).catch(() => {})
}

// 监听指定负责人弹窗打开
const onShowAssignManager = () => {
  selectedManagerId.value = null
  loadMembers()
}

// 初始化
onMounted(() => {
  loadDepartmentDetail()
})
</script>

<style scoped>
.oa-dept-detail {
  max-width: 430px;
  margin: 0 auto;
  min-height: 100vh;
  background: #f5f7fa;
  padding: 0 16px 20px;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 0 12px;
}
.header-title { font-size: 18px; font-weight: 600; margin: 0; color: #222; }

.loading-wrap { text-align: center; padding: 40px 0; }

/* 部门卡片 */
.dept-card {
  background: #fff;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(0,0,0,0.06);
  margin-bottom: 16px;
}
.dept-banner {
  background: linear-gradient(135deg, #3677ef 0%, #5b8def 100%);
  padding: 24px 20px;
  display: flex;
  align-items: center;
  gap: 16px;
}
.dept-icon {
  width: 60px; height: 60px;
  border-radius: 16px;
  display: flex; align-items: center; justify-content: center;
  background: rgba(255,255,255,0.25);
}
.dept-title h2 { color: #fff; font-size: 22px; margin: 0; }
.dept-title p { color: rgba(255,255,255,0.85); font-size: 13px; margin: 4px 0 0; }

.dept-stats {
  display: flex;
  align-items: center;
  padding: 20px 16px;
  background: #fff;
}
.stat-item {
  flex: 1;
  text-align: center;
}
.stat-num { display: block; font-size: 24px; font-weight: bold; color: #3677ef; }
.stat-label { font-size: 12px; color: #888; }
.stat-divider { width: 1px; height: 30px; background: #eee; }

.dept-info-list {
  padding: 0 16px 16px;
}
.info-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid #f5f5f5;
}
.info-item:last-child { border-bottom: none; }
.info-label { font-size: 14px; color: #888; }
.info-value { font-size: 14px; color: #333; text-align: right; }

/* 快捷操作 */
.action-row {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 10px;
  margin-bottom: 16px;
}
.action-item {
  background: #fff;
  border-radius: 12px;
  padding: 16px 8px;
  text-align: center;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
  cursor: pointer;
}
.action-item:active { transform: scale(0.95); }
.action-item span { display: block; font-size: 12px; color: #666; margin-top: 6px; }

/* 子部门 */
.section { margin-bottom: 16px; }
.section-title { font-size: 16px; font-weight: 600; color: #222; margin: 0 0 12px; }
.sub-dept-list {
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}
.sub-dept-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  border-bottom: 1px solid #f5f5f5;
  cursor: pointer;
}
.sub-dept-item:last-child { border-bottom: none; }
.sub-dept-item:active { background: #f5f7fa; }
.sub-dept-info { flex: 1; }
.sub-dept-name { font-size: 15px; color: #222; margin: 0; font-weight: 500; }
.sub-dept-manager { font-size: 12px; color: #888; margin: 2px 0 0; }

/* 负责人选择 */
.manager-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 8px;
  border-bottom: 1px solid #f5f5f5;
  cursor: pointer;
}
.manager-item.active { background: #f0f7ff; }
.manager-info { flex: 1; }
.manager-name { font-size: 15px; margin: 0; color: #222; font-weight: 500; }
.manager-position { font-size: 12px; color: #888; margin: 2px 0 0; }

.empty-state {
  text-align: center;
  padding: 60px 0;
  color: #ccc;
}
.empty-state p { margin-top: 8px; font-size: 14px; }

.safe-bottom { height: 20px; }
</style>