<template>
  <div class="dept-node">
    <div class="dept-card" @click="toggleExpand">
      <div class="dept-left">
        <van-icon :name="isExpanded ? 'arrow-down' : 'arrow-right'" size="14" color="#999" />
        <div class="dept-icon" :style="{ background: getColor(level) }">
          <van-icon name="cluster-o" size="16" color="#fff" />
        </div>
        <div class="dept-info">
          <span class="dept-name">{{ dept.name }}</span>
          <span class="dept-manager">负责人：{{ dept.managerName || '未指定' }}</span>
        </div>
      </div>
      <div class="dept-right">
        <span class="dept-count">{{ dept.memberCount || 0 }}人</span>
        <span class="dept-level">Lv.{{ dept.level }}</span>
        <van-icon name="ellipsis" size="18" color="#999" @click.stop="showMenu = true" />
      </div>
    </div>

    <div class="children-wrap" v-if="isExpanded">
      <div class="members-section" v-if="currentDeptMembers.length > 0">
        <div class="members-title">
          <van-icon name="user-o" size="14" color="#3677ef" />
          <span>成员 ({{ currentDeptMembers.length }})</span>
        </div>
        <div class="member-list">
          <div class="member-item" v-for="member in currentDeptMembers" :key="member.id">
            <van-image round width="32" height="32" :src="member.avatar || '/default-avatar.png'" />
            <div class="member-info">
              <span class="member-name">{{ member.name }}</span>
              <span class="member-position">{{ member.position }}</span>
              <span class="member-contact">{{ member.phone }} · {{ member.email }}</span>
            </div>
            <van-tag size="mini" :type="member.status === '在职' ? 'success' : 'danger'">
              {{ member.status }}
            </van-tag>
          </div>
        </div>
      </div>

      <DeptNode 
        v-for="child in children" 
        :key="child.id"
        :dept="child"
        :level="level + 1"
        :children="getChildDepts(child.id)"
        :members="getChildMembers(child.id)"
        :expanded="expanded"
        @toggle="$emit('toggle', $event)"
        @edit="$emit('edit', $event)"
        @delete="$emit('delete', $event)"
        @addChild="$emit('addChild', $event)"
        @setManager="$emit('setManager', $event)"
        @viewMembers="$emit('viewMembers', $event)"
        @transfer="$emit('transfer', $event)"
      />
    </div>

    <van-action-sheet v-model:show="showMenu" :actions="actions" @select="onSelect" cancel-text="取消" />
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'

const props = defineProps({
  dept: Object,
  level: Number,
  children: Array,
  members: Array,
  expanded: Boolean
})

const emit = defineEmits(['toggle', 'edit', 'delete', 'addChild', 'setManager', 'viewMembers', 'transfer'])

const internalExpanded = ref(false)
const showMenu = ref(false)

const isExpanded = computed(() => {
  return internalExpanded.value
})

const currentDeptMembers = computed(() => {
  if (!props.members || !props.dept) return []
  return props.members.filter(m => m.departmentId === props.dept.id)
})

const getColor = (level) => {
  const colors = ['#3677ef', '#6c5ce7', '#00b894', '#fdcb6e', '#e17055', '#0984e3']
  return colors[(level - 1) % colors.length]
}

const getChildDepts = (parentId) => {
  return props.children.filter(c => c.parentId === parentId)
}

const getChildMembers = (deptId) => {
  return props.members.filter(m => m.departmentId === deptId)
}

const toggleExpand = () => {
  internalExpanded.value = !internalExpanded.value
}

watch(() => props.expanded, (newVal) => {
  if (newVal !== undefined) {
    internalExpanded.value = newVal
  }
})

const actions = [
  { name: '编辑部门', value: 'edit' },
  { name: '添加子部门', value: 'addChild' },
  { name: '指定负责人', value: 'setManager' },
  { name: '查看成员', value: 'viewMembers' },
  { name: '调岗', value: 'transfer' },
  { name: '删除部门', value: 'delete', color: '#ee0a24' }
]

const onSelect = (action) => {
  showMenu.value = false
  emit(action.value, props.dept.id)
}
</script>

<style scoped>
.dept-node {
  display: flex;
  flex-direction: column;
}
.dept-card {
  background: #fff;
  border-radius: 12px;
  padding: 14px 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
  cursor: pointer;
  transition: all 0.2s;
}
.dept-card:active { transform: scale(0.98); }
.dept-left {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 1;
  min-width: 0;
}
.dept-icon {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.dept-info {
  flex: 1;
  min-width: 0;
}
.dept-name {
  font-size: 15px;
  font-weight: 500;
  color: #222;
  display: block;
}
.dept-manager {
  font-size: 12px;
  color: #999;
}
.dept-right {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}
.dept-count {
  font-size: 12px;
  color: #3677ef;
  background: #e8f0ff;
  padding: 2px 10px;
  border-radius: 10px;
}
.dept-level {
  font-size: 10px;
  color: #bbb;
  background: #f5f5f5;
  padding: 2px 8px;
  border-radius: 8px;
}
.children-wrap {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-top: 6px;
  margin-left: 32px;
}

.members-section {
  background: #f8fafc;
  border-radius: 10px;
  padding: 12px;
  margin-bottom: 4px;
}

.members-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 500;
  color: #3677ef;
  margin-bottom: 8px;
}

.member-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.member-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px 8px;
  background: #fff;
  border-radius: 8px;
}

.member-info {
  flex: 1;
  min-width: 0;
}

.member-name {
  font-size: 13px;
  font-weight: 500;
  color: #333;
  display: block;
}

.member-position {
  font-size: 11px;
  color: #999;
  display: block;
}

.member-contact {
  font-size: 10px;
  color: #bbb;
  display: block;
  margin-top: 2px;
}
</style>