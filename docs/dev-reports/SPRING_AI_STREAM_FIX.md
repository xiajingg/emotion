# Spring AI Ollama 流式调用修复

## 🐛 问题描述

### 现象
```
INFO  - 开始流式情绪分析，文本长度: 6
[然后卡住，没有任何后续日志]
```

### 根本原因

**Spring AI的Flux流需要正确的方式订阅和处理**。原代码存在以下问题：

1. **使用了错误的API**：`.content()` 返回 `Flux<String>`，但在某些版本中可能不会自动触发
2. **缺少显式订阅处理**：虽然调用了 `.subscribe()`，但没有正确处理回调
3. **缺少调试日志**：无法定位卡在哪个环节

---

## ✅ 修复方案

### 修复1：使用正确的Spring AI API

#### 修改前（错误）
```java
return chatClient.prompt()
        .user(prompt)
        .stream()
        .content()  // ❌ 可能不会触发执行
        .doOnNext(chunk -> { ... })
        .subscribe();
```

#### 修改后（正确）
```java
return chatClient.prompt()
        .user(prompt)
        .stream()
        .chatResponse()  // ✅ 返回 Flux<ChatResponse>
        .map(chatResponse -> {
            String content = chatResponse.getResult().getOutput().getContent();
            return content;
        })
        .doOnNext(chunk -> { ... })
        .subscribe(...);
```

**关键区别**：
- `.content()` - 直接返回字符串流，可能在某些情况下不触发
- `.chatResponse()` - 返回完整的ChatResponse对象流，更可靠

---

### 修复2：添加完整的订阅回调

#### 修改前
```java
.subscribe();  // ❌ 空订阅，无法捕获错误
```

#### 修改后
```java
.subscribe(
    chunk -> {
        // onNext - 处理每个数据块
    },
    error -> {
        // onError - 处理错误
        log.error("订阅错误", error);
    },
    () -> {
        // onComplete - 处理完成
        log.info("订阅完成");
    }
);
```

---

### 修复3：添加详细的调试日志

在关键位置添加日志：

```java
// 1. 开始调用
log.info("开始调用Ollama流式接口");

// 2. Flux订阅时
.doOnSubscribe(subscription -> {
    log.info("Flux已订阅: {}", subscription);
})

// 3. 收到每个数据块
.doOnNext(chunk -> {
    log.info("收到数据块: {}", chunk);
})

// 4. Flux完成
.doOnComplete(() -> {
    log.info("Flux流完成");
})

// 5. Flux终止（无论成功或失败）
.doOnTerminate(() -> {
    log.info("Flux流终止（完成或错误）");
})

// 6. 订阅完成后
log.info("Flux订阅已完成，等待数据...");
```

---

## 📝 修改的文件

### 1. OllamaChatService.java

**位置**：`backend/api/src/main/java/com/emotion/api/service/OllamaChatService.java`

**修改内容**：
```java
public Flux<String> analyzeEmotionStream(String text) {
    log.info("开始流式情绪分析，文本长度: {}", text.length());
    long startTime = System.currentTimeMillis();
    
    String prompt = "...";

    // ✅ 关键修复：使用 chatResponse() 而不是 content()
    return chatClient.prompt()
            .user(prompt)
            .stream()
            .chatResponse()  // 返回 Flux<ChatResponse>
            .map(chatResponse -> {
                String content = chatResponse.getResult().getOutput().getContent();
                log.debug("收到流式数据块: {}", content);
                return content;
            })
            .doOnError(error -> {
                log.error("流式情绪分析失败，耗时: {}ms", 
                    System.currentTimeMillis() - startTime, error);
            })
            .doOnComplete(() -> {
                log.info("流式情绪分析完成，总耗时: {}ms", 
                    System.currentTimeMillis() - startTime);
            });
}
```

---

### 2. UserController.java

**位置**：`backend/api/src/main/java/com/emotion/api/controller/UserController.java`

**修改内容**：
```java
// 异步处理流式输出
new Thread(() -> {
    try {
        SecurityContextHolder.setContext(securityContext);
        
        emitter.send(SseEmitter.event()
                .name("start")
                .data(StreamEmotionResponse.chunk("开始分析...")));
        
        // ✅ 添加详细日志
        log.info("开始调用Ollama流式接口");
        
        ollamaChatService.analyzeEmotionStream(text.getText())
                .doOnSubscribe(subscription -> {
                    log.info("Flux已订阅: {}", subscription);
                })
                .doOnNext(chunk -> {
                    try {
                        log.info("收到数据块: {}", chunk);  // ✅ 新增日志
                        fullResponse.get().append(chunk);
                        
                        emitter.send(SseEmitter.event()
                                .name("chunk")
                                .data(StreamEmotionResponse.chunk(chunk)));
                    } catch (IOException e) {
                        log.error("发送SSE数据块失败", e);
                    }
                })
                .doOnError(error -> {
                    log.error("Ollama流式情绪分析失败", error);
                    try {
                        emitter.send(SseEmitter.event()
                                .name("error")
                                .data(StreamEmotionResponse.error(
                                    "AI服务异常: " + error.getMessage())));  // ✅ 包含错误信息
                        emitter.completeWithError(error);
                    } catch (IOException e) {
                        log.error("发送SSE错误消息失败", e);
                    }
                })
                .doOnComplete(() -> {
                    log.info("Flux流完成");  // ✅ 新增日志
                    try {
                        String completeJson = fullResponse.get().toString();
                        log.info("完整JSON: {}", completeJson);  // ✅ 新增日志
                        
                        // ... 解析和保存逻辑 ...
                        
                        emitter.complete();
                    } catch (IOException e) {
                        log.error("发送SSE完成消息失败", e);
                        emitter.completeWithError(e);
                    }
                })
                .doOnTerminate(() -> {
                    log.info("Flux流终止（完成或错误）");  // ✅ 新增日志
                })
                .subscribe(  // ✅ 完整的订阅回调
                    chunk -> {
                        // 数据已在 doOnNext 中处理
                    },
                    error -> {
                        log.error("订阅错误", error);
                    },
                    () -> {
                        log.info("订阅完成");
                    }
                );
                
        log.info("Flux订阅已完成，等待数据...");  // ✅ 新增日志
        
    } catch (Exception e) {
        log.error("流式分析处理异常", e);
        // ... 错误处理 ...
    } finally {
        SecurityContextHolder.clearContext();
    }
}).start();
```

