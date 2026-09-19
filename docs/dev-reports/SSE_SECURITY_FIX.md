# SSE流式输出AccessDeniedException问题分析与解决

## 📋 问题描述

### 错误日志
```
2026-05-05T11:14:42.718+08:00  INFO  - 流式分析请求成功接收
2026-05-05T11:15:43.574+08:00 ERROR - Servlet.service() threw exception

org.springframework.security.access.AccessDeniedException: Access Denied
```

### 现象
1. **第一个请求成功**：流式分析接口正常接收请求
2. **60秒后失败**：出现 `AccessDeniedException` 权限拒绝错误
3. **时间点规律**：恰好在SSE超时时间（60秒）时发生

---

## 🔍 根本原因分析

### 原因1：SecurityContext在异步线程中丢失 ⭐⭐⭐

**问题本质**：
```java
// 主线程中，Spring Security自动设置SecurityContext
@CurrentUser UserPrincipal userPrincipal  // ✅ 有认证信息

// 但在新线程中，SecurityContext不会自动传递
new Thread(() -> {
    // ❌ 这里SecurityContext为null或空
    ollamaChatService.analyzeEmotionStream(text.getText())
        .doOnNext(chunk -> {
            // 尝试访问受保护资源时会失败
        })
})
```

**为什么会失败**：
1. Spring Security使用 `ThreadLocal` 存储SecurityContext
2. `ThreadLocal` 是线程隔离的，新线程无法访问父线程的SecurityContext
3. 当异步线程中尝试访问需要认证的资源时，Security判断为未认证
4. 抛出 `AccessDeniedException`

### 原因2：SSE超时时间过短

原代码设置超时为 **60秒**：
```java
SseEmitter emitter = new SseEmitter(60000L);  // 60秒
```

对于复杂的AI分析任务，60秒可能不够，导致连接中断触发异常。

### 原因3：请求缓存干扰SSE长连接

Spring Security默认启用请求缓存，可能干扰SSE的长连接机制。

---

## ✅ 解决方案

### 方案1：在异步线程中传递SecurityContext（已实施）⭐⭐⭐

#### 修改位置
`UserController.java` - `submitTextStream()` 方法

#### 核心代码
```java
@PostMapping(value = "/api/v1/submitTextStream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public SseEmitter submitTextStream(@CurrentUser UserPrincipal userPrincipal, @RequestBody SubmitTextDTO text) {
    
    // ✅ 步骤1：在主线程中保存SecurityContext
    org.springframework.security.core.context.SecurityContext securityContext = 
        org.springframework.security.core.context.SecurityContextHolder.getContext();
    
    // 增加超时时间到180秒（3分钟）
    SseEmitter emitter = new SseEmitter(180000L);
    
    // 异步处理
    new Thread(() -> {
        try {
            // ✅ 步骤2：在异步线程中恢复SecurityContext
            org.springframework.security.core.context.SecurityContextHolder.setContext(securityContext);
            
            // ... 原有的业务逻辑 ...
            
        } catch (Exception e) {
            // 错误处理
        } finally {
            // ✅ 步骤3：清理SecurityContext，避免内存泄漏
            org.springframework.security.core.context.SecurityContextHolder.clearContext();
        }
    }).start();
    
    return emitter;
}
```

#### 为什么有效
- **保存**：在主线程（有认证信息）中捕获SecurityContext
- **传递**：将SecurityContext对象传递给异步线程
- **恢复**：在异步线程开始时设置SecurityContext
- **清理**：完成后清除，防止内存泄漏

---

### 方案2：禁用请求缓存（已实施）⭐⭐

#### 修改位置
`SecurityConfig.java` - `securityFilterChain()` 方法

#### 核心代码
```java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.csrf(csrf -> csrf.disable())
        .authorizeHttpRequests(auth -> auth
            // ... 其他配置 ...
        )
        .sessionManagement(session -> session
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        )
        // ✅ 新增：禁用请求缓存，避免SSE连接问题
        .requestCache(cache -> cache.disable());
    
    http.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);
    return http.build();
}
```

#### 为什么有效
- SSE是长连接，请求缓存可能导致连接状态混乱
- 禁用缓存后，每个SSE事件都独立处理

---

### 方案3：增加超时时间（已实施）⭐

#### 修改内容
```java
// 原来：60秒
SseEmitter emitter = new SseEmitter(60000L);

// 修改后：180秒（3分钟）
SseEmitter emitter = new SseEmitter(180000L);
```

#### 为什么有效
- AI分析可能需要较长时间（特别是长文本）
- 给予足够的时间完成分析，避免中途超时

---

## 🧪 验证方法

### 1. 重启后端服务
```bash
cd /Users/xiajing/emotion/backend/api
mvn clean package
java -jar target/emotion-api-0.0.1-SNAPSHOT.jar
```

### 2. 测试流式接口

#### 测试用例1：短文本
```json
POST /user/api/v1/submitTextStream
{
  "text": "今天很开心"
}
```
**预期**：快速返回结果，无错误

#### 测试用例2：长文本
```json
POST /user/api/v1/submitTextStream
{
  "text": "今天工作非常忙碌，早上开了一个重要的会议，下午又处理了很多紧急的任务，虽然很累但是很有成就感..."
}
```
**预期**：
- 流式输出持续10-30秒
- 最终完整返回结果
- **无AccessDeniedException错误**

#### 测试用例3：超长等待
输入一段需要AI长时间分析的文本，观察是否超过60秒后仍然正常工作。

### 3. 查看日志

**成功标志**：
```
INFO  - 流式分析请求: {...} : {"text":"..."}
INFO  - Ollama流式情绪分析完成
```

**失败标志**（修复前）：
```
ERROR - Servlet.service() threw exception
org.springframework.security.access.AccessDeniedException: Access Denied
```

