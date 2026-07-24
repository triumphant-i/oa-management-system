<template>
  <div class="page-container">
    <div class="page-header">
      <h2>📬 消息中心</h2>
      <div class="header-actions">
        <el-button type="primary" plain @click="markAllRead">
          <el-icon><Check /></el-icon> 全部已读
        </el-button>
        <el-button type="danger" plain @click="deleteAllRead">
          <el-icon><Delete /></el-icon> 清空已读
        </el-button>
        <el-button @click="loadMessages">
          <el-icon><Refresh /></el-icon> 刷新
        </el-button>
      </div>
    </div>

    <!-- 消息统计 -->
    <el-row :gutter="20" style="margin-bottom:20px;">
      <el-col :span="6" v-for="item in msgStats" :key="item.title">
        <el-card :body-style="{ padding: '16px' }">
          <div class="stat-item">
            <div class="stat-label">{{ item.title }}</div>
            <div class="stat-number" :style="{ color: item.color }">{{ item.value }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 消息列表 -->
    <el-card>
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane label="全部消息" name="all">
          <el-table :data="filteredMessages" style="width:100%;" v-loading="loading" stripe>
            <el-table-column prop="id" label="ID" width="70" />
            <el-table-column prop="title" label="标题" min-width="180">
              <template #default="{ row }">
                <span :style="{ fontWeight: row.isRead ? 'normal' : 'bold' }">
                  {{ row.title }}
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="bizType" label="类型" width="120">
              <template #default="{ row }">
                <el-tag :type="getBizTypeTag(row.bizType)">
                  {{ getBizTypeLabel(row.bizType) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="content" label="内容" min-width="200">
              <template #default="{ row }">
                <span class="content-text">{{ row.content }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="时间" width="180">
              <template #default="{ row }">
                {{ formatTime(row.createTime) }}
              </template>
            </el-table-column>
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="row.isRead ? 'info' : 'warning'" size="small">
                  {{ row.isRead ? '已读' : '未读' }}
                </el-tag>
                <el-tag v-if="row.isTodo" type="danger" size="small" style="margin-left:4px;">待办</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="200" fixed="right">
              <template #default="{ row }">
                <el-button size="small" type="primary" plain @click="viewMessage(row)">查看</el-button>
                <el-button 
                  v-if="!row.isRead" 
                  size="small" type="success" plain 
                  @click="markRead(row)"
                >标记已读</el-button>
                <el-button 
                  v-if="row.bizType === 'approval' && row.bizId" 
                  size="small" type="warning" plain 
                  @click="goToApproval(row.bizId)"
                >去审批</el-button>
                <el-button 
                  v-if="row.bizType === 'meeting' && row.bizId" 
                  size="small" type="primary" plain 
                  @click="goToMeeting(row.bizId)"
                >查看会议</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="未读消息" name="todo">
          <el-table :data="todoMessages" style="width:100%;" v-loading="loading" stripe>
            <el-table-column prop="id" label="ID" width="70" />
            <el-table-column prop="title" label="标题" min-width="180">
              <template #default="{ row }">
                <span style="font-weight:bold;">{{ row.title }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="bizType" label="类型" width="120">
              <template #default="{ row }">
                <el-tag :type="getBizTypeTag(row.bizType)">
                  {{ getBizTypeLabel(row.bizType) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="content" label="内容" min-width="200">
              <template #default="{ row }">
                <span class="content-text">{{ row.content }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="时间" width="180">
              <template #default="{ row }">
                {{ formatTime(row.createTime) }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="200" fixed="right">
              <template #default="{ row }">
                <el-button size="small" type="primary" plain @click="viewMessage(row)">查看</el-button>
                <el-button size="small" type="success" plain @click="markRead(row)">标记已读</el-button>
                <el-button 
                  v-if="row.bizType === 'approval' && row.bizId" 
                  size="small" type="warning" plain 
                  @click="goToApproval(row.bizId)"
                >去审批</el-button>
                <el-button 
                  v-if="row.bizType === 'meeting' && row.bizId" 
                  size="small" type="primary" plain 
                  @click="goToMeeting(row.bizId)"
                >查看会议</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="已读消息" name="read">
          <el-table :data="readMessages" style="width:100%;" v-loading="loading" stripe>
            <el-table-column prop="id" label="ID" width="70" />
            <el-table-column prop="title" label="标题" min-width="180" />
            <el-table-column prop="bizType" label="类型" width="120">
              <template #default="{ row }">
                <el-tag :type="getBizTypeTag(row.bizType)">
                  {{ getBizTypeLabel(row.bizType) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="content" label="内容" min-width="200">
              <template #default="{ row }">
                <span class="content-text">{{ row.content }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="时间" width="180">
              <template #default="{ row }">
                {{ formatTime(row.createTime) }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="140">
              <template #default="{ row }">
                <el-button size="small" type="primary" plain @click="viewMessage(row)">查看</el-button>
                <el-button 
                  v-if="row.bizType === 'approval' && row.bizId" 
                  size="small" type="warning" plain 
                  @click="goToApproval(row.bizId)"
                >去审批</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>

      <!-- 分页 -->
      <div class="pagination">
        <el-pagination
          v-model:page-size="pageSize"
          v-model:current-page="currentPage"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadMessages"
          @current-change="loadMessages"
        />
      </div>
    </el-card>

    <!-- ===== 消息详情对话框 ===== -->
    <el-dialog v-model="detailVisible" title="消息详情" width="600px">
      <div v-if="detailData">
        <div class="detail-title">{{ detailData.title }}</div>
        <div class="detail-meta">
          <el-tag :type="getBizTypeTag(detailData.bizType)" size="small">
            {{ getBizTypeLabel(detailData.bizType) }}
          </el-tag>
          <span class="detail-time">{{ formatTime(detailData.createTime) }}</span>
          <el-tag :type="detailData.isRead ? 'info' : 'warning'" size="small">
            {{ detailData.isRead ? '已读' : '未读' }}
          </el-tag>
          <el-tag v-if="detailData.isTodo" type="danger" size="small">待办</el-tag>
        </div>
        <el-divider />
        <div class="detail-content">{{ detailData.content }}</div>
        <div v-if="detailData.bizId" class="detail-actions">
          <el-divider />
          <div style="display:flex;gap:12px;align-items:center;flex-wrap:wrap;">
            <span style="color:#999;font-size:14px;">关联业务：</span>
            <el-button 
              v-if="detailData.bizType === 'approval'" 
              size="small" type="warning" 
              @click="goToApproval(detailData.bizId)"
            >
              查看审批详情
            </el-button>
            <el-button 
              v-if="detailData.bizType === 'meeting'" 
              size="small" type="primary" 
              @click="goToMeeting(detailData.bizId)"
            >
              查看会议详情
            </el-button>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button v-if="detailData && !detailData.isRead" type="primary" @click="markReadFromDetail">标记已读</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Check, Delete, Refresh } from '@element-plus/icons-vue'

// ==================== API 导入 ====================
import { 
  getMyMessages, 
  markMessageRead, 
  markAllMessagesRead,
  getMessageDetail,
  deleteMessage
} from '@/api/message'

const router = useRouter()

// ==================== 数据 ====================
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const activeTab = ref('all')
const messages = ref([])

// ==================== 消息统计 ====================
const msgStats = ref([
  { title: '总消息', value: 0, color: '#409eff' },
  { title: '未读', value: 0, color: '#e6a23c' },
  { title: '已读', value: 0, color: '#67c23a' },
  { title: '待办', value: 0, color: '#f56c6c' }
])

// ==================== 筛选 ====================
const filteredMessages = computed(() => messages.value)

const todoMessages = computed(() => messages.value.filter(item => !item.isRead && item.isTodo))

const readMessages = computed(() => messages.value.filter(item => item.isRead))

// ==================== 详情 ====================
const detailVisible = ref(false)
const detailData = ref(null)

// ==================== 工具方法 ====================
const formatTime = (time) => {
  if (!time) return ''
  return time.replace('T', ' ')
}

// 业务类型标签
const getBizTypeTag = (bizType) => {
  const map = {
    'approval': 'warning',
    'meeting': 'primary',
    'SYSTEM': 'danger',
    'system': 'danger'
  }
  return map[bizType] || 'info'
}

const getBizTypeLabel = (bizType) => {
  const map = {
    'approval': '审批消息',
    'meeting': '会议消息',
    'SYSTEM': '系统通知',
    'system': '系统通知'
  }
  return map[bizType] || bizType || '未知'
}

// ==================== 加载消息 ====================
const loadMessages = async () => {
  loading.value = true
  try {
    // 使用后端接口 /message/list
    // category: all=全部, todo=待办, cc=抄送, system=系统通知
    const res = await getMyMessages({
      category: activeTab.value === 'todo' ? 'todo' : 'all',
      page: currentPage.value,
      size: pageSize.value
    })

    if (res.code === 0) {
      const data = res.data || {}
      // 后端返回格式：{ total, data: [...] }
      messages.value = data.data || data.records || data.list || []
      total.value = data.total || messages.value.length
      updateStats()
    } else {
      ElMessage.error(res.message || res.msg || '加载消息失败')
    }
  } catch (error) {
    console.error('加载消息失败:', error)
    ElMessage.error('加载消息失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

const updateStats = () => {
  const unread = messages.value.filter(item => !item.isRead).length
  const read = messages.value.filter(item => item.isRead).length
  const todo = messages.value.filter(item => !item.isRead && item.isTodo).length
  msgStats.value[0].value = total.value
  msgStats.value[1].value = unread
  msgStats.value[2].value = read
  msgStats.value[3].value = todo
}

// ==================== 标签页切换 ====================
const handleTabChange = (tab) => {
  activeTab.value = tab
  currentPage.value = 1
  loadMessages()
}

// ==================== 查看消息 ====================
const viewMessage = async (row) => {
  try {
    // 如果未读，先标记已读并获取详情
    if (!row.isRead) {
      const res = await markMessageRead(row.id)
      if (res.code === 0) {
        row.isRead = true
        updateStats()
        // 如果有 jumpUrl，跳转
        if (res.data && res.data.jumpUrl) {
          detailVisible.value = false
          router.push(res.data.jumpUrl)
          return
        }
      }
    }
    // 获取消息详情
    const res = await getMessageDetail(row.id)
    if (res.code === 0) {
      detailData.value = res.data
      detailVisible.value = true
    } else {
      ElMessage.error(res.message || res.msg || '获取详情失败')
    }
  } catch (error) {
    console.error('查看消息失败:', error)
    ElMessage.error('操作失败')
  }
}

// ==================== 跳转 ====================
const goToApproval = (approvalId) => {
  detailVisible.value = false
  router.push(`/approval/detail/${approvalId}`)
}

const goToMeeting = (meetingId) => {
  detailVisible.value = false
  router.push(`/meeting/detail/${meetingId}`)
}

// ==================== 标记已读 ====================
const markRead = async (row) => {
  try {
    const res = await markMessageRead(row.id)
    if (res.code === 0) {
      row.isRead = true
      updateStats()
      ElMessage.success('已标记为已读')
      // 如果有 jumpUrl，跳转
      if (res.data && res.data.jumpUrl) {
        setTimeout(() => {
          router.push(res.data.jumpUrl)
        }, 500)
      }
    } else {
      ElMessage.error(res.message || res.msg || '操作失败')
    }
  } catch (error) {
    console.error('标记已读失败:', error)
    ElMessage.error('操作失败')
  }
}

const markReadFromDetail = async () => {
  if (detailData.value) {
    await markRead(detailData.value)
    detailData.value.isRead = true
  }
}

const markAllRead = async () => {
  try {
    const category = activeTab.value === 'todo' ? 'todo' : 'all'
    const res = await markAllMessagesRead(category)
    if (res.code === 0) {
      messages.value.forEach(item => {
        if (category === 'todo') {
          if (!item.isRead && item.isTodo) item.isRead = true
        } else {
          if (!item.isRead) item.isRead = true
        }
      })
      updateStats()
      const count = res.data?.affected || 0
      ElMessage.success(`已全部标记为已读${count > 0 ? '（' + count + '条）' : ''}`)
    } else {
      ElMessage.error(res.message || res.msg || '操作失败')
    }
  } catch (error) {
    console.error('全部已读失败:', error)
    ElMessage.error('操作失败')
  }
}

// ==================== 清空已读 ====================
const deleteAllRead = async () => {
  try {
    // 获取所有已读消息ID
    const readIds = messages.value.filter(item => item.isRead).map(item => item.id)
    if (readIds.length === 0) {
      ElMessage.warning('暂无已读消息')
      return
    }
    
    let deletedCount = 0
    for (const id of readIds) {
      try {
        await deleteMessage(id)
        deletedCount++
      } catch (e) {
        console.error('删除消息失败:', e)
      }
    }
    
    messages.value = messages.value.filter(item => !item.isRead)
    total.value = messages.value.length
    updateStats()
    ElMessage.success(`已清空 ${deletedCount} 条已读消息`)
  } catch (error) {
    console.error('清空已读失败:', error)
    ElMessage.error('操作失败')
  }
}

// ==================== 生命周期 ====================
onMounted(() => {
  loadMessages()
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

.header-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.stat-item {
  text-align: center;
}

.stat-label {
  font-size: 14px;
  color: #999;
}

.stat-number {
  font-size: 28px;
  font-weight: bold;
  margin-top: 4px;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.content-text {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
}

.detail-title {
  font-size: 20px;
  font-weight: bold;
  margin-bottom: 12px;
}

.detail-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
  flex-wrap: wrap;
}

.detail-time {
  color: #999;
  font-size: 14px;
}

.detail-content {
  font-size: 15px;
  line-height: 1.8;
  color: #333;
  padding: 8px 0;
  white-space: pre-wrap;
}

.detail-actions {
  margin-top: 8px;
}
</style>