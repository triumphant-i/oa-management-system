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
      <div class="avatar-wrapper" @click="showAvatarUpload = true">
        <van-image round width="80" height="80" :src="userInfo.avatar || '/default-avatar.png'" />
        <div class="avatar-edit-mask">
          <van-icon name="camera-o" size="24" color="#fff" />
        </div>
      </div>
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
          <span class="info-label">账号名</span>
          <span class="info-value">{{ userInfo.username }}</span>
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
          <span class="info-label">班次</span>
          <span class="info-value">{{ userInfo.shiftName ? userInfo.shiftName + ' (' + userInfo.workStart + '-' + userInfo.workEnd + ')' : '未分配班次' }}</span>
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
          <span class="info-label">账号名</span>
          <van-field v-model="editData.username" placeholder="请输入账号名" class="edit-field" />
        </div>
        <div class="info-item">
          <span class="info-label">性别</span>
          <van-field
            :model-value="genderMap[editData.gender] || ''"
            placeholder="请选择性别"
            is-link
            @click="showGenderPicker = true"
            class="edit-field"
          />
        </div>

        <div class="info-item readonly">
          <span class="info-label">工号</span>
          <span class="info-value readonly-value">{{ userInfo.employeeId }}</span>
        </div>
        <div class="info-item readonly">
          <span class="info-label">部门</span>
          <span class="info-value readonly-value">{{ userInfo.department }}</span>
        </div>
        <div class="info-item readonly">
          <span class="info-label">职位</span>
          <span class="info-value readonly-value">{{ userInfo.position }}</span>
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

    <van-popup v-model:show="showGenderPicker" position="bottom" round>
      <van-picker :columns="genderColumns" @confirm="onConfirmGender" @cancel="showGenderPicker = false" title="选择性别" />
    </van-popup>

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

    <van-popup v-model:show="showAvatarUpload" position="center" round style="width:80%;">
      <div style="padding:20px;">
        <h3 style="margin:0 0 16px;text-align:center;">上传头像</h3>
        
        <div style="display:flex;flex-direction:column;gap:12px;">
          <van-button type="primary" block @click="chooseAvatar">
            <van-icon name="plus" /> 选择图片
          </van-button>
          
          <div v-if="avatarPreview" style="text-align:center;">
            <van-image :src="avatarPreview" round width="120" height="120" style="margin:0 auto;" />
          </div>
          
          <div v-if="avatarPreview" style="display:flex;gap:12px;">
            <van-button plain block @click="avatarPreview = ''">取消</van-button>
            <van-button type="primary" block @click="uploadAvatar" :loading="uploading">
              确认上传
            </van-button>
          </div>
        </div>
      </div>
    </van-popup>

    <input type="file" ref="avatarInput" accept="image/*" style="display:none;" @change="onAvatarChange" />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { getMyInfo, updateProfile, updatePassword } from '@/api/profile'
import { getEmployeeShift } from '@/api/attendance'
import request from '@/utils/request'

const router = useRouter()

const userInfo = reactive({
  id: null,
  name: '',
  username: '',
  gender: '',
  employeeId: '',
  department: '',
  departmentId: null,
  position: '',
  joinDate: '',
  phone: '',
  email: '',
  avatar: '',
  role: '',
  shiftId: null,
  shiftName: '',
  workStart: '',
  workEnd: ''
})

const isEditing = ref(false)
const saving = ref(false)
const editData = reactive({
  name: '',
  username: '',
  gender: '',
  position: '',
  phone: '',
  email: ''
})

const showChangePassword = ref(false)
const passwordSubmitting = ref(false)
const showGenderPicker = ref(false)
const showAvatarUpload = ref(false)
const avatarPreview = ref('')
const avatarInput = ref(null)
const uploading = ref(false)

const genderColumns = [
  { text: '男', value: '男' },
  { text: '女', value: '女' }
]

const genderMap = {
  '男': '男',
  '女': '女'
}

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
      userInfo.username = data.username || ''
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
      userInfo.shiftId = data.shiftId
      userInfo.shiftName = data.shiftName || ''
      userInfo.workStart = data.workStart || ''
      userInfo.workEnd = data.workEnd || ''
    }
  } catch (error) {
    console.error('获取个人信息失败', error)
  }
}

const fetchEmployeeShift = async (employeeId) => {
  try {
    const res = await getEmployeeShift(employeeId)
    if (res.code === 0 && res.data) {
      employeeShift.value = res.data
    }
  } catch (error) {
    console.error('获取员工班次失败', error)
  }
}

const formatShiftTime = (timeStr) => {
  if (!timeStr) return ''
  return timeStr.substring(0, 5)
}

const startEdit = () => {
  editData.name = userInfo.name
  editData.username = userInfo.username
  editData.gender = userInfo.gender
  editData.position = userInfo.position
  editData.phone = userInfo.phone
  editData.email = userInfo.email
  isEditing.value = true
}

const cancelEdit = () => {
  isEditing.value = false
}

const onConfirmGender = ({ selectedValues }) => {
  editData.gender = selectedValues[0]
  showGenderPicker.value = false
}

const saveEdit = async () => {
  // 前端验证
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
  
  // 手机格式验证
  const phoneRegex = /^1[3-9]\d{9}$/
  if (!phoneRegex.test(editData.phone)) {
    showToast('请输入正确的11位手机号')
    return
  }
  
  // 邮箱格式验证
  const emailRegex = /^[a-zA-Z0-9](?:[a-zA-Z0-9._%+-]{0,61}[a-zA-Z0-9])?@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z]{2,})+$/
  if (!emailRegex.test(editData.email)) {
    showToast('请输入正确的邮箱格式')
    return
  }

  saving.value = true
  try {
    const res = await updateProfile({
      name: editData.name,
      username: editData.username,
      gender: editData.gender,
      phone: editData.phone,
      email: editData.email
    })
    if (res.code === 0) {
      showToast('✅ 个人信息已更新！')
      userInfo.name = editData.name
      userInfo.username = editData.username
      userInfo.gender = editData.gender
      userInfo.phone = editData.phone
      userInfo.email = editData.email
      isEditing.value = false
    }
  } catch (error) {
    // 响应拦截器已处理业务错误提示，此处仅处理网络等未知错误
    console.error('更新失败', error)
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

const chooseAvatar = () => {
  avatarInput.value?.click()
}

const onAvatarChange = (event) => {
  const file = event.target.files?.[0]
  if (file) {
    const reader = new FileReader()
    reader.onload = (e) => {
      avatarPreview.value = e.target?.result
    }
    reader.readAsDataURL(file)
  }
}

const uploadAvatar = async () => {
  const file = avatarInput.value?.files?.[0]
  if (!file) {
    showToast('请选择图片')
    return
  }

  uploading.value = true
  try {
    const formData = new FormData()
    formData.append('file', file)

    const res = await request({
      url: '/profile/uploadAvatar',
      method: 'post',
      data: formData,
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })

    if (res.code === 0) {
      showToast('✅ 头像上传成功！')
      userInfo.avatar = res.data?.avatar || ''
      showAvatarUpload.value = false
      avatarPreview.value = ''
    } else {
      showToast(res.message || '上传失败')
    }
  } catch (error) {
    showToast('上传失败，请稍后重试')
  } finally {
    uploading.value = false
    avatarInput.value.value = ''
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

.avatar-wrapper {
  position: relative;
  cursor: pointer;
}

.avatar-edit-mask {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 30px;
  background: rgba(0,0,0,0.5);
  border-radius: 0 0 40px 40px;
  display: flex;
  align-items: center;
  justify-content: center;
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