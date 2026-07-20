package com.southwind.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.southwind.annotation.RequirePermission;
import com.southwind.entity.Dict;
import com.southwind.enums.RoleType;
import com.southwind.service.DictService;
import com.southwind.util.ResultVOUtil;
import com.southwind.util.ValidatorUtil;
import com.southwind.vo.ResultVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/dict")
public class DictController {

    @Autowired
    private DictService dictService;

    @GetMapping("/list")
    public ResultVO list() {
        List<Dict> dicts = dictService.list();
        return ResultVOUtil.success(dicts);
    }

    @GetMapping("/findById/{id}")
    public ResultVO findById(@PathVariable("id") Integer id) {
        try {
            ValidatorUtil.validId(id, "字典ID");
            Dict dict = dictService.getById(id);
            if (dict == null) {
                return ResultVOUtil.fail("数据字典不存在");
            }
            return ResultVOUtil.success(dict);
        } catch (IllegalArgumentException e) {
            return ResultVOUtil.fail(e.getMessage());
        }
    }

    @GetMapping("/findByKey/{key}")
    public ResultVO findByKey(@PathVariable("key") String key) {
        QueryWrapper<Dict> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("key", key);
        Dict dict = dictService.getOne(queryWrapper);
        if (dict == null) {
            return ResultVOUtil.fail("数据字典不存在");
        }
        return ResultVOUtil.success(dict);
    }

    @PostMapping("/save")
    @RequirePermission(roles = {RoleType.SYSTEM_ADMIN}, description = "添加数据字典")
    public ResultVO save(@RequestBody Dict dict) {
        try {
            ValidatorUtil.notEmpty(dict.getKey(), "字典键");
            ValidatorUtil.notEmpty(dict.getName(), "字典名称");
            
            QueryWrapper<Dict> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("key", dict.getKey());
            if (dictService.getOne(queryWrapper) != null) {
                return ResultVOUtil.fail("字典键已存在");
            }
            
            dict.setCreateTime(LocalDateTime.now());
            dict.setUpdateTime(LocalDateTime.now());
            
            boolean save = dictService.save(dict);
            if (!save) return ResultVOUtil.fail("操作失败");
            return ResultVOUtil.success(null);
        } catch (IllegalArgumentException e) {
            return ResultVOUtil.fail(e.getMessage());
        }
    }

    @PutMapping("/update")
    @RequirePermission(roles = {RoleType.SYSTEM_ADMIN}, description = "更新数据字典")
    public ResultVO update(@RequestBody Dict dict) {
        try {
            ValidatorUtil.validId(dict.getId(), "字典ID");
            
            Dict existing = dictService.getById(dict.getId());
            if (existing == null) {
                return ResultVOUtil.fail("数据字典不存在");
            }
            
            dict.setUpdateTime(LocalDateTime.now());
            boolean update = dictService.updateById(dict);
            if (!update) return ResultVOUtil.fail("操作失败");
            return ResultVOUtil.success(null);
        } catch (IllegalArgumentException e) {
            return ResultVOUtil.fail(e.getMessage());
        }
    }

    @DeleteMapping("/deleteById/{id}")
    @RequirePermission(roles = {RoleType.SYSTEM_ADMIN}, description = "删除数据字典")
    public ResultVO deleteById(@PathVariable("id") Integer id) {
        try {
            ValidatorUtil.validId(id, "字典ID");
            
            Dict dict = dictService.getById(id);
            if (dict == null) {
                return ResultVOUtil.fail("数据字典不存在");
            }
            
            boolean delete = dictService.removeById(id);
            if (!delete) return ResultVOUtil.fail("操作失败");
            return ResultVOUtil.success(null);
        } catch (IllegalArgumentException e) {
            return ResultVOUtil.fail(e.getMessage());
        }
    }
}