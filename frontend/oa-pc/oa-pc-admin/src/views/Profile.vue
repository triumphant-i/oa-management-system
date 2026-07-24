<template>
  <div class="page-container">
    <div class="page-header">
      <h2>👤 个人中心</h2>
    </div>

    <el-row :gutter="20">
      <!-- 左侧个人信息 -->
      <el-col :span="8">
        <el-card>
          <div class="profile-avatar">
            <el-avatar :size="120" :src="userInfo.avatar || ''">
              {{ userInfo.name?.charAt(0) || '管' }}
            </el-avatar>
            <div class="profile-name">{{ userInfo.name || '超级管理员' }}</div>
            <div class="profile-role">
              <el-tag type="danger">系统管理员</el-tag>
            </div>
          </div>
          <el-divider />
          <div class="profile-info">
            <div class="info-item">
              <span class="info-label">用户名</span>
              <span class="info-value">{{ userInfo.username || '-' }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">邮箱</span>
              <span class="info-value">{{ userInfo.email || '未设置' }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">手机号</span>
              <span class="info-value">{{ userInfo.phone || '未设置' }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">所属部门</span>
              <span class="info-value">{{ userInfo.departmentName || '未分配' }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">登录时间</span>
              <span class="info-value">{{ loginTime }}</span>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 右侧操作区 -->
      <el-col :span="16">
        <el-card>
          <template #header>
            <span>修改个人信息</span>
          </template>
          <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
            <el-form-item label="姓名" prop="name">
              <el-input v-model="form.name" placeholder="请输入姓名" />
            </el-form-item>
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="form.email" placeholder="请输入邮箱" />
            </el-form-item>
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="form.phone" placeholder="请输入手机号" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleUpdate" :loading="updateLoading">保存修改</el-button>
              <el-button @click="resetForm">重置</el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <!-- 修改密码 -->
        <el-card style="margin-top:20px;">
          <template #header>
            <span>修改密码</span>
          </template>
          <el-form :model="pwdForm" :rules="pwdRules" ref="pwdFormRef" label-width="100px">
            <el-form-item label="原密码" prop="oldPassword">
              <el-input v-model="pwdForm.oldPassword" type="password" placeholder="请输入原密码" show-password />
            </el-form-item>
            <el-form-item label="新密码" prop="newPassword">
              <el-input v-model="pwdForm.newPassword" type="password" placeholder="请输入新密码" show-password />
            </el-form-item>
            <el-form-item label="确认密码" prop="confirmPassword">
              <el-input v-model="pwdForm.confirmPassword" type="password" placeholder="请再次输入新密码" show-password />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleChangePassword" :loading="pwdLoading">修改密码</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'

// ==================== 当前用户 ====================
const userInfo = ref({
  id: 1,
  username: 'admin',
  name: '超级管理员',
  email: 'admin@oa.com',
  phone: '13800138000',
  avatar: '',
  departmentName: '总公司'
})

const loginTime = ref(new Date().toLocaleString())

// ==================== 个人信息表单 ====================
const formRef = ref()
const updateLoading = ref(false)
const form = reactive({
  name: '',
  email: '',
  phone: ''
})

const rules = {
  name: [{ required: true, message: '请输入姓名' }],
  email: [{ type: 'email', message: '请输入正确的邮箱格式' }]
}

// ==================== 修改密码 ====================
const pwdFormRef = ref()
const pwdLoading = ref(false)
const pwdForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const pwdRules = {
  oldPassword: [{ required: true, message: '请输入原密码' }],
  newPassword: [
    { required: true, message: '请输入新密码' },
    { min: 3, message: '密码至少3位' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码' },
    {
      validator: (rule, value, callback) => {
        if (value !== pwdForm.newPassword) {
          callback(new Error('两次密码输入不一致'))
        } else {
          callback()
        }
      }
    }
  ]
}

// ==================== 加载用户信息 ====================
const loadUserInfo = () => {
  const info = localStorage.getItem('userInfo')
  if (info) {
    try {
      const data = JSON.parse(info)
      userInfo.value = { ...userInfo.value, ...data }
      form.name = data.name || ''
      form.email = data.email || ''
      form.phone = data.phone || ''
    } catch (e) {
      console.error('解析用户信息失败:', e)
    }
  }
}

// ==================== 更新个人信息 ====================
const handleUpdate = async () => {
  await formRef.value.validate(async (valid) => {
    if (!valid) return

    updateLoading.value = true
    try {
      // 模拟更新
      await new Promise(resolve => setTimeout(resolve, 800))
      
      // 更新本地存储
      const info = JSON.parse(localStorage.getItem('userInfo') || '{}')
      info.name = form.name
      info.email = form.email
      info.phone = form.phone
      localStorage.setItem('userInfo', JSON.stringify(info))
      userInfo.value = { ...userInfo.value, ...form }
      
      ElMessage.success('个人信息更新成功')
    } catch (error) {
      console.error('更新失败:', error)
      ElMessage.error('更新失败，请稍后重试')
    } finally {
      updateLoading.value = false
    }
  })
}

const resetForm = () => {
  form.name = userInfo.value.name || ''
  form.email = userInfo.value.email || ''
  form.phone = userInfo.value.phone || ''
}

// ==================== 修改密码 ====================
const handleChangePassword = async () => {
  await pwdFormRef.value.validate(async (valid) => {
    if (!valid) return

    pwdLoading.value = true
    try {
      await new Promise(resolve => setTimeout(resolve, 800))
      ElMessage.success('密码修改成功，请重新登录')
      
      // 清空密码表单
      pwdForm.oldPassword = ''
      pwdForm.newPassword = ''
      pwdForm.confirmPassword = ''
    } catch (error) {
      console.error('修改密码失败:', error)
      ElMessage.error('修改密码失败，请稍后重试')
    } finally {
      pwdLoading.value = false
    }
  })
}

// ==================== 生命周期 ====================
onMounted(() => {
  loadUserInfo()
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

.profile-avatar {
  text-align: center;
  padding: 10px 0;
}

.profile-name {
  font-size: 20px;
  font-weight: bold;
  margin-top: 12px;
  color: #1a1a2e;
}

.profile-role {
  margin-top: 8px;
}

.profile-info {
  padding: 4px 0;
}

.info-item {
  display: flex;
  justify-content: space-between;
  padding: 10px 0;
  border-bottom: 1px solid #f5f5f5;
}

.info-item:last-child {
  border-bottom: none;
}

.info-label {
  color: #999;
  font-size: 14px;
}

.info-value {
  color: #333;
  font-size: 14px;
}
</style>