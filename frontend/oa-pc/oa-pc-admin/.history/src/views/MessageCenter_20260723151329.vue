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
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="title" label="标题" min-width="200">
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

        <el-tab-pane label="未读消息" name="unread">
          <el-table :data="unreadMessages" style="width:100%;" v-loading="loading" stripe>
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="title" label="标题" min-width="200">
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
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="title" label="标题" min-width="200" />
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
            <el-tag v-if="detailData.eventType" size="small" type="info">
              事件：{{ getEventTypeLabel(detailData.eventType) }}
            </el-tag>
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
  { title: '待处理', value: 0, color: '#f56c6c' }
])

// ==================== 筛选 ====================
const filteredMessages = computed(() => messages.value)

const unreadMessages = computed(() => messages.value.filter(item => !item.isRead))

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
    'meeting': 'primary'
  }
  return map[bizType] || 'info'
}

const getBizTypeLabel = (bizType) => {
  const map = {
    'approval': '审批消息',
    'meeting': '会议消息'
  }
  return map[bizType] || bizType || '未知'
}

// 事件类型标签
const getEventTypeLabel = (eventType) => {
  const map = {
    'SUBMITTED': '提交申请',
    'APPROVED_NODE': '节点通过',
    'APPROVED_FINAL': '审批通过',
    'REJECTED': '被驳回',
    'WITHDRAWN': '已撤回',
    'CC_ADDED': '抄送',
    'MEETING_CREATED': '会议创建',
    'MEETING_UPDATED': '会议更新',
    'MEETING_CANCELLED': '会议取消',
    'MEETING_STARTED': '会议开始',
    'MEETING_ENDED': '会议结束'
  }
  return map[eventType] || eventType || '未知'
}

