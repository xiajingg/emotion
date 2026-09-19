# SSE流式输出最终修复方案

## 🐛 问题汇总

### 问题1：大量空数据块
**现象**：后端日志显示几百行"收到数据块："，但都是空的

**原因**：
- Ollama返回的Flux中包含大量空白字符（空格、换行符等）
- 没有过滤这些空数据块
- 导致SSE发送大量无效事件

### 问题2：AccessDeniedException错误
**现象**：SSE完成后报 `org.springframework.security.access.AccessDeniedException`

**原因**：
- 异步线程中SecurityContext管理不当
- SSE完成后Spring Security过滤器链被再次触发
- SecurityContext已被清理，导致认证失败

### 问题3：前端没收到数据
**现象**：后端正常发送SSE事件，但前端没有响应

**原因**：
- 前端SSE解析逻辑过于严格
- 空数据事件被过滤掉，但可能影响后续事件解析
- 缺少调试日志，无法定位问题

---

## ✅ 修复方案

### 修复1：后端过滤空数据块

**文件**：`UserController.java`

```java
ollamaChatService.analyzeEmotionStream(text.getText())
    .doOnSubscribe(subscription -> {
        log.info("Flux已订阅: {}", subscription);
    })
    // ✅ 关键修复：过滤空数据块
    .filter(chunk -> chunk != null && !chunk.trim().isEmpty())
    .doOnNext(chunk -> {
        try {
            log.debug("收到数据块: [{}]", chunk);  // 改为DEBUG级别
            fullResponse.get().append(chunk);
            
            emitter.send(SseEmitter.event()
                    .name("chunk")
                    .data(StreamEmotionResponse.chunk(chunk)));
            log.debug("SSE数据块发送成功");
        } catch (IOException e) {
            log.error("发送SSE数据块失败", e);
        }
    })
    // ... 其他处理
```

**效果**：
- ✅ 只发送非空数据块
- ✅ 减少日志量（INFO → DEBUG）
- ✅ 提高性能

---

### 修复2：添加SSE生命周期回调

**文件**：`UserController.java`

```java
// 创建SSE emitter
SseEmitter emitter = new SseEmitter(180000L);

// ✅ 添加SSE生命周期回调
emitter.onCompletion(() -> {
    log.info("SSE连接已完成");
});

emitter.onTimeout(() -> {
    log.warn("SSE连接超时");
});

emitter.onError((throwable) -> {
    log.error("SSE连接错误", throwable);
});
```

**效果**：
- ✅ 正确处理SSE连接的生命周期
- ✅ 避免Security异常
- ✅ 提供清晰的日志

---

### 修复3：优化SecurityContext管理

**文件**：`UserController.java`

```java
// 在Flux终止时清理SecurityContext
.doOnTerminate(() -> {
    log.info("Flux流终止（完成或错误）");
    // ✅ 在Flux终止后清理SecurityContext
    org.springframework.security.core.context.SecurityContextHolder.clearContext();
})
.subscribe(...);

// 异常时也清理
} catch (Exception e) {
    log.error("流式分析处理异常", e);
    try {
        emitter.send(...);
        emitter.completeWithError(e);
    } catch (IOException ex) {
        log.error("发送SSE错误消息失败", ex);
    } finally {
        // ✅ 异常时也清理SecurityContext
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }
}
```

**效果**：
- ✅ 确保SecurityContext总是被清理
- ✅ 避免内存泄漏
- ✅ 防止Security异常

---

### 修复4：前端优化SSE解析

**文件**：`miniprogram/utils/request.js`

#### 4.1 添加详细日志

```javascript
console.log('[SSE] 开始流式请求:', options.url);

wx.request({
    // ...
    success(res) {
        console.log('[SSE] 收到响应, statusCode:', res.statusCode);
        console.log('[SSE] 响应数据类型:', typeof res.data);
        
        if (typeof responseText === 'string') {
            console.log('[SSE] 响应文本长度:', responseText.length);
            const events = parseSSEEvents(responseText);
            console.log('[SSE] 解析出事件数量:', events.length);
            
            events.forEach((event, index) => {
                console.log(`[SSE] 事件${index}: name=${event.name}, data长度=${event.data.length}`);
                // ... 处理事件
            });
        }
    }
});
```

