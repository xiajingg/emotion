package com.emotion.api.repository.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("video")
public class Video {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("video_id")
    private String videoId;

    @TableField("user_id")
    private String userId;

    // 视频的路径
    @TableField("video_path")
    private String videoPath;

    // 视频时长
    @TableField("file_duration")
    private Integer videoDuration;

    // 视频生成的状态，例如：待处理、生成中、已完成、失败
    @TableField("status")
    private String status;

    // 创建时间
    @TableField("create_time")
    private Date createTime;

    // 更新时间
    @TableField("updated_time")
    private Date updatedTime;
}

