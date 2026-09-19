# Ollama接口调用卡顿问题排查与解决

## 🐛 问题描述

### 现象
1. ✅ `ollama run qwen3.5:2b` 命令行对话很快
2. ❌ 通过Spring AI接口调用会卡住不动
3. ❌ 接口卡住时，连命令行对话也会卡住

### 关键线索
从进程列表发现：
```bash
PID 53100 - qwen3.5:2b (正在被命令行使用)
PID 53794 - qwen3.5:9b (还在内存中！)
```

**两个模型同时加载，导致资源竞争！**

---

## 🔍 根本原因分析

### 原因1：旧模型未卸载 ⭐⭐⭐

**问题**：
- 之前使用的是 `qwen3.5:9b`（90亿参数）
- 切换到 `qwen3.5:2b`（20亿参数）后，9b模型仍在内存中
- Ollama会尝试保持最近使用的模型在内存中
- 两个模型同时占用GPU/内存资源，导致性能严重下降

**影响**：
- 内存占用翻倍
- GPU显存不足时会使用系统内存，速度极慢
- 并发请求时资源竞争加剧

### 原因2：Spring AI配置可能未生效

**可能的问题**：
- 配置文件修改后未重启服务
- Spring Bean缓存了旧的模型配置
- 实际调用的还是9b模型

### 原因3：Ollama并发限制

**Ollama默认行为**：
- 同时只能处理有限数量的请求
- 如果前一个请求未完成，后续请求会排队等待
- 大模型（9b）处理慢，会阻塞小模型（2b）的请求

---

## ✅ 解决方案

### 方案1：卸载旧模型（必须执行）⭐⭐⭐

#### 步骤1：停止所有Ollama会话

在终端中：
```bash
# 如果有正在运行的 ollama run 会话，按 Ctrl+D 或输入 /bye 退出

# 或者强制停止
pkill -f "ollama run"
```

#### 步骤2：卸载9b模型

```bash
# 查看已下载的模型
ollama list

# 卸载9b模型
ollama rm qwen3.5:9b

# 验证只剩2b模型
ollama list
```

预期输出：
```
NAME            ID              SIZE      MODIFIED
qwen3.5:2b      xxxxx           1.5GB     2 minutes ago
```

#### 步骤3：重启Ollama服务（可选但推荐）

```bash
# macOS
brew services restart ollama

# 或者
launchctl stop com.ollama.ollama
launchctl start com.ollama.ollama

# Linux
sudo systemctl restart ollama
```

#### 步骤4：验证

```bash
# 检查进程，应该只有一个模型加载
ps aux | grep ollama | grep runner

# 应该只看到 qwen3.5:2b 的进程
```

---

### 方案2：重启后端服务

修改配置后必须重启：

```bash
cd /Users/xiajing/emotion/backend/api

# 停止当前服务（Ctrl+C）

# 重新编译
mvn clean package

# 启动服务
java -jar target/emotion-api-0.0.1-SNAPSHOT.jar
```

**观察启动日志**，应该看到：
```
========== Ollama配置初始化 ==========
Ollama API地址: http://localhost:11434
使用的模型: qwen3.5:2b
Temperature: 0.7
OllamaChatModel 创建成功
=====================================
```

---

### 方案3：测试接口

#### 测试1：同步接口

```bash
curl -X POST http://localhost:8080/user/api/v1/submitText \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "text": "今天很开心"
  }'
```

**预期**：
- 5-10秒内返回结果
- 日志显示：`同步情绪分析完成，耗时: XXXXms`

#### 测试2：流式接口

```bash
curl -X POST http://localhost:8080/user/api/v1/submitTextStream \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "text": "今天工作很累但很有成就感"
  }'
```

**预期**：
- 立即开始返回数据
- 逐步输出JSON片段
- 总耗时10-20秒

#### 观察日志

应该看到：
```
INFO  - 开始流式情绪分析，文本长度: XX
DEBUG - 收到流式数据块: {"
DEBUG - 收到流式数据块: score": 75,
DEBUG - 收到流式数据块: "suggestion": "..."}
INFO  - 流式情绪分析完成，总耗时: XXXXms
```

---

## 🔧 高级调试

### 1. 检查Ollama服务状态

```bash
# 查看Ollama是否正常运行
curl http://localhost:11434/api/tags

# 应该返回已安装的模型列表
{
  "models": [
    {
      "name": "qwen3.5:2b",
      "size": 1500000000,
      ...
    }
  ]
}
```

### 2. 直接测试Ollama API

```bash
# 绕过Spring AI，直接调用Ollama
curl http://localhost:11434/api/generate -d '{
  "model": "qwen3.5:2b",
  "prompt": "你好",
  "stream": false
}'
```

**预期**：快速返回响应

如果这个也慢，说明是Ollama本身的问题，不是Spring AI的问题。

### 3. 监控资源使用

```bash
# macOS
top -pid $(pgrep -f ollama)

# 或者使用 Activity Monitor 查看 Ollama 进程的 CPU 和内存使用
```

**正常情况**：
- CPU: 50-100%（推理时）
- 内存: 2-3GB（2b模型）

