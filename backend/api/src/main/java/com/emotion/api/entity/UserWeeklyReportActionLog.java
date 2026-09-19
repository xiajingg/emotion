package com.emotion.api.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户周报订阅行为日志（用户画像用）
 * 每条订阅授权、每次推送发送都独立记录，永不更新
 */
@Data
@TableName("user_weekly_report_action_log")
public class UserWeeklyReportActionLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 微信OpenID */
    private String openId;

    /** 订阅消息模板ID */
    private String templateId;

    /** 行为类型: SUBSCRIBE-授权订阅, PUSH-推送发送 */
    private String actionType;

    /** 行为发生时间 */
    private LocalDateTime actionTime;

    /** 推送是否成功(仅PUSH类型): 1-成功, 0-失败 */
    private Integer pushSuccess;

    /** 推送失败错误码 */
    private String pushErrCode;

    /** 推送失败错误信息 */
    private String pushErrMsg;

    /** DB 层 DEFAULT CURRENT_TIMESTAMP，MyBatis-Plus 不介入 */
    @TableField(insertStrategy = FieldStrategy.NEVER)
    private LocalDateTime createTime;
}
