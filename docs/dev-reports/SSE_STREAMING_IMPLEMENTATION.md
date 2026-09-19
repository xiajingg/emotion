# SSE流式输出功能实现说明

## 概述
本次修改将"AI情绪日记"项目中的情绪分析功能从同步响应改为SSE（Server-Sent Events）流式输出，实现逐字显示AI分析结果的打字机效果。

---

## 修改内容

### 1. 后端修改

#### 1.1 已有接口
后端已经实现了流式接口 `/user/api/v1/submitTextStream`，位于：
- **文件**: `backend/api/src/main/java/com/emotion/api/controller/UserController.java`
- **行号**: 314-488

#### 1.2 关键特性
- ✅ 使用 `SseEmitter` 实现SSE流式输出
- ✅ 异步处理AI分析请求（新线程）
- ✅ 分三种事件类型：
  - `start`: 开始分析
  - `chunk`: 文本片段（逐字推送）
  - `complete`: 完成（包含完整JSON结果）
  - `error`: 错误信息

#### 1.3 Service层支持
- **文件**: `backend/api/src/main/java/com/emotion/api/service/OllamaChatService.java`
- **方法**: `analyzeEmotionStream(String text)` (第89-99行)
- **返回**: `Flux<String>` 流式文本片段

#### 1.4 DTO支持
- **文件**: `backend/api/src/main/java/com/emotion/api/dto/StreamEmotionResponse.java`
- **作用**: 封装SSE事件响应格式

---

### 2. 前端修改

#### 2.1 utils/request.js - 新增流式请求方法

**新增函数**: `requestStream(options, onChunk, onComplete, onError)`

**功能**:
- 支持SSE格式的流式请求
- 启用分块传输 (`enableChunked: true`)
- 超时时间延长至120秒
- 自动解析SSE事件格式
- 提供三个回调函数：
  - `onChunk`: 接收每个文本片段
  - `onComplete`: 接收完整结果
  - `onError`: 处理错误

**SSE解析函数**: `parseSSEEvents(text)`
- 解析标准SSE格式（`event:` 和 `data:` 字段）
- 支持多事件分割

**导出**: 在 `module.exports` 中新增 `requestStream`

---

#### 2.2 pages/index/index.js - 情绪分析页面

**导入修改**:
```javascript
const { request, loginWithWechat, requestWithLogin, requestStream } = require('../../utils/request');
```

**Data新增字段**:
```javascript
{
  streamingText: '',        // 流式输出的文本
  showStreamingResult: false // 是否显示流式结果
}
```

**submitText() 方法重写**:
- 改用 `requestStream` 替代 `request`
- 调用接口改为 `/user/api/v1/submitTextStream`
- 实现三个回调：
  1. **onChunk**: 逐字追加到 `streamingText`，实时更新UI
  2. **onComplete**: 解析完整JSON，设置最终结果（emotion、emotionRatio、reminder）
  3. **onError**: 显示错误提示

**流程**:
1. 清空之前的结果
2. 发起流式请求
3. 实时接收并显示AI生成的文本
4. 完成后解析JSON，显示结构化结果
5. 刷新剩余次数
6. 滚动到结果区域

---

#### 2.3 pages/index/index.wxml - 模板更新

**结果区域条件修改**:
```html
<!-- 原来 -->
<view class="result-section" wx:if="{{emotion}}">

<!-- 修改后 -->
<view class="result-section" wx:if="{{emotion || showStreamingResult}}">
```

**新增流式输出区域**:
```html
<!-- 流式输出区域 -->
<view class="streaming-area" wx:if="{{showStreamingResult && !emotion}}">
  <view class="streaming-text">{{streamingText}}</view>
  <view class="streaming-cursor">|</view>
</view>

<!-- 最终结果区域 -->
<view class="result-content" wx:if="{{emotion}}">
  <!-- 原有的emotion、emotionRatio、reminder显示 -->
</view>
```

**逻辑**:
- 当 `showStreamingResult=true` 且 `emotion=''` 时，显示流式文本+闪烁光标
- 当 `emotion` 有值时，显示最终的结构化结果

---

#### 2.4 pages/index/index.wxss - 样式新增

**新增样式类**:

1. **.streaming-area**
   - 渐变背景（紫色系）
   - 最小高度120rpx
   - 淡入动画

2. **.streaming-text**
   - 字体大小28rpx
   - 行高1.8（便于阅读）
   - 自动换行

3. **.streaming-cursor**
   - 紫色光标 `|`
   - 闪烁动画（1秒周期）

