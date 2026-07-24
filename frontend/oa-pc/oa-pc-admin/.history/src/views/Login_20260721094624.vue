<template>
  <div class="login-container">
    <div class="login-box">
      <!-- 顶部装饰区域 -->
      <div class="login-header">
        <div class="logo-wrap">
          <div class="logo-icon">
            <el-icon :size="36" color="#fff">
              <Monitor />
            </el-icon>
          </div>
          <h1 class="logo-title">企业智慧OA</h1>
          <p class="logo-desc">Enterprise Smart OA System</p>
        </div>
      </div>

      <!-- 登录表单 -->
      <div class="login-form">
        <h2 class="form-title">欢迎登录</h2>
        <p class="form-desc">请选择身份并输入账号密码登录系统</p>

        <el-form :model="form" :rules="rules" ref="formRef" label-width="0">
          <!-- 身份选择 -->
          <el-form-item>
            <div class="role-select">
              <div 
                class="role-option" 
                :class="{ active: selectedRole === 'admin' }"
                @click="selectRole('admin')"
              >
                <el-icon :size="20" :color="selectedRole === 'admin' ? '#409eff' : '#999'">
                  <Setting />
                </el-icon>
                <span>系统管理员</span>
              </div>
              <div 
                class="role-option" 
                :class="{ active: selectedRole === 'manager' }"
                @click="selectRole('manager')"
              >
                <el-icon :size="20" :color="selectedRole === 'manager' ? '#409eff' : '#999'">
                  <User />
                </el-icon>
                <span>部门主管</span>
              </div>
              <div 
                class="role-option" 
                :class="{ active: selectedRole === 'employee' }"
                @click="selectRole('employee')"
              >
                <el-icon :size="20" :color="selectedRole === 'employee' ? '#409eff' : '#999'">
                  <UserFilled />
                </el-icon>
                <span>普通员工</span>
              </div>
            </div>
          </el-form-item>

          <!-- 账号输入 -->
          <el-form-item prop="username">
            <el-input
              v-model="form.username"
              placeholder="请输入账号"
              size="large"
              :prefix-icon="User"
              clearable
            />
          </el-form-item>

          <!-- 密码输入 -->
          <el-form-item prop="password">
            <el-input
              v-model="form.password"
              type="password"
              placeholder="请输入密码"
              size="large"
              :prefix-icon="Lock"
              show-password
              @keyup.enter="handleLogin"
            />
          </el-form-item>

          <!-- 登录选项 -->
          <el-form-item>
            <div class="login-options">
              <el-checkbox v-model="rememberMe">记住我</el-checkbox>
              <span class="forget-pwd" @click="handleForget">忘记密码？</span>
            </div>
          </el-form-item>

          <!-- 登录按钮 -->
          <el-form-item>
            <el-button
              type="primary"
              size="large"
              style="width:100%;"
              :loading="loading"
              @click="handleLogin"
            >登 录</el-button>
          </el-form-item>
        </el-form>

        <!-- 快捷测试入口 -->
        <div class="quick-login">
          <span class="quick-tip">快速测试：</span>
          <el-tag 
            size="small" 
            type="success" 
            @click="quickLogin('admin')" 
            style="cursor:pointer;"
          >管理员</el-tag>
          <el-tag 
            size="small" 
            type="primary" 
            @click="quickLogin('manager')" 
            style="cursor:pointer;"
          >主管</el-tag>
          <el-tag 
            size="small" 
            type="warning" 
            @click="quickLogin('employee')" 
            style="cursor:pointer;"
          >员工</el-tag>
        </div>

        <!-- 注册入口 -->
        <div class="register-tip">
          还没有账号？<span class="register-link" @click="handleRegister">立即注册</span>
        </div>

        <!-- 底部版权信息 -->
        <div class="login-footer">
          <p>© 2026 企业智慧OA管理系统</p>
          <p class="footer-version">v3.0.0</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, Setting, Monitor, UserFilled } from '@element-plus/icons-vue'
import request from '@/api/request'

const router = useRouter()
const formRef = ref()
const loading = ref(false)

