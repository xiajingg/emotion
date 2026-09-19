package com.emotion.api.util;

import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.UUID;

/**
 * RustFS 文件存储工具类
 * 用于与 RustFS 对象存储服务进行交互（兼容 S3/MinIO API）
 */
@Component
public class RustFsUtil {

    @Value("${rustfs.endpoint}")
    private String endpoint;

    @Value("${rustfs.accessKey}")
    private String accessKey;

    @Value("${rustfs.secretKey}")
    private String secretKey;

    @Value("${rustfs.bucketName}")
    private String bucketName;

    private MinioClient getRustFsClient() {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }

    /**
     * 上传文件到 RustFS
     * @param file 要上传的文件
     * @return 文件的访问 URL
     * @throws Exception 上传异常
     */
    public String uploadFile(MultipartFile file) throws Exception {
        String fileName = UUID.randomUUID().toString() + "-" + file.getOriginalFilename();
        MinioClient rustFsClient = getRustFsClient();

        rustFsClient.putObject(
                PutObjectArgs.builder()
                        .bucket(bucketName)
                        .object(fileName)
                        .stream(file.getInputStream(), file.getSize(), -1)
                        .contentType(file.getContentType())
                        .build()
        );
        return endpoint + "/" + bucketName + "/" + fileName;
    }

    /**
     * 从 RustFS 获取文件流
     * @param fileName 文件名
     * @return 文件输入流
     * @throws Exception 下载异常
     */
    public InputStream getFile(String fileName) throws Exception {
        MinioClient rustFsClient = getRustFsClient();

        return rustFsClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucketName)
                        .object(fileName)
                        .build()
        );
    }

    public byte[] getFileBytesByUrl(String fileUrl) throws Exception {
        String fileName = fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
        try (InputStream fileStream = getFile(fileName);
             ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
            byte[] data = new byte[4096];
            int bytesRead;
            while ((bytesRead = fileStream.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, bytesRead);
            }
            return buffer.toByteArray();
        }
    }
}
