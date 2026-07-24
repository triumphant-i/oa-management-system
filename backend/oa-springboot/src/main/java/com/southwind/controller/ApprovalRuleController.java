package com.southwind.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.southwind.annotation.RequirePermission;
import com.southwind.entity.ApprovalRole;
import com.southwind.entity.ApprovalRule;
import com.southwind.enums.RoleType;
import com.southwind.mapper.ApprovalRoleMapper;
import com.southwind.mapper.ApprovalRuleMapper;
import com.southwind.service.flowable.ApprovalRuleEvaluator;
import com.southwind.util.ResultVOUtil;
import com.southwind.vo.ResultVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/approval-rule")
@RequirePermission(roles = RoleType.SYSTEM_ADMIN, description = "审批规则管理仅限系统管理员")
public class ApprovalRuleController {
    @Autowired private ApprovalRuleMapper ruleMapper;
    @Autowired private ApprovalRuleEvaluator ruleEvaluator;
    @Autowired private ApprovalRoleMapper approvalRoleMapper;

    /**
     * 获取审批角色枚举列表（从数据库读取，供前端多选下拉框使用）
     */
    @GetMapping("/roles")
    public ResultVO listRoles() {
        List<ApprovalRole> roles = approvalRoleMapper.selectList(null);
        List<Map<String, Object>> result = roles.stream().map(r -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("code", r.getRoleCode());
            item.put("name", r.getRoleName());
            return item;
        }).collect(Collectors.toList());
        return ResultVOUtil.success(result);
    }

    @GetMapping
    public ResultVO list(@RequestParam(required = false) String approvalType) {
        QueryWrapper<ApprovalRule> q = new QueryWrapper<ApprovalRule>().orderByAsc("approval_type").orderByAsc("priority").orderByAsc("id");
        if (approvalType != null && !approvalType.trim().isEmpty()) q.eq("approval_type", approvalType);
        return ResultVOUtil.success(ruleMapper.selectList(q));
    }
    @PostMapping
    public ResultVO create(@RequestBody ApprovalRule rule) { return save(rule, false); }
    @PutMapping("/{id}")
    public ResultVO update(@PathVariable Long id, @RequestBody ApprovalRule rule) { rule.setId(id); return save(rule, true); }
    @DeleteMapping("/{id}")
    public ResultVO delete(@PathVariable Long id) {
        if (ruleMapper.deleteById(id) == 0) return ResultVOUtil.fail("规则不存在");
        ruleEvaluator.evictCache(); return ResultVOUtil.success(null);
    }
    private ResultVO save(ApprovalRule rule, boolean updating) {
        String error = validate(rule);
        if (error != null) return ResultVOUtil.fail(error);
        List<ApprovalRule> sameType = ruleMapper.selectList(new QueryWrapper<ApprovalRule>().eq("approval_type", rule.getApprovalType()));
        for (ApprovalRule other : sameType) {
            if (updating && other.getId().equals(rule.getId())) continue;
            if (overlaps(rule, other)) return ResultVOUtil.fail("与规则 #" + other.getId() + " 的区间重叠");
        }
        int affected = updating ? ruleMapper.updateById(rule) : ruleMapper.insert(rule);
        if (affected == 0) return ResultVOUtil.fail(updating ? "规则不存在或保存失败" : "新增规则失败");
        ruleEvaluator.evictCache(); return ResultVOUtil.success(rule);
    }
    private String validate(ApprovalRule r) {
        if (blank(r.getApprovalType()) || blank(r.getConditionField()) || blank(r.getRequiredRoles()) || blank(r.getSignType())) return "审批类型、条件字段、审批角色和会签方式不能为空";
        if (r.getMinValue() != null && r.getMaxValue() != null && r.getMinValue().compareTo(r.getMaxValue()) >= 0) return "区间下限必须小于上限";
        return null;
    }
    private boolean overlaps(ApprovalRule a, ApprovalRule b) {
        if (!a.getConditionField().equals(b.getConditionField())) return false;
        java.math.BigDecimal low = a.getMinValue() == null ? b.getMinValue() : b.getMinValue() == null ? a.getMinValue() : a.getMinValue().max(b.getMinValue());
        java.math.BigDecimal high = a.getMaxValue() == null ? b.getMaxValue() : b.getMaxValue() == null ? a.getMaxValue() : a.getMaxValue().min(b.getMaxValue());
        return low == null || high == null || low.compareTo(high) < 0;
    }
    private boolean blank(String value) { return value == null || value.trim().isEmpty(); }
}