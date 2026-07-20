<template>
  <div class="oa-notice">
    <div class="banner">
      <div class="banner-text">
        <h2 class="banner-title">公告中心</h2>
        <p class="banner-desc">及时获取 · 最新动态</p>
      </div>
      <div class="banner-right">
        <van-button 
          v-if="isAdmin" 
          type="primary" 
          size="small" 
          round 
          @click="showPublish = true"
          style="background:rgba(255,255,255,0.2);border:none;color:#fff;"
        >
          <van-icon name="plus" /> 发布公告
        </van-button>
        <div class="banner-icon">
          <van-icon name="bullhorn-o" size="48" color="#00b894" />
        </div>
      </div>
    </div>

    <!-- 阅读统计 -->
    <div class="stat-row">
      <div class="stat-card" style="background: linear-gradient(135deg, #fff, #f0fff4);" @click="activeFilter = 'all'">
        <p class="stat-label">总公告</p>
        <span class="stat-num" style="color: #00b894;">{{ availableCount }}</span>
      </div>
      <div class="stat-card" style="background: linear-gradient(135deg, #fff, #fff8ef);" @click="activeFilter = 'unread'">
        <p class="stat-label">未读</p>
        <span class="stat-num" style="color: #ff6b35;">{{ getCount('unread') }}</span>
      </div>
      <div class="stat-card" style="background: linear-gradient(135deg, #fff, #f0f7ff);" @click="activeFilter = 'read'">
        <p class="stat-label">已读</p>
        <span class="stat-num" style="color: #3677ef;">{{ getCount('read') }}</span>
      </div>
      <div class="stat-card" style="background: linear-gradient(135deg, #fff, #fef0f0);" @click="showStatsDialog = true">
        <p class="stat-label">阅读率</p>
        <span class="stat-num" style="color: #6c5ce7;">{{ readRate }}%</span>
      </div>
    </div>

    <!-- 分类筛选 -->
    <div class="filter-tabs">
      <span class="filter-tab" :class="{ active: activeFilter === 'all' }" @click="activeFilter = 'all'">全部</span>
      <span class="filter-tab" :class="{ active: activeFilter === '重要' }" @click="activeFilter = '重要'">重要</span>
      <span class="filter-tab" :class="{ active: activeFilter === '通知' }" @click="activeFilter = '通知'">通知</span>
      <span class="filter-tab" :class="{ active: activeFilter === '制度' }" @click="activeFilter = '制度'">制度</span>
      <span class="filter-tab" :class="{ active: activeFilter === '会议纪要' }" @click="activeFilter = '会议纪要'">会议纪要</span>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading-wrap">
      <van-loading size="24px">加载中...</van-loading>
    </div>

    <!-- 公告列表 -->
    <div v-else class="notice-list">
      <div class="notice-item" v-for="item in filteredList" :key="item.id" @click="goDetail(item.id)">
        <!-- 置顶标识 -->
        <div class="notice-pinned" v-if="item.isTop === 1">
          <van-icon name="fire" size="14" color="#e17055" />
          <span class="pinned-text">置顶</span>
        </div>

        <div class="notice-header">
          <van-tag :type="getPriorityTagType(item.priority)" size="small">
            {{ item.priority || '普通' }}
          </van-tag>
          <span class="notice-title">{{ item.title }}</span>
          <span class="notice-time">{{ formatTime(item.publishTime) }}</span>
        </div>
        <p class="notice-desc">{{ item.content ? item.content.substring(0, 80) + '...' : '暂无内容' }}</p>
        <div class="notice-footer">
          <span class="notice-author">{{ item.publisherName || '未知' }}</span>
          <div class="notice-right">
            <span class="notice-category" v-if="item.category">{{ item.category }}</span>
            <!-- 管理员操作按钮 -->
            <div class="admin-actions" v-if="isAdmin">
              <van-icon 
                :name="item.isTop === 1 ? 'fire' : 'fire-o'" 
                size="16" 
                :color="item.isTop === 1 ? '#e17055' : '#ccc'"
                @click.stop="togglePin(item)" 
                :title="item.isTop === 1 ? '取消置顶' : '置顶'"
              />
              <van-icon 
                name="edit" 
                size="16" 
                color="#3677ef" 
                @click.stop="openEditNotice(item)" 
                title="编辑"
              />
              <van-icon 
                name="delete-o" 
                size="16" 
                color="#ee0a24" 
                @click.stop="deleteNotice(item)" 
                title="删除"
              />
            </div>
          </div>
        </div>
      </div>
      <div class="empty-state" v-if="filteredList.length === 0">
        <van-icon name="notes-o" size="48" color="#ccc" />
        <p>暂无公告</p>
      </div>
    </div>

    <!-- ===== 底部返回键 ===== -->
    <div class="bottom-bar">
      <van-button plain block size="large" @click="$router.back()" class="back-btn">
        <van-icon name="arrow-left" size="18" /> 返回
      </van-button>
    </div>

    <div class="safe-bottom"></div>

    <!-- ============================================= -->
    <!-- ===== 发布/编辑公告弹窗 ===== -->
    <!-- ============================================= -->
    <van-popup v-model:show="showPublish" position="bottom" round style="padding:20px 16px 30px;max-height:85vh;overflow-y:auto;">
      <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:16px;">
        <h3 style="margin:0;">{{ isEditNotice ? '编辑公告' : '发布公告' }}</h3>
        <van-icon name="close" size="22" @click="closePublish" />
      </div>

      <van-form @submit="onPublish">
        <van-cell-group inset>
          <van-field
            v-model="publishData.title"
            label="标题"
            placeholder="请输入公告标题"
            :rules="[{ required: true, message: '请输入公告标题' }]"
          />
          <van-field
            v-model="publishData.category"
            label="分类"
            placeholder="请选择公告分类"
            is-link
            @click="showCategoryPicker = true"
            :rules="[{ required: true, message: '请选择公告分类' }]"
          />
          <van-field
            v-model="publishData.priority"
            label="优先级"
            placeholder="请选择优先级"
            is-link
            @click="showPriorityPicker = true"
            :rules="[{ required: true, message: '请选择优先级' }]"
          />
          <van-field
            v-model="publishData.content"
            label="内容"
            placeholder="请输入公告内容"
            type="textarea"
            rows="5"
            :rules="[{ required: true, message: '请输入公告内容' }]"
          />
          <van-field name="isTop" label="置顶">
            <template #input>
              <van-switch v-model="publishData.isTop" size="22" active-color="#3677ef" />
            </template>
          </van-field>
        </van-cell-group>

        <div style="display:flex;gap:12px;margin-top:16px;">
          <van-button plain block @click="closePublish">取消</van-button>
          <van-button type="primary" block native-type="submit" :loading="publishing">
            {{ isEditNotice ? '保存修改' : '确认发布' }}
          </van-button>
        </div>
      </van-form>
    </van-popup>

    <!-- 分类选择 -->
    <van-action-sheet v-model:show="showCategoryPicker" title="选择公告分类">
      <div class="picker-list">
        <div class="picker-item" v-for="item in noticeCategories" :key="item" @click="publishData.category = item; showCategoryPicker = false">
          <span>{{ item }}</span>
          <van-icon v-if="publishData.category === item" name="success" color="#3677ef" />
        </div>
      </div>
    </van-action-sheet>

    <!-- 优先级选择 -->
    <van-action-sheet v-model:show="showPriorityPicker" title="选择优先级">
      <div class="picker-list">
        <div class="picker-item" v-for="item in priorityOptions" :key="item" @click="publishData.priority = item; showPriorityPicker = false">
          <span>{{ item }}</span>
          <van-icon v-if="publishData.priority === item" name="success" color="#3677ef" />
        </div>
      </div>
    </van-action-sheet>

    <!-- ============================================= -->
    <!-- ===== 阅读统计弹窗 ===== -->
    <!-- ============================================= -->
    <van-popup v-model:show="showStatsDialog" position="bottom" round style="padding:20px 16px 30px;max-height:70vh;overflow-y:auto;">
      <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:16px;">
        <h3 style="margin:0;">阅读统计</h3>
        <van-icon name="close" size="22" @click="showStatsDialog = false" />
      </div>

      <!-- 总览 -->
      <div class="stats-overview">
        <div class="stats-ring">
          <div class="ring-progress" :style="{
            background: `conic-gradient(#00b894 0deg ${readRate * 3.6}deg, #f0f0f0 ${readRate * 3.6}deg 360deg)`
          }">
            <span class="ring-text">{{ readRate }}%</span>
          </div>
          <span class="ring-label">总体阅读率</span>
        </div>
        <div class="stats-numbers">
          <div class="stats-number-item">
            <span class="num" style="color:#00b894;">{{ getCount('read') }}</span>
            <span class="label">已读</span>
          </div>
          <div class="stats-number-item">
            <span class="num" style="color:#ff6b35;">{{ getCount('unread') }}</span>
            <span class="label">未读</span>
          </div>
          <div class="stats-number-item">
            <span class="num" style="color:#3677ef;">{{ availableCount }}</span>
            <span class="label">总数</span>
          </div>
        </div>
      </div>

      <!-- 各公告阅读详情 -->
      <div class="stats-detail">
        <div class="stats-detail-title">各公告阅读情况</div>
        <div class="stats-detail-item" v-for="item in noticeList" :key="item.id">
          <span class="detail-name">{{ item.title }}</span>
          <div class="detail-bar">
            <div class="detail-bar-fill" :style="{ width: getReadPercent(item) + '%' }"></div>
          </div>
          <span class="detail-percent">{{ getReadPercent(item) }}%</span>
        </div>
      </div>
    </van-popup>

    <!-- 删除确认弹窗 -->
    <van-dialog v-model:show="showDeleteDialog" title="删除公告" message="确定要永久删除该公告吗？此操作不可恢复！" show-cancel-button confirm-button-color="#ee0a24" @confirm="confirmDelete" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showToast, showConfirmDialog } from 'vant'
