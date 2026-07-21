<script setup>
// ... 其他代码

const handleSubmit = async () => {
  // ... 登录逻辑
  
  if (res.code === 0) {
    const token = res.data?.token
    const employee = res.data?.employee || res.data
    
    // 保存 Token
    localStorage.setItem('token', token)
    
    // ✅ 保存用户信息（必须包含 role）
    const userInfo = {
      id: employee?.id,
      username: employee?.username || form.username,
      name: employee?.name || form.username,
      role: employee?.role || 'EMPLOYEE',  // ⚠️ 关键：必须有 role
      departmentId: employee?.departmentId,
      avatar: employee?.avatar || '',
      phone: employee?.phone || '',
      email: employee?.email || ''
    }
    localStorage.setItem('userInfo', JSON.stringify(userInfo))
    
    // 保存员工ID
    if (employee?.id) {
      localStorage.setItem('employeeId', String(employee.id))
    }
    
    console.log('登录成功，用户信息:', userInfo)
    console.log('用户角色:', userInfo.role)
    
    ElMessage.success(`欢迎回来，${userInfo.name}！`)
    router.replace('/employee')
  }
}
</script>