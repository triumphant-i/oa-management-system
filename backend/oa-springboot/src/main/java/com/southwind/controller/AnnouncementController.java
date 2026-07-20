package com.southwind.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.southwind.common.UserContext;
import com.southwind.entity.Announcement;
import com.southwind.entity.Employee;
import com.southwind.service.AnnouncementReadStatusService;
import com.southwind.service.AnnouncementService;
import com.southwind.service.EmployeeService;
import com.southwind.util.ResultVOUtil;
import com.southwind.vo.PageVO;
import com.southwind.vo.ResultVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/announcement")
public class AnnouncementController {

    @Autowired
    private AnnouncementService announcementService;

    @Autowired
    private AnnouncementReadStatusService announcementReadStatusService;

    @Autowired
    private EmployeeService employeeService;

    @PostMapping("/publish")
    public ResultVO publish(@RequestBody Announcement announcement) {
        announcement.setStatus("已发布");
        announcement.setPublishTime(LocalDateTime.now());
        announcement.setCreateTime(LocalDateTime.now());
        boolean save = announcementService.save(announcement);
        if (!save) return ResultVOUtil.fail("发布失败");
        return ResultVOUtil.success(null);
    }

    @GetMapping("/list")
    public ResultVO list() {
        QueryWrapper<Announcement> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status","已发布");
        queryWrapper.orderByDesc("is_top");
        queryWrapper.orderByDesc("publish_time");
        List<Announcement> list = announcementService.list(queryWrapper);

        Integer employeeId = null;
        try {
            UserContext.UserInfo userInfo = UserContext.getCurrentUser();
            if (userInfo != null) {
            employeeId = userInfo.getUserId();
        }
        } catch (Exception e) {
        }

        if (employeeId != null) {
            for (Announcement announcement : list) {
                Integer readStatus = announcementReadStatusService.getReadStatus(announcement.getId(), employeeId);
                announcement.setIsRead(readStatus);
            }
        }

        return ResultVOUtil.success(list);
    }

    @GetMapping("/list/{page}/{size}")
    public ResultVO listPage(@PathVariable("page") Integer page, @PathVariable("size") Integer size) {
        Page<Announcement> announcementPage = new Page<>(page, size);
        QueryWrapper<Announcement> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status","已发布");
        queryWrapper.orderByDesc("is_top");
        queryWrapper.orderByDesc("publish_time");
        Page<Announcement> resultPage = announcementService.page(announcementPage, queryWrapper);

        Integer employeeId = null;
        try {
            UserContext.UserInfo userInfo = UserContext.getCurrentUser();
            if (userInfo != null) {
                employeeId = userInfo.getUserId();
            }
        } catch (Exception e) {
        }

        if (employeeId != null) {
            for (Announcement announcement : resultPage.getRecords()) {
                Integer readStatus = announcementReadStatusService.getReadStatus(announcement.getId(), employeeId);
                announcement.setIsRead(readStatus);
            }
        }

        PageVO pageVO = new PageVO();
        pageVO.setTotal(resultPage.getTotal());
        pageVO.setData(resultPage.getRecords());
        pageVO.setCurrent(resultPage.getCurrent());
        pageVO.setSize(resultPage.getSize());
        return ResultVOUtil.success(pageVO);
    }

    @GetMapping("/findById/{id}")
    public ResultVO findById(@PathVariable("id") Integer id) {
        Announcement announcement = announcementService.getById(id);
        if (announcement == null) {
            return ResultVOUtil.fail("公告不存在");
        }

        try {
            UserContext.UserInfo userInfo = UserContext.getCurrentUser();
            if (userInfo != null) {
                Integer readStatus = announcementReadStatusService.getReadStatus(id, userInfo.getUserId());
                announcement.setIsRead(readStatus);
                announcementReadStatusService.markAsRead(id, userInfo.getUserId());
            }
        } catch (Exception e) {
        }

        return ResultVOUtil.success(announcement);
    }

    @PutMapping("/update")
    public ResultVO update(@RequestBody Announcement announcement) {
        announcement.setUpdateTime(LocalDateTime.now());
        boolean update = announcementService.updateById(announcement);
        if (!update) return ResultVOUtil.fail("更新失败");
        return ResultVOUtil.success(null);
    }