4. **@keyframes blink**
   - 光标闪烁效果（0-50%显示，51-100%隐藏）

5. **@keyframes fadeIn**
   - 流式区域淡入效果

---

## 技术要点

### 1. SSE vs WebSocket
- **SSE**: 单向通信（服务器→客户端），适合本场景
- **WebSocket**: 双向通信，更复杂
- **选择SSE原因**: 
  - 只需要服务器推送AI生成内容
  - 实现简单，基于HTTP
  - 自动重连机制

### 2. 微信小程序限制
⚠️ **重要**: 微信小程序的 `wx.request` **不原生支持SSE**

**解决方案**:
- 使用 `enableChunked: true` 启用分块传输
- 等待整个响应完成后解析SSE格式
- 这种方式**不是真正的实时流式**，但能模拟效果

**真正的实时方案**（如需）:
- 使用 `wx.connectSocket()` WebSocket
- 后端改造为WebSocket协议
- 或使用轮询方式模拟

### 3. 用户体验优化
- ✅ 流式文本显示时隐藏最终结果
- ✅ 闪烁光标提示正在生成
- ✅ 渐变背景区分流式和最终状态
- ✅ 平滑过渡动画
- ✅ 错误处理友好提示

---

## 测试建议

### 1. 功能测试
- [ ] 输入短文本（5-20字），观察流式输出
- [ ] 输入长文本（100-200字），观察流式输出
- [ ] 检查流式文本是否逐字显示
- [ ] 检查最终结果是否正确解析
- [ ] 检查剩余次数是否正确刷新

### 2. 异常测试
- [ ] 网络断开时的错误提示
- [ ] AI服务异常时的错误处理
- [ ] Token过期时的401处理
- [ ] 文本超过2000字的校验

### 3. 性能测试
- [ ] 多次连续提交，观察内存占用
- [ ] 长时间未响应时的超时处理
- [ ] 快速点击提交按钮的防重复

---

## 已知问题与改进方向

### 当前限制
1. **非真正实时**: 由于微信小程序限制，实际是等待完整响应后解析
2. **用户体验**: 用户看到的是"一次性显示所有文本"而非"逐字出现"

### 改进方案（可选）

#### 方案A: 前端模拟打字机效果
```javascript
// 在onComplete中收到完整JSON后
const fullText = result.reminder; // AI建议文本
let index = 0;
const timer = setInterval(() => {
  if (index < fullText.length) {
    this.setData({
      streamingText: this.data.streamingText + fullText[index]
    });
    index++;
  } else {
    clearInterval(timer);
  }
}, 50); // 每50ms显示一个字
```

#### 方案B: 后端分段返回
修改后端，让AI先生成情绪标签和建议，再逐步返回详细分析

#### 方案C: WebSocket改造
- 后端增加WebSocket端点
- 前端使用 `wx.connectSocket()`
- 实现真正的实时推送

---

## 代码位置汇总

| 文件 | 修改类型 | 说明 |
|-----|---------|------|
| `utils/request.js` | 新增 | `requestStream()` 和 `parseSSEEvents()` |
| `pages/index/index.js` | 修改 | `submitText()` 方法重写，新增data字段 |
| `pages/index/index.wxml` | 修改 | 新增流式输出区域 |
| `pages/index/index.wxss` | 新增 | 流式区域样式和动画 |
| `UserController.java` | 已有 | `/submitTextStream` 接口（无需修改） |
| `OllamaChatService.java` | 已有 | `analyzeEmotionStream()` 方法（无需修改） |
| `StreamEmotionResponse.java` | 已有 | SSE响应DTO（无需修改） |

---

## 部署步骤

1. **后端**: 无需修改，确保服务正常运行
2. **前端**: 
   - 上传修改后的文件到微信开发者工具
   - 编译预览
   - 真机测试
3. **验证**: 
   - 提交心情文本
   - 观察是否有流式输出效果
   - 检查最终结果是否正确

---

## 回滚方案

如需回滚到同步版本：

1. **index.js**: 
   - 恢复 `submitText()` 为原来的 `request()` 调用
   - 删除 `streamingText` 和 `showStreamingResult` 字段

2. **index.wxml**:
   - 删除流式输出区域
   - 恢复 `wx:if="{{emotion}}"`

3. **index.wxss**:
   - 删除 `.streaming-area` 等相关样式

4. **request.js**:
   - 可保留 `requestStream()` 函数（不影响原有功能）

---

**文档版本**: v1.0  
**更新日期**: 2026-05-05  
**作者**: AI Assistant
