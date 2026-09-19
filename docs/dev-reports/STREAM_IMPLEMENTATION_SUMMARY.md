# 心灵花园流式分析功能改造总结

## 📋 改造概述

本次改造将微信小程序"心灵花园"页面的情感分析功能从同步响应改为流式输出(SSE),提升用户体验,让用户能够实时看到 AI 分析结果的生成过程。

---

## ✅ 已完成的后端改造

### 1. 新增文件

#### 1.1 StreamEmotionResponse.java
**路径**: `/api/src/main/java/com/emotion/api/dto/StreamEmotionResponse.java`

**作用**: SSE 流式响应的数据传输对象

**关键特性**:
- 支持三种事件类型: `chunk`(文本片段)、`complete`(完成)、`error`(错误)
- 提供静态工厂方法简化创建
- 包含时间戳用于前端调试

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StreamEmotionResponse {
    private String type;      // chunk/complete/error
    private String data;      // 数据内容
    private Long timestamp;   // 时间戳
    
    public static StreamEmotionResponse chunk(String content) { ... }
    public static StreamEmotionResponse complete(String fullJson) { ... }
    public static StreamEmotionResponse error(String message) { ... }
}
```

### 2. 修改文件

#### 2.1 OllamaChatService.java
**路径**: `/api/src/main/java/com/emotion/api/service/OllamaChatService.java`

**新增方法**:
```java
/**
 * 情绪分析 - 流式输出
 */