#### 4.2 优化parseSSEEvents函数

```javascript
const parseSSEEvents = (text) => {
    const events = [];
    const lines = text.split('\n');
    let currentEvent = { name: 'message', data: '' };
    
    for (let i = 0; i < lines.length; i++) {
        const line = lines[i];  // ✅ 不要trim，保留原始内容
        
        if (line.startsWith('event:')) {
            currentEvent.name = line.substring(6).trim();
        } else if (line.startsWith('data:')) {
            // ✅ 只去掉'data:'前缀，保留后面的空格
            currentEvent.data = line.substring(5);
        } else if (line.trim() === '' && currentEvent.data) {
            // 空行表示一个事件结束
            // ✅ trim data，去除首尾空白
            currentEvent.data = currentEvent.data.trim();
            if (currentEvent.data) {  // ✅ 只添加非空数据的事件
                events.push({ ...currentEvent });
            }
            currentEvent = { name: 'message', data: '' };
        }
    }
    
    // 添加最后一个事件
    if (currentEvent.data) {
        currentEvent.data = currentEvent.data.trim();
        if (currentEvent.data) {
            events.push(currentEvent);
        }
    }
    
    return events;
};
```

**关键改进**：
1. ✅ 不提前trim整行，保留原始格式
2. ✅ 只在提取data后trim
3. ✅ 过滤掉空数据事件
4. ✅ 添加详细日志便于调试

---

## 🧪 测试步骤

### 1. 重启后端服务

```bash
cd /Users/xiajing/emotion/backend/api
mvn clean package
java -jar target/emotion-api-0.0.1-SNAPSHOT.jar
```

### 2. 观察后端日志

**成功的日志序列**：
```
INFO  - 流式分析请求: {...}
INFO  - 开始流式情绪分析，文本长度: 6
INFO  - 开始调用Ollama流式接口
INFO  - Flux已订阅: reactor.core.publisher.FluxMap$...
DEBUG - 收到数据块: [{"score": 75, ...}]  ← 只有非空数据
DEBUG - SSE数据块发送成功
INFO  - Flux流完成
INFO  - 完整JSON: {"score": 75, "suggestion": "..."}
INFO  - Flux流终止（完成或错误）
INFO  - SSE连接已完成  ← 新增
INFO  - 订阅完成
INFO  - 流式情绪分析完成，总耗时: XXXXms
```

**不应该看到的**：
- ❌ 几百行"收到数据块："（空的）
- ❌ AccessDeniedException错误

### 3. 观察前端日志

在微信开发者工具的Console中应该看到：

```
[SSE] 开始流式请求: /user/api/v1/submitTextStream
[SSE] 收到响应, statusCode: 200
[SSE] 响应数据类型: string
[SSE] 响应文本长度: XXX
[SSE] 解析出事件数量: X
[SSE] 事件0: name=chunk, data长度=X
[SSE] 处理chunk事件
[SSE] 事件1: name=complete, data长度=X
[SSE] 处理complete事件
```

### 4. 验证前端UI

- ✅ 应该看到流式输出的文本
- ✅ 闪烁光标动画
- ✅ 最终显示完整的情感分析结果

---

## 📊 性能对比

### 修复前

| 指标 | 数值 |
|-----|------|
| 空数据块数量 | 几百个 ❌ |
| 日志行数 | 500+行 ❌ |
| 响应时间 | 43秒 ❌ |
| 前端接收 | 无数据 ❌ |
| 错误 | AccessDeniedException ❌ |

### 修复后（预期）

