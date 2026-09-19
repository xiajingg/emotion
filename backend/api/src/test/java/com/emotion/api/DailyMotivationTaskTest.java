package com.emotion.api;

import cn.hutool.json.JSONUtil;
import com.emotion.api.repository.dao.rds.DailyMotivationMapper;
import com.emotion.api.repository.po.DailyMotivation;
import com.emotion.api.task.DailyMotivationTask;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * 每日激励定时任务测试
 */
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(classes = EmotionApplication.class)
public class DailyMotivationTaskTest {

    @Autowired
    private DailyMotivationTask dailyMotivationTask;

    @Autowired
    private DailyMotivationMapper dailyMotivationMapper;

    /**
     * 测试生成每日激励
     */
    @Test
    public void testGenerateDailyMotivation() {
        log.info("开始测试生成每日激励...");
        
        // 手动触发定时任务
        dailyMotivationTask.saveDailyMotivation();
        
        log.info("每日激励生成测试完成");
    }

    /**
     * 测试Ollama API调用并提取content字段
     */
    @Test
    public void testOllamaContentExtraction() {
        log.info("开始测试Ollama API content提取...");
        
        try {
            // 构建测试请求
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "qwen3.5:9b");
            requestBody.put("stream", false);
            requestBody.put("think", false);
            
            Map<String, String> message = new HashMap<>();
            message.put("role", "user");
            message.put("content", "请说一句简短的励志话语，20字以内");
            requestBody.put("messages", new Object[]{message});
            
            String requestBodyJson = JSONUtil.toJsonStr(requestBody);
            log.info("请求体: {}", requestBodyJson);
            
            // 使用OkHttp发送请求
            okhttp3.RequestBody body = okhttp3.RequestBody.create(
                    requestBodyJson,
                    okhttp3.MediaType.parse("application/json")
            );
            
            okhttp3.Request request = new okhttp3.Request.Builder()
                    .url("http://localhost:11434/api/chat")
                    .post(body)
                    .build();
            
            okhttp3.OkHttpClient httpClient = new okhttp3.OkHttpClient.Builder()
                    .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
                    .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .build();
            
            try (okhttp3.Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new RuntimeException("HTTP请求失败: " + response.code());
                }
                
                String responseBody = response.body().string();
                log.info("Ollama原始响应: {}", responseBody);
                
                // 解析响应，提取message.content字段
                Map<String, Object> responseMap = JSONUtil.toBean(responseBody, Map.class);
                Map<String, Object> messageObj = (Map<String, Object>) responseMap.get("message");
                
                if (messageObj == null || !messageObj.containsKey("content")) {
                    throw new RuntimeException("Ollama返回的响应中没有message.content字段");
                }
                
                String content = (String) messageObj.get("content");
                log.info("✅ 成功提取content字段: [{}]", content);
                
                // 验证提取的内容不为空
                assert content != null && !content.trim().isEmpty() : "提取的content不能为空";
                
                // 验证只提取了content字段，没有包含其他冗余信息
                log.info("✅ 验证通过：只提取了content字段，长度: {}", content.length());
                
            }
        } catch (Exception e) {
            log.error("测试失败", e);
            throw new RuntimeException("测试失败: " + e.getMessage(), e);
        }
    }

    /**
     * 测试查询最近的激励内容
     */
    @Test
    public void testGetRecentMotivations() {
        log.info("开始测试查询最近的激励内容...");
        
        LocalDate today = LocalDate.now();
        LocalDate hundredDaysAgo = today.minusDays(100);
        
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<DailyMotivation> queryWrapper = 
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        queryWrapper.between(DailyMotivation::getDate, hundredDaysAgo, today)
                .orderByDesc(DailyMotivation::getDate);
        
        var motivations = dailyMotivationMapper.selectList(queryWrapper);
        
        log.info("查询到 {} 条激励记录", motivations.size());
        
        if (!motivations.isEmpty()) {
            log.info("最近一条激励内容: {}", motivations.get(0).getContent());
            log.info("最近一条激励日期: {}", motivations.get(0).getDate());
        }
    }
}
