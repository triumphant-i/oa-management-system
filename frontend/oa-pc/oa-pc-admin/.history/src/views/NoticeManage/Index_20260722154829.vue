<template>
  <div class="page-container">
    <div class="page-header">
      <h2>公告管理</h2>
      <div class="header-actions">
        <el-button type="primary" @click="openAddDialog">
          <el-icon><Plus /></el-icon> 发布公告
        </el-button>
        <el-button @click="loadData">
          <el-icon><Refresh /></el-icon> 刷新
        </el-button>
        <el-button type="info" plain @click="showStatsDialog = true">
          <el-icon><DataAnalysis /></el-icon> 阅读统计
        </el-button>
      </div>
    </div>

    <!-- 统计卡片 -->
    <el-row :gutter="20" style="margin-bottom:20px;">
      <el-col :span="6">
        <el-card :body-style="{ padding: '16px' }">
          <div class="stat-item">
            <div class="stat-label">总公告</div>
            <div class="stat-number" style="color:#409eff;">{{ total }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card :body-style="{ padding: '16px' }">
          <div class="stat-item">
            <div class="stat-label">总阅读量</div>
            <div class="stat-number" style="color:#67c23a;">{{ totalReadCount }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card :body-style="{ padding: '16px' }">
          <div class="stat-item">
            <div class="stat-label">平均阅读率</div>
            <div class="stat-number" style="color:#e6a23c;">{{ avgReadRate }}%</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card :body-style="{ padding: '16px' }">
          <div class="stat-item">
            <div class="stat-label">最高阅读量</div>
            <div class="stat-number" style="color:#f56c6c;">{{ maxReadCount }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 搜索栏 -->
    <el-card class="search-card">
      <el-form :inline="true" :model="searchForm" @keyup.enter="handleSearch">
        <el-form-item label="标题">
          <el-input 
            v-model="searchForm.title" 
            placeholder="请输入公告标题" 
            clearable 
            style="width:220px;"
            @keyup.enter="handleSearch"
            maxlength="100"
          />
        </el-form-item>
        <el-form-item label="分类">
          <el-select 
            v-model="searchForm.category" 
            placeholder="请选择分类" 
            clearable 
            @change="handleSearch"
            style="width:160px;"
          >
            <el-option label="全部" value="" />
            <el-option label="公司新闻" value="公司新闻" />
            <el-option label="通知公告" value="通知公告" />
            <el-option label="政策制度" value="政策制度" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select 
            v-model="searchForm.status" 
            placeholder="请选择状态" 
            clearable 
            @change="handleSearch"
            style="width:140px;"
          >
            <el-option label="全部" value="" />
            <el-option label="已发布" value="已发布" />
            <el-option label="已撤回" value="已撤回" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon> 查询
          </el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 公告表格 -->
    <el-card>
      <el-table :data="tableData" style="width:100%;" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="title" label="标题" min-width="200">
          <template #default="{ row }">
            <span v-if="row.isTop === 1" style="color:#e6a23c;">📌</span>
            <el-link type="primary" @click="openDetailDialog(row)">
              {{ row.title.length > 50 ? row.title.substring(0, 50) + '...' : row.title }}
            </el-link>
            <el-tooltip 
              v-if="row.title.length > 50" 
              :content="row.title" 
              placement="top"
            >
              <el-icon style="color:#409eff;cursor:pointer;margin-left:4px;">
                <InfoFilled />
              </el-icon>
            </el-tooltip>
          </template>
        </el-table-column>
        <el-table-column prop="category" label="分类" width="120">
          <template #default="{ row }">
            <el-tag>{{ row.category }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="publisherName" label="发布人" width="120">
          <template #default="{ row }">
            {{ row.publisherName || row.publisher || '系统管理员' }}
          </template>
        </el-table-column>
        <el-table-column prop="publishTime" label="发布时间" width="160">
          <template #default="{ row }">
            {{ formatTime(row.publishTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="readCount" label="阅读量" width="100" sortable>
          <template #default="{ row }">
            <el-tag size="small" type="info">{{ row.readCount || 0 }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === '已发布' ? 'success' : 'info'">
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="info" text @click="openDetailDialog(row)">查看</el-button>
            
            <el-button 
              v-if="row.status === '已发布'" 
              size="small" 
              :type="row.isTop === 1 ? 'success' : 'warning'" 
              text
              @click="handleTop(row)"
            >
              {{ row.isTop === 1 ? '取消置顶' : '置顶' }}
            </el-button>
            
            <el-button 
              size="small" 
              type="danger" 
              text
              @click="handleDelete(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination">
        <el-pagination
          v-model:page-size="pageSize"
          v-model:current-page="currentPage"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadData"
          @current-change="loadData"
        />
      </div>
    </el-card>

    <!-- ===== 发布公告对话框 ===== -->
    <el-dialog 
      v-model="dialogVisible" 
      title="发布公告" 
      width="600px"
      @close="resetForm"
    >
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="标题" prop="title">
          <el-input 
            v-model="form.title" 
            placeholder="请输入公告标题（不超过100个字）" 
            maxlength="100"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="分类" prop="category">
          <el-select v-model="form.category" placeholder="请选择分类" style="width:100%;">
            <el-option label="公司新闻" value="公司新闻" />
            <el-option label="通知公告" value="通知公告" />
            <el-option label="政策制度" value="政策制度" />
          </el-select>
        </el-form-item>
        <el-form-item label="内容" prop="content">
          <el-input 
            v-model="form.content" 
            type="textarea" 
            rows="6" 
            placeholder="请输入公告内容"
            maxlength="5000"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="置顶">
          <el-switch v-model="form.isTop" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">发布</el-button>
      </template>
    </el-dialog>

    <!-- ===== 公告详情对话框 ===== -->
    <el-dialog 
      v-model="detailVisible" 
      title="公告详情" 
      width="700px"
      :close-on-click-modal="false"
    >
      <div class="detail-wrap" v-if="detailData">
        <div class="detail-header">
          <h2 class="detail-title">{{ detailData.title }}</h2>
          <div class="detail-meta">
            <span>{{ detailData.category }}</span>
            <span>{{ detailData.publisherName || detailData.publisher || '系统管理员' }}</span>
            <span>{{ formatTime(detailData.publishTime) }}</span>
            <span>👁️ {{ detailData.readCount || 0 }} 次阅读</span>
            <el-tag 
              :type="detailData.status === '已发布' ? 'success' : 'info'" 
              size="small"
            >
              {{ detailData.status }}
            </el-tag>
            <el-tag 
              v-if="detailData.isTop === 1" 
              type="warning" 
              size="small"
            >📌 置顶</el-tag>
          </div>
        </div>
        <el-divider />
        <div class="detail-content">
          {{ detailData.content || '暂无内容' }}
        </div>
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- ===== 阅读统计对话框 ===== -->
    <el-dialog v-model="showStatsDialog" title="公告阅读统计" width="700px">
      <!-- 总览统计 -->
      <el-row :gutter="16" style="margin-bottom:20px;">
        <el-col :span="8">
          <div class="stats-card" style="background:#f0f7ff;">
            <div class="stats-card-number" style="color:#409eff;">{{ statsOverview.total }}</div>
            <div class="stats-card-label">总公告</div>
          </div>
        </el-col>
        <el-col :span="8">
          <div class="stats-card" style="background:#f0fff4;">
            <div class="stats-card-number" style="color:#67c23a;">{{ statsOverview.totalReads }}</div>
            <div class="stats-card-label">总阅读量</div>
          </div>
        </el-col>
        <el-col :span="8">
          <div class="stats-card" style="background:#fff8ef;">
            <div class="stats-card-number" style="color:#e6a23c;">{{ statsOverview.avgRate }}%</div>
            <div class="stats-card-label">平均阅读率</div>
          </div>
        </el-col>
      </el-row>

      <!-- 各公告阅读统计 -->
      <el-table :data="statsData" style="width:100%;" max-height="350" stripe>
        <el-table-column prop="title" label="公告标题" min-width="200">
          <template #default="{ row }">
            <el-link type="primary" @click="openDetailDialog({ id: row.id })">
              {{ row.title.length > 30 ? row.title.substring(0, 30) + '...' : row.title }}
            </el-link>
          </template>
        </el-table-column>
        <el-table-column prop="readCount" label="阅读量" width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small" type="info">{{ row.readCount || 0 }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="totalCount" label="总人数" width="100" align="center">
          <template #default="{ row }">
            {{ row.totalCount || 0 }}
          </template>
        </el-table-column>
        <el-table-column label="阅读率" width="200">
          <template #default="{ row }">
            <div class="rate-bar-wrap">
              <div class="rate-bar" :style="{ width: (row.readRate || 0) + '%' }"></div>
              <span class="rate-text">{{ row.readRate || 0 }}%</span>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Search, InfoFilled, DataAnalysis } from '@element-plus/icons-vue'
import {
  getAnnouncementPage,
  getAnnouncementByCategory,
  getAnnouncementById,
  publishAnnouncement,
  withdrawAnnouncement,
  setTopAnnouncement,
  cancelTopAnnouncement,
  deleteAnnouncement,
  getAnnouncementReadStats,
  markReadAnnouncement
} from '@/api/announcement'

// ==================== 数据 ====================
const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const detailVisible = ref(false)
const showStatsDialog = ref(false)
const formRef = ref()
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const tableData = ref([])
const detailData = ref(null)
const allData = ref([])
const statsData = ref([])

// 获取当前用户信息
const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}')

const searchForm = reactive({
  title: '',
  category: '',
  status: ''
})

const form = reactive({
  title: '',
  category: '',
  content: '',
  isTop: 0
})

const rules = {
  title: [
    { required: true, message: '请输入公告标题', trigger: 'blur' },
    { max: 100, message: '标题不能超过100个字', trigger: 'blur' }
  ],
  category: [{ required: true, message: '请选择分类', trigger: 'change' }],
  content: [
    { required: true, message: '请输入公告内容', trigger: 'blur' },
    { max: 5000, message: '内容不能超过5000个字', trigger: 'blur' }
  ]
}

// ==================== 统计计算 ====================
const totalReadCount = computed(() => {
  return tableData.value.reduce((sum, item) => sum + (item.readCount || 0), 0)
})

const avgReadRate = computed(() => {
  const published = tableData.value.filter(item => item.status === '已发布')
  if (published.length === 0) return 0
  const total = published.reduce((sum, item) => sum + (item.readCount || 0), 0)
  const totalEmployees = published.length > 0 ? published[0]?.totalEmployees || 100 : 100
  return Math.round((total / (published.length * Math.max(totalEmployees, 1))) * 100)
})

const maxReadCount = computed(() => {
  if (tableData.value.length === 0) return 0
  return Math.max(...tableData.value.map(item => item.readCount || 0))
})

const statsOverview = computed(() => {
  const total = statsData.value.length
  const totalReads = statsData.value.reduce((sum, item) => sum + (item.readCount || 0), 0)
  const avgRate = statsData.value.length > 0 
    ? Math.round(statsData.value.reduce((sum, item) => sum + (item.readRate || 0), 0) / statsData.value.length)
    : 0
  return { total, totalReads, avgRate }
})

// ==================== 方法 ====================
const formatTime = (time) => {
  if (!time) return ''
  return time.replace('T', ' ')
}

// ==================== 前端过滤 ====================
const filterAnnouncements = (list) => {
  let result = [...list]
  
  if (searchForm.title && searchForm.title.trim()) {
    const keyword = searchForm.title.trim().toLowerCase()
    result = result.filter(item => 
      item.title.toLowerCase().includes(keyword)
    )
  }
  
  if (searchForm.category) {
    result = result.filter(item => item.category === searchForm.category)
  }
  
  if (searchForm.status) {
    result = result.filter(item => item.status === searchForm.status)
  }
  
  return result
}

// 加载数据
const loadData = async () => {
  loading.value = true
  try {
    let response = null
    
    if (searchForm.category) {
      response = await getAnnouncementByCategory(searchForm.category)
    } else {
      response = await getAnnouncementPage(currentPage.value, pageSize.value)
    }
    
    if (response && response.code === 0) {
      const data = response.data
      let list = []
      
      if (data && data.data && Array.isArray(data.data)) {
        list = data.data
      } else if (Array.isArray(data)) {
        list = data
      } else if (data && data.records && Array.isArray(data.records)) {
        list = data.records
      } else {
        list = []
      }
      
      allData.value = list
      
      const filtered = filterAnnouncements(list)
      
      const start = (currentPage.value - 1) * pageSize.value
      const end = start + pageSize.value
      tableData.value = filtered.slice(start, end)
      total.value = filtered.length
    } else {
      ElMessage.error(response?.msg || '加载公告失败')
      tableData.value = []
      total.value = 0
      allData.value = []
    }
  } catch (error) {
    console.error('加载公告异常:', error)
    ElMessage.error('加载公告失败')
    tableData.value = []
    total.value = 0
    allData.value = []
  } finally {
    loading.value = false
  }
}

// 加载阅读统计
const loadStats = async () => {
  try {
    const res = await getAnnouncementReadStats()
    if (res.code === 0 && res.data) {
      statsData.value = res.data.stats || []
    }
  } catch (error) {
    console.error('加载阅读统计失败:', error)
  }
}

// ==================== 查看详情 ====================
const openDetailDialog = async (row) => {
  try {
    // 如果有内容直接显示
    if (row.content) {
      detailData.value = row
      detailVisible.value = true
      return
    }
    
    const res = await getAnnouncementById(row.id)
    if (res.code === 0) {
      detailData.value = res.data
      detailVisible.value = true
      // 标记已读（如果是已发布状态）
      if (res.data.status === '已发布') {
        try {
          await markReadAnnouncement(row.id)
        } catch (error) {
          console.error('标记已读失败:', error)
        }
      }
    } else {
      ElMessage.error(res.msg || '获取详情失败')
    }
  } catch (error) {
    console.error('获取详情失败:', error)
    ElMessage.error('获取详情失败')
  }
}

// 搜索
const handleSearch = () => {
  currentPage.value = 1
  loadData()
}

// 重置搜索
const resetSearch = () => {
  searchForm.title = ''
  searchForm.category = ''
  searchForm.status = ''
  currentPage.value = 1
  loadData()
}

// 打开新增对话框
const openAddDialog = () => {
  form.title = ''
  form.category = ''
  form.content = ''
  form.isTop = 0
  dialogVisible.value = true
}

// 重置表单
const resetForm = () => {
  form.title = ''
  form.category = ''
  form.content = ''
  form.isTop = 0
  formRef.value?.resetFields()
}

// 提交发布
const handleSubmit = async () => {
  await formRef.value.validate(async (valid) => {
    if (!valid) return

    submitting.value = true
    try {
      const submitData = {
        ...form,
        publisherId: userInfo.id || 1,
        publisherName: userInfo.name || '系统管理员'
      }

      const res = await publishAnnouncement(submitData)

      if (res.code === 0) {
        ElMessage.success('发布成功')
        dialogVisible.value = false
        loadData()
      } else {
        ElMessage.error(res.msg || '发布失败')
      }
    } catch (error) {
      console.error('发布失败:', error)
      ElMessage.error('发布失败，请稍后重试')
    } finally {
      submitting.value = false
    }
  })
}

// 置顶/取消置顶
const handleTop = async (row) => {
  const isCurrentlyTop = row.isTop === 1
  
  try {
    await ElMessageBox.confirm(
      isCurrentlyTop ? '确定取消置顶该公告吗？' : '确定置顶该公告吗？',
      '提示',
      { confirmButtonText: '确定', cancelButtonText: '取消' }
    )

    const res = isCurrentlyTop 
      ? await cancelTopAnnouncement(row.id)
      : await setTopAnnouncement(row.id)

    if (res.code === 0) {
      ElMessage.success(isCurrentlyTop ? '已取消置顶' : '置顶成功')
      loadData()
    } else {
      ElMessage.error(res.msg || '操作失败')
    }
  } catch {
    // 用户取消
  }
}

// 删除公告
const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要永久删除公告「${row.title}」吗？\n此操作不可恢复！`,
      '警告',
      { 
        confirmButtonText: '确定删除', 
        cancelButtonText: '取消', 
        type: 'warning' 
      }
    )
    
    const res = await deleteAnnouncement(row.id)
    if (res.code === 0) {
      ElMessage.success('公告已永久删除')
      loadData()
    } else {
      ElMessage.error(res.msg || '删除失败')
    }
  } catch {
    // 用户取消
  }
}

// ==================== 生命周期 ====================
onMounted(() => {
  loadData()
  loadStats()
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

.search-card {
  margin-bottom: 20px;
}

.search-card :deep(.el-form-item) {
  margin-bottom: 0;
}

.search-card :deep(.el-form-item__label) {
  font-weight: 500;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

/* ===== 统计卡片 ===== */
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

/* ===== 详情样式 ===== */
.detail-wrap {
  padding: 8px 0;
}

.detail-header {
  margin-bottom: 16px;
}

.detail-title {
  margin: 0 0 12px 0;
  font-size: 22px;
  color: #1a1a2e;
  word-break: break-all;
}

.detail-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  font-size: 14px;
  color: #999;
}

.detail-meta span {
  display: flex;
  align-items: center;
  gap: 4px;
}

.detail-content {
  font-size: 15px;
  line-height: 1.8;
  color: #333;
  padding: 8px 0;
  white-space: pre-wrap;
  word-break: break-all;
  min-height: 100px;
}

/* ===== 阅读统计对话框样式 ===== */
.stats-card {
  padding: 16px;
  border-radius: 8px;
  text-align: center;
}

.stats-card-number {
  font-size: 32px;
  font-weight: bold;
}

.stats-card-label {
  font-size: 14px;
  color: #666;
  margin-top: 4px;
}

.rate-bar-wrap {
  display: flex;
  align-items: center;
  gap: 8px;
}

.rate-bar {
  height: 8px;
  background: linear-gradient(90deg, #409eff, #67c23a);
  border-radius: 4px;
  transition: width 0.5s;
  min-width: 4px;
}

.rate-text {
  font-size: 13px;
  color: #666;
  white-space: nowrap;
  min-width: 40px;
}

/* ===== 表格操作按钮优化 ===== */
:deep(.el-table .cell) {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 4px;
}

:deep(.el-table .el-button) {
  margin: 0;
}

:deep(.el-table .el-button--text) {
  padding: 4px 8px;
}

/* ===== 标题列长文本处理 ===== */
:deep(.el-table .el-link) {
  max-width: 300px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  display: inline-block;
  vertical-align: middle;
}
</style>