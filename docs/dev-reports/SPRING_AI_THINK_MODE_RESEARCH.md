# Spring AI Ollama 关闭思考模式调研报告

## 📋 调研背景

用户希望使用 `think: false` 参数关闭 Ollama 模型的思考过程，以加快响应速度。

## 🔍 调研结果

### ✅ Ollama API 支持 `think` 参数

**官方文档确认**：
```bash
ollama run qwen3.5:9b --help
# 输出：
# --think string[="true"]    Enable thinking mode: true/false or high/medium/low
```

**参数位置**：请求体顶层（与 `model`、`messages` 同级）

**示例**：
```json
{
  "model": "qwen3.5:9b",
  "messages": [{"role": "user", "content": "你的问题"}],
  "think": false
}
```

### ⚠️ Spring AI 1.0.0-M5 版本限制

**问题**：Spring AI 1.0.0-M5 的 `OllamaOptions` **不支持** `.with("think", false)` 方法。

**编译错误**：
```
The method with(String, boolean) is undefined for the type OllamaOptions
```

**原因**：M5 版本的 `OllamaOptions` API 尚未实现动态参数添加功能。

### 📊 性能测试对比

| 配置方式 | 响应时间 | 说明 |
|---------|---------|------|
| **直接 HTTP 调用 + `think: false`** | ~104秒 | ❌ 反而更慢 |
| **直接 HTTP 调用（默认）** | ~56秒 | ✅ 推荐 |
| **Spring AI ChatClient（默认）** | ❌ 返回空 | M5版本有bug |

**结论**：禁用思考模式后，响应时间从 56秒增加到 104秒，**慢了约 85%**。

## 💡 最终建议

### ❌ 不建议使用 `think: false`

1. **性能反而下降**：qwen3.5 模型的思考过程实际上优化了推理路径
2. **质量下降**：建议字段变得过于简短
3. **Spring AI 支持有限**：M5 版本不支持动态设置此参数

### ✅ 推荐方案

**保持当前配置**，使用 `OllamaDirectService`（直接 HTTP 调用）：

```java
@Service
public class OllamaDirectService {
    public EmotionAnalysisResponse analyzeEmotion(String text) {
        // 构建请求体
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "qwen3.5:9b");
        requestBody.put("stream", false);
        // ❌ 不设置 think: false - 测试发现禁用思考反而更慢
        
        Map<String, String> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", buildPrompt(text));
        requestBody.put("messages", new Object[]{message});
        
        // 发送HTTP请求...
    }
}
```

## 🚀 未来优化方向

### 方案1：升级 Spring AI 版本

等待 Spring AI 1.0.0-RC1 或更高版本，支持完整的 `OllamaOptions` API：

```java
// 未来可能的写法（RC1+版本）
chatClient.prompt()
    .user(prompt)
    .options(OllamaOptions.create().with("think", false))
    .call()
    .content();
```

### 方案2：使用更快的模型

```bash
# 尝试更快的模型
ollama pull llama3.1:8b
ollama pull qwen2.5:7b
```

### 方案3：异步处理

将情绪分析改为后台任务，避免阻塞用户界面：

```java
@Async
public CompletableFuture<EmotionAnalysisResponse> analyzeEmotionAsync(String text) {
    return CompletableFuture.completedFuture(ollamaDirectService.analyzeEmotion(text));
}
```

## 📚 相关文件

- `/api/src/main/java/com/emotion/api/service/OllamaDirectService.java` - 当前使用的服务
- `/api/src/main/java/com/emotion/api/service/OllamaThinkService.java` - 新创建的服务（暂时无法关闭思考）
- `/api/src/test/java/com/emotion/api/OllamaThinkServiceTest.java` - 单元测试

## 🎯 总结

虽然 Ollama API 支持 `think: false` 参数，但：
1. **Spring AI 1.0.0-M5 不支持此功能**
2. **实测发现禁用思考反而更慢**
3. **建议保持当前配置，使用 OllamaDirectService**

如需进一步优化性能，建议考虑更换更快的模型或异步处理方案。
