package com.southwind.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.southwind.entity.Meeting;
import com.southwind.entity.MeetingRoom;
import com.southwind.service.MeetingRoomService;
import com.southwind.service.MeetingService;
import com.southwind.util.ResultVOUtil;
import com.southwind.vo.PageVO;
import com.southwind.vo.ResultVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 会议室控制器
 * 改造自RoomController
 */
@RestController
@RequestMapping("/meetingRoom")
public class MeetingRoomController {

    @Autowired
    private MeetingRoomService meetingRoomService;

    @Autowired
    private MeetingService meetingService;

    /**
     * 添加会议室
     */
    @PostMapping("/save")
    public ResultVO save(@RequestBody MeetingRoom meetingRoom) {
        meetingRoom.setStatus("可用");
        boolean save = meetingRoomService.save(meetingRoom);
        if (!save) return ResultVOUtil.fail("添加失败");
        return ResultVOUtil.success(null);
    }

    /**
     * 查询所有会议室
     */
    @GetMapping("/list")
    public ResultVO list() {
        List<MeetingRoom> list = meetingRoomService.list();
        return ResultVOUtil.success(list);
    }

    /**
     * 分页查询
     */
    @GetMapping("/list/{page}/{size}")
    public ResultVO listPage(@PathVariable("page") Integer page, @PathVariable("size") Integer size) {
        Page<MeetingRoom> roomPage = new Page<>(page, size);
        Page<MeetingRoom> resultPage = meetingRoomService.page(roomPage, null);

        PageVO pageVO = new PageVO();
        pageVO.setTotal(resultPage.getTotal());
        pageVO.setData(resultPage.getRecords());
        return ResultVOUtil.success(pageVO);
    }

    /**
     * 根据ID查询
     */
    @GetMapping("/findById/{id}")
    public ResultVO findById(@PathVariable("id") Integer id) {
        MeetingRoom meetingRoom = meetingRoomService.getById(id);
        if (meetingRoom == null) {
            return ResultVOUtil.fail("会议室不存在");
        }
        return ResultVOUtil.success(meetingRoom);
    }

    /**
     * 根据ID查询（detail接口）
     */
    @GetMapping("/detail/{id}")
    public ResultVO detail(@PathVariable("id") Integer id) {
        MeetingRoom meetingRoom = meetingRoomService.getById(id);
        if (meetingRoom == null) {
            return ResultVOUtil.fail("会议室不存在");
        }
        return ResultVOUtil.success(meetingRoom);
    }

    /**
     * 更新会议室
     */
    @PutMapping("/update")
    public ResultVO update(@RequestBody MeetingRoom meetingRoom) {
        boolean update = meetingRoomService.updateById(meetingRoom);
        if (!update) return ResultVOUtil.fail("更新失败");
        return ResultVOUtil.success(null);
    }

    /**
     * 删除会议室
     */
    @DeleteMapping("/deleteById/{id}")
    public ResultVO deleteById(@PathVariable("id") Integer id) {
        // 检查该会议室是否存在未结束的会议预约
        QueryWrapper<Meeting> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("room_id", id);
        queryWrapper.ne("status", "已取消");
        queryWrapper.ne("status", "已结束");
        long count = meetingService.count(queryWrapper);
        if (count > 0) {
            return ResultVOUtil.fail("该会议室下还有未结束的会议预约，无法删除");
        }
        
        boolean delete = meetingRoomService.removeById(id);
        if (!delete) return ResultVOUtil.fail("删除失败");
        return ResultVOUtil.success(null);
    }

    /**
     * 查询可用会议室
     */
    @GetMapping("/available")
    public ResultVO getAvailableRooms() {
        QueryWrapper<MeetingRoom> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", "可用");
        List<MeetingRoom> list = meetingRoomService.list(queryWrapper);
        return ResultVOUtil.success(list);
    }

    /**
     * 更新会议室状态
     */
    @PutMapping("/updateStatus/{id}/{status}")
    public ResultVO updateStatus(@PathVariable("id") Integer id, @PathVariable("status") String status) {
        MeetingRoom meetingRoom = new MeetingRoom();
        meetingRoom.setId(id);
        meetingRoom.setStatus(status);
        boolean update = meetingRoomService.updateById(meetingRoom);
        if (!update) return ResultVOUtil.fail("更新状态失败");
        return ResultVOUtil.success(null);
    }

    /**
     * 按容量查询
     */
    @GetMapping("/findByCapacity/{minCapacity}")
    public ResultVO findByCapacity(@PathVariable("minCapacity") Integer minCapacity) {
        QueryWrapper<MeetingRoom> queryWrapper = new QueryWrapper<>();
        queryWrapper.ge("capacity", minCapacity);
        queryWrapper.eq("status", "可用");
        List<MeetingRoom> list = meetingRoomService.list(queryWrapper);
        return ResultVOUtil.success(list);
    }
}