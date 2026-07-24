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
          />
        </el-form-item>

        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            size="large"
            :prefix-icon="Lock"
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

      <!-- 添加调试信息 -->
      <el-alert
        v-if="showDebug"
        title="调试信息"
        type="info"
        :closable="false"
        style="margin-top:12px;"
      >
        <template #default>
          <div style="font-size:12px;word-break:break-all;">
            <p><strong>状态:</strong> {{ debugStatus }}</p>
            <p><strong>响应:</strong> {{ debugResponse }}</p>
          </div>
        </template>
      </el-alert>

      <div class="demo-accounts">
        <div class="demo-title">演示账号</div>
        <div class="demo-list">
          <el-tag size="small" type="success" @click="fillAccount('admin', '123456')" style="cursor:pointer;">admin / 123456 (超级管理员)</el-tag>
          <el-tag size="small" type="primary" @click="fillAccount('zhangsan', '123456')" style="cursor:pointer;">zhangsan / 123456 (部门经理)</el-tag>
          <el-tag size="small" type="warning" @click="fillAccount('emp001', '123456')" style="cursor:pointer;">emp001 / 123456 (普通员工)</el-tag>
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
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import request from '@/api/request'

const router = useRouter()
const formRef = ref()
const loading = ref(false)
const showDebug = ref(true) // 显示调试信息
const debugStatus = ref('等待登录...')
const debugResponse = ref('')

const form = reactive({
  username: 'admin',
  password: '123456'
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
  ElMessage.success(`已填充账号: ${username}`)
}

const handleSubmit = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    loading.value = true
    debugStatus.value = '正在登录...'
    debugResponse.value = ''

    try {
      console.log('=== 开始登录 ===')
      console.log('用户名:', form.username)
      console.log('密码:', form.password)

      const res = await request({
        url: '/employee/login',
        method: 'POST',
        data: {
          username: form.username,
          password: form.password
        }
      })

      console.log('登录响应:', res)
      debugResponse.value = JSON.stringify(res, null, 2)

      if (res.code === 0) {
        const token = res.data?.token
        const employee = res.data?.employee
        
        if (!token) {
          ElMessage.error('登录失败：未获取到Token')
          debugStatus.value = '❌ 未获取到Token'
          loading.value = false
          return
        }
        
        debugStatus.value = '✅ 登录成功'
        
        // 保存 Token
        localStorage.setItem('token', token)
        
        // 保存用户信息
        const userInfo = {
          id: employee.id,
          username: employee.username,
          name: employee.name,
          role: employee.role || 'EMPLOYEE',
          departmentId: employee.departmentId
        }
        localStorage.setItem('userInfo', JSON.stringify(userInfo))
        
        ElMessage.success('登录成功！')
        
        setTimeout(() => {
          router.replace('/employee')
        }, 300)
      } else {
        debugStatus.value = '❌ 登录失败'
        ElMessage.error(res.msg || res.message || '登录失败')
      }
    } catch (error) {
      console.error('登录失败:', error)
      debugStatus.value = '❌ 请求异常'
      debugResponse.value = error.message || '未知错误'
      
      // 如果是网络错误，给出更友好的提示
      if (error.message && error.message.includes('Network Error')) {
        ElMessage.error('网络连接失败，请检查后端服务是否启动 (http://localhost:8080)')
      } else if (error.message && error.message.includes('密码错误')) {
        ElMessage.error('密码错误，请重试 (提示: 数据库密码可能需要重置)')
      } else {
        ElMessage.error(error.message || '登录失败，请检查网络连接')
      }
    } finally {
      loading.value = false
    }
  })
}
</script>

<style scoped>
.login-container {
  width: 100%;
  height: 100vh;
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%);
  display: flex;
  justify-content: center;
  align-items: center;
}

.login-box {
  width: 460px;
  padding: 45px 40px 30px;
  background: #ffffff;
  border-radius: 16px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  max-height: 95vh;
  overflow-y: auto;
}

.logo-wrap {
  text-align: center;
  margin-bottom: 32px;
}

.logo-icon {
  width: 72px;
  height: 72px;
  margin: 0 auto 14px;
  background: linear-gradient(135deg, #409eff, #1d7cf0);
  border-radius: 18px;
  display: flex;
  justify-content: center;
  align-items: center;
  color: #fff;
  font-size: 28px;
  font-weight: bold;
  box-shadow: 0 8px 24px rgba(64, 158, 255, 0.35);
}

.title {
  margin: 0;
  color: #1a1a2e;
  font-size: 24px;
  font-weight: bold;
  letter-spacing: 3px;
}

.sub-title {
  margin: 6px 0 0;
  color: #999;
  font-size: 14px;
  letter-spacing: 4px;
}

.demo-accounts {
  margin-top: 20px;
  padding: 12px 16px;
  background: #f8f9fa;
  border-radius: 8px;
}

.demo-title {
  font-size: 12px;
  color: #999;
  margin-bottom: 8px;
}

.demo-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.demo-list .el-tag {
  font-size: 11px;
}

.demo-list .el-tag:hover {
  opacity: 0.8;
}

.footer-info {
  text-align: center;
  margin-top: 22px;
  color: #bbb;
  font-size: 13px;
}

.footer-info .divider {
  margin: 0 10px;
}

:deep(.el-input__wrapper) {
  height: 46px;
  border-radius: 10px;
}

:deep(.el-button) {
  height: 48px;
  border-radius: 10px;
  font-size: 16px;
  letter-spacing: 6px;
}

:deep(.el-alert) {
  border-radius: 8px;
}

:deep(.el-alert__content) {
  width: 100%;
}
</style>