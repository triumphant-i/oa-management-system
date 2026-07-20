<template>
  <div class="oa-message">
    <div class="page-header">
      <van-icon name="arrow-left" size="22" @click="$router.back()" />
      <h1 class="header-title">消息中心</h1>
      <div class="header-right">
        <van-button text type="primary" size="small" @click="handleMarkAllRead" :loading="markingAll">
          全部已读
        </van-button>
      </div>
    </div>

    <div class="tab-bar">
      <span class="tab-item" :class="{ active: activeTab === 'all' }" @click="activeTab = 'all'">全部</span>
      <span class="tab-item" :class="{ active: activeTab === 'unread' }" @click="activeTab = 'unread'">未读</span>
      <span class="tab-item" :class="{ active: activeTab === 'system' }" @click="activeTab = 'system'">系统通知</span>
      <span class="tab-item" :class="{ active: activeTab === 'approval' }" @click="activeTab = 'approval'">审批消息</span>
    </div>

    <div class="msg-list" v-if="!loading">
      <div class="msg-item" v-for="item in filteredMessages" :key="item.id" @click="viewDetail(item)">
        <div class="msg-avatar">
          <van-icon :name="getMsgIcon(item.msgType)" size="28" :color="getMsgColor(item.msgType)" />
        </div>
        <div class="msg-content">
          <div class="msg-header">
            <span class="msg-sender">{{ item.senderName || '系统' }}</span>
            <span class="msg-time">{{ formatTime(item.createTime) }}</span>
          </div>
          <div class="msg-title">{{ item.title }}</div>
          <div class="msg-preview">{{ item.content }}</div>
        </div>
        <div class="msg-status">
          <van-tag v-if="item.isRead === 0" color="#ee0a24" size="mini">未读</van-tag>
          <van-icon v-if="item.isTop === 1" name="star-o" size="16" color="#fdcb6e" />
        </div>
      </div>
      <div class="empty-state" v-if="filteredMessages.length === 0">
        <van-icon name="message-o" size="48" color="#ccc" />
        <p>暂无消息</p>
      </div>
    </div>

    <div class="loading-state" v-else>
      <van-loading type="spinner" color="#3677ef" />
      <p>加载中...</p>
    </div>

    <van-popup v-model:show="showDetail" position="bottom" round style="padding:20px 16px 30px;max-height:80vh;overflow-y:auto;">
      <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:16px;">
        <h3 style="margin:0;">📩 消息详情</h3>
        <van-icon name="close" size="22" @click="showDetail = false" />
      </div>

      <div class="detail-info" v-if="currentMessage">
        <div class="detail-header">
          <div class="detail-icon">
            <van-icon :name="getMsgIcon(currentMessage.msgType)" size="32" :color="getMsgColor(currentMessage.msgType)" />
          </div>
          <div class="detail-meta">
            <span class="detail-sender">{{ currentMessage.senderName || '系统' }}</span>
            <span class="detail-time">{{ formatTime(currentMessage.createTime) }}</span>
          </div>
        </div>

        <div class="detail-title">{{ currentMessage.title }}</div>
        <div class="detail-content">{{ currentMessage.content }}</div>

        <div class="detail-footer">
          <span class="detail-type">{{ getMsgTypeName(currentMessage.msgType) }}</span>
          <span class="detail-status" :class="currentMessage.isRead === 1 ? 'read' : 'unread'">
            {{ currentMessage.isRead === 1 ? '已读' : '未读' }}
          </span>
        </div>

        <div class="detail-actions">
          <van-button 
            v-if="currentMessage.msgType === 'APPROVAL' && currentMessage.relatedId" 
            type="primary" 
            block 
            size="large"
            @click="goToApplyDetail"
          >
            <van-icon name="eye-o" size="18" /> 查看申请详情
          </van-button>
        </div>
      </div>
    </van-popup>

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
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { getMessageList, markAllAsRead, markAsRead, getMessageDetail } from '@/api/message'

const router = useRouter()

const loading = ref(true)
const markingAll = ref(false)
const activeTab = ref('all')
const messageList = ref([])
const showDetail = ref(false)
const currentMessage = ref(null)

const filteredMessages = computed(() => {
  let messages = messageList.value
  if (activeTab.value === 'unread') {
    messages = messages.filter(m => m.isRead === 0)
  } else if (activeTab.value === 'system') {
    messages = messages.filter(m => m.msgType === 'SYSTEM')
  } else if (activeTab.value === 'approval') {
    messages = messages.filter(m => m.msgType === 'APPROVAL')
  }
  return messages.sort((a, b) => {
    if (a.isTop !== b.isTop) return b.isTop - a.isTop
    return new Date(b.createTime) - new Date(a.createTime)
  })
})

const getMsgIcon = (type) => {
  const icons = {
    SYSTEM: 'bell-o',
    APPROVAL: 'records',
    ATTENDANCE: 'clock-o',
    MEETING: 'location-o',
    ANNOUNCEMENT: 'bullhorn-o',
    OTHER: 'message-o'
  }
  return icons[type] || 'message-o'
}

const getMsgColor = (type) => {
  const colors = {
    SYSTEM: '#3677ef',
    APPROVAL: '#fdcb6e',
    ATTENDANCE: '#ff6b35',
    MEETING: '#6c5ce7',
    ANNOUNCEMENT: '#00b894',
    OTHER: '#999'
  }
  return colors[type] || '#999'
}

