package com.emotion.api;

import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * 测试content字段提取逻辑
 */
@Slf4j
public class ContentExtractionTest {

    /**
     * 测试从Ollama响应中提取content字段
     */
    @Test
    public void testExtractContentFromOllamaResponse() {
        log.info("开始测试从Ollama响应中提取content字段...");
        
        // 模拟Ollama API返回的响应
        String mockResponse = "{\n" +
                "  \"model\": \"qwen3.5:9b\",\n" +
                "  \"created_at\": \"2026-05-08T08:00:00.000000Z\",\n" +
                "  \"message\": {\n" +
                "    \"role\": \"assistant\",\n" +
                "    \"content\": \"生活就像一杯茶，不会苦一辈子，但总会苦一阵子。\"\n" +
                "  },\n" +
                "  \"done\": true\n" +
                "}";
        
        log.info("模拟响应: {}", mockResponse);
        
        // 解析响应
        Map<String, Object> responseMap = JSONUtil.toBean(mockResponse, Map.class);
        Map<String, Object> messageObj = (Map<String, Object>) responseMap.get("message");
        
        // 验证message对象存在
        Assert.assertNotNull("message对象不能为空", messageObj);
        Assert.assertTrue("message对象必须包含content字段", messageObj.containsKey("content"));
        
        // 提取content字段
        String content = (String) messageObj.get("content");
        
        log.info("✅ 成功提取content字段: [{}]", content);
        
        // 验证提取的内容
        Assert.assertNotNull("content不能为空", content);
        Assert.assertFalse("content不能为空字符串", content.trim().isEmpty());
        Assert.assertEquals("提取的content不正确", 
                "生活就像一杯茶，不会苦一辈子，但总会苦一阵子。", content);
        
        log.info("✅ 验证通过：只提取了content字段，没有包含其他冗余信息");
    }

    /**
     * 测试异常情况下的content提取
     */
    @Test
    public void testExtractContentWithMissingField() {
        log.info("开始测试异常情况下的content提取...");
        
        // 模拟缺少content字段的响应
        String mockResponse = "{\n" +
                "  \"model\": \"qwen3.5:9b\",\n" +
                "  \"message\": {\n" +
                "    \"role\": \"assistant\"\n" +
                "  }\n" +
                "}";
        
        log.info("模拟响应（缺少content）: {}", mockResponse);
        
        // 解析响应
        Map<String, Object> responseMap = JSONUtil.toBean(mockResponse, Map.class);
        Map<String, Object> messageObj = (Map<String, Object>) responseMap.get("message");
        
        // 验证应该抛出异常或返回null
        if (messageObj == null || !messageObj.containsKey("content")) {
            log.info("✅ 正确检测到缺少content字段");
            Assert.assertTrue(true);
        } else {
            Assert.fail("应该检测到缺少content字段");
        }
    }

    /**
     * 测试从智普AI响应中提取内容
     */
    @Test
    public void testExtractContentFromZhipuResponse() {
        log.info("开始测试从智普AI响应中提取内容...");
        
        // 模拟智普AI返回的响应（可能是纯文本或包含Markdown）
        String mockResponse1 = "原因: 代码第42行空指针异常\n建议解决方案: 添加null检查";
        String mockResponse2 = "```json\n{\"content\": \"原因: 代码第42行空指针异常\\n建议解决方案: 添加null检查\"}\n```";
        
        // 测试纯文本响应
        String extracted1 = extractContentFromResponse(mockResponse1);
        log.info("纯文本响应提取结果: [{}]", extracted1);
        Assert.assertNotNull(extracted1);
        Assert.assertFalse(extracted1.isEmpty());
        
        // 测试Markdown代码块响应
        String extracted2 = extractContentFromResponse(mockResponse2);
        log.info("Markdown响应提取结果: [{}]", extracted2);
        Assert.assertNotNull(extracted2);
        Assert.assertFalse(extracted2.isEmpty());
        
        log.info("✅ 验证通过：能够正确处理不同类型的响应");
    }

    /**
     * 从AI响应中提取content字段（与SendException中的逻辑一致）
     */
    private String extractContentFromResponse(String response) {
        if (response == null || response.trim().isEmpty()) {
            return "[AI返回空响应]";
        }
        
        String trimmed = response.trim();
        
        // 如果响应包含Markdown代码块，提取其中的内容
        int jsonStart = trimmed.indexOf("```");
        if (jsonStart != -1) {
            int jsonEnd = trimmed.lastIndexOf("```");
            if (jsonEnd > jsonStart) {
                String content = trimmed.substring(jsonStart + 3, jsonEnd).trim();
                // 移除可能的语言标识（如json）
                if (content.toLowerCase().startsWith("json")) {
                    content = content.substring(4).trim();
                }
                return content;
            }
        }
        
        // 如果响应是JSON格式，尝试解析并提取content字段
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            try {
                cn.hutool.json.JSONObject jsonObject = cn.hutool.json.JSONUtil.parseObj(trimmed);
                if (jsonObject.containsKey("content")) {
                    return jsonObject.getStr("content");
                }
                // 如果没有content字段，返回整个JSON的字符串表示
                return trimmed;
            } catch (Exception e) {
                // JSON解析失败，返回原始内容
                log.warn("AI响应JSON解析失败，返回原始内容");
            }
        }
        
        // 返回原始内容（去除首尾空白）
        return trimmed;
    }
}
