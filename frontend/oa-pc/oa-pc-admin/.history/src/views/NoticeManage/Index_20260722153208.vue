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
      </div>
    </div>

    <!-- 搜索栏 -->
    <el-card class="search-card">
      <el-form :inline="true" :model="searchForm" @keyup.enter="handleSearch">
        <el-form-item label="标题">
          <el-input 
            v-model="searchForm.title" 
            placeholder="请输入公告标题" 
            clearable 
            style="width:200px;"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="searchForm.category" placeholder="请选择分类" clearable @change="handleSearch">
            <el-option label="全部" value="" />
            <el-option label="公司新闻" value="公司新闻" />
            <el-option label="通知公告" value="通知公告" />
            <el-option label="政策制度" value="政策制度" />
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
            <el-link type="primary" @click="openDetailDialog(row)">{{ row.title }}</el-link>
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
        <el-table-column prop="publishTime" label="发布时间" width="180">
          <template #default="{ row }">
            {{ formatTime(row.publishTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="readCount" label="阅读量" width="100">
          <template #default="{ row }">
            {{ row.readCount || 0 }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === '已发布' ? 'success' : 'info'">
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="info" text @click="openDetailDialog(row)">查看</el-button>
            
            <el-button 
              v-if="row.status === '已发布'" 
              size="small" type="warning" text 
              @click="handleTop(row)"
            >
              {{ row.isTop === 1 ? '取消置顶' : '置顶' }}
            </el-button>
            
            <el-button 
              v-if="row.status === '已发布'" 
              size="small" type="danger" text 
              @click="handleWithdraw(row)"
            >撤回</el-button>
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
          <el-input v-model="form.title" placeholder="请输入公告标题" />
        </el-form-item>
        <el-form-item label="分类" prop="category">
          <el-select v-model="form.category" placeholder="请选择分类">
            <el-option label="公司新闻" value="公司新闻" />
            <el-option label="通知公告" value="通知公告" />
            <el-option label="政策制度" value="政策制度" />
          </el-select>
        </el-form-item>
        <el-form-item label="内容" prop="content">
          <el-input v-model="form.content" type="textarea" rows="6" placeholder="请输入公告内容" />
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
            <span>{{ detailData.readCount || 0 }} 次阅读</span>
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
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import {
  getAnnouncementPage,
  getAnnouncementByCategory,
  getAnnouncementById,
  publishAnnouncement,
  withdrawAnnouncement,
  setTopAnnouncement,
  cancelTopAnnouncement
} from '@/api/announcement'

// ==================== 数据 ====================
const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const detailVisible = ref(false)
const formRef = ref()
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const tableData = ref([])
const detailData = ref(null)
const allData = ref([]) // 存储所有数据用于前端过滤

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
  title: [{ required: true, message: '请输入公告标题', trigger: 'blur' }],
  category: [{ required: true, message: '请选择分类', trigger: 'change' }],
  content: [{ required: true, message: '请输入公告内容', trigger: 'blur' }]
}

// ==================== 方法 ====================
const formatTime = (time) => {
  if (!time) return ''
  return time.replace('T', ' ')
}

// ==================== 前端过滤（支持标题搜索） ====================
const filterAnnouncements = (list) => {
  let result = [...list]
  
  // 按标题过滤（模糊搜索）
  if (searchForm.title && searchForm.title.trim()) {
    const keyword = searchForm.title.trim().toLowerCase()
    result = result.filter(item => 
      item.title.toLowerCase().includes(keyword)
    )
  }
  
  // 按分类过滤
  if (searchForm.category) {
    result = result.filter(item => item.category === searchForm.category)
  }
  
  return result
}

// 加载数据
const loadData = async () => {
  loading.value = true
  try {
    console.log('========== 加载公告数据 ==========')
    console.log('当前页码:', currentPage.value)
    console.log('每页大小:', pageSize.value)
    console.log('搜索条件:', searchForm)
    
    let response = null
    
    // 如果有分类搜索，使用分类查询
    if (searchForm.category) {
      console.log('按分类查询:', searchForm.category)
      response = await getAnnouncementByCategory(searchForm.category)
    } else {
      // 分页查询
      console.log('分页查询: /announcement/list/', currentPage.value, '/', pageSize.value)
      response = await getAnnouncementPage(currentPage.value, pageSize.value)
    }
    
    console.log('API 响应:', response)
    
    if (response && response.code === 0) {
      const data = response.data
      console.log('响应数据:', data)
      
      let list = []
      
      // 后端返回的是 PageVO 结构: { total, data, current, size }
      if (data && data.data && Array.isArray(data.data)) {
        list = data.data
        console.log('从 PageVO 解析: total=', data.total, ', data.length=', list.length)
      } else if (Array.isArray(data)) {
        list = data
        console.log('从数组解析: 共', list.length, '条')
      } else if (data && data.records && Array.isArray(data.records)) {
        list = data.records
        console.log('从 MyBatis Plus 分页解析: total=', data.total)
      } else {
        list = []
        console.warn('未知数据结构:', data)
      }
      
      // 存储所有数据
      allData.value = list
      
      // 前端过滤（支持标题搜索）
      const filtered = filterAnnouncements(list)
      
      // 分页处理（前端分页）
      const start = (currentPage.value - 1) * pageSize.value
      const end = start + pageSize.value
      tableData.value = filtered.slice(start, end)
      total.value = filtered.length
      
      console.log('过滤后数据: total=', total.value, ', 当前页数据=', tableData.value.length)
      
      if (tableData.value.length === 0 && filtered.length === 0) {
        console.warn('公告列表为空，请检查数据库中是否有数据')
      }
    } else {
      console.error('API 返回错误:', response?.msg || '未知错误')
      ElMessage.error(response?.msg || '加载公告失败')
      tableData.value = []
      total.value = 0
      allData.value = []
    }
  } catch (error) {
    console.error('加载公告异常:', error)
    console.error('错误详情:', error.message)
    ElMessage.error('加载公告失败：' + (error.message || '网络错误'))
    tableData.value = []
    total.value = 0
    allData.value = []
  } finally {
    loading.value = false
    console.log('=================================')
  }
}

// ==================== 查看详情 ====================
const openDetailDialog = async (row) => {
  try {
    if (row.content) {
      detailData.value = row
      detailVisible.value = true
      return
    }
    
    const res = await getAnnouncementById(row.id)
    if (res.code === 0) {
      detailData.value = res.data
      detailVisible.value = true
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
  // 如果有关键词，重新加载数据
  if (searchForm.title && searchForm.title.trim()) {
    // 需要重新获取所有数据然后过滤
    loadData()
  } else {
    loadData()
  }
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
  const isTop = row.isTop === 1
  try {
    await ElMessageBox.confirm(
      isTop ? '确定取消置顶该公告吗？' : '确定置顶该公告吗？',
      '提示',
      { confirmButtonText: '确定', cancelButtonText: '取消' }
    )

    const res = isTop 
      ? await cancelTopAnnouncement(row.id)
      : await setTopAnnouncement(row.id)

    if (res.code === 0) {
      ElMessage.success(isTop ? '已取消置顶' : '置顶成功')
      loadData()
    } else {
      ElMessage.error(res.msg || '操作失败')
    }
  } catch {
    // 用户取消
  }
}

// 撤回
const handleWithdraw = async (row) => {
  try {
    await ElMessageBox.confirm(
      '确定撤回该公告吗？撤回后员工将无法看到该公告。',
      '提示',
      { confirmButtonText: '确定', cancelButtonText: '取消' }
    )

    const res = await withdrawAnnouncement(row.id)
    if (res.code === 0) {
      ElMessage.success('已撤回')
      loadData()
    } else {
      ElMessage.error(res.msg || '操作失败')
    }
  } catch {
    // 用户取消
  }
}

// ==================== 生命周期 ====================
onMounted(() => {
  console.log('========== 公告管理页面加载 ==========')
  console.log('用户信息:', userInfo)
  loadData()
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
}

.search-card {
  margin-bottom: 20px;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
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
</style>