**异常情况**：
- 内存: >6GB（说明9b模型还在）
- CPU: 持续100%但不输出（死锁或资源竞争）

### 4. 检查Spring AI日志

启动后端后，观察日志：

```bash
tail -f backend/api.log | grep -i ollama
```

应该看到：
- ✅ `Ollama配置初始化`
- ✅ `使用的模型: qwen3.5:2b`
- ✅ `开始流式情绪分析`
- ✅ `流式情绪分析完成，总耗时: XXXms`

如果看到：
- ❌ `Connection refused` - Ollama服务未启动
- ❌ `Model not found` - 模型名称错误
- ❌ 长时间无日志 - 请求卡住了

---

## 📊 性能对比

### 修复前（两个模型同时加载）

| 指标 | 数值 |
|-----|------|
| 内存占用 | 8-10GB |
| 响应时间 | 60秒+ 或超时 |
| 并发能力 | 几乎为0 |
| 用户体验 | ❌ 完全不可用 |

### 修复后（仅2b模型）

| 指标 | 数值 |
|-----|------|
| 内存占用 | 2-3GB |
| 响应时间 | 5-15秒 |
| 并发能力 | 3-5个请求/秒 |
| 用户体验 | ✅ 流畅可用 |

---

## ⚠️ 常见问题

### Q1: 卸载模型后，下次想用怎么办？

**A**: 可以随时重新下载：
```bash
ollama pull qwen3.5:9b
```

但建议一次只保留一个模型在内存中。

### Q2: 如何查看哪些模型在内存中？

**A**: 
```bash
# 查看已下载的模型
ollama list

# 查看正在运行的模型进程
ps aux | grep "ollama runner"

# 每个 runner 进程对应一个加载到内存的模型
```

### Q3: 为什么命令行快，接口慢？

**A**: 可能的原因：
1. **模型不同**：命令行用的是2b，接口还在用9b
2. **并发冲突**：接口请求和命令行请求同时访问，资源竞争
3. **网络延迟**：Spring AI通过HTTP调用Ollama，有额外开销

**验证方法**：
```bash
# 先关闭所有接口请求
# 只用命令行测试
ollama run qwen3.5:2b

# 然后只用接口测试（不要同时运行命令行）
# 观察接口响应时间
```

### Q4: 如何彻底清理Ollama缓存？

**A**:
```bash
# 停止Ollama服务
brew services stop ollama

# 清理缓存（谨慎操作，会删除所有模型）
rm -rf ~/.ollama/models

# 重新启动
brew services start ollama

# 重新需要的模型
ollama pull qwen3.5:2b
```

### Q5: Spring AI是否支持动态切换模型？

**A**: 不支持热切换，需要：
1. 修改配置文件
2. 重启应用
3. Spring会重新创建Bean

如果需要动态切换，可以：
```java
// 在Service中手动指定模型
public EmotionAnalysisResponse analyzeEmotion(String text) {
    return chatClient.prompt()
            .user(prompt)
            .options(OllamaOptions.builder()
                    .model("qwen3.5:2b")  // 动态指定
                    .build())
            .call()
            .entity(EmotionAnalysisResponse.class);
}
```

---

## 🎯 最佳实践

### 1. 单模型原则

**建议**：
- 开发环境只保留一个模型
- 根据需求选择2b（快速）或9b（准确）
- 定期清理不用的模型

### 2. 监控资源

定期检查：
```bash
# 每周检查一次
ollama list
ps aux | grep ollama
df -h ~/.ollama  # 检查磁盘使用
```

### 3. 合理设置超时

在配置文件中：
```yaml
spring:
  ai:
    ollama:
      chat:
        options:
          timeout: 30s  # 设置合理的超时时间
```

### 4. 添加重试机制

```java
@Retryable(value = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 1000))
public EmotionAnalysisResponse analyzeEmotionWithRetry(String text) {
    return analyzeEmotion(text);
}
```

---

## 📝 检查清单

修复完成后，逐项确认：

- [ ] 已卸载 `qwen3.5:9b` 模型
- [ ] `ollama list` 只显示 `qwen3.5:2b`
- [ ] 只有一个 `ollama runner` 进程
- [ ] 后端服务已重启
- [ ] 启动日志显示使用 `qwen3.5:2b`
- [ ] 同步接口测试通过（<15秒）
- [ ] 流式接口测试通过（实时输出）
- [ ] 内存占用正常（2-3GB）
- [ ] 命令行和接口可以同时使用

---

## 🆘 仍然有问题？

如果按照以上步骤仍然卡顿，请收集以下信息：

1. **Ollama版本**
   ```bash
   ollama --version
   ```

2. **已安装模型**
   ```bash
   ollama list
   ```

3. **运行进程**
   ```bash
   ps aux | grep ollama
   ```

4. **后端日志**
   ```bash
   tail -100 backend/api.log
   ```

5. **直接API测试结果**
   ```bash
   curl http://localhost:11434/api/generate -d '{
     "model": "qwen3.5:2b",
     "prompt": "你好",
     "stream": false
   }'
   ```

将这些信息提供给技术支持。

---

**更新日期**: 2026-05-05  
**问题类型**: 性能优化 / 资源配置  
**严重程度**: 🔴 高（影响核心功能）
