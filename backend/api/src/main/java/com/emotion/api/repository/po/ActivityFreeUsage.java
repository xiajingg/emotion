package com.emotion.api.repository.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import java.time.LocalDate;
import com.baomidou.mybatisplus.annotation.TableId;
import java.time.LocalDateTime;
import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import jakarta.persistence.Transient;

/**
 * <p>
 * 记录每日免费抢使用次数成功的活动
 * </p>
 *
 * @author xiajing
 * @since 2024-12-12
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("activity_free_usage")
public class ActivityFreeUsage implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID，自增，唯一标识每一条记录
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID（关联到用户表），确保可以追溯到具体用户
     */
    private Long userId;

    /**
     * openId, 不保存数据库
     */
    @Transient
    private String openId;
    
    /**
     * 用户昵称，不保存数据库
     */
    @Transient
    private String nickname;

    /**
     * 活动日期，用于区分不同的抢购活动日
     */
    private LocalDate activityDate;

    /**
     * 抢购时间，记录用户点击抢购按钮的具体时间
     */
    private LocalDateTime grabTime;

    /**
     * 获得的额外使用次数，必填，表示用户通过抢购获得的额外使用次数
     */
    private Integer rewardUsageCount;

    /**
     * 创建时间，默认为当前时间，记录记录创建的时间
     */
    private LocalDateTime createdAt;


}