| 指标 | 数值 |
|-----|------|
| 空数据块数量 | 0个 ✅ |
| 日志行数 | 20-30行 ✅ |
| 响应时间 | 5-15秒 ✅ |
| 前端接收 | 正常接收 ✅ |
| 错误 | 无错误 ✅ |

---

## 🔍 调试技巧

### 后端调试

1. **查看过滤后的数据块数量**
   ```bash
   # 启动时开启DEBUG日志
   java -jar target/emotion-api-0.0.1-SNAPSHOT.jar --logging.level.com.emotion.api=DEBUG
   ```

2. **监控SSE连接状态**
   ```bash
   tail -f emotion-api.log | grep -E "(SSE|Flux|chunk)"
   ```

3. **检查SecurityContext清理**
   ```bash
   tail -f emotion-api.log | grep -i "security"
   ```

### 前端调试

1. **打开微信开发者工具Console**
   - 查看所有 `[SSE]` 开头的日志
   - 确认事件数量和类型

2. **检查Network标签**
   - 找到 `/submitTextStream` 请求
   - 查看响应内容和状态码

3. **断点调试**
   - 在 `requestStream` 函数中设置断点
   - 逐步检查事件解析过程

---

## ⚠️ 注意事项

### 1. 日志级别

生产环境建议：
```yaml
logging:
  level:
    com.emotion.api.controller.UserController: INFO  # 不是DEBUG
    com.emotion.api.service.OllamaChatService: INFO
```

开发环境可以开启DEBUG：
```yaml
logging:
  level:
    com.emotion.api: DEBUG
```

### 2. SSE超时时间

当前设置为180秒（3分钟），根据实际情况调整：
- 短文本：60-120秒
- 中等文本：120-180秒
- 长文本：180-300秒

### 3. 并发控制

如果大量用户同时使用，考虑添加限流：
```java
// 使用RateLimiter
private final RateLimiter rateLimiter = RateLimiter.create(50); // 每秒50个请求

@PostMapping("/api/v1/submitTextStream")
public SseEmitter submitTextStream(...) {
    if (!rateLimiter.tryAcquire()) {
        throw new RuntimeException("系统繁忙，请稍后重试");
    }
    // ...
}
```

---

## 🎯 验收标准

修复完成后，逐项确认：

- [ ] 后端日志中没有大量空数据块
- [ ] 后端日志中没有AccessDeniedException错误
- [ ] 后端日志显示"SSE连接已完成"
- [ ] 前端Console显示SSE事件解析日志
- [ ] 前端UI显示流式输出文本
- [ ] 前端UI显示最终分析结果
- [ ] 响应时间在合理范围内（5-15秒）
- [ ] 多次测试都稳定工作

---

## 📝 修改的文件清单

1. ✅ `backend/api/src/main/java/com/emotion/api/controller/UserController.java`
   - 添加空数据块过滤
   - 添加SSE生命周期回调
   - 优化SecurityContext管理
   - 调整日志级别

2. ✅ `miniprogram/utils/request.js`
   - 添加详细调试日志
   - 优化parseSSEEvents函数
   - 过滤空数据事件

---

## 🚀 后续优化建议

### 1. 使用WebSocket替代SSE

微信小程序对WebSocket支持更好：
```javascript
// 前端
const socket = wx.connectSocket({
    url: 'wss://your-domain/ws/emotion-analysis'
});

socket.onMessage((res) => {
    const data = JSON.parse(res.data);
    // 处理数据
});
```

### 2. 添加取消功能

允许用户中途取消分析：
```java
emitter.onTimeout(() -> {
    // 取消Ollama请求
    disposable.dispose();
});
```

### 3. 缓存常见结果

对于相同的输入，缓存结果：
```java
@Cacheable(value = "emotion-analysis", key = "#text")
public Flux<String> analyzeEmotionStream(String text) {
    // ...
}
```

---

**修复日期**: 2026-05-05  
**修复版本**: v1.3  
**严重程度**: 🔴 高（核心功能阻塞）
