package com.emotion.api.dto;

import lombok.Data;

import java.util.List;

@Data
public class SubmitTextDTO {

    private String text;

    private List<Long> uploadFileId;

    private String longitude;

    private String latitude;

    // 默认值为0，表示普通提交，1表示分享
    private Integer type = 0;

    // 补签到，1表示是补签到，0表示不是补签到
    private Integer supplementarySignIn = 0;

    // 补签到时间
    private String supplementarySignInTime;

    private String uniqueCode;

    // 场景标签（可选）：压力焦虑/职场委屈/恋爱焦虑/不知道怎么回/睡前emo/情绪涂鸦
    private String scene;

    // 任务类型（可选）：REPLY_RESCUE/SLEEP_RUMINATION/PRESSURE_RESCUE/GENERAL
    private String taskType;

    // 细分场景枚举（可选）：boss_criticism/colleague_blame/partner_cold/friend_boundary/family_pressure/sleep_loop/pressure_breakdown
    private String scenarioKey;
}
