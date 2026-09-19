package com.emotion.api.controller;

import cn.hutool.core.codec.Base64;
import com.emotion.api.config.BaseResult;
import com.emotion.api.config.user.CurrentUser;
import com.emotion.api.config.user.UserPrincipal;
import com.emotion.api.repository.dao.rds.ImageUploadRecordMapper;
import com.emotion.api.repository.po.ImageUploadRecord;
import com.emotion.api.util.RustFsUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

@RestController
@RequestMapping("/files")
public class FileController {

    @Autowired
    private RustFsUtil rustFsUtil;

    @Autowired
    private ImageUploadRecordMapper imageUploadRecordMapper;

    // 上传图片并保存记录
    @PostMapping("/upload")
    public BaseResult<Long> uploadFile(@RequestParam("file") MultipartFile file, @CurrentUser UserPrincipal userPrincipal) throws Exception {
        // 上传文件到 RustFS 并获取文件访问 URL
        String fileUrl = rustFsUtil.uploadFile(file);
        // 保存记录到数据库
        ImageUploadRecord record = new ImageUploadRecord(userPrincipal.getUserId(), fileUrl);
        imageUploadRecordMapper.insert(record);
        // 返回保存后的记录ID以及访问图片的URL
        return BaseResult.success(record.getId());
    }

    // 根据记录 ID 查看图片
    @GetMapping("/view")
    public ResponseEntity<byte[]> viewFile(@RequestParam("id") Long id) throws Exception {
        // 通过记录ID查找对应的图片记录
        ImageUploadRecord record = imageUploadRecordMapper.selectById(id);
        // 从 RustFS 获取文件流
        String fileUrl = record.getImageUrl();
        String fileName = fileUrl.substring(fileUrl.lastIndexOf("/") + 1);  // 提取文件名
        InputStream fileStream = rustFsUtil.getFile(fileName);

        // 将 InputStream 转为 byte[] 形式
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[1024];
        int bytesRead;
        while ((bytesRead = fileStream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, bytesRead);
        }
        byte[] fileBytes = buffer.toByteArray();

        // 返回图片内容
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_JPEG);  // 假设图片类型为 JPEG，根据需要调整
        return ResponseEntity.ok().headers(headers).body(fileBytes);

    }
}
