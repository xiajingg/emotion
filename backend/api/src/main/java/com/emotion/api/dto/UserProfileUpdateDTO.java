package com.emotion.api.dto;

import lombok.Data;

/**
 * 用户资料更新DTO
 */
@Data
public class UserProfileUpdateDTO {
    private String nickname;   // 昵称（可选）
    private String constellation;  // 星座（可选）
}
