package com.emotion.api.exception;

/**
 * 业务异常类
 * 用于处理可预期的业务逻辑错误，不会触发钉钉告警
 */
public class BusinessException extends RuntimeException {
    
    private String code;
    
    public BusinessException(String message) {
        super(message);
        this.code = "400";
    }
    
    public BusinessException(String code, String message) {
        super(message);
        this.code = code;
    }
    
    public String getCode() {
        return code;
    }
}