// ==================== 加载消息 ====================
const loadMessages = async () => {
  loading.value = true
  try {
    // TODO: 替换为真实 API 调用
    // 从后端获取消息（只获取审批和会议类型）
    // const res = await getMessages({ bizTypes: ['approval', 'meeting'] })
    // if (res.code === 0) {
    //   messages.value = res.data || []
    //   total.value = messages.value.length
    //   updateStats()
    // }

    // 模拟数据 - 只包含审批和会议消息
    await new Promise(resolve => setTimeout(resolve, 300))
    messages.value = [
      // ===== 审批消息 =====
      { 
        id: 1, 
        title: '张三的请假申请待审批', 
        bizType: 'approval',
        eventType: 'SUBMITTED',
        content: '员工张三提交了请假申请（2026-07-22 至 2026-07-24），等待您审批。', 
        createTime: new Date(Date.now() - 1000 * 60 * 10).toISOString(), 
        isRead: false,
        bizId: 101
      },
      { 
        id: 2, 
        title: '李四的报销申请待审批', 
        bizType: 'approval',
        eventType: 'SUBMITTED',
        content: '员工李四提交了报销申请，金额 ¥1,200.00，等待您审批。', 
        createTime: new Date(Date.now() - 1000 * 60 * 45).toISOString(), 
        isRead: false,
        bizId: 102
      },
      { 
        id: 3, 
        title: '王五的出差申请待审批', 
        bizType: 'approval',
        eventType: 'SUBMITTED',
        content: '员工王五提交了出差申请（北京，2026-07-25 至 2026-07-27），等待您审批。', 
        createTime: new Date(Date.now() - 1000 * 60 * 120).toISOString(), 
        isRead: false,
        bizId: 103
      },
      { 
        id: 4, 
        title: '赵六的加班申请已通过', 
        bizType: 'approval',
        eventType: 'APPROVED_FINAL',
        content: '您的加班申请（2026-07-20）已通过审批。', 
        createTime: new Date(Date.now() - 1000 * 60 * 60 * 3).toISOString(), 
        isRead: true,
        bizId: 104
      },
      { 
        id: 5, 
        title: '钱七的请假申请已拒绝', 
        bizType: 'approval',
        eventType: 'REJECTED',
        content: '员工钱七的请假申请已被拒绝，原因：部门人手不足。', 
        createTime: new Date(Date.now() - 1000 * 60 * 60 * 6).toISOString(), 
        isRead: true,
        bizId: 105
      },
      { 
        id: 6, 
        title: '新员工入职审批', 
        bizType: 'approval',
        eventType: 'SUBMITTED',
        content: '新员工孙八的入职申请待审批，请尽快处理。', 
        createTime: new Date(Date.now() - 1000 * 60 * 5).toISOString(), 
        isRead: false,
        bizId: 106
      },
      { 
        id: 7, 
        title: '周总的采购申请待审批', 
        bizType: 'approval',
        eventType: 'SUBMITTED',
        content: '周总提交了采购申请，金额 ¥5,000.00，等待您审批。', 
        createTime: new Date(Date.now() - 1000 * 60 * 30).toISOString(), 
        isRead: false,
        bizId: 107
      },

      // ===== 会议消息 =====
      { 
        id: 8, 
        title: '项目启动会邀请', 
        bizType: 'meeting',
        eventType: 'MEETING_CREATED',
        content: '您被邀请参加「项目启动会」，时间：2026-07-23 09:00，地点：第一会议室。', 
        createTime: new Date(Date.now() - 1000 * 60 * 60 * 2).toISOString(), 
        isRead: false,
        bizId: 201
      },
      { 
        id: 9, 
        title: '部门周例会时间变更', 
        bizType: 'meeting',
        eventType: 'MEETING_UPDATED',
        content: '您参加的「部门周例会」时间已变更，新时间：2026-07-24 14:00，地点：第二会议室。', 
        createTime: new Date(Date.now() - 1000 * 60 * 60 * 5).toISOString(), 
        isRead: true,
        bizId: 202
      },
      { 
        id: 10, 
        title: '产品评审会议通知', 
        bizType: 'meeting',
        eventType: 'MEETING_CREATED',
        content: '您被邀请参加「产品评审会议」，时间：2026-07-25 15:00，地点：第三会议室。', 
        createTime: new Date(Date.now() - 1000 * 60 * 20).toISOString(), 
        isRead: false,
        bizId: 203
      },
      { 
        id: 11, 
        title: '季度总结会议已取消', 
        bizType: 'meeting',
        eventType: 'MEETING_CANCELLED',
        content: '您参加的「季度总结会议」已被取消，请关注后续通知。', 
        createTime: new Date(Date.now() - 1000 * 60 * 60 * 8).toISOString(), 
        isRead: true,
        bizId: 204
      },
      { 
        id: 12, 
        title: '技术分享会即将开始', 
        bizType: 'meeting',
        eventType: 'MEETING_STARTED',
        content: '您参加的「技术分享会」已开始，请前往第四会议室。', 
        createTime: new Date(Date.now() - 1000 * 60 * 15).toISOString(), 
        isRead: false,
        bizId: 205
      }
    ]
    total.value = messages.value.length
    updateStats()
  } catch (error) {
    console.error('加载消息失败:', error)
    ElMessage.error('加载消息失败')
  } finally {
    loading.value = false
  }
}

const updateStats = () => {
  const unread = messages.value.filter(item => !item.isRead).length
  const read = messages.value.filter(item => item.isRead).length
  const pending = messages.value.filter(item => 
    !item.isRead && item.bizType === 'approval'
  ).length
  msgStats.value[0].value = total.value
  msgStats.value[1].value = unread
  msgStats.value[2].value = read
  msgStats.value[3].value = pending
}

// ==================== 标签页切换 ====================
const handleTabChange = (tab) => {
  currentPage.value = 1
}

// ==================== 查看消息 ====================
const viewMessage = (row) => {
  detailData.value = row
  detailVisible.value = true
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
const markRead = (row) => {
  const index = messages.value.findIndex(item => item.id === row.id)
  if (index !== -1) {
    messages.value[index].isRead = true
    updateStats()
    ElMessage.success('已标记为已读')
  }
}

const markReadFromDetail = () => {
  if (detailData.value) {
    markRead(detailData.value)
    detailData.value.isRead = true
  }
}

const markAllRead = () => {
  messages.value.forEach(item => item.isRead = true)
  updateStats()
  ElMessage.success('已全部标记为已读')
}

const deleteAllRead = () => {
  messages.value = messages.value.filter(item => !item.isRead)
  total.value = messages.value.length
  updateStats()
  ElMessage.success('已清空已读消息')
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