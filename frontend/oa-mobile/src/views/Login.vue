<template>
  <div class="oa-login">
    <!-- 顶部装饰区域 -->
    <div class="login-header">
      <div class="logo-wrap">
        <div class="logo-icon">
          <van-icon name="records" size="40" color="#fff" />
        </div>
        <h1 class="logo-title">企业智慧OA</h1>
        <p class="logo-desc">Enterprise Smart OA System</p>
      </div>
    </div>

    <!-- 登录表单 -->
    <div class="login-form">
      <h2 class="form-title">欢迎登录</h2>
      <p class="form-desc">请选择身份并输入账号密码登录系统</p>

      <form @submit.prevent="handleLogin">
        <!-- 身份选择 -->
        <div class="input-wrap">
          <div class="input-label">登录身份</div>
          <div class="role-select">
            <div 
              class="role-option" 
              :class="{ active: selectedRole === 'admin' }"
              @click="selectRole('admin')"
            >
              <van-icon name="setting-o" size="20" :color="selectedRole === 'admin' ? '#3677ef' : '#999'" />
              <span>系统管理员</span>
            </div>
            <div 
              class="role-option" 
              :class="{ active: selectedRole === 'manager' }"
              @click="selectRole('manager')"
            >
              <van-icon name="user-o" size="20" :color="selectedRole === 'manager' ? '#3677ef' : '#999'" />
              <span>部门主管</span>
            </div>
            <div 
              class="role-option" 
              :class="{ active: selectedRole === 'employee' }"
              @click="selectRole('employee')"
            >
              <van-icon name="user" size="20" :color="selectedRole === 'employee' ? '#3677ef' : '#999'" />
              <span>普通员工</span>
            </div>
          </div>
        </div>

        <!-- 账号输入 -->
        <div class="input-wrap">
          <div class="input-label">账号</div>
          <div class="input-box">
            <van-icon name="user-o" size="20" color="#999" class="input-icon" />
            <input 
              v-model="form.username" 
              type="text" 
              placeholder="请输入账号" 
              class="input-field"
            />
          </div>
        </div>

        <!-- 密码输入 -->
        <div class="input-wrap">
          <div class="input-label">密码</div>
          <div class="input-box">
            <van-icon name="lock-o" size="20" color="#999" class="input-icon" />
            <input 
              v-model="form.password" 
              type="password" 
              placeholder="请输入密码" 
              class="input-field"
              @keyup.enter="handleLogin"
            />
          </div>
        </div>

        <!-- 登录选项 -->
        <div class="login-options">
          <van-checkbox v-model="rememberMe">记住我</van-checkbox>
          <span class="forget-pwd" @click="handleForget">忘记密码？</span>
        </div>

        <!-- 登录按钮 -->
        <div class="login-btn-wrap">
          <van-button 
            round 
            block 
            type="primary" 
            :loading="loading" 
            loading-text="登录中..."
            @click="handleLogin"
          >
            登 录
          </van-button>
        </div>

        <!-- 快捷测试入口 -->
        <div class="quick-login">
          <span class="quick-tip">快速测试：</span>
          <span class="quick-item" @click="quickLogin('admin')">管理员</span>
          <span class="quick-item" @click="quickLogin('manager')">主管</span>
          <span class="quick-item" @click="quickLogin('employee')">员工</span>
        </div>

        <!-- 注册入口 -->
        <div class="register-tip">
          还没有账号？<span class="register-link" @click="handleRegister">立即注册</span>
        </div>
      </form>
    </div>

    <!-- 底部版权信息 -->
    <div class="login-footer">
      <p>© 2026 企业智慧OA管理系统</p>
      <p class="footer-version">v3.0.0</p>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { login } from '@/api/employee'

const router = useRouter()

// ===== 账号密码映射（根据数据库实际数据） =====
const users = {
  admin: { username: 'admin', password: '123456', role: 'admin', roleName: '系统管理员' },
  // 数据库中的部门主管账号是 zhangsan 和 lisi
  manager: { username: 'zhangsan', password: '123456', role: 'manager', roleName: '部门主管' },
  employee: { username: 'emp001', password: '123456', role: 'employee', roleName: '普通员工' }
}

// ===== 默认选中的身份 =====
const selectedRole = ref('employee')
const form = ref({
  username: 'emp001',
  password: '123456'
})
const rememberMe = ref(true)
const loading = ref(false)

// ===== 选择身份 =====
const selectRole = (role) => {
  selectedRole.value = role
}

// ===== 快速登录 =====
const quickLogin = (role) => {
  selectedRole.value = role
  form.value.username = users[role].username
  form.value.password = users[role].password
  setTimeout(() => {
    handleLogin()
  }, 300)
}

