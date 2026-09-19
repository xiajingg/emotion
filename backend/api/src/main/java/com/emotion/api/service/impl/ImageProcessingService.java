//package com.emotion.api.service.impl;
//
//import com.emotion.api.repository.dao.rds.VideoMapper;
//import com.emotion.api.repository.po.Video;
//import org.springframework.amqp.core.Message;
//import org.springframework.amqp.rabbit.annotation.RabbitListener;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//
//import java.io.File;
//import java.io.FileOutputStream;
//import java.io.IOException;
//import java.util.Date;
//
//@Service
//public class ImageProcessingService {
//
//    @Autowired
//    private VideoMapper videoMapper;
//
//    // 视频存储路径
//    private static final String VIDEO_STORAGE_PATH = "C:\\Users\\dyz\\Videos\\Captures\\";
//
//    @RabbitListener(queues = "imageQueue")
//    public void processImage(byte[] imageBytes, Message message) throws IOException {
//        // 从消息属性中获取imageId
//        String imageId = message.getMessageProperties().getMessageId();
//        String videoPath = VIDEO_STORAGE_PATH + imageId + ".mp4";
//
//        // 保存视频文件到指定路径
//        File videoFile = new File(videoPath);
//        try (FileOutputStream fos = new FileOutputStream(videoFile)) {
//            fos.write(imageBytes); // 此处应该是处理后的视频文件字节流，这里只是简单示例
//        }
//
//        // 更新数据库中的视频信息
//        Video video = videoMapper.selectById(imageId);
//        if (video != null) {
//            video.setVideoDuration(30); // 假设视频时长为30秒
//            video.setUpdatedTime(new Date());
//            videoMapper.updateById(video);
//        }
//    }
//
//    private String generateVideoFromImage(byte[] imageBytes) {
//        // 实现图片生成视频的逻辑，并返回视频存储路径
//        // 此处为伪代码
//        return "C:\\Users\\dyz\\Videos\\Captures\\123.mp4";
//    }
//}
