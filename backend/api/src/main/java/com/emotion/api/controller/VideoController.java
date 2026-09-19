//package com.emotion.api.controller;
//
//import cn.hutool.core.io.resource.InputStreamResource;
//import com.emotion.api.config.user.CurrentUser;
//import com.emotion.api.config.user.UserPrincipal;
//import com.emotion.api.repository.dao.rds.VideoMapper;
//import com.emotion.api.repository.po.Video;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.MediaType;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//import org.springframework.web.multipart.MultipartFile;
//
//import java.io.IOException;
//import java.util.Date;
//import java.util.List;
//import java.util.UUID;
//import java.io.File;
//import java.io.FileInputStream;
//import java.io.InputStream;
//
//
//@RestController
//@RequestMapping("/api/videos")
//public class VideoController {
//
//    @Autowired
//    private VideoMapper videoMapper;
//
//    // 视频存储路径
//    private static final String VIDEO_STORAGE_PATH = "C:\\Users\\dyz\\Videos\\Captures\\";
//
//    @PostMapping("/upload")
//    public ResponseEntity<String> uploadImage(@CurrentUser UserPrincipal userPrincipal, @RequestParam("image") MultipartFile image) throws IOException {
//        if (image.isEmpty()) {
//            return ResponseEntity.badRequest().body("Image cannot be empty");
//        }
//
//        if (userPrincipal == null) {
//            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated");
//        }
//
//        // 将图片发送到RabbitMQ
//        String imageId = java.util.UUID.randomUUID().toString();
//        rabbitTemplate.convertAndSend("imageQueue", image.getBytes(), message -> {
//            message.getMessageProperties().setMessageId(imageId);
//            return message;
//        });
//
//        // 创建Video实体并保存到数据库
//        Video video = new Video();
//        video.setVideoId(UUID.randomUUID().toString());
//        video.setUserId(userPrincipal.getUserOpenId()); // 假设用户ID为1，实际应用中应从请求中获取
//        video.setVideoPath(VIDEO_STORAGE_PATH + imageId + ".mp4"); // 设置视频存储路径
//        video.setVideoDuration(0); // 初始时长为0，可根据实际视频处理结果更新
//        video.setCreateTime(new Date());
//        video.setUpdatedTime(new Date());
//        videoMapper.insert(video);
//
//        return ResponseEntity.ok("Image uploaded successfully");
//    }
//
//    @GetMapping("/users/{userId}")
//    public List<Video> getVideosByUserId(@CurrentUser UserPrincipal userPrincipal) {
//        return videoMapper.selectVideosByUserId(userPrincipal.getUserOpenId());
//    }
//
//    @GetMapping("/download/{videoId}")
//    public ResponseEntity<InputStreamResource> downloadVideo(@PathVariable String videoId) throws IOException {
//        Video video = videoMapper.selectById(videoId);
//        if (video == null) {
//            return ResponseEntity.notFound().build();
//        }
//
//        String videoPath = video.getVideoPath();
//        if (videoPath == null || videoPath.isEmpty()) {
//            return ResponseEntity.badRequest().build();
//        }
//
//        // 创建一个文件对象
//        File file = new File(videoPath);
//        if (!file.exists() || !file.canRead()) {
//            return ResponseEntity.notFound().build();
//        }
//
//        // 创建一个InputStream
//        try (InputStream inputStream = new FileInputStream(file)) {
//            // 创建一个InputStreamResource
//            InputStreamResource resource = new InputStreamResource(inputStream);
//            // 创建一个ResponseEntity，包含文件类型和InputStreamResource
//            return ResponseEntity.ok()
//                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
//                    .contentLength(file.length())
//                    .body(resource);
//        }
//    }
//}
