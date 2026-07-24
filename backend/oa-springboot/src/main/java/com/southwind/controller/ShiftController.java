package com.southwind.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.southwind.entity.Shift;
import com.southwind.service.ShiftService;
import com.southwind.util.ResultVOUtil;
import com.southwind.vo.PageVO;
import com.southwind.vo.ResultVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/shift")
public class ShiftController {

    @Autowired
    private ShiftService shiftService;

    @GetMapping("/list")
    public ResultVO list() {
        List<Shift> shifts = shiftService.getStandardShifts();
        return ResultVOUtil.success(shifts);
    }

    @GetMapping("/listAll/{page}/{size}")
    public ResultVO listAll(@PathVariable("page") Integer page, @PathVariable("size") Integer size) {
        Page<Shift> shiftPage = new Page<>(page, size);
        QueryWrapper<Shift> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByDesc("is_custom");
        queryWrapper.orderByAsc("id");
        Page<Shift> resultPage = shiftService.page(shiftPage, queryWrapper);
        PageVO pageVO = new PageVO();
        pageVO.setTotal(resultPage.getTotal());
        pageVO.setData(resultPage.getRecords());
        return ResultVOUtil.success(pageVO);
    }

    @GetMapping("/get/{id}")
    public ResultVO get(@PathVariable("id") Integer id) {
        Shift shift = shiftService.getById(id);
        if (shift == null) {
            return ResultVOUtil.fail("班次不存在");
        }
        return ResultVOUtil.success(shift);
    }

    @PostMapping("/add")
    public ResultVO add(@RequestBody Shift shift) {
        shift.setIsCustom(0);
        shift.setCreateTime(LocalDateTime.now());
        shift.setUpdateTime(LocalDateTime.now());
        boolean save = shiftService.save(shift);
        if (!save) {
            return ResultVOUtil.fail("添加失败");
        }
        return ResultVOUtil.success("添加成功");
    }

    @PostMapping("/update")
    public ResultVO update(@RequestBody Shift shift) {
        shift.setUpdateTime(LocalDateTime.now());
        boolean update = shiftService.updateById(shift);
        if (!update) {
            return ResultVOUtil.fail("更新失败");
        }
        return ResultVOUtil.success("更新成功");
    }

    @PostMapping("/delete/{id}")
    public ResultVO delete(@PathVariable("id") Integer id) {
        Shift shift = shiftService.getById(id);
        if (shift != null && shift.getIsDefault() == 1) {
            return ResultVOUtil.fail("不能删除默认班次");
        }
        boolean remove = shiftService.removeById(id);
        if (!remove) {
            return ResultVOUtil.fail("删除失败");
        }
        return ResultVOUtil.success("删除成功");
    }

    @PostMapping("/createCustom")
    public ResultVO createCustom(@RequestBody Shift shift) {
        shift.setIsCustom(1);
        shift.setIsDefault(0);
        shift.setCreateTime(LocalDateTime.now());
        shift.setUpdateTime(LocalDateTime.now());
        boolean save = shiftService.save(shift);
        if (!save) {
            return ResultVOUtil.fail("创建专属班次失败");
        }
        return ResultVOUtil.success("创建成功");
    }

    @GetMapping("/getDefault")
    public ResultVO getDefault() {
        Shift shift = shiftService.getDefaultShift();
        return ResultVOUtil.success(shift);
    }

    @PostMapping("/setDefault/{id}")
    public ResultVO setDefault(@PathVariable("id") Integer id) {
        Shift shift = shiftService.getById(id);
        if (shift == null) {
            return ResultVOUtil.fail("班次不存在");
        }
        if (shift.getIsCustom() == 1) {
            return ResultVOUtil.fail("专属班次不能设为默认");
        }
        QueryWrapper<Shift> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("is_default", 1);
        List<Shift> defaults = shiftService.list(queryWrapper);
        for (Shift s : defaults) {
            s.setIsDefault(0);
            shiftService.updateById(s);
        }
        shift.setIsDefault(1);
        shiftService.updateById(shift);
        return ResultVOUtil.success("设置成功");
    }
}