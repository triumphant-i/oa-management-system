<!-- src/views/Dashboard.vue -->
<template>
  <div class="dashboard">
    <el-row :gutter="20">
      <el-col :span="6" v-for="card in statistics" :key="card.title">
        <el-card class="stat-card" :body-style="{ padding: '20px' }">
          <div class="stat-content">
            <div class="stat-left">
              <div class="stat-title">{{ card.title }}</div>
              <div class="stat-number">{{ card.value }}</div>
            </div>
            <div class="stat-icon" :style="{ background: card.color }">
              <!-- ✅ 使用 markRaw 或直接使用字符串 -->
              <el-icon :size="28">
                <component :is="card.icon" />
              </el-icon>
            </div>
          </div>
          <div class="stat-footer">
            <span>{{ card.subTitle }}</span>
            <span class="stat-change" :class="card.changeType">
              {{ card.change }}
            </span>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表区域 -->
    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :span="16">
        <el-card>
          <template #header>
            <span>近7天审批趋势</span>
          </template>
          <div class="chart-placeholder">
            📊 审批趋势图表区域
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card>
          <template #header>
            <span>待办事项</span>
          </template>
          <div class="todo-list">
            <div v-for="todo in todos" :key="todo.id" class="todo-item">
              <el-tag :type="todo.type" size="small">{{ todo.tag }}</el-tag>
              <span class="todo-title">{{ todo.title }}</span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 快捷入口 -->
    <el-card style="margin-top: 20px;">
      <template #header>
        <span>快捷入口</span>
      </template>
      <div class="quick-links">
        <div 
          v-for="link in quickLinks" 
          :key="link.path" 
          class="quick-item"
          @click="goTo(link.path)"
        >
          <el-icon :size="32"><component :is="link.icon" /></el-icon>
          <span>{{ link.title }}</span>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, markRaw } from 'vue'
import { useRouter } from 'vue-router'
import { 
  User, Document, Bell, Calendar, 
  TrendCharts, Clock, Checked, Tickets 
} from '@element-plus/icons-vue'
import { hasPermission, PERMISSIONS } from '@/utils/permission'

const router = useRouter()

// ✅ 使用 markRaw 包装图标组件，避免响应式警告
const statistics = ref([
  {
    title: '员工总数',
    value: '128',
    icon: markRaw(User),
    color: '#409eff',
    subTitle: '较上月',
    change: '+12%',
    changeType: 'up'
  },
  {
    title: '待审批',
    value: '8',
    icon: markRaw(Document),
    color: '#e6a23c',
    subTitle: '待处理',
    change: '5项紧急',
    changeType: 'warning'
  },
  {
    title: '今日考勤',
    value: '96%',
    icon: markRaw(Calendar),
    color: '#67c23a',
    subTitle: '出勤率',
    change: '+2%',
    changeType: 'up'
  },
  {
    title: '未读消息',
    value: '15',
    icon: markRaw(Bell),
    color: '#f56c6c',
    subTitle: '新消息',
    change: '3条置顶',
    changeType: 'danger'
  }
])

const todos = ref([
  { id: 1, title: '张三 - 年假申请', tag: '审批', type: 'warning' },
  { id: 2, title: '李四 - 出差报销', tag: '审批', type: 'warning' },
  { id: 3, title: '每周例会通知', tag: '公告', type: 'primary' }
])

const quickLinks = computed(() => {
  const links = []
  
  if (hasPermission(PERMISSIONS.APPROVAL_APPLY)) {
    links.push({ path: '/approval', title: '发起审批', icon: markRaw(Document) })
  }
  if (hasPermission(PERMISSIONS.ATTENDANCE_CHECK)) {
    links.push({ path: '/attendance', title: '考勤打卡', icon: markRaw(Calendar) })
  }
  if (hasPermission(PERMISSIONS.MEETING_RESERVE)) {
    links.push({ path: '/meetingRoom', title: '预约会议', icon: markRaw(Tickets) })
  }
  if (hasPermission(PERMISSIONS.NOTICE_VIEW)) {
    links.push({ path: '/notice', title: '查看公告', icon: markRaw(Bell) })
  }
  
  return links
})

const goTo = (path) => {
  router.push(path)
}
</script>

<style scoped>
.dashboard {
  padding: 0;
}

.stat-card {
  border-radius: 8px;
}

.stat-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.stat-left {
  flex: 1;
}

.stat-title {
  font-size: 14px;
  color: #909399;
}

.stat-number {
  font-size: 28px;
  font-weight: bold;
  color: #303133;
  margin-top: 4px;
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
}

.stat-footer {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #ebeef5;
  font-size: 12px;
  color: #909399;
  display: flex;
  justify-content: space-between;
}

.stat-change {
  font-weight: 500;
}

.stat-change.up {
  color: #67c23a;
}

.stat-change.down {
  color: #f56c6c;
}

.stat-change.warning {
  color: #e6a23c;
}

.stat-change.danger {
  color: #f56c6c;
}

.chart-placeholder {
  height: 300px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
  border-radius: 8px;
  color: #909399;
  font-size: 16px;
}

.todo-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.todo-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  background: #f5f7fa;
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.2s;
}

.todo-item:hover {
  background: #e8ecf1;
}

.todo-title {
  font-size: 14px;
  color: #303133;
}

.quick-links {
  display: flex;
  gap: 20px;
  flex-wrap: wrap;
}

.quick-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 20px 30px;
  background: #f5f7fa;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
  color: #409eff;
}

.quick-item:hover {
  background: #ecf5ff;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.15);
}

.quick-item span {
  font-size: 14px;
  color: #303133;
}
</style>