package com.project.stockwork.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public CommonResult<Void> handleBiz(BizException e) {
        return CommonResult.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public CommonResult<Void> handleBody(HttpMessageNotReadableException e) {
        log.warn("请求参数解析失败", e);
        return CommonResult.fail(400, "请求参数格式不正确");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public CommonResult<Void> handleUpload(MaxUploadSizeExceededException e) {
        return CommonResult.fail(400, "上传文件超过大小限制（最大 5MB）");
    }

    @ExceptionHandler(Exception.class)
    public CommonResult<Void> handleException(Exception e) {
        log.error("系统异常", e);
        String msg = e.getMessage() == null ? "系统内部错误" : e.getMessage();
        return CommonResult.fail(500, msg);
    }
}
