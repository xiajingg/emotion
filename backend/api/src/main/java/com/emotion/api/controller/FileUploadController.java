//package com.emotion.api.controller;
//
////import com.aliyun.oss.OSS;
////import com.aliyun.oss.OSSClientBuilder;
////import com.aliyun.oss.model.PutObjectRequest;
////import com.aliyun.oss.model.PutObjectResult;
////import org.springframework.beans.factory.annotation.Value;
//import org.springframework.web.bind.annotation.*;
//import org.springframework.web.multipart.MultipartFile;
//
//import java.io.InputStream;
//import java.util.Date;
//
//@RestController
//@RequestMapping("/api/files")
//public class FileUploadController {
//
////    @Value("${aliyun.oss.endpoint}")
//    private String ossEndpoint;
//
////    @Value("${aliyun.oss.accessKeyId}")
//    private String ossAccessKeyId;
//
////    @Value("${aliyun.oss.accessKeySecret}")
//    private String ossAccessKeySecret;
//
////    @Value("${aliyun.oss.bucketName}")
//    private String ossBucketName;
//
//
////    @PostMapping("/upload")
////    public String uploadFile(@RequestParam("file") MultipartFile file) {
////        OSS ossClient = new OSSClientBuilder().build(ossEndpoint, ossAccessKeyId, ossAccessKeySecret);
////        try {
////            // 使用UUID生成唯一的文件名，避免文件名冲突
////            String fileName = java.util.UUID.randomUUID() + "." + getFileExtension(file.getOriginalFilename());
////            InputStream inputStream = file.getInputStream();
////            PutObjectRequest putObjectRequest = new PutObjectRequest(ossBucketName, fileName, inputStream);
////            PutObjectResult putObjectResult = ossClient.putObject(putObjectRequest);
////            ossClient.generatePresignedUrl(ossBucketName, fileName, new Date(System.currentTimeMillis() + 3600 * 1000L)).toString();
////            return fileName;
////        } catch (Exception e) {
////            e.printStackTrace();
////            return "Upload failed: " + e.getMessage();
////        } finally {
////            ossClient.shutdown();
////        }
////    }
//
//    @GetMapping("/get-url")
//    public String getFileUrl(@RequestParam String videoUrl) {
//
//        String objectName = videoUrl;
//        OSS ossClient = new OSSClientBuilder().build(ossEndpoint, ossAccessKeyId, ossAccessKeySecret);
//        try {
//            // 生成一个签名URL，有效期设置为，例如，1小时
//            Date expiration = new Date(System.currentTimeMillis() + 3600 * 1000L);
//            String signedUrl = ossClient.generatePresignedUrl(ossBucketName, objectName, expiration).toString();
//            return signedUrl;
//        } catch (Exception e) {
//            e.printStackTrace();
//            return "";
//        } finally {
//            ossClient.shutdown();
//        }
//    }
//
//    private String getFileExtension(String fileName) {
//        if (fileName.lastIndexOf(".") != -1 && fileName.lastIndexOf(".") != 0) {
//            return fileName.substring(fileName.lastIndexOf(".") + 1);
//        } else {
//            return "";
//        }
//    }
//
//}
