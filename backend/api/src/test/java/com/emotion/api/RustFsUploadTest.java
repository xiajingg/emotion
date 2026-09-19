package com.emotion.api;

import com.emotion.api.util.RustFsUtil;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.InputStream;

/**
 * RustFS 文件上传下载功能测试
 * 用于验证从 MinIO 迁移到 RustFS 后的文件存储功能是否正常
 * 
 * 注意：此测试会在 emotion 桶中保留测试文件，方便在管理平台查看
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = EmotionApplication.class)
public class RustFsUploadTest {

    @Autowired
    private RustFsUtil rustFsUtil;

    // 固定文件名，方便在管理平台查找
    private static final String TEST_IMAGE_NAME = "rustfs-test-sample.jpg";
    private static final String TEST_DOC_NAME = "rustfs-test-sample.txt";

    /**
     * 测试1: 上传示例图片文件（保留在 RustFS 中，可在管理平台查看）
     */
    @Test
    public void testUploadSampleImage() throws Exception {
        System.out.println("=== 测试1: 上传示例图片 ===");

        // 创建一个模拟图片文件（使用固定文件名）
        byte[] imageContent = generateSampleImageData();
        MockMultipartFile mockFile = new MockMultipartFile(
                "file",
                TEST_IMAGE_NAME,
                "image/jpeg",
                imageContent
        );

        // 上传文件到 RustFS
        String fileUrl = rustFsUtil.uploadFile(mockFile);

        // 验证返回的 URL 不为空
        Assert.assertNotNull("上传返回的 URL 不应为空", fileUrl);
        Assert.assertFalse("上传返回的 URL 不应为空字符串", fileUrl.isEmpty());

        System.out.println("✅ 示例图片上传成功!");
        System.out.println("   文件 URL: " + fileUrl);
        System.out.println("   文件大小: " + imageContent.length + " bytes");

        // 验证 URL 格式是否正确
        Assert.assertTrue("URL 应包含 localhost:9000", fileUrl.contains("localhost:9000"));
        Assert.assertTrue("URL 应包含 bucketName 'emotion'", fileUrl.contains("emotion"));
        Assert.assertTrue("URL 应包含文件名", fileUrl.contains(TEST_IMAGE_NAME));

        System.out.println("✅ URL 格式验证通过");
        System.out.println("   📌 提示: 你可以在 RustFS 管理平台查看此文件");
        System.out.println("=== 测试1 完成 ===\n");
    }

    /**
     * 测试2: 上传示例文档并下载验证（保留在 RustFS 中）
     */
    @Test
    public void testUploadAndDownloadSampleDoc() throws Exception {
        System.out.println("=== 测试2: 上传示例文档并下载验证 ===");

        // 1. 准备测试内容
        String originalContent = buildSampleDocumentContent();
        
        MockMultipartFile mockFile = new MockMultipartFile(
                "file",
                TEST_DOC_NAME,
                "text/plain",
                originalContent.getBytes("UTF-8")
        );

        // 2. 上传文件
        String fileUrl = rustFsUtil.uploadFile(mockFile);
        System.out.println("✅ 示例文档上传成功");
        System.out.println("   文件 URL: " + fileUrl);

        // 3. 从 URL 中提取文件名
        String fileName = fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
        System.out.println("   提取的文件名: " + fileName);

        // 4. 下载文件
        InputStream inputStream = rustFsUtil.getFile(fileName);
        Assert.assertNotNull("下载的文件流不应为空", inputStream);
        System.out.println("✅ 文件下载成功");

        // 5. 读取文件内容并验证
        java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
        byte[] data = new byte[1024];
        int bytesRead;
        while ((bytesRead = inputStream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, bytesRead);
        }
        byte[] downloadedBytes = buffer.toByteArray();
        String downloadedContent = new String(downloadedBytes, "UTF-8");

        // 6. 验证内容一致性
        Assert.assertEquals("下载的内容应与上传的内容一致", originalContent, downloadedContent);
        System.out.println("✅ 文件内容验证通过");
        System.out.println("   原始大小: " + originalContent.length() + " 字符");
        System.out.println("   下载大小: " + downloadedContent.length() + " 字符");

        // 7. 关闭输入流
        inputStream.close();

        System.out.println("   📌 提示: 此文件也保留在 RustFS 中，可在管理平台查看");
        System.out.println("=== 测试2 完成 ===\n");
    }

    /**
     * 测试3: 验证已上传的文件可以再次下载
     */
    @Test
    public void testReDownloadUploadedFile() throws Exception {
        System.out.println("=== 测试3: 验证已上传文件的重新下载 ===");

        // 先上传一个文件
        String testContent = "This file will be uploaded and then re-downloaded to verify persistence.";
        String persistentFileName = "rustfs-persistence-test.txt";
        
        MockMultipartFile mockFile = new MockMultipartFile(
                "file",
                persistentFileName,
                "text/plain",
                testContent.getBytes("UTF-8")
        );

        // 上传
        String fileUrl = rustFsUtil.uploadFile(mockFile);
        System.out.println("✅ 文件已上传: " + fileUrl);

        // 等待一小段时间确保存储完成
        Thread.sleep(100);

        // 从 URL 提取文件名并重新下载
        String fileName = fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
        InputStream inputStream = rustFsUtil.getFile(fileName);
        
        java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
        byte[] data = new byte[1024];
        int bytesRead;
        while ((bytesRead = inputStream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, bytesRead);
        }
        String downloadedContent = new String(buffer.toByteArray(), "UTF-8");
        inputStream.close();

        // 验证
        Assert.assertEquals("重新下载的内容应一致", testContent, downloadedContent);
        System.out.println("✅ 文件重新下载验证通过");
        System.out.println("   文件在 RustFS 中持久化存储正常");
        System.out.println("=== 测试3 完成 ===\n");
    }

    // ==================== 辅助方法 ====================

    /**
     * 生成示例图片数据（模拟 JPEG 文件头 + 一些数据）
     */
    private byte[] generateSampleImageData() {
        // JPEG 文件头
        byte[] jpegHeader = new byte[]{
            (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0,  // SOI + APP0 marker
            0x00, 0x10, 0x4A, 0x46, 0x49, 0x46, 0x00, 0x01,      // JFIF identifier
            0x01, 0x01, 0x00, 0x48, 0x00, 0x48, 0x00, 0x00       // Version + density
        };
        
        // 添加一些模拟数据
        StringBuilder sb = new StringBuilder();
        sb.append("RustFS Test Image Data\n");
        sb.append("========================\n");
        sb.append("This is a sample image file for testing RustFS storage.\n");
        sb.append("Upload time: ").append(java.time.LocalDateTime.now()).append("\n");
        sb.append("Purpose: Verify file upload and download functionality.\n");
        sb.append("You can see this file in the RustFS management console.\n");
        
        byte[] content = sb.toString().getBytes();
        
        // 合并头部和内容
        byte[] result = new byte[jpegHeader.length + content.length];
        System.arraycopy(jpegHeader, 0, result, 0, jpegHeader.length);
        System.arraycopy(content, 0, result, jpegHeader.length, content.length);
        
        return result;
    }

    /**
     * 构建示例文档内容
     */
    private String buildSampleDocumentContent() {
        StringBuilder sb = new StringBuilder();
        sb.append("========================================\n");
        sb.append("  RustFS 文件存储测试文档\n");
        sb.append("========================================\n\n");
        sb.append("测试时间: ").append(java.time.LocalDateTime.now()).append("\n");
        sb.append("测试目的: 验证 RustFS 对象存储的上传和下载功能\n\n");
        sb.append("测试项目:\n");
        sb.append("  1. 文件上传到 emotion 存储桶\n");
        sb.append("  2. 从 RustFS 下载文件\n");
        sb.append("  3. 验证文件内容完整性\n");
        sb.append("  4. 确认文件在管理平台可见\n\n");
        sb.append("配置信息:\n");
        sb.append("  - Endpoint: http://localhost:9000\n");
        sb.append("  - Bucket: emotion\n");
        sb.append("  - Access Key: (从配置读取，不落盘)\n\n");
        sb.append("说明:\n");
        sb.append("  此文件是单元测试自动生成的测试样本，\n");
        sb.append("  用于验证 emotion-backend 项目从 MinIO \n");
        sb.append("  迁移到 RustFS 后的文件存储功能。\n\n");
        sb.append("========================================\n");
        sb.append("  测试完成 - 文件已成功存储\n");
        sb.append("========================================\n");
        return sb.toString();
    }
}
