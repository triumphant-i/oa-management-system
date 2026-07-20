<template>
  <div class="oa-custom">
    <!-- 顶部导航 -->
    <div class="page-header">
      <van-icon name="arrow-left" size="22" @click="$router.back()" />
      <h1 class="header-title">自定义工作台</h1>
      <div class="header-right">
        <van-button type="primary" size="small" @click="saveCustom" :loading="saving">保存</van-button>
      </div>
    </div>

    <!-- 提示 -->
    <div class="tip-bar">
      <van-icon name="info-o" size="16" color="#3677ef" />
      <span>长按拖动调整顺序，取消勾选隐藏功能</span>
    </div>

    <!-- 功能列表 -->
    <div class="module-list">
      <div 
        class="module-item" 
        v-for="(item, index) in moduleList" 
        :key="item.key"
        :class="{ dragging: dragIndex === index }"
        draggable="true"
        @dragstart="onDragStart($event, index)"
        @dragend="onDragEnd"
        @dragover="onDragOver($event, index)"
        @drop="onDrop($event, index)"
      >
        <div class="drag-handle">
          <van-icon name="bars" size="18" color="#ccc" />
        </div>
        <div class="module-icon" :style="{ background: item.bgColor }">
          <van-icon :name="item.icon" size="20" :color="item.color" />
        </div>
        <span class="module-name">{{ item.name }}</span>
        <div class="module-switch">
          <van-switch v-model="item.visible" size="20" active-color="#3677ef" />
        </div>
      </div>
    </div>

    <!-- 底部按钮 -->
    <div class="bottom-actions">
      <van-button plain block @click="resetDefault" class="reset-btn">
        恢复默认布局
      </van-button>
    </div>

    <div class="safe-bottom"></div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showToast, showConfirmDialog } from 'vant'

const router = useRouter()

const saving = ref(false)
const dragIndex = ref(null)

// ===== 所有可配置的模块 =====
const defaultModules = [
  { key: 'approval', name: '审批中心', icon: 'records', color: '#3677ef', bgColor: '#e8f0ff', visible: true },
  { key: 'attendance', name: '考勤打卡', icon: 'clock-o', color: '#ff6b35', bgColor: '#fff0e8', visible: true },
  { key: 'notice', name: '公告中心', icon: 'bullhorn-o', color: '#00b894', bgColor: '#e8f8f0', visible: true },
  { key: 'meeting', name: '会议室', icon: 'location-o', color: '#6c5ce7', bgColor: '#f0e8ff', visible: true },
  { key: 'employee', name: '通讯录', icon: 'friends-o', color: '#3677ef', bgColor: '#e8f0ff', visible: true },
  { key: 'department', name: '组织架构', icon: 'cluster-o', color: '#fdcb6e', bgColor: '#fff8e8', visible: true },
  { key: 'apply', name: '发起申请', icon: 'edit', color: '#e17055', bgColor: '#ffe8f0', visible: true },
  { key: 'profile', name: '个人中心', icon: 'user-o', color: '#00b894', bgColor: '#e8f8ff', visible: true },
  { key: 'system', name: '系统管理', icon: 'setting-o', color: '#e17055', bgColor: '#f0e8e8', visible: true },
  { key: 'more', name: '更多', icon: 'apps-o', color: '#888', bgColor: '#f0f0f0', visible: true }
]

const moduleList = ref([...defaultModules])

// ===== 从 localStorage 加载 =====
const loadCustom = () => {
  const saved = localStorage.getItem('customModules')
  if (saved) {
    try {
      const parsed = JSON.parse(saved)
      // 合并：保留默认模块的所有字段，只覆盖 visible 和顺序
      const merged = defaultModules.map(defaultItem => {
        const savedItem = parsed.find(p => p.key === defaultItem.key)
        return savedItem ? { ...defaultItem, visible: savedItem.visible } : defaultItem
      })
      // 按保存的顺序排序
      const orderMap = {}
      parsed.forEach((item, index) => {
        orderMap[item.key] = index
      })
      merged.sort((a, b) => {
        const aIndex = orderMap[a.key] !== undefined ? orderMap[a.key] : 999
        const bIndex = orderMap[b.key] !== undefined ? orderMap[b.key] : 999
        return aIndex - bIndex
      })
      moduleList.value = merged
    } catch (e) {
      console.error('加载自定义布局失败', e)
    }
  }
}

// ===== 保存自定义 =====
const saveCustom = () => {
  saving.value = true
  setTimeout(() => {
    const data = moduleList.value.map(item => ({
      key: item.key,
      visible: item.visible
    }))
    localStorage.setItem('customModules', JSON.stringify(data))
    saving.value = false
    showToast('✅ 布局已保存')
    // 通知首页刷新
    window.dispatchEvent(new Event('customChanged'))
  }, 500)
}

// ===== 恢复默认 =====
const resetDefault = () => {
  showConfirmDialog({
    title: '确认恢复',
    message: '确定要恢复默认布局吗？',
    confirmButtonText: '确定恢复'
  }).then(() => {
    moduleList.value = defaultModules.map(item => ({ ...item }))
    showToast('已恢复默认布局')
  }).catch(() => {})
}

// ===== 拖拽排序 =====
const onDragStart = (e, index) => {
  dragIndex.value = index
  e.dataTransfer.effectAllowed = 'move'
}

const onDragEnd = () => {
  dragIndex.value = null
}

const onDragOver = (e, index) => {
  e.preventDefault()
}

const onDrop = (e, index) => {
  e.preventDefault()
  if (dragIndex.value === null || dragIndex.value === index) return
  
  const items = [...moduleList.value]
  const [draggedItem] = items.splice(dragIndex.value, 1)
  items.splice(index, 0, draggedItem)
  moduleList.value = items
  dragIndex.value = null
}

// ===== 初始化 =====
onMounted(() => {
  loadCustom()
})
</script>

<style scoped>
.oa-custom {
  max-width: 430px;
  margin: 0 auto;
  min-height: 100vh;
  background: #f5f7fa;
  padding: 0 16px 20px;
  padding-bottom: 80px;
  box-shadow: 0 0 20px rgba(0,0,0,0.06);
}

/* ===== 顶部导航 ===== */
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 0 12px;
  background: #fff;
  margin: 0 -16px 0;
  padding: 16px 16px 12px;
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

/* ===== 提示 ===== */
.tip-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  background: #f0f7ff;
  border-radius: 10px;
  margin-bottom: 16px;
  font-size: 13px;
  color: #888;
}

/* ===== 模块列表 ===== */
.module-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.module-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
  cursor: grab;
  transition: all 0.2s;
  border: 2px solid transparent;
}
.module-item:active {
  cursor: grabbing;
}
.module-item.dragging {
  opacity: 0.5;
  border-color: #3677ef;
  transform: scale(0.98);
}
.drag-handle {
  cursor: grab;
  padding: 4px;
}
.drag-handle:active {
  cursor: grabbing;
}
.module-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.module-name {
  flex: 1;
  font-size: 15px;
  color: #333;
  font-weight: 500;
}
.module-switch {
  flex-shrink: 0;
}

/* ===== 底部按钮 ===== */
.bottom-actions {
  padding: 16px 0 8px;
}
.reset-btn {
  border-radius: 12px !important;
  height: 44px !important;
  border-color: #e17055 !important;
  color: #e17055 !important;
}
.reset-btn:active {
  background: #fff0e8 !important;
}

.safe-bottom { height: 20px; }
</style>