public Flux<String> analyzeEmotionStream(String text) {
    String prompt = "请分析以下文本的情绪...";
    
    return chatClient.prompt()
            .user(prompt)
            .stream()        // 关键:使用 stream() 而非 call()
            .content();      // 返回 Flux<String>
}
```

**技术要点**:
- 引入 `reactor.core.publisher.Flux` 支持响应式流
- 使用 Spring AI 的 `.stream()` API 实现流式调用
- 保持原有的 `analyzeEmotion()` 方法不变(向后兼容)

#### 2.2 UserController.java
**路径**: `/api/src/main/java/com/emotion/api/controller/UserController.java`

**新增接口**:
```java
@PostMapping(value = "/api/v1/submitTextStream", 
             produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public SseEmitter submitTextStream(@CurrentUser UserPrincipal userPrincipal, 
                                   @RequestBody SubmitTextDTO text)
```

**核心功能**:
1. **参数校验**: 签到校验、文本长度校验
2. **SSE 连接管理**: 创建 `SseEmitter`,设置 60 秒超时
3. **异步流处理**: 使用新线程处理流式输出,避免阻塞
4. **事件推送**:
   - `start`: 开始分析
   - `chunk`: 实时推送 AI 生成的文本片段
   - `complete`: 分析完成,发送完整结果并保存数据库
   - `error`: 错误处理
5. **JSON 解析**: 从 AI 响应中提取 JSON,处理可能的 markdown 格式
6. **情绪标签映射**: 根据分数(1-100)映射到中文情绪标签

**辅助方法**:
```java
// 从AI响应中提取JSON字符串(处理markdown代码块)
private String extractJsonFromResponse(String response)

// 根据分数获取情绪标签
private String getEmotionLabel(Integer score)
```

**导入新增**:
```java
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.util.concurrent.atomic.AtomicReference;
```

### 3. 测试文件

#### 3.1 StreamEmotionTest.java
**路径**: `/api/src/test/java/com/emotion/api/StreamEmotionTest.java`

**测试用例**:
- `testStreamEmotionAnalysis()`: 测试正面情绪的流式分析
- `testStreamNegativeEmotion()`: 测试负面情绪的流式分析

**运行方式**:
```bash
mvn test -Dtest=StreamEmotionTest
```

---

## 🎯 接口对比

### 原有非流式接口(保留)
```
POST /user/api/v1/submitText
Content-Type: application/json

请求:
{
  "text": "今天心情很好",
  "type": 0
}

响应 (等待 AI 完成后一次性返回):
{
  "code": 200,
  "data": {
    "emotion": "开心",
    "emotionRatio": 75,
    "reminder": "保持好心情哦"
  }
}
```

### 新增流式接口
```
POST /user/api/v1/submitTextStream
Content-Type: application/json
Accept: text/event-stream

请求:
{
  "text": "今天心情很好",
  "type": 0
}

响应 (SSE 流式推送):

event: start
data: {"type":"chunk","data":"开始分析...","timestamp":1234567890}

event: chunk
data: {"type":"chunk","data":"{\"score\":","timestamp":1234567891}

event: chunk
data: {"type":"chunk","data":"75,\"suggestion\":\"保持好心情\"}","timestamp":1234567892}

event: complete
data: {"type":"complete","data":"{\"emotion\":\"开心\",\"emotionRatio\":75,\"reminder\":\"保持好心情哦\"}","timestamp":1234567893}
```

---

## 📱 前端接入方案

### 方案一:真正的流式输出(推荐)

**适用场景**: 追求最佳用户体验,希望用户实时看到 AI 思考过程

**实现要点**:
1. 使用 `wx.request` 的 `enableChunked: true`
2. 监听 `onChunkReceived` 事件接收数据块
3. 手动解析 SSE 格式(`event:` 和 `data:`)
4. 实时更新 UI 显示流式内容

**参考文档**: [STREAM_API_GUIDE.md](../ops/STREAM_API_GUIDE.md) 中的"方法一"

### 方案二:模拟流式效果

**适用场景**: 小程序环境对 SSE 支持不佳,或实现复杂度考虑

**实现要点**:
1. 使用原有的非流式接口 `/user/api/v1/submitText`
2. 等待完整响应后,前端用打字机动画逐字显示
3. 用户体验接近流式,但实际是传统 HTTP 请求

**参考文档**: [STREAM_API_GUIDE.md](../ops/STREAM_API_GUIDE.md) 中的"方法二"

---

## 🔧 技术栈说明

### 后端技术
- **Spring Boot**: Web 框架
- **Spring AI 1.0.0-M5**: AI 集成框架
- **Ollama**: 本地 AI 模型服务(qwen3.5:9b)
- **Project Reactor**: 响应式编程(Flux)
- **SSE (Server-Sent Events)**: 服务器推送技术

### 前端技术
- **微信小程序**: 原生小程序框架
- **wx.request**: 网络请求 API
- **TextDecoder**: UTF-8 解码器

---

## ⚠️ 注意事项

### 1. 依赖检查
确保 `pom.xml` 中包含以下依赖:
```xml
<!-- Spring AI Ollama (已存在) -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-ollama-spring-boot-starter</artifactId>
    <version>1.0.0-M5</version>
</dependency>
```

Project Reactor 会被 Spring AI 自动引入,无需额外配置。

### 2. Ollama 服务
确保 Ollama 服务正在运行:
```bash
# 检查 Ollama 是否运行
curl http://localhost:11434/api/tags

# 如果没有运行,启动 Ollama
ollama serve

# 拉取模型(如果尚未拉取)
ollama pull qwen3.5:9b
```

### 3. CORS 配置
如果前后端分离部署,需要配置 CORS:
```java
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/user/api/v1/submitTextStream")
                .allowedOrigins("*")
                .allowedMethods("POST")
                .allowedHeaders("*");
    }
}
```

### 4. 超时设置
- **SSE 超时**: 60 秒(在 `SseEmitter` 构造函数中设置)
- **Ollama 超时**: 默认配置,可在 `OllamaConfig` 中调整

### 5. 内存管理
流式输出会占用一定内存,注意:
- 及时关闭 SSE 连接
- 避免同时处理过多流式请求
- 监控 JVM 堆内存使用情况

### 6. 微信小程序限制
- 必须在微信公众平台配置合法域名
- 开发时可开启"不校验合法域名"进行调试
- `enableChunked` 在部分旧版本微信客户端可能不支持

---

## 🧪 测试建议

### 后端测试
```bash
# 1. 单元测试
mvn test -Dtest=StreamEmotionTest