    @DeleteMapping("/deleteById/{id}")
    public ResultVO deleteById(@PathVariable("id") Integer id) {
        boolean delete = announcementService.removeById(id);
        if (!delete) return ResultVOUtil.fail("删除失败");
        return ResultVOUtil.success(null);
    }

    @PutMapping("/setTop/{id}")
    public ResultVO setTop(@PathVariable("id") Integer id) {
        UpdateWrapper<Announcement> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", id).set("is_top", 1).set("update_time", LocalDateTime.now());
        
        boolean update = announcementService.update(updateWrapper);
        if (!update) return ResultVOUtil.fail("置顶失败");
        return ResultVOUtil.success(null);
    }

    @PutMapping("/cancelTop/{id}")
    public ResultVO cancelTop(@PathVariable("id") Integer id) {
        UpdateWrapper<Announcement> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", id).set("is_top", 0).set("update_time", LocalDateTime.now());
        
        boolean update = announcementService.update(updateWrapper);
        if (!update) return ResultVOUtil.fail("取消置顶失败");
        return ResultVOUtil.success(null);
    }

    @GetMapping("/findByCategory/{category}")
    public ResultVO findByCategory(@PathVariable("category") String category) {
        QueryWrapper<Announcement> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("category", category);
        queryWrapper.eq("status", "已发布");
        queryWrapper.orderByDesc("is_top");
        queryWrapper.orderByDesc("publish_time");
        List<Announcement> list = announcementService.list(queryWrapper);
        return ResultVOUtil.success(list);
    }

    @PutMapping("/markRead/{id}")
    public ResultVO markRead(@PathVariable("id") Integer id) {
        try {
            UserContext.UserInfo userInfo = UserContext.getCurrentUser();
            if (userInfo == null) {
                return ResultVOUtil.fail("未登录");
            }
            announcementReadStatusService.markAsRead(id, userInfo.getUserId());
            return ResultVOUtil.success(null);
        } catch (Exception e) {
            return ResultVOUtil.fail("操作失败");
        }
    }

    @GetMapping("/countUnread")
    public ResultVO countUnread() {
        try {
            UserContext.UserInfo userInfo = UserContext.getCurrentUser();
            if (userInfo == null) {
                return ResultVOUtil.fail("未登录");
            }
            Integer count = announcementReadStatusService.countUnread(userInfo.getUserId());
            Map<String, Integer> result = new HashMap<>();
            result.put("count", count != null ? count : 0);
            return ResultVOUtil.success(result);
        } catch (Exception e) {
            return ResultVOUtil.fail("操作失败");
        }
    }

    @PutMapping("/markAllRead")
    public ResultVO markAllRead() {
        try {
            UserContext.UserInfo userInfo = UserContext.getCurrentUser();
            if (userInfo == null) {
                return ResultVOUtil.fail("未登录");
            }
            announcementReadStatusService.markAllAsRead(userInfo.getUserId());
            return ResultVOUtil.success(null);
        } catch (Exception e) {
            return ResultVOUtil.fail("操作失败");
        }
    }

    @GetMapping("/readStats")
    public ResultVO getReadStats() {
        try {
            QueryWrapper<Announcement> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("status", "已发布");
            List<Announcement> announcements = announcementService.list(queryWrapper);

            QueryWrapper<Employee> employeeQueryWrapper = new QueryWrapper<>();
            employeeQueryWrapper.eq("status", "在职");
            Integer totalEmployees = employeeService.count(employeeQueryWrapper);
            if (totalEmployees == null) {
                totalEmployees = 0;
            }

            List<Map<String, Object>> statsList = new java.util.ArrayList<>();
            for (Announcement announcement : announcements) {
                Integer readCount = announcementReadStatusService.countReadByAnnouncementId(announcement.getId());
                if (readCount == null) {
                    readCount = 0;
                }
                
                int rate = 0;
                if (totalEmployees > 0) {
                    rate = (int) ((readCount * 100.0) / totalEmployees);
                }

                Map<String, Object> stat = new HashMap<>();
                stat.put("id", announcement.getId());
                stat.put("title", announcement.getTitle());
                stat.put("readCount", readCount);
                stat.put("totalCount", totalEmployees);
                stat.put("readRate", rate);
                statsList.add(stat);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("stats", statsList);
            result.put("totalEmployees", totalEmployees);
            return ResultVOUtil.success(result);
        } catch (Exception e) {
            e.printStackTrace();
            return ResultVOUtil.fail("获取统计失败");
        }
    }
}