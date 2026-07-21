<template>
  <div class="page-container">
    <div class="page-header">
      <h2>工作流管理</h2>
      <el-button type="primary" @click="dialogVisible = true">
        <el-icon><Plus /></el-icon> 新建流程模板
      </el-button>
    </div>

    <el-card>
      <el-tabs v-model="activeTab">
        <!-- 模板列表 -->
        <el-tab-pane label="模板列表" name="template">
          <el-table :data="templateList" style="width:100%;">
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="name" label="模板名称" width="180" />
            <el-table-column prop="type" label="业务类型" width="120">
              <template #default="{ row }">
                <el-tag>{{ row.type }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="version" label="版本" width="80" />
            <el-table-column prop="status" label="状态" width="120">
              <template #default="{ row }">
                <el-tag :type="statusMap[row.status]?.type || 'info'">
                  {{ statusMap[row.status]?.label || row.status }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="updateTime" label="更新时间" width="180" />
            <el-table-column label="操作" width="300" fixed="right">
              <template #default="{ row }">
                <el-button size="small" type="primary" text>编辑</el-button>
                <el-button
                  v-if="row.status === 'draft'"
                  size="small"
                  type="success"
                  text
                >发布</el-button>
                <el-button
                  v-if="row.status === 'published'"
                  size="small"
                  type="warning"
                  text
                >停用</el-button>
                <el-button size="small" type="danger" text>删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- 运行实例 -->
        <el-tab-pane label="运行实例" name="instance">
          <el-table :data="instanceList" style="width:100%;">
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="templateName" label="模板名称" width="180" />
            <el-table-column prop="businessId" label="业务单据" width="120" />
            <el-table-column prop="applicant" label="发起人" width="120" />
            <el-table-column prop="currentNode" label="当前节点" width="150" />
            <el-table-column prop="status" label="状态" width="120">
              <template #default="{ row }">
                <el-tag :type="instanceStatusMap[row.status]?.type || 'info'">
                  {{ instanceStatusMap[row.status]?.label || row.status }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="150">
              <template #default>
                <el-button size="small" type="info" text>查看</el-button>
                <el-button size="small" type="danger" text>终止</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 新建模板对话框 -->
    <el-dialog v-model="dialogVisible" title="新建流程模板" width="600px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="模板名称">
          <el-input v-model="form.name" placeholder="请输入模板名称" />
        </el-form-item>
        <el-form-item label="业务类型">
          <el-select v-model="form.type" placeholder="请选择业务类型">
            <el-option label="请假申请" value="leave" />
            <el-option label="出差申请" value="business" />
            <el-option label="加班申请" value="overtime" />
            <el-option label="报销申请" value="reimburse" />
            <el-option label="采购申请" value="purchase" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.desc" type="textarea" rows="3" />
        </el-form-item>
        <el-form-item label="审批节点">
          <div v-for="(node, index) in form.nodes" :key="index" style="margin-bottom:8px;">
            <el-input v-model="node.name" placeholder="节点名称" style="width:150px;margin-right:8px;" />
            <el-select v-model="node.approver" placeholder="审批人来源" style="width:150px;margin-right:8px;">
              <el-option label="部门主管" value="department_leader" />
              <el-option label="指定人员" value="specified" />
              <el-option label="发起人自选" value="self_select" />
            </el-select>
            <el-button type="danger" text @click="removeNode(index)">删除</el-button>
          </div>
          <el-button type="primary" text @click="addNode">
            <el-icon><Plus /></el-icon> 添加审批节点
          </el-button>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存草稿</el-button>
        <el-button type="success" @click="handlePublish">发布</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'

const activeTab = ref('template')
const dialogVisible = ref(false)

const statusMap = {
  draft: { label: '草稿', type: 'info' },
  published: { label: '已发布', type: 'success' },
  inactive: { label: '已停用', type: 'warning' }
}

const instanceStatusMap = {
  running: { label: '运行中', type: 'primary' },
  completed: { label: '已完成', type: 'success' },
  rejected: { label: '已拒绝', type: 'danger' },
  terminated: { label: '已终止', type: 'warning' }
}

const form = reactive({
  name: '',
  type: '',
  desc: '',
  nodes: [
    { name: '部门主管审批', approver: 'department_leader' }
  ]
})

const templateList = ref([
  { id: 1, name: '请假审批流程', type: '请假', version: 'v2.1', status: 'published', updateTime: '2026-07-20 08:00' },
  { id: 2, name: '报销审批流程', type: '报销', version: 'v1.0', status: 'draft', updateTime: '2026-07-19 14:00' }
])

const instanceList = ref([
  { id: 1, templateName: '请假审批流程', businessId: 'AP-2026-001', applicant: '张三', currentNode: '部门主管审批', status: 'running' },
  { id: 2, templateName: '报销审批流程', businessId: 'AP-2026-002', applicant: '李四', currentNode: '财务审批', status: 'completed' }
])

const addNode = () => {
  form.nodes.push({ name: '', approver: '' })
}

const removeNode = (index) => {
  form.nodes.splice(index, 1)
}

const handleSave = () => {
  ElMessage.success('草稿已保存')
  dialogVisible.value = false
}

const handlePublish = () => {
  ElMessage.success('模板已发布')
  dialogVisible.value = false
}
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
</style>