import { 
  getAnnouncementList, 
  publishAnnouncement, 
  updateAnnouncement, 
  setTopAnnouncement, 
  cancelTopAnnouncement,
  deleteAnnouncement,
  markReadAnnouncement,
  countUnreadAnnouncements,
  markAllReadAnnouncements,
  getAnnouncementReadStats
} from '@/api/announcement'

const router = useRouter()

// ===== 用户信息 =====
const isAdmin = computed(() => localStorage.getItem('role') === 'SYSTEM_ADMIN')
const userId = computed(() => parseInt(localStorage.getItem('employeeId')) || null)

// ===== 当前筛选 =====
const activeFilter = ref('all')

// ===== 加载状态 =====
const loading = ref(false)

// ===== 公告数据 =====
const noticeList = ref([])
const readStats = ref([])

// ===== 公告分类 =====
const noticeCategories = ['通知', '制度', '会议纪要']
const priorityOptions = ['紧急', '重要', '普通']

// ===== 可用公告数量 =====
const availableCount = computed(() => {
  return noticeList.value.length
})

// ===== 统计 =====
const getCount = (type) => {
  if (type === 'unread') {
    return noticeList.value.filter(item => item.isRead !== 1).length
  }
  if (type === 'read') {
    return noticeList.value.filter(item => item.isRead === 1).length
  }
  return noticeList.value.length
}