// =============================================
// ===== 登录方法（对接后端） =====
// =============================================
const handleLogin = async () => {
  if (!selectedRole.value) {
    showToast('请选择登录身份')
    return
  }
  if (!form.value.username.trim()) {
    showToast('请输入账号')
    return
  }
  if (!form.value.password.trim()) {
    showToast('请输入密码')
    return
  }
  
  loading.value = true
  
  try {
    // 调用后端登录接口
    const res = await login({
      username: form.value.username,
      password: form.value.password
    })
    
    if (res.code === 0) {
      const data = res.data
      const employee = data.employee
      
      // 保存登录信息
      localStorage.setItem('token', data.token)
      localStorage.setItem('isLogin', 'true')
      localStorage.setItem('employeeId', employee.id)
      localStorage.setItem('departmentId', employee.departmentId || '')
      localStorage.setItem('username', employee.username)
      localStorage.setItem('name', employee.name || employee.username)
      localStorage.setItem('role', employee.role)
      localStorage.setItem('roleName', 
        employee.role === 'SYSTEM_ADMIN' ? '系统管理员' :
        employee.role === 'DEPARTMENT_MANAGER' ? '部门主管' :
        employee.role === 'PROCESS_ADMIN' ? '流程管理员' : '普通员工'
      )
      
      showToast(`欢迎，${employee.name || employee.username}！`)
      
      // 跳转首页
      router.replace('/')
    } else {
      showToast(res.message || '登录失败，请检查账号密码')
    }
  } catch (error) {
    console.error('登录失败:', error)
    showToast('登录失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

const handleForget = () => {
  showToast('请联系管理员重置密码')
}

const handleRegister = () => {
  showToast('注册功能开发中')
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
.oa-login {
  min-height: 100vh;
  background: #f5f7fa;
  max-width: 430px;
  margin: 0 auto;
  box-shadow: 0 0 20px rgba(0,0,0,0.06);
  display: flex;
  flex-direction: column;
}

/* ===== 顶部装饰 ===== */
.login-header {
  background: linear-gradient(135deg, #3677ef 0%, #5b8def 100%);
  padding: 50px 40px 40px;
  text-align: center;
  border-radius: 0 0 40px 40px;
  flex-shrink: 0;
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
  background: rgba(255,255,255,0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 16px;
}
.logo-title {
  font-size: 28px;
  font-weight: bold;
  color: #fff;
  margin: 0;
  letter-spacing: 2px;
}
.logo-desc {
  color: rgba(255,255,255,0.8);
  font-size: 13px;
  margin: 6px 0 0;
  letter-spacing: 1px;
}

/* ===== 登录表单 ===== */
.login-form {
  flex: 1;
  background: #fff;
  margin: -24px 16px 0;
  border-radius: 20px;
  padding: 28px 24px 24px;
  box-shadow: 0 4px 20px rgba(0,0,0,0.06);
  position: relative;
  z-index: 2;
}
.form-title {
  font-size: 22px;
  font-weight: bold;
  color: #222;
  margin: 0 0 4px;
}
.form-desc {
  font-size: 14px;
  color: #888;
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
  border: 2px solid #f0f0f0;
  background: #fafafa;
  cursor: pointer;
  transition: all 0.2s;
}
.role-option:active {
  transform: scale(0.95);
}
.role-option.active {
  border-color: #3677ef;
  background: #f0f7ff;
}
.role-option span {
  font-size: 13px;
  color: #666;
}
.role-option.active span {
  color: #3677ef;
  font-weight: 500;
}

/* ===== 输入框 ===== */
.input-wrap {
  margin-bottom: 18px;
}
.input-label {
  font-size: 14px;
  font-weight: 500;
  color: #333;
  margin-bottom: 6px;
}
.input-box {
  display: flex;
  align-items: center;
  background: #f5f7fa;
  border-radius: 12px;
  padding: 0 14px;
  border: 2px solid #f5f7fa;
  transition: all 0.3s;
}
.input-box:focus-within {
  border-color: #3677ef;
  background: #fff;
  box-shadow: 0 0 0 4px rgba(54,119,239,0.1);
}
.input-icon {
  flex-shrink: 0;
  margin-right: 10px;
}
.input-field {
  flex: 1;
  background: transparent;
  border: none;
  outline: none;
  padding: 14px 0;
  font-size: 15px;
  color: #333;
  width: 100%;
}
.input-field::placeholder {
  color: #bbb;
  font-size: 14px;
}

/* ===== 登录选项 ===== */
.login-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 4px 0 20px;
}
.login-options :deep(.van-checkbox) {
  font-size: 14px;
  color: #888;
}
.forget-pwd {
  font-size: 14px;
  color: #3677ef;
  cursor: pointer;
}

/* ===== 登录按钮 ===== */
.login-btn-wrap {
  margin-bottom: 12px;
}
.login-btn-wrap :deep(.van-button) {
  background: #3677ef;
  border: none;
  height: 48px;
  font-size: 17px;
  letter-spacing: 2px;
  border-radius: 12px;
}

/* ===== 快速登录 ===== */
.quick-login {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 12px 0 16px;
  border-bottom: 1px solid #f5f5f5;
  margin-bottom: 16px;
  flex-wrap: wrap;
}
.quick-tip {
  font-size: 13px;
  color: #999;
}
.quick-item {
  font-size: 13px;
  color: #3677ef;
  padding: 4px 16px;
  border-radius: 20px;
  border: 1px solid #3677ef;
  cursor: pointer;
  transition: all 0.2s;
}
.quick-item:active {
  background: #3677ef;
  color: #fff;
}

/* ===== 注册入口 ===== */
.register-tip {
  text-align: center;
  font-size: 14px;
  color: #888;
}
.register-link {
  color: #3677ef;
  font-weight: 500;
  cursor: pointer;
}

/* ===== 底部版权 ===== */
.login-footer {
  text-align: center;
  padding: 20px 0 30px;
  flex-shrink: 0;
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
</style>