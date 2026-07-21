<template>
  <div class="login-container">
    <div class="login-box">
      <div class="logo-wrap">
        <div class="logo-icon">OA</div>
        <h3 class="title">智慧OA管理系统</h3>
        <p class="sub-title">企业数字化管理平台</p>
      </div>

      <el-form :model="form" :rules="rules" ref="formRef" label-width="0">
        <el-form-item prop="username">
          <el-input
            v-model="form.username"
            placeholder="请输入账号"
            size="large"
            :prefix-icon="User"
            clearable
          />
        </el-form-item>

        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            size="large"
            :prefix-icon="Lock"
            show-password
            @keyup.enter="handleSubmit"
          />
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            size="large"
            style="width:100%;"
            :loading="loading"
            @click="handleSubmit"
          >登 录</el-button>
        </el-form-item>
      </el-form>

      <div class="demo-accounts">
        <div class="demo-title">演示账号</div>
        <div class="demo-list">
          <el-tag 
            size="small" 
            type="success" 
            @click="fillAccount('admin', '123456')" 
            style="cursor:pointer;"
          >
            admin / 123456
          </el-tag>
          <el-tag 
            size="small" 
            type="primary" 
            @click="fillAccount('zhangsan', '123456')" 
            style="cursor:pointer;"
          >
            zhangsan / 123456
          </el-tag>
          <el-tag 
            size="small" 
            type="warning" 
            @click="fillAccount('emp001', '123456')" 
            style="cursor:pointer;"
          >
            emp001 / 123456
          </el-tag>
        </div>
        <div style="margin-top:6px;font-size:12px;color:#999;">
          💡 点击账号自动填充
        </div>
      </div>

      <div class="footer-info">
        <span>智慧OA v2.0</span>
        <span class="divider">|</span>
        <span>企业数字化管理平台</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import request from '@/api/request'

const router = useRouter()
const formRef = ref()
const loading = ref(false)

const form = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [
    { required: true, message: '请输入账号', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' }
  ]
}

// 点击标签自动填充
const fillAccount = (username, password) => {
  form.username = username
  form.password = password
}

const handleSubmit = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

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
        const token = res.data?.token
        const employee = res.data?.employee || res.data
        
        if (!token) {
          ElMessage.error('登录失败：未获取到Token')
          loading.value = false
          return
        }
        
        // 保存 Token
        localStorage.setItem('token', token)
        
        // 保存用户信息
        const userInfo = {
          id: employee?.id,
          username: employee?.username || form.username,
          name: employee?.name || form.username,
          role: employee?.role || 'EMPLOYEE',
          departmentId: employee?.departmentId,
          avatar: employee?.avatar || '',
          phone: employee?.phone || '',
          email: employee?.email || ''
        }
        localStorage.setItem('userInfo', JSON.stringify(userInfo))
        
        // 保存员工ID（用于其他页面）
        if (employee?.id) {
          localStorage.setItem('employeeId', String(employee.id))
        }
        
        ElMessage.success(`欢迎回来，${userInfo.name}！`)
        
        setTimeout(() => {
          router.replace('/employee')
        }, 300)
      } else {
        ElMessage.error(res.msg || res.message || '登录失败')
      }
    } catch (error) {
      console.error('登录失败:', error)
      
      // 根据不同错误给出友好提示
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
  })
}

// 页面加载时检查是否已登录
onMounted(() => {
  const token = localStorage.getItem('token')
  if (token) {
    router.replace('/employee')
  }
})
</script>

<style scoped>
.login-container {
  width: 100%;
  height: 100vh;
  background: url('@/assets/login-bg.jpg') no-repeat center center;
  background-size: cover;
  display: flex;
  justify-content: center;
  align-items: center;
}

.login-box {
  width: 440px;
  padding: 40px 36px 28px;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 16px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.2);
  backdrop-filter: blur(10px);
}

.logo-wrap {
  text-align: center;
  margin-bottom: 30px;
}

.logo-icon {
  width: 64px;
  height: 64px;
  margin: 0 auto 12px;
  background: linear-gradient(135deg, #409eff, #1d7cf0);
  border-radius: 16px;
  display: flex;
  justify-content: center;
  align-items: center;
  color: #fff;
  font-size: 26px;
  font-weight: bold;
  box-shadow: 0 8px 24px rgba(64, 158, 255, 0.35);
}

.title {
  margin: 0;
  color: #1a1a2e;
  font-size: 22px;
  font-weight: bold;
  letter-spacing: 2px;
}

.sub-title {
  margin: 4px 0 0;
  color: #999;
  font-size: 13px;
  letter-spacing: 3px;
}

.demo-accounts {
  margin-top: 18px;
  padding: 10px 14px;
  background: #f5f7fa;
  border-radius: 8px;
}

.demo-title {
  font-size: 12px;
  color: #999;
  margin-bottom: 6px;
}

.demo-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.demo-list .el-tag {
  font-size: 11px;
  cursor: pointer;
  transition: all 0.3s;
}

.demo-list .el-tag:hover {
  transform: scale(1.05);
  opacity: 0.8;
}

.footer-info {
  text-align: center;
  margin-top: 20px;
  color: #bbb;
  font-size: 12px;
}

.footer-info .divider {
  margin: 0 10px;
}

:deep(.el-input__wrapper) {
  height: 44px;
  border-radius: 10px;
}

:deep(.el-button) {
  height: 46px;
  border-radius: 10px;
  font-size: 16px;
  letter-spacing: 4px;
}
</style>