# 2. 接口测试(使用 curl)
curl -X POST http://localhost:8080/user/api/v1/submitTextStream \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{"text":"今天心情很好","type":0}' \
  -N  # -N 表示禁用缓冲,实时显示
```

### 前端测试
1. **微信开发者工具**:
   - 开启"不校验合法域名"
   - 查看 Console 和 Network 面板
   - 验证 SSE 事件接收正常

2. **真机测试**:
   - 测试不同网络环境(WiFi/4G/5G)
   - 测试长时间连接的稳定性
   - 测试异常情况(网络断开、服务器错误)

---

## 📊 性能优化建议

### 1. 连接池
Ollama 客户端已使用连接池,无需额外配置。

### 2. 并发控制
如果需要限制并发流式请求数量:
```java
// 使用 Semaphore 限制并发数
private static final Semaphore semaphore = new Semaphore(10);

@PostMapping(value = "/api/v1/submitTextStream", 
             produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public SseEmitter submitTextStream(...) {
    if (!semaphore.tryAcquire()) {
        // 返回繁忙提示
    }
    
    try {
        // 处理请求
    } finally {
        semaphore.release();
    }
}
```

### 3. 缓存策略
对于相同文本的分析结果,可以考虑缓存:
```java
@Cacheable(value = "emotionAnalysis", key = "#text")
public EmotionAnalysisResponse analyzeEmotion(String text) {
    // ...
}
```

---

## 🚀 部署步骤

### 1. 后端部署
```bash
# 1. 编译项目
mvn clean package -DskipTests

# 2. 上传 jar 包到服务器
scp target/emotion-api-0.0.1-SNAPSHOT.jar user@server:/path/to/deploy

# 3. 重启服务
systemctl restart emotion-api

# 4. 检查日志
tail -f /var/log/emotion-api.log
```

### 2. 前端部署
1. 在微信开发者工具中编译上传
2. 提交审核
3. 审核通过后发布

### 3. 验证
- 访问线上接口确认服务正常
- 小程序真机测试流式功能
- 监控错误日志

---

## 📝 后续优化方向

### 1. 功能增强
- [ ] 支持取消分析(中断 SSE 连接)
- [ ] 添加分析进度百分比
- [ ] 支持多轮对话上下文
- [ ] 添加情绪趋势图表

### 2. 性能优化
- [ ] 实现结果缓存(Redis)
- [ ] 优化 Ollama 模型参数
- [ ] 添加请求限流
- [ ] 异步持久化到数据库

### 3. 用户体验
- [ ] 添加骨架屏加载动画
- [ ] 优化错误提示文案
- [ ] 支持离线缓存历史分析
- [ ] 添加分享功能

### 4. 监控告警
- [ ] 添加接口响应时间监控
- [ ] SSE 连接失败率统计
- [ ] Ollama 服务健康检查
- [ ] 异常告警通知

---

## 📞 技术支持

如有问题,请检查:
1. 后端日志: `/var/log/emotion-api.log`
2. Ollama 服务状态: `curl http://localhost:11434/api/tags`
3. 小程序控制台错误信息
4. 网络连接是否正常

---

## ✨ 总结

本次改造成功实现了:
✅ 后端流式 SSE 接口开发  
✅ 保持原有非流式接口兼容  
✅ 完整的前端接入文档和示例代码  
✅ 单元测试覆盖  
✅ 详细的部署和测试指南  

流式输出显著提升了用户体验,让用户能够实时看到 AI 分析的过程,增强了产品的科技感和互动性。

**下一步**: 根据实际使用情况收集用户反馈,持续优化性能和体验。
