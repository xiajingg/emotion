package com.emotion.api.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @Autowired
    private SendException sendMsgToDingTalk;
    
    /**
     * 处理业务异常（不触发钉钉告警）
     * 用于可预期的业务逻辑错误，如：绑定自己、重复绑定等
     */
    @ExceptionHandler(BusinessException.class)
    @ResponseBody
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e) {
        ErrorResponse errorResponse = new ErrorResponse(e.getCode(), e.getMessage());
        // 只记录警告日志，不发送钉钉消息
        log.warn("业务异常: {}", e.getMessage());
        return new ResponseEntity<>(errorResponse, HttpStatus.OK);
    }
    
    /**
     * 处理程序中未捕获的异常
     * <p>
     * 该方法用于处理应用程序中未被捕获的异常。
     * 它作为全局异常处理器，可以确保程序在遇到意外错误时仍能以某种方式响应，而不是直接崩溃。
     *
     * @param e 未被捕获的异常实例
     * @return 返回一个错误信息字符串
     */
    @ExceptionHandler(Exception.class)
    // TODO 为什么加这个注解
    @ResponseBody
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        ErrorResponse errorResponse = new ErrorResponse("500", e.getMessage());
        // 打印详细异常日志
        log.error("进入全局异常捕获: Exception 类型: {}, 消息: {}, 发生在: ",
                e.getClass().getName(),
                e.getMessage(),
                e);
        sendMsgToDingTalk.sendMsgToDingTalk(e);
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
