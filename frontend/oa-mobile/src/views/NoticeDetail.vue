<template>
  <div class="oa-notice-detail">
    <!-- 顶部导航 -->
    <div class="page-header">
      <van-icon name="arrow-left" size="22" color="#333" @click="$router.back()" />
      <h1 class="header-title">公告详情</h1>
      <div class="header-right">
        <van-icon name="more-o" size="22" color="#333" v-if="isAdmin" @click="showActionSheet = true" />
      </div>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading-wrap">
      <van-loading size="24px">加载中...</van-loading>
    </div>

    <!-- 公告内容 -->
    <div v-else-if="detail" class="detail-content">
      <div class="detail-header">
        <div class="tag-row">
          <van-tag :type="getPriorityTagType(detail.priority)" size="medium">
            {{ detail.priority || '普通' }}
          </van-tag>
          <van-tag v-if="detail.isTop === 1" type="danger" size="medium">置顶</van-tag>
          <van-tag v-if="detail.category" plain type="primary" size="medium">
            {{ detail.category }}
          </van-tag>
        </div>
        <h2 class="detail-title">{{ detail.title }}</h2>
        <div class="detail-meta">
          <span class="detail-author">
            <van-icon name="user-o" size="14" />
            {{ detail.publisherName || '未知' }}
          </span>
          <span class="detail-time">
            <van-icon name="clock-o" size="14" />
            {{ formatTime(detail.publishTime) }}
          </span>
        </div>
      </div>

      <div class="detail-body">
        <p class="content-text">{{ detail.content || '暂无内容' }}</p>
      </div>

      <div class="detail-footer" v-if="detail.updateTime">
        <p class="update-info">最后更新：{{ formatTime(detail.updateTime) }}</p>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-else class="empty-state">
      <van-icon name="notes-o" size="48" color="#ccc" />
      <p>公告不存在或已被删除</p>
    </div>

    <!-- ===== 操作菜单 ===== -->
    <van-action-sheet v-model:show="showActionSheet" :actions="actions" @select="onActionSelect" cancel-text="取消" closeable />

    <!-- ===== 编辑弹窗 ===== -->
    <van-popup v-model:show="showEdit" position="bottom" round style="padding:20px 16px 30px;">
      <h3 style="margin-bottom:16px;text-align:center;">编辑公告</h3>
      <van-form @submit="onSubmitEdit">
        <van-field 
          v-model="editForm.title" 
          label="标题" 
          placeholder="请输入公告标题" 
          :rules="[{ required: true, message: '请输入标题' }]"
        />
        <van-field 
          v-model="editForm.category" 
          label="分类" 
          placeholder="通知/制度/会议纪要" 
          is-link 
          @click="showCategoryPicker = true"
          readonly
        />
        <van-field 
          v-model="editForm.priority" 
          label="优先级" 
          placeholder="紧急/重要/普通" 
          is-link 
          @click="showPriorityPicker = true"
          readonly
        />
        <van-field
          v-model="editForm.content"
          label="内容"
          type="textarea"
          placeholder="请输入公告内容"
          rows="6"
          :rules="[{ required: true, message: '请输入内容' }]"
        />
        <div style="display:flex;gap:12px;margin-top:16px;">
          <van-button plain block @click="showEdit = false">取消</van-button>
          <van-button type="primary" block native-type="submit" :loading="submitting">保存修改</van-button>
        </div>
      </van-form>
    </van-popup>

    <!-- 分类选择 -->
    <van-action-sheet v-model:show="showCategoryPicker" title="选择分类">
      <div class="picker-list">
        <div class="picker-item" v-for="item in categoryOptions" :key="item" @click="selectCategory(item)">
          <span>{{ item }}</span>
          <van-icon v-if="editForm.category === item" name="success" color="#00b894" />
        </div>
      </div>
    </van-action-sheet>

    <!-- 优先级选择 -->
    <van-action-sheet v-model:show="showPriorityPicker" title="选择优先级">
      <div class="picker-list">
        <div class="picker-item" v-for="item in priorityOptions" :key="item" @click="selectPriority(item)">
          <span>{{ item }}</span>
          <van-icon v-if="editForm.priority === item" name="success" color="#00b894" />
        </div>
      </div>
    </van-action-sheet>

    <!-- ===== 底部返回键 ===== -->
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
import { useRouter, useRoute } from 'vue-router'
import { showToast, showConfirmDialog } from 'vant'
import { 
  getAnnouncementById, 
  updateAnnouncement, 
  deleteAnnouncement,
  setTopAnnouncement,
  cancelTopAnnouncement
} from '@/api/announcement'

const router = useRouter()
const route = useRoute()

// 状态
const loading = ref(false)
const submitting = ref(false)
const detail = ref(null)

// 弹窗
const showActionSheet = ref(false)
const showEdit = ref(false)
const showCategoryPicker = ref(false)
const showPriorityPicker = ref(false)

// 编辑表单
const editForm = ref({
  id: null,
  title: '',
  category: '',
  priority: '',
  content: ''
})

// 选项
const categoryOptions = ['通知', '制度', '会议纪要']
const priorityOptions = ['紧急', '重要', '普通']

// 权限判断
const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}')
const isAdmin = computed(() => userInfo.role === 'SYSTEM_ADMIN')

// 操作菜单
const actions = computed(() => {
  if (!detail.value) return []
  const list = [
    { name: '编辑公告', action: 'edit' }
  ]
  
  if (detail.value.isTop === 1) {
    list.push({ name: '取消置顶', action: 'cancelTop' })
  } else {
    list.push({ name: '置顶公告', action: 'setTop' })
  }
  
  list.push({ name: '删除公告', action: 'delete', color: '#ee0a24' })
  
  return list
})

