# 心灵花园流式分析功能 - 前端接入指南

## 后端接口说明

### 接口地址
```
POST /user/api/v1/submitTextStream
Content-Type: application/json
Accept: text/event-stream
```

### 请求参数
```json
{
  "text": "用户输入的文本内容",
  "type": 0,  // 0:普通提交, 1:签到提交
  "supplementarySignIn": 0,  // 0:非补签, 1:补签
  "supplementarySignInTime": ""  // 补签时间，格式：yyyy-MM-dd HH:mm:ss
}
```

### SSE 事件类型

#### 1. start 事件 - 开始分析
```javascript
event: start
data: {"type":"chunk","data":"开始分析...","timestamp":1234567890}
```

#### 2. chunk 事件 - 文本片段（实时推送）
```javascript
event: chunk
data: {"type":"chunk","data":"{\"score\":","timestamp":1234567891}
```

#### 3. complete 事件 - 分析完成
```javascript
event: complete
data: {"type":"complete","data":"{\"emotion\":\"开心\",\"emotionRatio\":75,\"reminder\":\"保持好心情哦\"}","timestamp":1234567892}
```

#### 4. error 事件 - 错误信息
```javascript
event: error
data: {"type":"error","data":"输入文本过长","timestamp":1234567893}
```

---

## 微信小程序前端实现示例

### 方法一：使用 wx.request 配合 SSE（推荐）

由于微信小程序原生不支持 EventSource，需要手动实现 SSE 解析：

```javascript
// pages/garden/garden.js
Page({
  data: {
    userInput: '',
    analysisResult: '',
    isAnalyzing: false,
    streamContent: ''  // 用于累积流式内容
  },

  // 提交文本进行流式分析
  submitTextStream() {
    const that = this;
    const { userInput } = that.data;
    
    if (!userInput || userInput.trim() === '') {
      wx.showToast({ title: '请输入内容', icon: 'none' });
      return;
    }

    that.setData({ 
      isAnalyzing: true,
      streamContent: '',
      analysisResult: ''
    });

    // 显示加载提示
    wx.showLoading({ title: 'AI分析中...' });

    // 发起请求
    const requestTask = wx.request({
      url: 'https://your-domain.com/user/api/v1/submitTextStream',
      method: 'POST',
      header: {
        'Content-Type': 'application/json',
        'Authorization': 'Bearer ' + wx.getStorageSync('token')  // JWT token
      },
      data: {
        text: userInput,
        type: 0  // 普通提交
      },
      enableChunked: true,  // 启用分块传输
      responseType: 'text',  // 响应类型为文本
      success(res) {
        console.log('请求成功', res);
      },
      fail(err) {
        console.error('请求失败', err);
        wx.hideLoading();
        that.setData({ isAnalyzing: false });
        wx.showToast({ title: '网络请求失败', icon: 'none' });
      }
    });

    // 监听数据接收
    requestTask.onChunkReceived((res) => {
      const uint8Array = res.data;
      const decoder = new TextDecoder('utf-8');
      const text = decoder.decode(uint8Array);
      
      console.log('收到数据块:', text);
      
      // 解析SSE格式的数据
      that.parseSSEData(text);
    });
  },

  // 解析SSE数据
  parseSSEData(sseData) {
    const that = this;
    
    // SSE数据格式：event: xxx\ndata: {...}\n\n
    const lines = sseData.split('\n');
    
    let currentEvent = '';
    let currentData = '';
    
    for (let line of lines) {
      line = line.trim();
      
      if (line.startsWith('event:')) {
        currentEvent = line.substring(6).trim();
      } else if (line.startsWith('data:')) {
        currentData = line.substring(5).trim();
        
        // 如果当前行是空行，表示一个完整的事件结束
        if (currentData === '') {
          that.handleSSEEvent(currentEvent, currentData);
          currentEvent = '';
          currentData = '';
        }
      } else if (line === '') {
        // 空行表示事件结束，处理之前收集的数据
        if (currentEvent && currentData) {
          that.handleSSEEvent(currentEvent, currentData);
          currentEvent = '';
          currentData = '';
        }
      }
    }
    
    // 处理最后可能未结束的事件
    if (currentEvent && currentData) {
      that.handleSSEEvent(currentEvent, currentData);
    }
  },

  // 处理SSE事件
  handleSSEEvent(eventType, dataStr) {
    const that = this;
    
    try {
      const eventData = JSON.parse(dataStr);
      
      switch (eventType) {
        case 'start':
          console.log('开始分析');
          break;
          
        case 'chunk':
          // 实时显示AI生成的文本片段
          that.setData({
            streamContent: that.data.streamContent + eventData.data
          });
          break;
          
        case 'complete':
          // 分析完成，显示最终结果
          wx.hideLoading();
          const result = JSON.parse(eventData.data);
          that.setData({
            analysisResult: result,
            isAnalyzing: false
          });
          
          // 显示成功提示
          wx.showToast({ 
            title: '分析完成', 
            icon: 'success' 
          });
          break;
          
        case 'error':
          // 错误处理
          wx.hideLoading();
          that.setData({ isAnalyzing: false });
          wx.showToast({ 
            title: eventData.data || '分析失败', 
            icon: 'none' 
          });
          break;
      }
    } catch (e) {
      console.error('解析SSE数据失败', e, dataStr);
    }
  },

  // 输入框变化
  onInputChange(e) {
    this.setData({
      userInput: e.detail.value
    });
  }
});
```