const readRate = computed(() => {
  const total = getCount('all')
  if (total === 0) return 0
  return Math.round((getCount('read') / total) * 100)
})

// ===== 优先级标签类型 =====
const getPriorityTagType = (priority) => {
  const types = {
    '紧急': 'danger',
    '重要': 'warning',
    '普通': 'primary'
  }
  return types[priority] || 'primary'
}

// ===== 格式化时间 =====
const formatTime = (time) => {
  if (!time) return ''
  return time.replace('T', ' ').substring(0, 10)
}

// ===== 筛选列表 =====
const filteredList = computed(() => {
  let list = noticeList.value
  
  if (activeFilter.value === 'all') {
    return [...list].sort((a, b) => (b.isTop === 1 ? 1 : 0) - (a.isTop === 1 ? 1 : 0))
  }
  if (activeFilter.value === 'unread') {
    return list.filter(item => !item.isRead)
  }
  if (activeFilter.value === 'read') {
    return list.filter(item => item.isRead)
  }
  if (activeFilter.value === '重要') {
    return list.filter(item => item.priority === '重要')
  }
  return list.filter(item => item.category === activeFilter.value)
})

const getReadPercent = (item) => {
  const stat = readStats.value.find(s => s.id === item.id)
  return stat ? stat.readRate : 0
}

// =============================================
// ===== 发布/编辑公告 =====
// =============================================

const showPublish = ref(false)
const isEditNotice = ref(false)
const editNoticeId = ref(null)
const publishing = ref(false)
const showCategoryPicker = ref(false)
const showPriorityPicker = ref(false)

const publishData = ref({
  title: '',
  category: '',
  priority: '',
  content: '',
  isTop: false
})

