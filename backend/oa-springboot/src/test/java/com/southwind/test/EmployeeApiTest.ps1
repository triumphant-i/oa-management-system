$token = "eyJhbGciOiJIUzI1NiJ9.eyJyb2xlIjoiU1lTVEVNX0FETUlOIiwiZGVwYXJ0bWVudElkIjoxLCJuYW1lIjoi57O757uf566h55CG5ZGYIiwidXNlcklkIjoxLCJ1c2VybmFtZSI6ImFkbWluIiwic3ViIjoiYWRtaW4iLCJpYXQiOjE3ODQyNTI3MzUsImV4cCI6MTc4NDMzOTEzNX0.AIwgHpMja9KYmJ72vgTNG-QijCTsKZsyTjOAmfByCyk"

Write-Host "========== 测试员工管理功能 ==========" -ForegroundColor Green

# 1. 查询员工列表
Write-Host "`n1. 查询员工列表（第1页，每页10条）" -ForegroundColor Yellow
$headers = @{Authorization = "Bearer $token"}
$result1 = Invoke-RestMethod -Method Get -Uri "http://localhost:8080/employee/list/1/10" -Headers $headers
$result1 | ConvertTo-Json -Depth 5

# 2. 查询员工详情
Write-Host "`n2. 查询员工详情（ID=1）" -ForegroundColor Yellow
$result2 = Invoke-RestMethod -Method Get -Uri "http://localhost:8080/employee/findById/1" -Headers $headers
$result2 | ConvertTo-Json -Depth 5

# 3. 搜索员工
Write-Host "`n3. 搜索员工（姓名包含'张'）" -ForegroundColor Yellow
$result3 = Invoke-RestMethod -Method Get -Uri "http://localhost:8080/employee/search?key=name&value=张" -Headers $headers
$result3 | ConvertTo-Json -Depth 5

# 4. 按部门查询员工
Write-Host "`n4. 按部门查询员工（部门ID=1）" -ForegroundColor Yellow
$result4 = Invoke-RestMethod -Method Get -Uri "http://localhost:8080/employee/findByDepartment/1" -Headers $headers
$result4 | ConvertTo-Json -Depth 5

Write-Host "`n========== 测试完成 ==========" -ForegroundColor Green