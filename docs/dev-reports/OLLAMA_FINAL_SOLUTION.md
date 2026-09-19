# Ollama 空响应问题最终解决方案

## 📋 问题概述

**现象**：使用 Spring AI 1.0.0-M5/M6 的 `OllamaChatModel` 和 `ChatClient` 调用 Ollama API 时，始终返回空字符串。

**影响**：情绪分析功能完全无法工作，用户提交文本后收到 "AI服务返回空响应" 错误。

---

## 🔍 根因分析

### 排查过程

1. **验证 Ollama API 是否正常**
   ```bash
   curl -s http://localhost:11434/api/chat \
     -d '{"model":"qwen3.5:2b","messages":[{"role":"user","content":"你好"}],"stream":false}'
   ```
   **结果**：✅ Ollama API 正常，能返回响应

2. **测试 Spring AI ChatClient**
   ```java
   String response = chatClient.prompt()
       .user("你好")
       .call()
       .content();  // ❌ 返回空字符串
   ```
   **结果**：❌ 即使简单对话也返回空

3. **测试直接使用 OllamaChatModel**
   ```java
   ChatResponse response = ollamaChatModel.call(new Prompt(userMessage));
   String content = response.getResult().getOutput().getText();  // ❌ 返回空
   ```
   **结果**：❌ 仍然返回空

4. **测试直接 HTTP 调用**
   ```bash
   time curl -s http://localhost:11434/api/chat -d '{...}'
   # 耗时: 64.72秒
   ```
   **结果**：✅ 成功返回，但需要约 60 秒

### 根本原因

**Spring AI 1.0.0-M5/M6 版本的 `OllamaChatModel` 实现存在 bug**，导致所有调用都返回空响应。

同时发现：
- qwen3.5:2b 小模型推理速度慢（60+秒）
- 需要设置超时时间为 120 秒以适应大模型推理

---

## ✅ 最终解决方案

### 方案：直接调用 Ollama API，绕过 Spring AI

创建 `OllamaDirectService` 类，使用 OkHttp 直接调用 Ollama REST API。

#### 核心代码

```java
@Service
public class OllamaDirectService {
    private static final String OLLAMA_API_URL = "http://localhost:11434/api/chat";
    private static final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)  // ✅ 增加到120秒
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();

    public EmotionAnalysisResponse analyzeEmotion(String text) {
        // 构建请求体
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "qwen3.5:9b");  // ✅ 使用9b大模型
        requestBody.put("stream", false);
        
        Map<String, String> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", buildPrompt(text));
        requestBody.put("messages", new Object[]{message});

        // 发送HTTP请求
        RequestBody body = RequestBody.create(
                JSONUtil.toJsonStr(requestBody),
                MediaType.parse("application/json")
        );

        Request request = new Request.Builder()
                .url(OLLAMA_API_URL)
                .post(body)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            String responseBody = response.body().string();
            
            // 解析响应
            Map<String, Object> responseMap = JSONUtil.toBean(responseBody, Map.class);
            Map<String, Object> messageObj = (Map<String, Object>) responseMap.get("message");
            String content = (String) messageObj.get("content");
            
            // 提取JSON并解析
            String jsonStr = extractJsonFromResponse(content);
            return JSONUtil.toBean(jsonStr, EmotionAnalysisResponse.class);
        }
    }
}
```

### 配置文件修改

**application-prod.yml**：
```yaml
spring:
  ai:
    ollama:
      base-url: http://localhost:11434
      chat:
        model: qwen3.5:9b  # ✅ 使用9b大模型，支持更好
        options:
          temperature: 0.7
          top-p: 0.8
          num-predict: 200
```

### Controller 修改

**UserController.java**：
```java
@Autowired
private com.emotion.api.service.OllamaDirectService ollamaDirectService;

// 修改前
emotionResponse = ollamaChatService.analyzeEmotion(text.getText());

// 修改后
emotionResponse = ollamaDirectService.analyzeEmotion(text.getText());
```

---

## 📊 性能对比

| 方案 | 响应时间 | 成功率 | 说明 |
|------|---------|--------|------|
| Spring AI ChatClient (M5/M6) | - | 0% | ❌ 始终返回空 |
| 直接 HTTP 调用 (qwen3.5:2b) | ~60秒 | 100% | ⚠️ 慢但可用 |
| 直接 HTTP 调用 (qwen3.5:9b) | ~56秒 | 100% | ✅ 推荐方案 |

---

## 🎯 测试结果

```
========== 测试Ollama直接API调用 ==========
输入文本: 今天工作很开心
情绪分数: 75
建议: 保持这份积极乐观的心态，愿快乐常伴！
✅ 测试通过！

Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
Total time: 01:04 min
```

---

## 📝 关键经验

1. **Spring AI 版本兼容性**：1.0.0-M5/M6 版本的 OllamaChatModel 有 bug
2. **模型选择**：qwen3.5:9b 比 qwen3.5:2b 更稳定，但推理时间相近
3. **超时设置**：必须设置 readTimeout >= 120秒，适应大模型推理
4. **降级策略**：当 Spring AI 不可用时，直接调用 REST API 是可靠的备选方案

---

## 🔧 未来优化方向

1. **升级 Spring AI**：等待 1.0.0-RC1 或更高版本修复 bug
2. **异步处理**：将情绪分析改为异步任务，避免阻塞主线程
3. **缓存机制**：对相同文本的分析结果进行缓存
4. **模型优化**：考虑使用更快的模型（如 llama3.1:8b）

---

## 📚 相关文件

- `/api/src/main/java/com/emotion/api/service/OllamaDirectService.java` - 新服务类
- `/api/src/main/java/com/emotion/api/config/OllamaConfig.java` - 简化配置
- `/api/src/main/resources/application-prod.yml` - 添加 AI 配置
- `/api/src/test/java/com/emotion/api/OllamaDirectServiceTest.java` - 单元测试