---

## 📊 技术细节

### Spring Security的ThreadLocal机制

```java
// Spring Security内部实现（简化版）
public class SecurityContextHolder {
    // 使用ThreadLocal存储SecurityContext
    private static final ThreadLocal<SecurityContext> contextHolder = new ThreadLocal<>();
    
    public static SecurityContext getContext() {
        return contextHolder.get();  // 获取当前线程的SecurityContext
    }
    
    public static void setContext(SecurityContext context) {
        contextHolder.set(context);  // 设置当前线程的SecurityContext
    }
    
    public static void clearContext() {
        contextHolder.remove();  // 清除当前线程的SecurityContext
    }
}
```

**关键点**：
- `ThreadLocal` 是线程隔离的
- 每个线程有自己的SecurityContext副本
- 新线程不会继承父线程的ThreadLocal值

### SSE超时机制

```java
// SseEmitter内部逻辑（简化版）
public class SseEmitter {
    private final long timeout;
    private ScheduledFuture<?> timeoutTask;
    
    public SseEmitter(long timeout) {
        this.timeout = timeout;
        // 启动超时定时器
        this.timeoutTask = scheduler.schedule(() -> {
            completeWithError(new TimeoutException("SSE timeout"));
        }, timeout, TimeUnit.MILLISECONDS);
    }
}
```

**问题**：
- 超时后会调用 `completeWithError()`
- 可能触发Security的异常处理流程
- 如果此时SecurityContext丢失，就会报AccessDenied

---

## 🔧 其他可选方案

### 方案A：使用CompletableFuture替代Thread

```java
// 使用Spring的TaskExecutor，自动管理线程池和上下文
@Autowired
private TaskExecutor taskExecutor;

@PostMapping("/api/v1/submitTextStream")
public SseEmitter submitTextStream(...) {
    SseEmitter emitter = new SseEmitter(180000L);
    
    // CompletableFuture会自动继承父线程的某些上下文
    CompletableFuture.runAsync(() -> {
        // 业务逻辑
    }, taskExecutor).exceptionally(ex -> {
        log.error("异步任务失败", ex);
        return null;
    });
    
    return emitter;
}
```

**优点**：
- 更优雅的异步处理
- 可以使用Spring管理的线程池
- 更好的异常处理

### 方案B：使用WebFlux（响应式编程）

```java
// 完全响应式的实现
@PostMapping(value = "/api/v1/submitTextStream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public Flux<ServerSentEvent<String>> submitTextStream(...) {
    return ollamaChatService.analyzeEmotionStream(text.getText())
        .map(chunk -> ServerSentEvent.<String>builder()
            .event("chunk")
            .data(chunk)
            .build())
        .concatWith(Mono.just(ServerSentEvent.<String>builder()
            .event("complete")
            .data(finalResult)
            .build()));
}
```

**优点**：
- 原生支持流式输出
- 无需手动管理线程
- 更好的性能

**缺点**：
- 需要大幅重构代码
- 学习曲线陡峭

---

## ⚠️ 注意事项

### 1. 内存泄漏风险

**问题**：如果不及时清理SecurityContext，可能导致内存泄漏

**解决**：
```java
finally {
    // ✅ 必须清理
    SecurityContextHolder.clearContext();
}
```

### 2. 线程安全问题

**问题**：多个并发请求共享同一个SecurityContext对象

**解决**：
```java
// ✅ 每次请求都创建新的SecurityContext副本
SecurityContext securityContext = SecurityContextHolder.getContext();
// Spring Security的SecurityContext是可变的，但在这里只读，所以安全
```

### 3. 超时时间设置

**建议**：
- 短文本：60-120秒
- 中等文本：120-180秒
- 长文本：180-300秒

根据实际AI响应时间调整。

---

## 📈 性能优化建议

### 1. 使用线程池

```java
@Configuration
public class AsyncConfig {
    @Bean
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("sse-async-");
        executor.initialize();
        return executor;
    }
}
```

### 2. 监控SSE连接数

```java
// 添加指标监控
private AtomicInteger activeConnections = new AtomicInteger(0);

@PostMapping("/api/v1/submitTextStream")
public SseEmitter submitTextStream(...) {
    activeConnections.incrementAndGet();
    
    SseEmitter emitter = new SseEmitter(180000L);
    
    emitter.onCompletion(() -> {
        activeConnections.decrementAndGet();
    });
    
    emitter.onTimeout(() -> {
        activeConnections.decrementAndGet();
    });
    
    // ...
}
```

### 3. 限流保护

```java
// 使用RateLimiter限制并发SSE连接数
private final RateLimiter rateLimiter = RateLimiter.create(100); // 每秒100个

@PostMapping("/api/v1/submitTextStream")
public SseEmitter submitTextStream(...) {
    if (!rateLimiter.tryAcquire()) {
        throw new RuntimeException("系统繁忙，请稍后重试");
    }
    // ...
}
```

---

## 🎯 总结

### 问题根源
1. **SecurityContext在线程间不传递**（主要原因）
2. **SSE超时时间过短**
3. **请求缓存干扰**

### 解决方案
1. ✅ **传递SecurityContext到异步线程**（核心方案）
2. ✅ **增加超时时间到180秒**
3. ✅ **禁用请求缓存**

### 验证要点
- [ ] 短文本正常返回
- [ ] 长文本正常返回
- [ ] 超过60秒后仍正常工作
- [ ] 无AccessDeniedException错误
- [ ] 内存无泄漏

### 后续优化
- 使用线程池管理异步任务
- 添加监控和限流
- 考虑迁移到WebFlux

---

**修复日期**: 2026-05-05  
**修复版本**: v1.1  
**相关PR**: #SSE-Security-Fix