const getMsgTypeName = (type) => {
  const names = {
    SYSTEM: '系统通知',
    APPROVAL: '审批消息',
    ATTENDANCE: '考勤消息',
    MEETING: '会议提醒',
    ANNOUNCEMENT: '公告通知',
    OTHER: '其他消息'
  }
  return names[type] || '其他消息'
}

const formatTime = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  const now = new Date()
  const diff = now - date
  
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`
  
  const month = date.getMonth() + 1
  const day = date.getDate()
  const hour = String(date.getHours()).padStart(2, '0')
  const minute = String(date.getMinutes()).padStart(2, '0')
  return `${month}月${day}日 ${hour}:${minute}`
}

const loadMessages = async () => {
  loading.value = true
  try {
    const res = await getMessageList()
    if (res.code === 0 && res.data) {
      messageList.value = Array.isArray(res.data) ? res.data : []
    }
  } catch (error) {
    console.error('加载消息失败:', error)
    showToast('加载消息失败')
  } finally {
    loading.value = false
  }
}

const handleMarkAllRead = async () => {
  markingAll.value = true
  try {
    const res = await markAllAsRead()
    if (res.code === 0) {
      showToast('已全部标为已读')
      await loadMessages()
    } else {
      showToast(res.message || '操作失败')
    }
  } catch (error) {
    console.error('标记全部已读失败:', error)
    showToast('操作失败')
  } finally {
    markingAll.value = false
  }
}

const viewDetail = async (item) => {
  try {
    const res = await getMessageDetail(item.id)
    if (res.code === 0 && res.data) {
      currentMessage.value = res.data
      showDetail.value = true
      await loadMessages()
    }
  } catch (error) {
    console.error('获取消息详情失败:', error)
  }
}

const goToApplyDetail = () => {
  if (currentMessage.value && currentMessage.value.relatedId) {
    showDetail.value = false
    router.push(`/apply/detail/${currentMessage.value.relatedId}`)
  }
}

onMounted(async () => {
  await loadMessages()
})
</script>

<style scoped>
.oa-message {
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
  padding: 16px 16px 12px;
  background: #fff;
  margin: 0 -16px 0;
}
.header-title { font-size: 18px; font-weight: 600; margin: 0; color: #222; }
.header-right { display: flex; gap: 8px; }

.tab-bar {
  display: flex;
  gap: 8px;
  margin: 16px 0;
  flex-wrap: wrap;
}
.tab-item {
  padding: 6px 16px;
  background: #fff;
  border-radius: 20px;
  font-size: 14px;
  color: #666;
  cursor: pointer;
  transition: all 0.2s;
}
.tab-item.active {
  background: #3677ef;
  color: #fff;
}

.msg-list { display: flex; flex-direction: column; gap: 10px; }
.msg-item {
  display: flex;
  gap: 12px;
  padding: 14px 16px;
  background: #fff;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s;
}
.msg-item:active { transform: scale(0.98); }

.msg-avatar {
  width: 48px;
  height: 48px;
  background: #f5f7fa;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.msg-content { flex: 1; display: flex; flex-direction: column; gap: 4px; min-width: 0; }
.msg-header { display: flex; justify-content: space-between; align-items: center; }
.msg-sender { font-size: 15px; font-weight: 500; color: #222; }
.msg-time { font-size: 12px; color: #999; }
.msg-title { font-size: 14px; color: #333; font-weight: 500; }
.msg-preview { font-size: 13px; color: #888; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

.msg-status { display: flex; flex-direction: column; align-items: flex-end; gap: 4px; }

.detail-info { padding: 4px 0; }
.detail-header { display: flex; gap: 12px; margin-bottom: 16px; }
.detail-icon {
  width: 56px;
  height: 56px;
  background: #f5f7fa;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}
.detail-meta { display: flex; flex-direction: column; gap: 4px; }
.detail-sender { font-size: 16px; font-weight: 600; color: #222; }
.detail-time { font-size: 13px; color: #999; }

.detail-title {
  font-size: 18px;
  font-weight: 600;
  color: #222;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f0f0;
}

.detail-content {
  font-size: 15px;
  color: #444;
  line-height: 1.8;
  padding: 8px 0;
}

.detail-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 16px;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
}
.detail-type { font-size: 13px; color: #999; }
.detail-status { font-size: 13px; padding: 2px 10px; border-radius: 12px; }
.detail-status.read { color: #00b894; background: #e8f8f0; }
.detail-status.unread { color: #ee0a24; background: #fff0f0; }

.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 0;
  color: #999;
}
.loading-state p { margin-top: 12px; }

.empty-state {
  text-align: center;
  padding: 40px 0;
  color: #ccc;
}
.empty-state p { margin-top: 8px; font-size: 14px; }

.bottom-bar { padding: 16px 0 8px; }
.back-btn {
  border-radius: 12px !important;
  height: 48px !important;
  font-size: 16px !important;
  border-color: #3677ef !important;
  color: #3677ef !important;
}
.back-btn:active { background: #f0f7ff !important; }

.safe-bottom { height: 20px; }
</style>