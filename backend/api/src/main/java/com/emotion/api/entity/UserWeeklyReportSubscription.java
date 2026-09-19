package com.emotion.api.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户周报推送授权记录
 * 微信订阅消息为一次性：用户每次弹窗授权后记录在此，
 * 推送成功或微信明确拒收后 status 置为 0（已消耗），下次推送前需用户重新授权
 */
@Data
@TableName("user_weekly_report_subscription")
public class UserWeeklyReportSubscription {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String openId;

    private String templateId;

    /** 授权时间（每次弹窗"允许"的时间） */
    private LocalDateTime subscribeTime;

    /** 推送成功或微信明确拒收的时间（授权消耗时间） */
    private LocalDateTime lastSendTime;

    /** 状态：1-已授权待推送，0-已推送/已消耗 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
