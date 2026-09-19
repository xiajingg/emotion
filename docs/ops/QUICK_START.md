# 流式分析功能 - 快速开始指南

## 🚀 5分钟快速体验

### 第一步: 确保 Ollama 服务运行

```bash
# 检查 Ollama 是否运行
curl http://localhost:11434/api/tags

# 如果没有运行,启动 Ollama
ollama serve

# 确保已拉取模型
ollama pull qwen3.5:9b
```

### 第二步: 启动后端服务

```bash
cd /Users/xiajing/emotion/backend

# 编译项目
mvn clean package -DskipTests

# 启动服务
java -jar api/target/emotion-api-0.0.1-SNAPSHOT.jar
```

或者在 IDEA 中直接运行 `EmotionApplication`。

### 第三步: 测试流式接口

#### 方法 1: 使用提供的测试脚本

```bash
# 编辑脚本,替换 JWT_TOKEN
vim test_stream_api.sh

# 运行测试
./scripts/test_stream_api.sh
```

#### 方法 2: 使用 curl 手动测试

```bash
# 先获取一个有效的 JWT token (通过登录接口)
TOKEN="your_jwt_token_here"

# 测试流式接口
curl -X POST http://localhost:8080/user/api/v1/submitTextStream \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"text":"今天心情很好","type":0}' \
  -N
```

你会看到类似这样的输出:
```
event: start
data: {"type":"chunk","data":"开始分析...","timestamp":1234567890}

event: chunk
data: {"type":"chunk","data":"{\"","timestamp":1234567891}

event: chunk
data: {"type":"chunk","data":"score\":75","timestamp":1234567892}

event: chunk
data: {"type":"chunk","data:",\"suggestion\":\"保持好心情\"}","timestamp":1234567893}

event: complete
data: {"type":"complete","data":"{\"emotion\":\"开心\",\"emotionRatio\":75,\"reminder\":\"保持好心情哦\"}","timestamp":1234567894}
```

#### 方法 3: 使用 Postman/Apifox

1. 创建新请求
2. 设置:
   - Method: POST
   - URL: `http://localhost:8080/user/api/v1/submitTextStream`
   - Headers:
     - `Content-Type: application/json`
     - `Authorization: Bearer YOUR_TOKEN`
     - `Accept: text/event-stream`
   - Body (raw JSON):
     ```json
     {
       "text": "今天心情很好",
       "type": 0
     }
     ```
3. 点击 Send,选择 "Stream" 视图查看实时输出

### 第四步: 前端接入

参考 [STREAM_API_GUIDE.md](./STREAM_API_GUIDE.md) 中的微信小程序示例代码。

核心步骤:
1. 复制 `pages/garden/garden.js` 中的 `submitTextStream()` 方法
2. 复制 WXML 和 WXSS 样式
3. 替换 API 地址为你的后端域名
4. 在微信开发者工具中测试

---

## 🔍 常见问题排查

### Q1: 连接被拒绝 (Connection refused)

**原因**: 后端服务未启动或端口不对

**解决**:
```bash
# 检查服务是否运行
ps aux | grep emotion-api

# 检查端口占用
lsof -i :8080

# 启动服务
java -jar api/target/emotion-api-0.0.1-SNAPSHOT.jar
```

### Q2: Ollama 连接失败

**原因**: Ollama 服务未启动

**解决**:
```bash
# 启动 Ollama
ollama serve

# 验证 Ollama 是否正常
curl http://localhost:11434/api/tags
```

### Q3: JWT Token 无效

**原因**: Token 过期或未提供

**解决**:
```bash
# 先调用登录接口获取 token
curl -X POST http://localhost:8080/user/login \
  -H "Content-Type: application/json" \
  -d '{"username":"test","password":"123456"}'

# 返回的 token 用于后续请求
```

### Q4: SSE 连接立即断开

**原因**: 
- 参数校验失败
- 用户已签到
- 文本过长

**解决**: 检查返回的 error 事件:
```
event: error
data: {"type":"error","data":"已签到过","timestamp":1234567890}
```

### Q5: 前端收不到流式数据

**原因**: 
- 小程序版本过低不支持 `enableChunked`
- 未配置合法域名

**解决**:
1. 更新微信客户端到最新版本
2. 在微信公众平台配置服务器域名
3. 开发时开启"不校验合法域名"

---

## 📊 监控和调试

### 查看后端日志

```bash
# 实时查看日志
tail -f emotion-api.log

# 搜索特定关键词
grep "流式分析" emotion-api.log
grep "Ollama" emotion-api.log
```

### 关键日志示例

```
2024-05-05 10:30:15 INFO  UserController - 流式分析请求: {"userId":123,"userOpenId":"xxx"} : {"text":"今天心情很好","type":0}
2024-05-05 10:30:16 INFO  OllamaChatService - 开始流式情绪分析
2024-05-05 10:30:20 INFO  UserController - 流式分析完成,保存数据库
```

### 性能监控

```bash
# 监控 JVM 内存
jstat -gc <pid> 1000

# 监控线程
jstack <pid> | grep "SseEmitter"

# 监控 HTTP 连接
netstat -an | grep 8080
```

---

## 🎯 下一步

1. **阅读完整文档**: [STREAM_IMPLEMENTATION_SUMMARY.md](../dev-reports/STREAM_IMPLEMENTATION_SUMMARY.md)
2. **前端接入**: [STREAM_API_GUIDE.md](./STREAM_API_GUIDE.md)
3. **运行单元测试**: `mvn test -Dtest=StreamEmotionTest`
4. **真机测试**: 在真实设备上测试小程序流式功能
5. **性能优化**: 根据实际使用情况调整参数

---

## 📞 需要帮助?

- 查看完整文档: [STREAM_IMPLEMENTATION_SUMMARY.md](../dev-reports/STREAM_IMPLEMENTATION_SUMMARY.md)
- 前端接入指南: [STREAM_API_GUIDE.md](./STREAM_API_GUIDE.md)
- 检查后端日志: `emotion-api.log`
- 测试脚本: `test_stream_api.sh`

祝使用愉快! 🎉
