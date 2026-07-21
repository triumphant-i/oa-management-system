// src/directives/permission.js
import { hasPermission } from '@/utils/permission'

/**
 * 权限指令 v-permission
 * 
 * 用法：
 *   <!-- 单个权限 -->
 *   <el-button v-permission="'employee:add'">添加员工</el-button>
 *   
 *   <!-- 多个权限（有任意一个即可） -->
 *   <el-button v-permission="['employee:edit', 'employee:delete']">操作</el-button>
 *   
 *   <!-- 布尔值 -->
 *   <div v-permission="true">显示</div>
 *   <div v-permission="false">隐藏</div>
 */
const permission = {
  mounted(el, binding) {
    checkPermission(el, binding)
  },
  updated(el, binding) {
    checkPermission(el, binding)
  }
}

function checkPermission(el, binding) {
  const { value } = binding
  
  // 如果没有传值，默认隐藏
  if (value === undefined || value === null) {
    el.style.display = 'none'
    return
  }
  
  let hasPerm = false
  
  if (Array.isArray(value)) {
    // 数组：有任意一个权限即可
    hasPerm = value.some(p => hasPermission(p))
  } else if (typeof value === 'string') {
    // 字符串：必须有该权限
    hasPerm = hasPermission(value)
  } else if (typeof value === 'boolean') {
    // 布尔值：直接使用
    hasPerm = value
  } else {
    hasPerm = false
  }
  
  el.style.display = hasPerm ? '' : 'none'
}

export default permission