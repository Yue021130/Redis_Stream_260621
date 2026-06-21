package com.example.redisstream.exception;

import com.example.redisstream.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.validation.ConstraintViolationException;

/**
 * 全局异常处理。
 *
 * 把所有异常统一封装为 ApiResponse，避免直接抛出堆栈给调用方，
 * 同时记录错误日志方便排查。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ApiResponse<Void> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("参数错误：{}", e.getMessage());
        return ApiResponse.error(e.getMessage());
    }

    @ExceptionHandler({BindException.class, ConstraintViolationException.class})
    public ApiResponse<Void> handleValidation(Exception e) {
        log.warn("参数校验失败：{}", e.getMessage());
        return ApiResponse.error("请求参数不合法：" + e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return ApiResponse.error("系统内部错误：" + e.getMessage());
    }
}
