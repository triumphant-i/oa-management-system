// src/directives/permission.js
import { hasPermission } from '@/utils/permission'

/**
 * 权限指令 v-permission
 * 用法：
 *   <div v-permission="'employee:add'">只有有权限才显示</div>
 *   <div v-permission="['employee:add', 'employee:edit']">有任意权限就显示</div>
 */
export default {
  mounted(el, binding) {
    const { value } = binding
    
    if (!value) {
      return
    }
    
    let hasPerm = false
    
    if (Array.isArray(value)) {
      // 数组：有任意一个权限即可
      hasPerm = value.some(permission => hasPermission(permission))
    } else {
      // 字符串：必须有该权限
      hasPerm = hasPermission(value)
    }
    
    if (!hasPerm) {
      el.style.display = 'none'
      // 或者使用 v-if 的替代：el.parentNode?.removeChild(el)
    }
  }
}