package com.emotion.api.repository.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.time.LocalDateTime;
import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 业务次数记录表
 * </p>
 *
 * @author xiajing
 * @since 2024-11-27
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("user_function_record")
public class UserFunctionRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long userId;

    /**
     * 每日使用可使用总次数
     */
    private Long dailyLimitTimes;

    /**
     * 今日已经使用的次数
     */
    private Long usedTimesToday;

    /**
     * 用户可以使用的功能总次数
     */
    private Long totalUsageLimit;

    /**
     * 用户已经使用的功能次数
     */
    private Long usedUsageCount;

    /**
     * 总可用补签次数
     */
    private Integer totalRemakeLimit;

    /**
     * 转换图片每日次数
     */
    @TableField("txt_to_img_daily_total")
    private Long txt2ImgDailyTotal;

    /**
     * 今日已使用转换图片次数
     */
    @TableField("txt_to_img_daily_used")
    private Long txt2ImgDailyUsed;

    /**
     * 转换图片总次数
     */
    @TableField("txt_to_img_total")
    private Long txt2ImgTotal;

    /**
     * 转换图片已使用次数
     */
    @TableField("txt_to_img_used")
    private Long txt2ImgUsed;

    /**
     * 已使用补签次数
     */
    private Integer usedRemakeCount;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;


}
