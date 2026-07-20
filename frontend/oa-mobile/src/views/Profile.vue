<template>
  <div class="oa-profile">
    <div class="banner">
      <div class="banner-text">
        <h2 class="banner-title">个人中心</h2>
        <p class="banner-desc">个人信息 · 便捷管理</p>
      </div>
      <div class="banner-icon">
        <van-icon name="user-o" size="48" color="#00b894" />
      </div>
    </div>

    <div class="user-card">
      <van-image round width="80" height="80" :src="userInfo.avatar || '/default-avatar.png'" />
      <div class="user-info">
        <p class="user-name">{{ userInfo.name }}</p>
        <p class="user-role">{{ userInfo.department }} · {{ userInfo.position }}</p>
        <p class="user-dept">工号：{{ userInfo.employeeId }}</p>
      </div>
    </div>

    <div v-if="!isEditing">
      <div class="info-list">
        <div class="info-item">
          <span class="info-label">姓名</span>
          <span class="info-value">{{ userInfo.name }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">性别</span>
          <span class="info-value">{{ userInfo.gender }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">工号</span>
          <span class="info-value">{{ userInfo.employeeId }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">部门</span>
          <span class="info-value">{{ userInfo.department }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">职位</span>
          <span class="info-value">{{ userInfo.position }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">入职日期</span>
          <span class="info-value">{{ userInfo.joinDate }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">联系电话</span>
          <span class="info-value">{{ userInfo.phone }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">邮箱</span>
          <span class="info-value">{{ userInfo.email }}</span>
        </div>
      </div>

      <div class="edit-wrap">
        <van-button type="primary" block size="large" round @click="startEdit" class="edit-btn">
          编辑个人信息
        </van-button>
        <van-button plain block size="large" round @click="showChangePassword = true" class="pwd-btn">
          修改密码
        </van-button>
      </div>
    </div>

    <div v-else>
      <div class="info-list">
        <div class="info-item">
          <span class="info-label">姓名</span>
          <van-field v-model="editData.name" placeholder="请输入姓名" class="edit-field" />
        </div>
        <div class="info-item">
          <span class="info-label">性别</span>
          <van-field v-model="editData.gender" placeholder="请输入性别" class="edit-field" />
        </div>

        <div class="info-item readonly">
          <span class="info-label">工号</span>
          <span class="info-value readonly-value">{{ userInfo.employeeId }}</span>
        </div>
        <div class="info-item readonly">
          <span class="info-label">部门</span>
          <span class="info-value readonly-value">{{ userInfo.department }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">职位</span>
          <van-field v-model="editData.position" placeholder="请输入职位" class="edit-field" />
        </div>
        <div class="info-item readonly">
          <span class="info-label">入职日期</span>
          <span class="info-value readonly-value">{{ userInfo.joinDate }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">联系电话</span>
          <van-field v-model="editData.phone" placeholder="请输入联系电话" class="edit-field" />
        </div>
        <div class="info-item">
          <span class="info-label">邮箱</span>
          <van-field v-model="editData.email" placeholder="请输入邮箱" class="edit-field" />
        </div>
      </div>

      <div class="edit-tip">
        <van-icon name="info-o" size="14" color="#999" />
        <span>工号、部门和入职日期不可修改，如有问题请联系HR</span>
      </div>

      <div class="edit-wrap">
        <van-button type="primary" block size="large" round @click="saveEdit" class="edit-btn" :loading="saving">
          保存修改
        </van-button>
        <van-button plain block size="large" round @click="cancelEdit" class="cancel-btn" style="margin-top:10px;">
          取消
        </van-button>
      </div>
    </div>

    <div class="bottom-bar">
      <van-button plain block size="large" @click="$router.back()" class="back-btn">
        <van-icon name="arrow-left" size="18" /> 返回
      </van-button>
    </div>

    <div class="safe-bottom"></div>

    <van-popup v-model:show="showChangePassword" position="bottom" round style="padding:20px 16px 30px;">
      <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:16px;">
        <h3 style="margin:0;">🔒 修改密码</h3>
        <van-icon name="close" size="22" @click="showChangePassword = false" />
      </div>

      <van-form @submit="submitChangePassword">
        <van-cell-group inset>
          <van-field
            v-model="passwordData.oldPassword"
            label="原密码"
            type="password"
            placeholder="请输入原密码"
            :rules="[{ required: true, message: '请输入原密码' }]"
          />
          <van-field
            v-model="passwordData.newPassword"
            label="新密码"
            type="password"
            placeholder="请输入新密码"
            :rules="[{ required: true, message: '请输入新密码' }]"
          />
          <van-field
            v-model="passwordData.confirmPassword"
            label="确认密码"
            type="password"
            placeholder="请再次输入新密码"
            :rules="[{ required: true, message: '请确认新密码' }, { validator: confirmPasswordValidator, message: '两次输入的密码不一致' }]"
          />
        </van-cell-group>

        <div style="display:flex;gap:12px;margin-top:16px;">
          <van-button plain block @click="showChangePassword = false">取消</van-button>
          <van-button type="primary" block native-type="submit" :loading="passwordSubmitting">
            确认修改
          </van-button>
        </div>
      </van-form>
    </van-popup>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { getMyInfo, updateProfile, updatePassword } from '@/api/profile'

const router = useRouter()

const userInfo = reactive({
  id: null,
  name: '',
  gender: '',
  employeeId: '',
  department: '',
  departmentId: null,
  position: '',
  joinDate: '',
  phone: '',
  email: '',
  avatar: '',
  role: ''
})

const isEditing = ref(false)
const saving = ref(false)

const editData = reactive({
  name: '',
  gender: '',
  position: '',
  phone: '',
  email: ''
})

const showChangePassword = ref(false)
const passwordSubmitting = ref(false)

const passwordData = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const confirmPasswordValidator = (val) => {
  return val === passwordData.newPassword
}

const fetchMyInfo = async () => {
  try {
    const res = await getMyInfo()
    if (res.code === 0 && res.data) {
      const data = res.data
      userInfo.id = data.id
      userInfo.name = data.name
      userInfo.gender = data.gender || ''
      userInfo.employeeId = data.employeeId || ''
      userInfo.department = data.department || ''
      userInfo.departmentId = data.departmentId
      userInfo.position = data.position || ''
      userInfo.joinDate = data.joinDate || ''
      userInfo.phone = data.phone || ''
      userInfo.email = data.email || ''
      userInfo.avatar = data.avatar || ''
      userInfo.role = data.role || ''
    }
  } catch (error) {
    console.error('获取个人信息失败', error)
  }
}

const startEdit = () => {
  editData.name = userInfo.name
  editData.gender = userInfo.gender
  editData.position = userInfo.position
  editData.phone = userInfo.phone
  editData.email = userInfo.email
  isEditing.value = true
}

const cancelEdit = () => {
  isEditing.value = false
}

const saveEdit = async () => {
  if (!editData.name.trim()) {
    showToast('请输入姓名')
    return
  }
  if (!editData.phone.trim()) {
    showToast('请输入联系电话')
    return
  }
  if (!editData.email.trim()) {
    showToast('请输入邮箱')
    return
  }

  saving.value = true
  try {
    const res = await updateProfile({
      name: editData.name,
      gender: editData.gender,
      position: editData.position,
      phone: editData.phone,
      email: editData.email
    })
    if (res.code === 0) {
      showToast('✅ 个人信息已更新！')
      userInfo.name = editData.name
      userInfo.gender = editData.gender
      userInfo.position = editData.position
      userInfo.phone = editData.phone
      userInfo.email = editData.email
      isEditing.value = false
    } else {
      showToast(res.message || '更新失败')
    }
  } catch (error) {
    showToast('更新失败，请稍后重试')
  } finally {
    saving.value = false
  }
}

const submitChangePassword = async () => {
  if (passwordData.newPassword !== passwordData.confirmPassword) {
    showToast('两次输入的密码不一致')
    return
  }

  passwordSubmitting.value = true
  try {
    const res = await updatePassword({
      oldPassword: passwordData.oldPassword,
      newPassword: passwordData.newPassword
    })
    if (res.code === 0) {
      showToast('✅ 密码修改成功！')
      showChangePassword.value = false
      passwordData.oldPassword = ''
      passwordData.newPassword = ''
      passwordData.confirmPassword = ''
    } else {
      showToast(res.message || '修改失败')
    }
  } catch (error) {
    showToast('修改失败，请稍后重试')
  } finally {
    passwordSubmitting.value = false
  }
}

onMounted(() => {
  fetchMyInfo()
})
</script>

<style scoped>
.oa-profile {
  max-width: 430px;
  margin: 0 auto;
  min-height: 100vh;
  background: #f5f7fa;
  padding: 0 16px 20px;
  box-shadow: 0 0 20px rgba(0,0,0,0.06);
}

.banner {
  background: linear-gradient(135deg, #00b894 0%, #00cec9 100%);
  margin: 0 -16px 16px;
  padding: 36px 20px 28px;
  border-radius: 0 0 24px 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.banner-title { font-size: 24px; font-weight: bold; color: #fff; margin: 0; }
.banner-desc { color: rgba(255,255,255,0.85); font-size: 14px; margin: 4px 0 0; }
.banner-icon {
  background: rgba(255,255,255,0.2);
  border-radius: 50%;
  width: 60px;
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.user-card {
  background: #fff;
  border-radius: 14px;
  padding: 24px 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}
.user-info { flex: 1; }
.user-name { font-size: 20px; font-weight: bold; margin: 0; color: #222; }
.user-role { font-size: 14px; color: #888; margin: 4px 0 0; }
.user-dept { font-size: 13px; color: #bbb; margin: 2px 0 0; }

.info-list {
  background: #fff;
  border-radius: 14px;
  padding: 4px 16px;
  margin-bottom: 16px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}
.info-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 0;
  border-bottom: 1px solid #f5f5f5;
}
.info-item:last-child { border-bottom: none; }
.info-label {
  font-size: 15px;
  color: #888;
  flex-shrink: 0;
  width: 80px;
}
.info-value {
  font-size: 15px;
  color: #222;
  text-align: right;
}

.info-item.readonly {
  background: #f8f9fa;
  margin: 0 -16px;
  padding: 10px 16px;
  border-radius: 0;
}
.info-item.readonly .info-label { color: #aaa; }
.info-value.readonly-value { color: #bbb; }

.edit-field {
  padding: 0 !important;
  flex: 1;
}
.edit-field :deep(.van-field__body) {
  background: transparent !important;
  padding: 0 !important;
}
.edit-field :deep(input) {
  text-align: right;
  font-size: 15px;
  color: #222;
}

.edit-tip {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 4px 16px;
  font-size: 13px;
  color: #999;
}

.edit-wrap { padding: 4px 0 12px; }
.edit-btn {
  height: 48px;
  font-size: 16px;
  background: #00b894;
  border: none;
}
.edit-btn:active { opacity: 0.85; }
.pwd-btn {
  height: 48px;
  font-size: 16px;
  margin-top: 10px;
  border-color: #00b894;
  color: #00b894;
}
.pwd-btn:active { background: #e8f8f0; }
.cancel-btn {
  height: 48px;
  font-size: 16px;
  border-color: #ccc;
  color: #888;
}

.bottom-bar { padding: 16px 0 8px; }
.back-btn {
  border-radius: 12px !important;
  height: 48px !important;
  font-size: 16px !important;
  border-color: #00b894 !important;
  color: #00b894 !important;
}
.back-btn:active { background: #e8f8f0 !important; }

.safe-bottom { height: 20px; }
:deep(.van-popup) { border-radius: 16px 16px 0 0; }
</style>