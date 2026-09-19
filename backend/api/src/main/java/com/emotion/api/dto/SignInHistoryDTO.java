package com.emotion.api.dto;

import lombok.Data;

/**
 * 查询签到日期时，返回的DTO
 */
@Data
public class SignInHistoryDTO {
    private String data;

    // 0表示未签到，1表示签到
    private int signIn;
}
