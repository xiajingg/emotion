package com.emotion.api.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 定时任务执行记录实体类
 */
@Data
@TableName("scheduled_task_log")
public class ScheduledTaskLog {
    
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 任务名称
     */
    private String taskName;
    
    /**
     * 任务唯一标识(用于分布式锁)
     */
    private String taskKey;
    
    /**
     * 执行日期
     */
    private LocalDate executeDate;
    
    /**
     * 执行时间
     */
    private LocalDateTime executeTime;
    
    /**
     * 执行状态: 0-执行中, 1-成功, 2-失败
     */
    private Integer status;
    
    /**
     * 执行实例ID(服务器IP或容器ID)
     */
    private String instanceId;
    
    /**
     * 执行耗时(毫秒)
     */
    private Long durationMs;
    
    /**
     * 错误信息
     */
    private String errorMsg;
    
    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
