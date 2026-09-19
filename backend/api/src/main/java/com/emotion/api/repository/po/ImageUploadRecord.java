package com.emotion.api.repository.po;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName(value ="image_upload_record")
public class ImageUploadRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private String imageUrl;
    private LocalDateTime uploadTime;

    public ImageUploadRecord(Long userId, String fileUrl) {
        this.userId = userId;
        this.imageUrl = fileUrl;
    }
}