// ===== 账号密码映射（根据数据库实际数据） =====
const users = {
  admin: { username: 'admin', password: '123456', role: 'admin', roleName: '系统管理员' },
  manager: { username: 'zhangsan', password: '123456', role: 'manager', roleName: '部门主管' },
  employee: { username: 'emp001', password: '123456', role: 'employee', roleName: '普通员工' }
}

// ===== 默认选中的身份 =====
const selectedRole = ref('employee')
const form = reactive({
  username: 'emp001',
  password: '123456'
})
const rememberMe = ref(true)

// ===== 表单验证规则 =====
const rules = {
  username: [
    { required: true, message: '请输入账号', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' }
  ]
}

// ===== 选择身份 =====
const selectRole = (role) => {
  selectedRole.value = role
}

// ===== 快速登录 =====
const quickLogin = (role) => {
  selectedRole.value = role
  form.username = users[role].username
  form.password = users[role].password
  // 延迟执行登录，让界面更新
  setTimeout(() => {
    handleLogin()
  }, 300)
}

// =============================================
// ===== 登录方法（对接后端） =====
// =============================================
const handleLogin = async () => {
  if (!selectedRole.value) {
    ElMessage.warning('请选择登录身份')
    return
  }
  if (!form.username.trim()) {
    ElMessage.warning('请输入账号')
    return
  }
  if (!form.password.trim()) {
    ElMessage.warning('请输入密码')
    return
  }

  // 表单验证
  if (formRef.value) {
    try {
      await formRef.value.validate()
    } catch {
      return
    }
  }

  loading.value = true

  try {
    const res = await request({
      url: '/employee/login',
      method: 'POST',
      data: {
        username: form.username.trim(),
        password: form.password
      }
    })

    if (res.code === 0) {
      const data = res.data
      const employee = data?.employee || data
      
      // 保存登录信息
      if (data.token) {
        localStorage.setItem('token', data.token)
      }
      localStorage.setItem('isLogin', 'true')
      localStorage.setItem('employeeId', employee?.id || '')
      localStorage.setItem('departmentId', employee?.departmentId || '')
      localStorage.setItem('username', employee?.username || form.username)
      localStorage.setItem('name', employee?.name || employee?.username || form.username)
      localStorage.setItem('role', employee?.role || selectedRole.value)
      localStorage.setItem('roleName', 
        employee?.role === 'SYSTEM_ADMIN' ? '系统管理员' :
        employee?.role === 'DEPARTMENT_MANAGER' ? '部门主管' :
        employee?.role === 'PROCESS_ADMIN' ? '流程管理员' : '普通员工'
      )

      // 保存用户信息对象
      const userInfo = {
        id: employee?.id,
        username: employee?.username || form.username,
        name: employee?.name || employee?.username || form.username,
        role: employee?.role || selectedRole.value,
        departmentId: employee?.departmentId,
        avatar: employee?.avatar || '',
        phone: employee?.phone || '',
        email: employee?.email || ''
      }
      localStorage.setItem('userInfo', JSON.stringify(userInfo))

      ElMessage.success(`欢迎，${userInfo.name}！`)

      // 跳转首页
      setTimeout(() => {
        router.replace('/')
      }, 300)
    } else {
      ElMessage.error(res.msg || res.message || '登录失败，请检查账号密码')
    }
  } catch (error) {
    console.error('登录失败:', error)
    if (error.code === 'ERR_NETWORK') {
      ElMessage.error('网络连接失败，请检查后端服务')
    } else if (error.response?.status === 401) {
      ElMessage.error('账号或密码错误')
    } else {
      ElMessage.error(error.message || '登录失败，请稍后重试')
    }
  } finally {
    loading.value = false
  }
}

const handleForget = () => {
  ElMessage.info('请联系管理员重置密码')
}

const handleRegister = () => {
  ElMessage.info('注册功能开发中')
}

// =============================================
// ===== 如果已登录，直接跳转 =====
// =============================================
onMounted(() => {
  const isLogin = localStorage.getItem('isLogin')
  const token = localStorage.getItem('token')
  if (isLogin === 'true' && token) {
    router.replace('/')
  }
})
</script>

<style scoped>
.login-container {
  width: 100%;
  min-height: 100vh;
  background: linear-gradient(135deg, #e8edf5 0%, #d5dde8 100%);
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 40px 20px;
}

.login-box {
  width: 460px;
  max-width: 100%;
  background: #ffffff;
  border-radius: 24px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.12);
  overflow: hidden;
}

/* ===== 顶部装饰 ===== */
.login-header {
  background: linear-gradient(135deg, #409eff 0%, #1d7cf0 100%);
  padding: 40px 40px 32px;
  text-align: center;
}
.logo-wrap {
  display: flex;
  flex-direction: column;
  align-items: center;
}
.logo-icon {
  width: 72px;
  height: 72px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 16px;
}
.logo-title {
  font-size: 26px;
  font-weight: bold;
  color: #fff;
  margin: 0;
  letter-spacing: 2px;
}
.logo-desc {
  color: rgba(255, 255, 255, 0.8);
  font-size: 13px;
  margin: 6px 0 0;
  letter-spacing: 1px;
}

/* ===== 登录表单 ===== */
.login-form {
  padding: 28px 32px 24px;
}
.form-title {
  font-size: 22px;
  font-weight: bold;
  color: #1a1a2e;
  margin: 0 0 4px;
}
.form-desc {
  font-size: 14px;
  color: #999;
  margin: 0 0 24px;
}

/* ===== 身份选择 ===== */
.role-select {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 10px;
}
.role-option {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 12px 8px;
  border-radius: 12px;
  border: 2px solid #e8ecf1;
  background: #fafbfc;
  cursor: pointer;
  transition: all 0.25s ease;
}
.role-option:hover {
  border-color: #b3d1f0;
  background: #f5f9ff;
}
.role-option:active {
  transform: scale(0.95);
}
.role-option.active {
  border-color: #409eff;
  background: #ecf5ff;
  box-shadow: 0 0 0 4px rgba(64, 158, 255, 0.1);
}
.role-option span {
  font-size: 13px;
  color: #666;
}
.role-option.active span {
  color: #409eff;
  font-weight: 500;
}

/* ===== 登录选项 ===== */
.login-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}
.login-options :deep(.el-checkbox) {
  font-size: 14px;
  color: #888;
}
.forget-pwd {
  font-size: 14px;
  color: #409eff;
  cursor: pointer;
}
.forget-pwd:hover {
  color: #1d7cf0;
}

/* ===== 快速登录 ===== */
.quick-login {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 12px 0 16px;
  border-bottom: 1px solid #f0f2f5;
  margin-bottom: 16px;
  flex-wrap: wrap;
}
.quick-tip {
  font-size: 13px;
  color: #999;
}
.quick-login :deep(.el-tag) {
  cursor: pointer;
  transition: all 0.2s;
}
.quick-login :deep(.el-tag:hover) {
  transform: scale(1.05);
  opacity: 0.8;
}

/* ===== 注册入口 ===== */
.register-tip {
  text-align: center;
  font-size: 14px;
  color: #888;
  margin-bottom: 16px;
}
.register-link {
  color: #409eff;
  font-weight: 500;
  cursor: pointer;
}
.register-link:hover {
  color: #1d7cf0;
  text-decoration: underline;
}

/* ===== 底部版权 ===== */
.login-footer {
  text-align: center;
  padding-top: 12px;
  border-top: 1px solid #f0f2f5;
}
.login-footer p {
  font-size: 12px;
  color: #bbb;
  margin: 0;
}
.login-footer .footer-version {
  color: #ddd;
  margin-top: 4px;
  font-size: 11px;
}

/* ===== Element Plus 样式覆盖 ===== */
:deep(.el-input__wrapper) {
  height: 44px;
  border-radius: 10px;
  box-shadow: 0 0 0 1px #e0e4ea inset;
}
:deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #b3d1f0 inset;
}
:deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #409eff inset !important;
}

:deep(.el-button) {
  height: 46px;
  border-radius: 10px;
  font-size: 16px;
  letter-spacing: 4px;
}

:deep(.el-form-item) {
  margin-bottom: 18px;
}

:deep(.el-form-item:last-child) {
  margin-bottom: 0;
}
</style>