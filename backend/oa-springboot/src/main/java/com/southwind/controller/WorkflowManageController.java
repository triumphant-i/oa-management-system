package com.southwind.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.southwind.entity.Employee;
import com.southwind.mapper.EmployeeMapper;
import com.southwind.util.ResultVOUtil;
import com.southwind.vo.ResultVO;
import org.flowable.engine.HistoryService;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 工作流管理控制器
 * 提供流程定义、流程实例、流程部署等管理接口
 */
@RestController
@RequestMapping("/workflow")
public class WorkflowManageController {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowManageController.class);

    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private HistoryService historyService;

    @Autowired
    private ProcessEngine processEngine;

    @Autowired
    private EmployeeMapper employeeMapper;

    // 员工ID到姓名的缓存映射
    private Map<Integer, String> employeeIdToNameCache = new HashMap<>();

    /**
     * 获取员工姓名（带缓存）
     */
    private String getEmployeeNameById(String employeeIdStr) {
        if (employeeIdStr == null || employeeIdStr.trim().isEmpty()) {
            return null;
        }
        try {
            Integer employeeId = Integer.parseInt(employeeIdStr.trim());
            
            // 先查缓存
            if (employeeIdToNameCache.containsKey(employeeId)) {
                return employeeIdToNameCache.get(employeeId);
            }
            
            // 查数据库
            Employee employee = employeeMapper.selectById(employeeId);
            if (employee != null && employee.getName() != null) {
                employeeIdToNameCache.put(employeeId, employee.getName());
                return employee.getName();
            }
            return employeeIdStr; // 如果查不到，返回原ID
        } catch (NumberFormatException e) {
            return employeeIdStr; // 如果不是数字，直接返回
        }
    }

    /**
     * 查询所有流程定义列表
     */
    @GetMapping("/definitions")
    public ResultVO getProcessDefinitions() {
        try {
            List<ProcessDefinition> definitions = repositoryService.createProcessDefinitionQuery()
                    .latestVersion()
                    .orderByProcessDefinitionName()
                    .asc()
                    .list();

            List<Map<String, Object>> resultList = definitions.stream().map(def -> {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", def.getId());
                item.put("key", def.getKey());
                item.put("name", def.getName());
                item.put("version", def.getVersion());
                item.put("deploymentId", def.getDeploymentId());
                item.put("suspended", def.isSuspended());
                item.put("resourceName", def.getResourceName());
                return item;
            }).collect(Collectors.toList());

            logger.info("✓ 查询流程定义列表成功: {} 个定义", resultList.size());
            return ResultVOUtil.success(resultList);
        } catch (Exception e) {
            logger.error("❌ 查询流程定义列表失败: {}", e.getMessage(), e);
            return ResultVOUtil.fail("查询流程定义列表失败: " + e.getMessage());
        }
    }

    /**
     * 查询流程定义详情
     */
    @GetMapping("/definition/{processDefinitionId}")
    public ResultVO getProcessDefinitionDetail(@PathVariable String processDefinitionId) {
        try {
            ProcessDefinition definition = repositoryService.createProcessDefinitionQuery()
                    .processDefinitionId(processDefinitionId)
                    .singleResult();

            if (definition == null) {
                return ResultVOUtil.fail("流程定义不存在");
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("id", definition.getId());
            result.put("key", definition.getKey());
            result.put("name", definition.getName());
            result.put("version", definition.getVersion());
            result.put("deploymentId", definition.getDeploymentId());
            result.put("suspended", definition.isSuspended());
            result.put("resourceName", definition.getResourceName());

            logger.info("✓ 查询流程定义详情成功: {}", processDefinitionId);
            return ResultVOUtil.success(result);
        } catch (Exception e) {
            logger.error("❌ 查询流程定义详情失败: {}", e.getMessage(), e);
            return ResultVOUtil.fail("查询流程定义详情失败: " + e.getMessage());
        }
    }

    /**
     * 获取流程定义的BPMN XML原始内容
     */
    @GetMapping("/definition/{processDefinitionId}/xml")
    public ResultVO getProcessDefinitionXml(@PathVariable String processDefinitionId) {
        try {
            ProcessDefinition definition = repositoryService.createProcessDefinitionQuery()
                    .processDefinitionId(processDefinitionId)
                    .singleResult();

            if (definition == null) {
                return ResultVOUtil.fail("流程定义不存在: " + processDefinitionId);
            }

            String deploymentId = definition.getDeploymentId();
            String resourceName = definition.getResourceName();

            InputStream xmlStream = repositoryService.getResourceAsStream(deploymentId, resourceName);
            if (xmlStream == null) {
                return ResultVOUtil.fail("无法读取流程定义资源文件: " + resourceName);
            }

            String xmlContent;
            try {
                xmlContent = new String(xmlStream.readAllBytes(), StandardCharsets.UTF_8);
            } finally {
                xmlStream.close();
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("processDefinitionId", processDefinitionId);
            result.put("resourceName", resourceName);
            result.put("xml", xmlContent);

            logger.info("✓ 获取流程定义XML成功: {}, resourceName={}", processDefinitionId, resourceName);
            return ResultVOUtil.success(result);
        } catch (Exception e) {
            logger.error("❌ 获取流程定义XML失败: {}", e.getMessage(), e);
            return ResultVOUtil.fail("获取流程定义XML失败: " + e.getMessage());
        }
    }

    /**
     * 查询所有流程实例列表
     */
    @GetMapping("/instances")
    public ResultVO getProcessInstances(
            @RequestParam(value = "processDefinitionKey", required = false) String processDefinitionKey,
            @RequestParam(value = "status", required = false) String status) {
        try {
            List<ProcessInstance> instances = runtimeService.createProcessInstanceQuery()
                    .orderByStartTime()
                    .desc()
                    .list();

            List<Map<String, Object>> resultList = instances.stream()
                    .filter(instance -> {
                        if (processDefinitionKey != null && !processDefinitionKey.isEmpty()) {
                            return instance.getProcessDefinitionKey().equals(processDefinitionKey);
                        }
                        return true;
                    })
                    .map(instance -> {
                        Map<String, Object> item = new LinkedHashMap<>();
                        item.put("id", instance.getId());
                        item.put("processDefinitionId", instance.getProcessDefinitionId());
                        item.put("processDefinitionKey", instance.getProcessDefinitionKey());
                        item.put("processDefinitionName", instance.getProcessDefinitionName());
                        item.put("businessKey", instance.getBusinessKey());
                        item.put("suspended", instance.isSuspended());
                        item.put("startTime", formatDateTime(instance.getStartTime()));
                        item.put("endTime", null); // 运行中的流程实例尚未结束，无结束时间
                        
                        // 从流程变量获取审批相关信息
                        try {
                            Object approvalIdObj = runtimeService.getVariable(instance.getId(), "approvalId");
                            if (approvalIdObj != null) {
                                item.put("approvalId", approvalIdObj);
                            }
                            Object applicantNameObj = runtimeService.getVariable(instance.getId(), "applicantName");
                            if (applicantNameObj != null) {
                                item.put("applicantName", applicantNameObj);
                            }
                        } catch (Exception e) {
                            logger.warn("获取流程变量失败: processInstanceId={}, {}", instance.getId(), e.getMessage());
                        }
                        
                        return item;
                    })
                    .collect(Collectors.toList());

            logger.info("✓ 查询流程实例列表成功: {} 个实例", resultList.size());
            return ResultVOUtil.success(resultList);
        } catch (Exception e) {
            logger.error("❌ 查询流程实例列表失败: {}", e.getMessage(), e);
            return ResultVOUtil.fail("查询流程实例列表失败: " + e.getMessage());
        }
    }

    /**
     * 查询流程实例详情
     */
    @GetMapping("/instance/{processInstanceId}")
    public ResultVO getProcessInstanceDetail(@PathVariable String processInstanceId) {
        try {
            ProcessInstance instance = runtimeService.createProcessInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .singleResult();

            if (instance == null) {
                // 查询历史实例
                HistoricProcessInstance historicInstance = historyService.createHistoricProcessInstanceQuery()
                        .processInstanceId(processInstanceId)
                        .singleResult();
                
                if (historicInstance == null) {
                    return ResultVOUtil.fail("流程实例不存在");
                }

                Map<String, Object> result = new LinkedHashMap<>();
                result.put("id", historicInstance.getId());
                result.put("processDefinitionId", historicInstance.getProcessDefinitionId());
                result.put("processDefinitionKey", historicInstance.getProcessDefinitionKey());
                result.put("processDefinitionName", historicInstance.getProcessDefinitionName());
                result.put("businessKey", historicInstance.getBusinessKey());
                result.put("startTime", formatDateTime(historicInstance.getStartTime()));
                result.put("endTime", formatDateTime(historicInstance.getEndTime()));
                result.put("deleteReason", historicInstance.getDeleteReason());
                result.put("status", "已完成");

                logger.info("✓ 查询历史流程实例详情成功: {}", processInstanceId);
                return ResultVOUtil.success(result);
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("id", instance.getId());
            result.put("processDefinitionId", instance.getProcessDefinitionId());
            result.put("processDefinitionKey", instance.getProcessDefinitionKey());
            result.put("processDefinitionName", instance.getProcessDefinitionName());
            result.put("businessKey", instance.getBusinessKey());
            result.put("suspended", instance.isSuspended());
            result.put("startTime", formatDateTime(instance.getStartTime()));
            result.put("endTime", null); // 运行中的流程实例尚未结束，无结束时间
            result.put("status", "运行中");

            logger.info("✓ 查询流程实例详情成功: {}", processInstanceId);
            return ResultVOUtil.success(result);
        } catch (Exception e) {
            logger.error("❌ 查询流程实例详情失败: {}", e.getMessage(), e);
            return ResultVOUtil.fail("查询流程实例详情失败: " + e.getMessage());
        }
    }

    /**
     * 终止流程实例
     */
    @PutMapping("/instance/{processInstanceId}/terminate")
    public ResultVO terminateProcessInstance(
            @PathVariable String processInstanceId,
            @RequestParam(value = "reason", required = false) String reason) {
        try {
            ProcessInstance instance = runtimeService.createProcessInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .singleResult();

            if (instance == null) {
                return ResultVOUtil.fail("流程实例不存在或已结束");
            }

            String deleteReason = reason != null ? reason : "管理员手动终止";
            runtimeService.deleteProcessInstance(processInstanceId, deleteReason);

            logger.info("✓ 流程实例已终止: processInstanceId={}, reason={}", processInstanceId, deleteReason);
            return ResultVOUtil.success(null);
        } catch (Exception e) {
            logger.error("❌ 终止流程实例失败: {}", e.getMessage(), e);
            return ResultVOUtil.fail("终止流程实例失败: " + e.getMessage());
        }
    }

    /**
     * 暂停流程实例
     */
    @PutMapping("/instance/{processInstanceId}/suspend")
    public ResultVO suspendProcessInstance(@PathVariable String processInstanceId) {
        try {
            ProcessInstance instance = runtimeService.createProcessInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .singleResult();

            if (instance == null) {
                return ResultVOUtil.fail("流程实例不存在");
            }

            if (instance.isSuspended()) {
                return ResultVOUtil.fail("流程实例已被暂停");
            }

            runtimeService.suspendProcessInstanceById(processInstanceId);

            logger.info("✓ 流程实例已暂停: processInstanceId={}", processInstanceId);
            return ResultVOUtil.success(null);
        } catch (Exception e) {
            logger.error("❌ 暂停流程实例失败: {}", e.getMessage(), e);
            return ResultVOUtil.fail("暂停流程实例失败: " + e.getMessage());
        }
    }

    /**
     * 恢复流程实例（取消暂停）
     */
    @PutMapping("/instance/{processInstanceId}/activate")
    public ResultVO activateProcessInstance(@PathVariable String processInstanceId) {
        try {
            ProcessInstance instance = runtimeService.createProcessInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .singleResult();

            if (instance == null) {
                return ResultVOUtil.fail("流程实例不存在");
            }

            if (!instance.isSuspended()) {
                return ResultVOUtil.fail("流程实例未被暂停");
            }

            runtimeService.activateProcessInstanceById(processInstanceId);

            logger.info("✓ 流程实例已恢复: processInstanceId={}", processInstanceId);
            return ResultVOUtil.success(null);
        } catch (Exception e) {
            logger.error("❌ 恢复流程实例失败: {}", e.getMessage(), e);
            return ResultVOUtil.fail("恢复流程实例失败: " + e.getMessage());
        }
    }

    /**
     * 获取流程实例的历史任务信息（用于追踪审批进度）
     */
    @GetMapping("/instance/{processInstanceId}/history")
    public ResultVO getProcessHistory(@PathVariable String processInstanceId) {
        try {
            List<org.flowable.task.api.history.HistoricTaskInstance> historicTasks = 
                historyService.createHistoricTaskInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .orderByHistoricTaskInstanceStartTime()
                    .asc()
                    .list();

            List<Map<String, Object>> resultList = historicTasks.stream().map(task -> {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", task.getId());
                item.put("name", task.getName());
                item.put("assigneeId", task.getAssignee()); // 保存原始ID
                item.put("assignee", getEmployeeNameById(task.getAssignee())); // 转换为姓名
                item.put("owner", task.getOwner());
                item.put("createTime", formatDateTime(task.getCreateTime()));
                item.put("completeTime", formatDateTime(task.getEndTime()));
                item.put("dueDate", formatDateTime(task.getDueDate()));
                item.put("deleteReason", task.getDeleteReason());
                // 【新增】节点定义ID，用于前端按节点分组统计会签进度
                item.put("taskDefinitionKey", task.getTaskDefinitionKey());

                // 计算处理时长（分钟）
                if (task.getEndTime() != null) {
                    long duration = (task.getEndTime().getTime() - task.getCreateTime().getTime()) / 60000;
                    item.put("durationMinutes", duration);
                }

                // 【新增】读取该任务的局部变量 outcome（同意/拒绝），用于会签明细展示
                // 注意：必须是 setVariableLocal 设置的任务局部变量才能查到，
                // 流程级变量（execution scope）不会关联到具体某个 taskId
                try {
                    List<HistoricVariableInstance> outcomeVars = historyService
                            .createHistoricVariableInstanceQuery()
                            .taskId(task.getId())
                            .variableName("outcome")
                            .list();
                    if (!outcomeVars.isEmpty() && outcomeVars.get(0).getValue() != null) {
                        item.put("outcome", outcomeVars.get(0).getValue().toString());
                    }
                } catch (Exception ex) {
                    logger.warn("读取任务局部变量outcome失败: taskId={}, {}", task.getId(), ex.getMessage());
                }

                return item;
            }).collect(Collectors.toList());

            logger.info("✓ 查询流程历史任务成功: processInstanceId={}, 任务数={}", processInstanceId, resultList.size());
            return ResultVOUtil.success(resultList);
        } catch (Exception e) {
            logger.error("❌ 查询流程历史任务失败: {}", e.getMessage(), e);
            return ResultVOUtil.fail("查询流程历史任务失败: " + e.getMessage());
        }
    }

    /**
     * 查询流程实例的当前待办任务
     */
    @GetMapping("/instance/{processInstanceId}/tasks")
    public ResultVO getProcessPendingTasks(@PathVariable String processInstanceId) {
        try {
            List<org.flowable.task.api.Task> tasks = processEngine.getTaskService()
                    .createTaskQuery()
                    .processInstanceId(processInstanceId)
                    .active()
                    .orderByTaskCreateTime()
                    .asc()
                    .list();

            List<Map<String, Object>> resultList = tasks.stream().map(task -> {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", task.getId());
                item.put("name", task.getName());
                item.put("assigneeId", task.getAssignee()); // 保存原始ID
                item.put("assignee", getEmployeeNameById(task.getAssignee())); // 转换为姓名
                item.put("owner", task.getOwner());
                item.put("createTime", formatDateTime(task.getCreateTime()));
                item.put("dueDate", formatDateTime(task.getDueDate()));
                item.put("priority", task.getPriority());
                // 【新增】节点定义ID，与 current-activities 返回的 activityId 一一对应，
                // 用于前端精确判断"这几个待办任务是不是同一个会签节点上的并行实例"
                item.put("taskDefinitionKey", task.getTaskDefinitionKey());
                return item;
            }).collect(Collectors.toList());

            logger.info("✓ 查询流程待办任务成功: processInstanceId={}, 任务数={}", processInstanceId, resultList.size());
            return ResultVOUtil.success(resultList);
        } catch (Exception e) {
            logger.error("❌ 查询流程待办任务失败: {}", e.getMessage(), e);
            return ResultVOUtil.fail("查询流程待办任务失败: " + e.getMessage());
        }
    }

    /**
     * 获取流程实例当前所在的节点ID列表
     * 多实例并行审批时可能同时卡在多个节点
     * 流程已结束时返回空数组
     */
    @GetMapping("/instance/{processInstanceId}/current-activities")
    public ResultVO getCurrentActivities(@PathVariable String processInstanceId) {
        try {
            ProcessInstance instance = runtimeService.createProcessInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .singleResult();

            if (instance == null) {
                // 流程已结束，返回空数组，前端改用历史记录展示
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("processInstanceId", processInstanceId);
                result.put("active", false);
                result.put("activityIds", Collections.emptyList());
                logger.info("✓ 流程实例已结束，返回空活动节点: {}", processInstanceId);
                return ResultVOUtil.success(result);
            }

            List<String> activityIds = runtimeService.getActiveActivityIds(processInstanceId);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("processInstanceId", processInstanceId);
            result.put("active", true);
            result.put("activityIds", activityIds != null ? activityIds : Collections.emptyList());

            logger.info("✓ 获取当前活动节点成功: processInstanceId={}, activities={}", processInstanceId, activityIds);
            return ResultVOUtil.success(result);
        } catch (Exception e) {
            logger.error("❌ 获取当前活动节点失败: {}", e.getMessage(), e);
            return ResultVOUtil.fail("获取当前活动节点失败: " + e.getMessage());
        }
    }

    /**
     * 获取流程统计信息
     */
    @GetMapping("/statistics")
    public ResultVO getWorkflowStatistics() {
        try {
            Map<String, Object> stats = new LinkedHashMap<>();

            // 当前运行中的流程实例
            long runningInstances = runtimeService.createProcessInstanceQuery().count();
            stats.put("runningInstances", runningInstances);

            // 流程定义数
            long processDefinitionCount = repositoryService.createProcessDefinitionQuery().count();
            stats.put("processDefinitionCount", processDefinitionCount);

            // 已完成的流程实例
            long completedInstances = historyService.createHistoricProcessInstanceQuery()
                    .finished()
                    .count();
            stats.put("completedInstances", completedInstances);

            // 当前待办任务数
            long pendingTasks = processEngine.getTaskService().createTaskQuery().count();
            stats.put("pendingTasks", pendingTasks);

            logger.info("✓ 获取工作流统计信息成功");
            return ResultVOUtil.success(stats);
        } catch (Exception e) {
            logger.error("❌ 获取工作流统计信息失败: {}", e.getMessage(), e);
            return ResultVOUtil.fail("获取工作流统计信息失败: " + e.getMessage());
        }
    }

    /**
     * 工具方法：将 Date 转换为 LocalDateTime 字符串
     */
    private String formatDateTime(Date date) {
        if (date == null) {
            return null;
        }
        LocalDateTime dateTime = date.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
        return dateTime.toString();
    }
}