package com.emotion.api.repository.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.Date;

/**
 * <p>
 * 视频文件存储表
 * </p>
 *
 * @author xiajing
 * @since 2024-04-11
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("video_files")
public class VideoFiles implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 自增ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 上传人ID
     */
    @TableField("user_id")
    private Integer userId;

    /**
     * 视频名称
     */
    @TableField("video_name")
    private String videoName;

    /**
     * 视频作者姓名
     */
    @TableField("author_name")
    private String authorName;

    /**
     * 视频URL地址
     */
    @TableField("video_url")
    private String videoUrl;

    /**
     * 数据创建时间
     */
    @TableField("created_at")
    private Date createdAt;

    /**
     * 数据最后更新时间
     */
    @TableField("updated_at")
    private Date updatedAt;


}
