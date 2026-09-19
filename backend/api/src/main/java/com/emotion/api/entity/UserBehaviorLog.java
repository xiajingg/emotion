package com.emotion.api.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户行为分析实体类
 */
@Data
@TableName("user_behavior_log")
public class UserBehaviorLog {
    
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 用户ID
     */
    private String userId;
    
    /**
     * 行为类型: 1-查看每日补给, 2-查看星座运势, 3-点击探索菜单
     */
    private Integer behaviorType;
    
    /**
     * 关联的提醒类型: 1-每日补给, 2-星座运势
     */
    private Integer notificationType;
    
    /**
     * 当时是否有未读消息: 0-无, 1-有
     */
    private Integer hasUnread;
    
    /**
     * 行为发生时间
     */
    private LocalDateTime actionTime;
    
    /**
     * 设备信息(可选)
     */
    private String deviceInfo;
    
    /**
     * IP地址(可选)
     */
    private String ipAddress;
}
