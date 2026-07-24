// src/directives/permission.js
import { hasPermission } from '@/utils/permission'

export default {
  mounted(el, binding) {
    const { value } = binding
    if (value) {
      const hasPerm = hasPermission(value)
      if (!hasPerm) {
        el.style.display = 'none'
      }
    }
  },
  updated(el, binding) {
    const { value } = binding
    if (value) {
      const hasPerm = hasPermission(value)
      if (!hasPerm) {
        el.style.display = 'none'
      } else {
        el.style.display = ''
      }
    }
  }
}