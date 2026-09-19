package com.emotion.api.task;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.emotion.api.repository.dao.rds.DailyMotivationMapper;
import com.emotion.api.repository.po.DailyMotivation;
import com.emotion.api.service.OllamaDirectService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
public class DailyMotivationTask {
    @Autowired
    private DailyMotivationMapper dailyMotivationMapper;
    
    @Autowired
    private OllamaDirectService ollamaDirectService;
    // 每天00:00执行
    @Scheduled(cron = "0 0 0 * * ?")
    public void saveDailyMotivation() {
        // 检查未来7天的鸡汤是否已经生成
        for (int i = 1; i <= 7; i++) {
            LocalDate futureDate = LocalDate.now().plusDays(i);
            if (!isMotivationGenerated(futureDate)) {
                generateDailyMotivation(futureDate);
            }
        }
    }

    // 检查指定日期的鸡汤是否已经生成
    private boolean isMotivationGenerated(LocalDate date) {
        // 查询数据库，检查指定日期的鸡汤是否存在
        return dailyMotivationMapper.selectCount(new LambdaQueryWrapper<DailyMotivation>()
                .eq(DailyMotivation::getDate, date)) > 0;
    }

    // 生成指定日期的鸡汤
    private void generateDailyMotivation(LocalDate date) {
        try {
            // 查询数据里最近100条的数据
            String last100DaysMotivationContent = getLast100DaysMotivationContent();
            String userMessage = "给我说一句幽默的心灵鸡汤, 不要跟上面的历史内容重复.";
            String assistant = "你是一名心灵鸡汤专家, 你擅长写20字以内的短句心灵鸡汤.";
            
            // 构建完整的提示词
            String fullPrompt = assistant + "\n" + last100DaysMotivationContent + "\n" + userMessage;
            
            // 调用Ollama API获取激励语
            String result = callOllamaForMotivation(fullPrompt);
            
            if (result == null || result.trim().isEmpty()) {
                log.error("每日毒鸡汤生成失败：AI返回空内容，日期: " + date);
                return;
            }
            
            DailyMotivation dailyMotivation = new DailyMotivation();
            dailyMotivation.setContent(result.trim());
            dailyMotivation.setDate(date); // 设置生成的日期
            LocalDateTime nowTime = LocalDateTime.now();
            dailyMotivation.setCreatedTime(nowTime);
            dailyMotivation.setUpdatedTime(nowTime);
            int insertResult = dailyMotivationMapper.insert(dailyMotivation);
            if (insertResult > 0) {
                log.info("每日毒鸡汤更新成功！日期: " + date + ", 内容: " + result.trim());
            } else {
                log.error("每日毒鸡汤更新失败.. 日期: " + date);
            }
        } catch (Exception e) {
            log.error("生成每日毒鸡汤异常，日期: " + date, e);
        }
    }
    
    /**
     * 调用Ollama API获取激励语
     * 参考submitText接口的实现方式，只提取content字段
     */
    private String callOllamaForMotivation(String prompt) {
        try {
            // 直接调用Ollama API
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "qwen3.5:9b");
            requestBody.put("stream", false);
            requestBody.put("think", false);
            
            Map<String, String> message = new HashMap<>();
            message.put("role", "user");
            message.put("content", prompt);
            requestBody.put("messages", new Object[]{message});
            
            String requestBodyJson = JSONUtil.toJsonStr(requestBody);
            log.info("调用Ollama API生成激励语，请求体: {}", requestBodyJson);
            
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
                log.info("提取的激励语内容: [{}]", content);
                
                return content;
            }
        } catch (Exception e) {
            log.error("调用Ollama API生成激励语失败", e);
            throw new RuntimeException("生成激励语失败: " + e.getMessage(), e);
        }
    }

    /**
     * 查询前 100 天的毒鸡汤内容
     *
     * @return 前 100 天的毒鸡汤内容列表
     */
    private String getLast100DaysMotivationContent() {
        // 获取当前日期和前 100 天的日期
        LocalDate today = LocalDate.now();
        LocalDate hundredDaysAgo = today.minusDays(100);

        // 使用 LambdaQueryWrapper 查询
        LambdaQueryWrapper<DailyMotivation> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.between(DailyMotivation::getDate, hundredDaysAgo, today)
                .orderByDesc(DailyMotivation::getDate); // 按日期降序排序

        // 查询数据库，返回结果
        List<DailyMotivation> motivations = dailyMotivationMapper.selectList(queryWrapper);

        // 提取 content 字段
        return motivations.stream()
                .map(c -> c.getContent() + "\n----------------------")
                .collect(Collectors.joining("\n"));

    }
}