const openEditNotice = (item) => {
  isEditNotice.value = true
  editNoticeId.value = item.id
  publishData.value = {
    title: item.title,
    category: item.category || '',
    priority: item.priority || '',
    content: item.content || '',
    isTop: item.isTop === 1
  }
  showPublish.value = true
}

const closePublish = () => {
  showPublish.value = false
  isEditNotice.value = false
  editNoticeId.value = null
  publishData.value = { title: '', category: '', priority: '', content: '', isTop: false }
}

const onPublish = async () => {
  publishing.value = true
  try {
    if (isEditNotice.value) {
      const data = {
        id: editNoticeId.value,
        title: publishData.value.title,
        category: publishData.value.category,
        priority: publishData.value.priority,
        content: publishData.value.content,
        isTop: publishData.value.isTop ? 1 : 0
      }
      const res = await updateAnnouncement(data)
      if (res.code === 0) {
        showToast('公告已更新')
        loadNotices()
      } else {
        showToast(res.message || '更新失败')
      }
    } else {
      const data = {
        title: publishData.value.title,
        category: publishData.value.category,
        priority: publishData.value.priority,
        content: publishData.value.content,
        isTop: publishData.value.isTop ? 1 : 0,
        publisherId: userId.value,
        publisherName: localStorage.getItem('name') || localStorage.getItem('username')
      }
      const res = await publishAnnouncement(data)
      if (res.code === 0) {
        showToast('公告发布成功！')
        loadNotices()
      } else {
        showToast(res.message || '发布失败')
      }
    }
    closePublish()
  } catch (error) {
    console.error('发布失败：', error)
    showToast('操作失败')
  } finally {
    publishing.value = false
  }
}

// =============================================
// ===== 置顶/取消置顶 =====
// =============================================

const togglePin = async (item) => {
  try {
    let res
    if (item.isTop === 1) {
      res = await cancelTopAnnouncement(item.id)
    } else {
      res = await setTopAnnouncement(item.id)
    }
    if (res.code === 0) {
      showToast(item.isTop === 1 ? '已取消置顶' : '已置顶')
      loadNotices()
    } else {
      showToast(res.message || '操作失败')
    }
  } catch (error) {
    console.error('置顶操作失败：', error)
    showToast('操作失败')
  }
}

// =============================================
// ===== 删除 =====
// =============================================

const showDeleteDialog = ref(false)
const deleteTargetId = ref(null)

const deleteNotice = (item) => {
  deleteTargetId.value = item.id
  showDeleteDialog.value = true
}

const confirmDelete = async () => {
  try {
    const res = await deleteAnnouncement(deleteTargetId.value)
    if (res.code === 0) {
      showToast('公告已删除')
      loadNotices()
    } else {
      showToast(res.message || '删除失败')
    }
  } catch (error) {
    console.error('删除失败：', error)
    showToast('操作失败')
  } finally {
    showDeleteDialog.value = false
  }
}

// =============================================
// ===== 阅读统计 =====
// =============================================

const showStatsDialog = ref(false)

const loadReadStats = async () => {
  try {
    const res = await getAnnouncementReadStats()
    if (res.code === 0 && res.data) {
      readStats.value = res.data.stats || []
    }
  } catch (error) {
    console.error('加载阅读统计失败：', error)
  }
}

// =============================================
// ===== 跳转详情 =====
// =============================================

const goDetail = async (id) => {
  const notice = noticeList.value.find(item => item.id === id)
  if (notice && notice.isRead !== 1) {
    try {
      await markReadAnnouncement(id)
      notice.isRead = 1
    } catch (error) {
      console.error('标记已读失败：', error)
    }
  }
  router.push(`/notice/detail/${id}`)
}

// =============================================
// ===== 加载公告列表 =====
// =============================================