### 方法二：简化版（不使用真正的流式，但模拟流式效果）

如果小程序环境对 SSE 支持不好，可以使用传统的 HTTP 请求，然后在前端模拟打字机效果：

```javascript
// pages/garden/garden.js
Page({
  data: {
    userInput: '',
    analysisResult: null,
    isAnalyzing: false,
    displayText: ''  // 用于打字机效果显示
  },

  // 提交文本进行分析（传统方式）
  submitText() {
    const that = this;
    const { userInput } = that.data;
    
    if (!userInput || userInput.trim() === '') {
      wx.showToast({ title: '请输入内容', icon: 'none' });
      return;
    }

    that.setData({ 
      isAnalyzing: true,
      displayText: '',
      analysisResult: null
    });

    wx.showLoading({ title: 'AI分析中...' });

    wx.request({
      url: 'https://your-domain.com/user/api/v1/submitText',  // 使用非流式接口
      method: 'POST',
      header: {
        'Content-Type': 'application/json',
        'Authorization': 'Bearer ' + wx.getStorageSync('token')
      },
      data: {
        text: userInput,
        type: 0
      },
      success(res) {
        wx.hideLoading();
        
        if (res.statusCode === 200 && res.data.code === 200) {
          const result = res.data.data;
          that.setData({
            analysisResult: result,
            isAnalyzing: false
          });
          
          // 模拟打字机效果显示建议
          that.typewriterEffect(result.reminder);
          
          wx.showToast({ 
            title: '分析完成', 
            icon: 'success' 
          });
        } else {
          wx.showToast({ 
            title: res.data.message || '分析失败', 
            icon: 'none' 
          });
        }
      },
      fail(err) {
        wx.hideLoading();
        that.setData({ isAnalyzing: false });
        wx.showToast({ title: '网络请求失败', icon: 'none' });
      }
    });
  },

  // 打字机效果
  typewriterEffect(text) {
    const that = this;
    let index = 0;
    
    const timer = setInterval(() => {
      if (index < text.length) {
        that.setData({
          displayText: that.data.displayText + text.charAt(index)
        });
        index++;
      } else {
        clearInterval(timer);
      }
    }, 50);  // 每50ms显示一个字符
  },

  onInputChange(e) {
    this.setData({
      userInput: e.detail.value
    });
  }
});
```

### WXML 模板示例

```xml
<!-- pages/garden/garden.wxml -->
<view class="garden-container">
  <!-- 输入区域 -->
  <view class="input-section">
    <textarea 
      class="input-textarea" 
      placeholder="分享你的心情..." 
      value="{{userInput}}"
      bindinput="onInputChange"
      maxlength="2000"
    />
    <button 
      class="submit-btn" 
      bindtap="submitTextStream"
      disabled="{{isAnalyzing}}"
    >
      {{isAnalyzing ? '分析中...' : '开始分析'}}
    </button>
  </view>

  <!-- 流式输出展示区域 -->
  <view class="result-section" wx:if="{{streamContent || analysisResult}}">
    <view class="result-title">分析结果</view>
    
    <!-- 情绪标签和分数 -->
    <view class="emotion-info" wx:if="{{analysisResult}}">
      <view class="emotion-tag">{{analysisResult.emotion}}</view>
      <view class="emotion-score">情绪指数: {{analysisResult.emotionRatio}}</view>
    </view>
    
    <!-- 流式文本显示（打字机效果） -->
    <view class="stream-text" wx:if="{{streamContent}}">
      <text>{{streamContent}}</text>
    </view>
    
    <!-- 最终建议 -->
    <view class="suggestion" wx:if="{{analysisResult}}">
      <text class="suggestion-label">💡 建议：</text>
      <text class="suggestion-content">{{analysisResult.reminder}}</text>
    </view>
  </view>
</view>
```

