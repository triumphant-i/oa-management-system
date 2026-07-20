package com.southwind.config;

import com.southwind.util.ResultVOUtil;
import com.southwind.vo.ResultVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 * 统一处理系统异常，返回友好的错误信息
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理所有未捕获的异常
     */
    @ExceptionHandler(Exception.class)
    public ResultVO handleException(Exception e) {
        log.error("系统异常: ", e);
        return ResultVOUtil.fail("系统异常: " + e.getMessage());
    }

    /**
     * 处理非法参数异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResultVO handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("参数错误: {}", e.getMessage());
        return ResultVOUtil.fail(e.getMessage());
    }

    /**
     * 处理空指针异常
     */
    @ExceptionHandler(NullPointerException.class)
    public ResultVO handleNullPointerException(NullPointerException e) {
        log.error("空指针异常: ", e);
        return ResultVOUtil.fail("系统错误: 数据为空");
    }

    /**
     * 处理运行时异常
     */
    @ExceptionHandler(RuntimeException.class)
    public ResultVO handleRuntimeException(RuntimeException e) {
        log.error("运行时异常: ", e);
        return ResultVOUtil.fail("操作失败: " + e.getMessage());
    }

    /**
     * 处理会议室时间冲突异常
     */
    @ExceptionHandler(com.southwind.exception.MeetingConflictException.class)
    public ResultVO handleMeetingConflictException(com.southwind.exception.MeetingConflictException e) {
        log.warn("会议室预约冲突: {}", e.getMessage());
        return ResultVOUtil.fail(e.getMessage());
    }
}