const loadNotices = async () => {
  loading.value = true
  try {
    const res = await getAnnouncementList()
    console.log('公告列表：', res)
    if (res.code === 0) {
      noticeList.value = Array.isArray(res.data) ? res.data : []
      await loadReadStats()
    } else {
      showToast(res.message || '获取公告列表失败')
    }
  } catch (error) {
    console.error('加载公告列表失败：', error)
    showToast('加载失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

// =============================================
// ===== 生命周期 =====
// =============================================

onMounted(() => {
  loadNotices()
})
</script>

<style scoped>
.oa-notice {
  max-width: 430px;
  margin: 0 auto;
  min-height: 100vh;
  background: #f5f7fa;
  padding: 0 16px 20px;
  box-shadow: 0 0 20px rgba(0,0,0,0.06);
}

/* ===== Banner ===== */
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
.banner-right {
  display: flex;
  align-items: center;
  gap: 12px;
}
.banner-icon {
  background: rgba(255,255,255,0.2);
  border-radius: 50%;
  width: 60px;
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* ===== 统计卡片 ===== */
.stat-row {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr 1fr;
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
.stat-label { font-size: 12px; color: #888; margin: 0; }
.stat-num { font-size: 28px; font-weight: bold; }

/* ===== 筛选标签 ===== */
.filter-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 14px;
  overflow-x: auto;
}
.filter-tabs::-webkit-scrollbar { display: none; }
.filter-tab {
  padding: 6px 16px;
  border-radius: 20px;
  font-size: 13px;
  color: #888;
  background: #fff;
  white-space: nowrap;
  cursor: pointer;
  box-shadow: 0 2px 4px rgba(0,0,0,0.04);
}
.filter-tab.active { background: #00b894; color: #fff; }

/* ===== 加载状态 ===== */
.loading-wrap { text-align: center; padding: 40px 0; }

/* ===== 公告列表 ===== */
.notice-list { display: flex; flex-direction: column; gap: 12px; }
.notice-item {
  background: #fff;
  border-radius: 14px;
  padding: 16px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
  cursor: pointer;
  position: relative;
}
.notice-item:active { transform: scale(0.98); }

/* ===== 置顶标识 ===== */
.notice-pinned {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-bottom: 6px;
}
.pinned-text {
  font-size: 12px;
  color: #e17055;
  font-weight: 500;
}

/* ===== 公告头部 ===== */
.notice-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}
.notice-title { flex: 1; font-size: 16px; font-weight: 500; color: #222; }
.notice-time { font-size: 12px; color: #bbb; flex-shrink: 0; }

/* ===== 公告内容 ===== */
.notice-desc { 
  font-size: 14px; 
  color: #888; 
  margin: 0 0 10px; 
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* ===== 公告底部 ===== */
.notice-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.notice-author { font-size: 12px; color: #aaa; }
.notice-right {
  display: flex;
  align-items: center;
  gap: 10px;
}
.notice-category { 
  font-size: 12px; 
  color: #00b894; 
  background: #e8f8f0; 
  padding: 2px 12px; 
  border-radius: 12px; 
}

.admin-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}
.admin-actions .van-icon { cursor: pointer; }
.admin-actions .van-icon:active { opacity: 0.5; }

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

.safe-bottom { height: 20px; }

/* ============================================= */
/* ===== 阅读统计弹窗 ===== */
/* ============================================= */
.stats-overview {
  display: flex;
  align-items: center;
  gap: 30px;
  padding: 10px 0 20px;
}
.stats-ring {
  display: flex;
  flex-direction: column;
  align-items: center;
}
.ring-progress {
  width: 100px;
  height: 100px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
}
.ring-progress::after {
  content: '';
  position: absolute;
  width: 74px;
  height: 74px;
  border-radius: 50%;
  background: #fff;
}
.ring-text {
  position: relative;
  z-index: 1;
  font-size: 22px;
  font-weight: bold;
  color: #222;
}
.ring-label {
  font-size: 13px;
  color: #999;
  margin-top: 8px;
}

.stats-numbers {
  display: flex;
  gap: 20px;
}
.stats-number-item {
  text-align: center;
}
.stats-number-item .num {
  display: block;
  font-size: 28px;
  font-weight: bold;
}
.stats-number-item .label {
  font-size: 12px;
  color: #999;
}

.stats-detail {
  border-top: 1px solid #f0f0f0;
  padding-top: 16px;
}
.stats-detail-title {
  font-size: 14px;
  font-weight: 500;
  color: #333;
  margin-bottom: 12px;
}
.stats-detail-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px 0;
}
.detail-name {
  font-size: 13px;
  color: #555;
  width: 100px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.detail-bar {
  flex: 1;
  height: 6px;
  background: #f0f0f0;
  border-radius: 4px;
  overflow: hidden;
}
.detail-bar-fill {
  height: 100%;
  background: linear-gradient(90deg, #00b894, #00cec9);
  border-radius: 4px;
  transition: width 0.5s;
}
.detail-percent {
  font-size: 12px;
  color: #888;
  width: 40px;
  text-align: right;
}
</style>