### WXSS 样式示例

```css
/* pages/garden/garden.wxss */
.garden-container {
  padding: 20rpx;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.input-section {
  background: white;
  border-radius: 20rpx;
  padding: 30rpx;
  margin-bottom: 30rpx;
}

.input-textarea {
  width: 100%;
  min-height: 200rpx;
  padding: 20rpx;
  font-size: 28rpx;
  border: 2rpx solid #e0e0e0;
  border-radius: 10rpx;
  margin-bottom: 20rpx;
}

.submit-btn {
  width: 100%;
  background: linear-gradient(90deg, #667eea, #764ba2);
  color: white;
  border-radius: 10rpx;
}

.submit-btn[disabled] {
  background: #ccc;
}

.result-section {
  background: white;
  border-radius: 20rpx;
  padding: 30rpx;
}

.result-title {
  font-size: 32rpx;
  font-weight: bold;
  margin-bottom: 20rpx;
  color: #333;
}

.emotion-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20rpx;
  padding: 20rpx;
  background: #f5f5f5;
  border-radius: 10rpx;
}

.emotion-tag {
  font-size: 36rpx;
  font-weight: bold;
  color: #667eea;
}

.emotion-score {
  font-size: 28rpx;
  color: #666;
}

.stream-text {
  padding: 20rpx;
  background: #f9f9f9;
  border-radius: 10rpx;
  margin-bottom: 20rpx;
  line-height: 1.6;
  font-size: 28rpx;
  color: #333;
}

.suggestion {
  padding: 20rpx;
  background: #fff9e6;
  border-left: 4rpx solid #ffc107;
  border-radius: 10rpx;
}

.suggestion-label {
  font-weight: bold;
  color: #ff9800;
}

.suggestion-content {
  font-size: 28rpx;
  color: #666;
  line-height: 1.6;
}
```

---

## 注意事项

### 1. Token 认证
确保在请求头中携带有效的 JWT token：
```javascript
'Authorization': 'Bearer ' + wx.getStorageSync('token')
```

### 2. 域名配置
在微信小程序后台配置合法的服务器域名：
- 登录微信公众平台
- 开发 -> 开发管理 -> 开发设置 -> 服务器域名
- 添加你的后端域名到 request 合法域名

### 3. 超时处理
SSE 连接可能因为网络问题中断，建议添加重连机制：
```javascript
let retryCount = 0;
const maxRetries = 3;

function connectWithRetry() {
  if (retryCount >= maxRetries) {
    wx.showToast({ title: '连接失败，请稍后重试', icon: 'none' });
    return;
  }
  
  // 发起请求
  // ...
  
  // 如果连接断开，尝试重连
  retryCount++;
  setTimeout(connectWithRetry, 2000 * retryCount);
}
```

### 4. 内存管理
及时清理定时器和不必要的引用，避免内存泄漏：
```javascript
onUnload() {
  // 页面卸载时清理资源
  if (this.typewriterTimer) {
    clearInterval(this.typewriterTimer);
  }
}
```

### 5. 用户体验优化
- 添加加载动画
- 显示实时进度
- 提供取消分析的选项
- 缓存历史分析结果

---

## 测试建议

### 1. 本地测试
使用微信开发者工具进行本地调试：
- 开启"不校验合法域名"选项
- 查看 Network 面板确认 SSE 连接正常

### 2. 真机测试
- 测试不同网络环境（WiFi、4G、5G）
- 测试长时间连接的稳定性
- 测试异常情况（网络断开、服务器错误等）

### 3. 性能测试
- 监控内存使用情况
- 测试并发请求的处理能力
- 验证流式输出的流畅度

---

## 常见问题

### Q1: 小程序不支持 EventSource 怎么办？
A: 使用 `wx.request` 的 `enableChunked: true` 和 `onChunkReceived` 来手动实现 SSE 解析。

### Q2: 流式数据解析不完整怎么办？
A: 确保正确处理 SSE 格式的空行分隔符，并处理最后一个可能未完成的事件。

### Q3: 如何实现打字机效果？
A: 使用 `setInterval` 逐字显示文本，或使用 CSS 动画。

### Q4: SSE 连接超时怎么办？
A: 后端设置合理的超时时间（如60秒），前端实现重连机制。

---

## 总结

流式输出可以显著提升用户体验，让用户实时看到 AI 分析的过程。虽然微信小程序对 SSE 的支持有限，但通过 `enableChunked` 和手动解析，仍然可以实现类似的效果。

如果流式实现过于复杂，也可以考虑使用传统的 HTTP 请求配合前端的打字机动画来模拟流式效果。