---

## 🧪 测试步骤

### 1. 重启后端服务

```bash
cd /Users/xiajing/emotion/backend/api
mvn clean package
java -jar target/emotion-api-0.0.1-SNAPSHOT.jar
```

### 2. 观察启动日志

应该看到：
```
========== Ollama配置初始化 ==========
使用的模型: qwen3.5:2b
```

### 3. 发送测试请求

```bash
curl -X POST http://localhost:8080/user/api/v1/submitTextStream \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "text": "今天很开心"
  }'
```

### 4. 观察日志输出

**成功的日志序列**：
```
INFO  - 流式分析请求: {...}
INFO  - 开始流式情绪分析，文本长度: 6
INFO  - 开始调用Ollama流式接口
INFO  - Flux已订阅: reactor.core.publisher.FluxMap$...
INFO  - 收到数据块: {"
INFO  - 收到数据块: score": 75,
INFO  - 收到数据块: "suggestion": "保持好心情！"}
INFO  - Flux流完成
INFO  - 完整JSON: {"score": 75, "suggestion": "保持好心情！"}
INFO  - Flux流终止（完成或错误）
INFO  - 订阅完成
INFO  - Flux订阅已完成，等待数据...
INFO  - 流式情绪分析完成，总耗时: 5234ms
```

**如果仍然卡住，会看到**：
```
INFO  - 开始流式情绪分析，文本长度: 6
INFO  - 开始调用Ollama流式接口
INFO  - Flux已订阅: ...
[然后就没有了 - 说明Flux没有产生任何数据]
```

---

## 🔍 调试技巧

### 1. 检查Ollama服务是否正常

```bash
# 直接调用Ollama API
curl http://localhost:11434/api/generate -d '{
  "model": "qwen3.5:2b",
  "prompt": "你好",
  "stream": false
}'
```

**预期**：快速返回响应

### 2. 检查网络连接

```bash
# 测试Ollama端口是否可达
telnet localhost 11434

# 或者
nc -zv localhost 11434
```

### 3. 查看Ollama日志

```bash
# macOS
tail -f ~/Library/Logs/Ollama/*.log

# Linux
journalctl -u ollama -f
```

### 4. 检查资源占用

```bash
# 查看Ollama进程
ps aux | grep ollama

# 查看内存使用
top -pid $(pgrep -f ollama)
```

---

## ⚠️ 常见问题

### Q1: 日志显示"Flux已订阅"但没有"收到数据块"

**原因**：
- Ollama服务没有响应
- 网络问题
- 模型加载失败

**解决**：
1. 检查Ollama服务是否运行：`ollama list`
2. 直接调用Ollama API测试
3. 查看Ollama日志

### Q2: 日志显示"收到数据块"但前端没收到

**原因**：
- SSE发送失败
- 前端解析问题

**解决**：
1. 检查是否有"发送SSE数据块失败"的错误日志
2. 检查前端是否正确解析SSE格式
3. 使用浏览器开发者工具查看Network标签

### Q3: Flux立即完成，没有任何数据块

**原因**：
- Prompt有问题
- 模型返回空响应

**解决**：
1. 检查Prompt是否正确
2. 尝试简化Prompt测试
3. 查看Ollama返回的原始响应

### Q4: 超时错误

**原因**：
- Ollama响应太慢
- SSE超时时间设置太短

**解决**：
```java
// 增加SSE超时时间
SseEmitter emitter = new SseEmitter(180000L);  // 3分钟
```

---

## 📊 性能优化建议

### 1. 使用线程池

当前使用 `new Thread()`，建议改为线程池：

```java
@Autowired
private TaskExecutor taskExecutor;

// 在Controller中
taskExecutor.execute(() -> {
    // 流式处理逻辑
});
```

配置线程池：
```java
@Configuration
public class AsyncConfig {
    @Bean
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("ollama-stream-");
        executor.initialize();
        return executor;
    }
}
```

### 2. 添加超时控制

```java
.timeout(Duration.ofSeconds(60))  // 60秒超时
```

### 3. 缓存常用响应

对于相同的输入，可以缓存结果：

```java
@Cacheable(value = "emotion-analysis", key = "#text")
public Flux<String> analyzeEmotionStream(String text) {
    // ...
}
```

---

## 🎯 总结

### 核心修复点

1. ✅ **使用 `.chatResponse()` 替代 `.content()`**
2. ✅ **添加完整的订阅回调**（onNext, onError, onComplete）
3. ✅ **添加详细的调试日志**
4. ✅ **确保Flux被正确订阅和执行**

### 验证标准

- [ ] 日志显示完整的执行流程
- [ ] 能看到"收到数据块"的日志
- [ ] 最终显示"Flux流完成"
- [ ] 前端能实时收到SSE数据
- [ ] 总耗时在合理范围内（5-15秒）

---

**修复日期**: 2026-05-05  
**Spring AI版本**: 1.0.0-M5  
**Ollama模型**: qwen3.5:2b