// 优先级标签类型
const getPriorityTagType = (priority) => {
  const types = {
    '紧急': 'danger',
    '重要': 'warning',
    '普通': 'primary'
  }
  return types[priority] || 'primary'
}

// 格式化时间
const formatTime = (time) => {
  if (!time) return ''
  return time.replace('T', ' ').substring(0, 19)
}

// 加载详情
const loadDetail = async () => {
  loading.value = true
  const id = route.params.id
  try {
    const res = await getAnnouncementById(id)
    console.log('公告详情：', res)
    if (res.code === 0 && res.data) {
      detail.value = res.data
      editForm.value = {
        id: res.data.id,
        title: res.data.title || '',
        category: res.data.category || '',
        priority: res.data.priority || '',
        content: res.data.content || ''
      }
    } else {
      showToast(res.message || '获取详情失败')
    }
  } catch (error) {
    console.error('加载详情失败：', error)
    showToast('加载失败')
  } finally {
    loading.value = false
  }
}

// 操作菜单选择
const onActionSelect = (item) => {
  showActionSheet.value = false
  
  switch (item.action) {
      case 'edit':
        showEdit.value = true
        break
      case 'setTop':
        handleSetTop()
        break
      case 'cancelTop':
        handleCancelTop()
        break
      case 'delete':
        handleDelete()
        break
    }
}

// 置顶
const handleSetTop = async () => {
  try {
    const res = await setTopAnnouncement(detail.value.id)
    if (res.code === 0) {
      showToast('置顶成功')
      loadDetail()
    } else {
      showToast(res.message || '置顶失败')
    }
  } catch (error) {
    console.error('置顶失败：', error)
    showToast('操作失败')
  }
}

// 取消置顶
const handleCancelTop = async () => {
  try {
    const res = await cancelTopAnnouncement(detail.value.id)
    if (res.code === 0) {
      showToast('取消置顶成功')
      loadDetail()
    } else {
      showToast(res.message || '取消置顶失败')
    }
  } catch (error) {
    console.error('取消置顶失败：', error)
    showToast('操作失败')
  }
}

// 删除
const handleDelete = async () => {
  try {
    await showConfirmDialog({
      title: '确认删除',
      message: '确定要删除此公告吗？此操作不可恢复！',
      confirmButtonText: '确定删除',
      confirmButtonColor: '#ee0a24'
    })
    
    const res = await deleteAnnouncement(detail.value.id)
    if (res.code === 0) {
      showToast('删除成功')
      router.back()
    } else {
      showToast(res.message || '删除失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败：', error)
      showToast('操作失败')
    }
  }
}

// 选择分类
const selectCategory = (item) => {
  editForm.value.category = item
  showCategoryPicker.value = false
}

// 选择优先级
const selectPriority = (item) => {
  editForm.value.priority = item
  showPriorityPicker.value = false
}

// 提交编辑
const onSubmitEdit = async () => {
  submitting.value = true
  try {
    const data = {
      id: editForm.value.id,
      title: editForm.value.title,
      category: editForm.value.category,
      priority: editForm.value.priority,
      content: editForm.value.content
    }
    const res = await updateAnnouncement(data)
    if (res.code === 0) {
      showToast('更新成功')
      showEdit.value = false
      loadDetail()
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

// 初始化
onMounted(() => {
  loadDetail()
})
</script>

<style scoped>
.oa-notice-detail {
  max-width: 430px;
  margin: 0 auto;
  min-height: 100vh;
  background: #f5f7fa;
  padding: 0 16px 20px;
  box-shadow: 0 0 20px rgba(0,0,0,0.06);
}

/* ===== 顶部导航 ===== */
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 16px 12px;
  background: #fff;
  margin: 0 -16px 16px;
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

.loading-wrap { text-align: center; padding: 40px 0; }

/* ===== 公告内容 ===== */
.detail-content {
  background: #fff;
  border-radius: 14px;
  padding: 20px;
  margin-bottom: 16px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}

.detail-header {
  border-bottom: 1px solid #f0f0f0;
  padding-bottom: 16px;
  margin-bottom: 16px;
}

.tag-row {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}

.detail-title {
  font-size: 20px;
  font-weight: bold;
  color: #222;
  margin: 0 0 12px;
  line-height: 1.4;
}

.detail-meta {
  display: flex;
  align-items: center;
  gap: 16px;
  font-size: 13px;
  color: #999;
}
.detail-author, .detail-time {
  display: flex;
  align-items: center;
  gap: 4px;
}

.detail-body {
  font-size: 15px;
  line-height: 1.8;
  color: #444;
}
.content-text {
  margin: 0;
  white-space: pre-wrap;
  word-wrap: break-word;
}

.detail-footer {
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
}
.update-info {
  font-size: 12px;
  color: #bbb;
  margin: 0;
}

/* ===== 选择器 ===== */
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

/* ===== 空状态 ===== */
.empty-state {
  text-align: center;
  padding: 60px 20px;
  color: #ccc;
}
.empty-state p { margin-top: 12px; font-size: 15px; }

/* ===== 底部返回键 ===== */
.bottom-bar {
  padding: 16px 0 8px;
}
.back-btn {
  border-radius: 12px !important;
  height: 48px !important;
  font-size: 16px !important;
  border-color: #00b894 !important;
  color: #00b894 !important;
}
.back-btn:active {
  background: #e8f8f0 !important;
}

.safe-bottom { height: 20px; }
</style>