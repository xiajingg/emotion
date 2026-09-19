# 心灵花园流式输出 - 前端修改指南

## 📍 需要修改的文件

**文件路径**: `/Users/xiajing/WebstormProjects/miniprogram/pages/index/index.js`

---

## 🔧 修改步骤

### 第一步：修改 data 字段

在 `data` 中添加流式输出相关的字段：

```javascript
Page({
  data: {
    textData: '',
    isAnalyzing: false,
    emotion: '',
    emotionRatio: '',
    reminder: '',
    remainingUses: 0,
    totalUses: 0,
    today: '',
    loading: false,
    loadingText: '加载中...',
    rewardedVideoAd: null,
    clickCount: 0,
    // 快速打卡相关
    todayCheckedIn: false,
    consecutiveDays: 0,
    
    // === 新增：流式输出相关字段 ===
    streamContent: '',      // 流式输出的原始内容（用于调试）
    displayReminder: '',    // 打字机效果显示的建议文本
    analysisComplete: false // 分析是否完成
  },
  // ...
});
```

---

### 第二步：替换 submitText 方法

将原有的 `submitText()` 方法（第85-125行）**完全替换**为以下代码：

```javascript
// ---- 提交文本进行情绪分析（流式版本）----
async submitText() {
  if (this.data.textData.trim().length < 5) {
    wx.showToast({ title: '心情描述至少5个字', icon: 'none' });
    return;
  }

  if (this.data.isAnalyzing) return;

  // 重置状态
  this.setData({ 
    isAnalyzing: true,
    streamContent: '',
    displayReminder: '',
    emotion: '',
    emotionRatio: '',
    reminder: '',
    analysisComplete: false
  });

  // 显示加载提示
  wx.showLoading({ title: 'AI分析中...' });

  try {
    // 发起流式请求
    const requestTask = wx.request({
      url: '/user/api/v1/submitTextStream',  // 使用流式接口
      method: 'POST',
      header: {
        'Content-Type': 'application/json'
      },
      data: {
        text: this.data.textData,
        type: 0  // 普通提交
      },
      enableChunked: true,   // 关键：启用分块传输
      responseType: 'text',  // 响应类型为文本
      success: (res) => {
        console.log('流式请求成功', res);
      },
      fail: (err) => {
        console.error('流式请求失败', err);
        wx.hideLoading();
        this.setData({ isAnalyzing: false });
        wx.showToast({ title: '网络请求失败', icon: 'none' });
      }
    });

    // 监听数据块接收
    requestTask.onChunkReceived((res) => {
      const uint8Array = res.data;
      const decoder = new TextDecoder('utf-8');
      const text = decoder.decode(uint8Array);
      
      console.log('收到数据块:', text);
      
      // 解析SSE格式的数据
      this.parseSSEData(text);
    });

  } catch (err) {
    console.error('分析失败:', err);
    wx.hideLoading();
    this.setData({ isAnalyzing: false });
    wx.showToast({ title: '分析失败，请重试', icon: 'none' });
  }
},
```

---

### 第三步：添加 SSE 解析方法

在 `submitText()` 方法之后，添加以下两个新方法：

```javascript
// ---- 解析SSE数据 ----
parseSSEData(sseData) {
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
        this.handleSSEEvent(currentEvent, currentData);
        currentEvent = '';
        currentData = '';
      }
    } else if (line === '') {
      // 空行表示事件结束，处理之前收集的数据
      if (currentEvent && currentData) {
        this.handleSSEEvent(currentEvent, currentData);
        currentEvent = '';
        currentData = '';
      }
    }
  }
  
  // 处理最后可能未结束的事件
  if (currentEvent && currentData) {
    this.handleSSEEvent(currentEvent, currentData);
  }
},

// ---- 处理SSE事件 ----
handleSSEEvent(eventType, dataStr) {
  try {
    // 跳过空数据
    if (!dataStr || dataStr.trim() === '') return;
    
    const eventData = JSON.parse(dataStr);
    
    switch (eventType) {
      case 'start':
        console.log('开始分析');
        break;
        
      case 'chunk':
        // 实时显示AI生成的文本片段（累积显示）
        const newContent = this.data.streamContent + eventData.data;
        this.setData({
          streamContent: newContent
        });
        break;
        
      case 'complete':
        // 分析完成，隐藏loading，显示最终结果
        wx.hideLoading();
        
        try {
          const result = JSON.parse(eventData.data);
          
          this.setData({
            emotion: result.emotion || '未知',
            emotionRatio: (result.emotionRatio || '0') + '%',
            reminder: result.reminder || '暂无建议',
            analysisComplete: true,
            isAnalyzing: false
          });
          
          // 对建议文本使用打字机效果
          this.typeWriterTo('displayReminder', result.reminder || '暂无建议', 50);
          
          // 刷新剩余次数
          this.getRemainingUses();
          
          // 滚动到分析结果区域
          setTimeout(() => {
            wx.pageScrollTo({
              selector: '#resultSection',
              duration: 300
            });
          }, 100);
          
          wx.showToast({ 
            title: '分析完成', 
            icon: 'success' 
          });
        } catch (e) {
          console.error('解析完整结果失败', e, eventData.data);
        }
        break;
        
      case 'error':
        // 错误处理
        wx.hideLoading();
        this.setData({ isAnalyzing: false });
        wx.showToast({ 
          title: eventData.data || '分析失败', 
          icon: 'none' 
        });
        break;
        
      default:
        console.warn('未知事件类型:', eventType);
    }
  } catch (e) {
    console.error('解析SSE数据失败', e, dataStr);
  }
},
```

---

### 第四步：修改 WXML 显示逻辑

**文件路径**: `/Users/xiajing/WebstormProjects/miniprogram/pages/index/index.wxml`

找到第96-117行的结果展示部分，修改为：

```xml
<!-- 分析结果 -->
<view class="result-section" id="resultSection" wx:if="{{emotion || streamContent}}">
  <view class="result-card">
    <view class="result-header">
      <text class="result-icon">📊</text>
      <text class="result-title">分析结果</text>
    </view>
    <view class="result-content">
      <!-- 情感类型和指数（分析完成后显示） -->
      <view class="result-item" wx:if="{{analysisComplete}}">
        <text class="result-label">情感类型：</text>
        <text class="result-value">{{emotion}}</text>
      </view>
      <view class="result-item" wx:if="{{analysisComplete}}">
        <text class="result-label">情感指数：</text>
        <text class="result-value">{{emotionRatio}}</text>
      </view>
      
      <!-- 流式输出过程（分析中显示） -->
      <view class="result-item stream-text" wx:if="{{streamContent && !analysisComplete}}">
        <text class="result-label">AI思考中：</text>
        <text class="result-value stream-content">{{streamContent}}</text>
      </view>
      
      <!-- 情感建议（打字机效果） -->
      <view class="result-item" wx:if="{{displayReminder || (analysisComplete && reminder)}}">
        <text class="result-label">情感建议：</text>
        <text class="result-value">{{displayReminder || reminder}}</text>
      </view>
    </view>
  </view>
</view>
```

---

### 第五步：添加流式文本样式

**文件路径**: `/Users/xiajing/WebstormProjects/miniprogram/pages/index/index.wxss`

在文件末尾添加以下样式：

```css
/* 流式输出文本样式 */
.stream-text {
  background: #f0f4ff;
  padding: 20rpx;
  border-radius: 12rpx;
  margin: 10rpx 0;
}

.stream-content {
  color: #667eea;
  font-style: italic;
  word-break: break-all;
  line-height: 1.6;
}

/* 打字机光标效果 */
.result-value {
  position: relative;
}

.analysis-complete .result-value::after {
  content: '|';
  animation: blink 1s infinite;
  color: #667eea;
}

@keyframes blink {
  0%, 50% { opacity: 1; }
  51%, 100% { opacity: 0; }
}
```

---

## ✅ 修改完成检查清单

- [ ] 已添加 `streamContent`, `displayReminder`, `analysisComplete` 到 data
- [ ] 已替换 `submitText()` 方法为流式版本
- [ ] 已添加 `parseSSEData()` 方法
- [ ] 已添加 `handleSSEEvent()` 方法
- [ ] 已修改 WXML 中的结果显示逻辑
- [ ] 已添加流式文本的 CSS 样式
- [ ] 已将接口地址从 `/user/api/v1/submitText` 改为 `/user/api/v1/submitTextStream`

---

## 🧪 测试步骤

### 1. 本地测试（微信开发者工具）

1. 打开微信开发者工具
2. 加载项目 `/Users/xiajing/WebstormProjects/miniprogram`
3. 开启"不校验合法域名、web-view（业务域名）、TLS 版本以及 HTTPS 证书"
4. 在"心灵花园"页面输入文本并点击"提交开始分析"
5. 观察 Console 面板，应该看到：
   ```
   收到数据块: event: start...
   收到数据块: event: chunk...
   收到数据块: event: complete...
   ```
6. 观察 UI，应该看到：
   - AI 思考过程实时显示
   - 分析完成后显示情感类型、指数和建议
   - 建议文本有打字机效果

### 2. 真机测试

1. 在微信公众平台配置后端域名为合法域名
2. 编译上传代码
3. 使用体验版或正式版在真机上测试
4. 测试不同网络环境（WiFi、4G、5G）

---

## 🔍 常见问题排查

### Q1: 收不到数据块

**可能原因**:
- 后端服务未启动
- 接口地址错误
- 未启用 `enableChunked`

**解决方法**:
```javascript
// 确认配置正确
wx.request({
  url: '/user/api/v1/submitTextStream',  // 确认路径
  enableChunked: true,   // 必须设置为 true
  responseType: 'text',  // 必须设置为 text
  // ...
});
```

### Q2: SSE 解析失败

**可能原因**:
- 数据格式不正确
- JSON 解析错误

**解决方法**:
```javascript
// 在 parseSSEData 中添加日志
console.log('原始SSE数据:', sseData);
console.log('解析后的事件:', currentEvent);
console.log('解析后的数据:', currentData);
```

### Q3: 流式内容不显示

**可能原因**:
- WXML 条件渲染有误
- 数据绑定错误

**解决方法**:
```xml
<!-- 临时调试：始终显示 streamContent -->
<view>{{streamContent}}</view>
```

### Q4: 打字机效果不工作

**可能原因**:
- `typeWriterTo` 方法未定义
- 字段名不匹配

**解决方法**:
```javascript
// 确认 util.js 中有 typeWriter 方法
const { typeWriter } = require('../../utils/util');

// 或者直接使用已有的 typeWriterTo 方法
this.typeWriterTo('displayReminder', text, 50);
```

---

## 📊 预期效果

### 用户体验流程：

1. **用户输入文本** → 点击"提交开始分析"
2. **显示 loading** → "AI分析中..."
3. **实时显示 AI 思考过程** → 看到 JSON 数据逐步生成
4. **分析完成** → 显示：
   - 情感类型（如：开心）
   - 情感指数（如：75%）
   - 情感建议（打字机效果逐字显示）
5. **自动滚动** → 滚动到结果区域
6. **刷新次数** → 更新剩余使用次数

### 视觉效果：

```
┌─────────────────────────────┐
│  📊 分析结果                │
├─────────────────────────────┤
│  情感类型：开心             │
│  情感指数：75%              │
│                             │
│  AI思考中：                 │
│  {"score":75,"suggestion"...│  ← 实时流式显示
│                             │
│  情感建议：                 │
│  保持好心情哦|              │  ← 打字机效果
└─────────────────────────────┘
```

---

## 🎯 优化建议

### 1. 隐藏原始 JSON，只显示友好文本

如果想让用户看不到原始 JSON，可以修改 `handleSSEEvent` 中的 `chunk` 处理：

```javascript
case 'chunk':
  // 不显示原始JSON，只显示loading动画
  // 或者显示"AI正在思考..."的动画
  break;
```

### 2. 添加进度条

```javascript
// 在 data 中添加
progress: 0

// 在 chunk 事件中更新
this.setData({
  progress: Math.min(this.data.progress + 5, 90)  // 最多到90%
});

// 在 complete 事件中设置为100%
this.setData({ progress: 100 });
```

### 3. 支持取消分析

```javascript
// 添加取消按钮
cancelAnalysis() {
  if (this._requestTask) {
    this._requestTask.abort();
    this.setData({ isAnalyzing: false });
    wx.showToast({ title: '已取消分析', icon: 'none' });
  }
}
```

---

## 📝 总结

本次改造将心灵花园的情绪分析功能从同步请求升级为流式输出，显著提升了用户体验：

✅ **实时反馈**：用户能看到 AI 的思考过程  
✅ **科技感强**：流式输出让产品更有 AI 感  
✅ **无缝衔接**：保持原有UI风格，只是增加了动态效果  
✅ **向后兼容**：如果流式失败，可以降级到原接口  

**下一步**：根据实际使用情况收集用户反馈，持续优化体验。

---

## 🆘 需要帮助？

如果遇到问题，请检查：
1. 后端服务是否正常运行
2. Ollama 服务是否启动
3. 微信开发者工具的 Console 日志
4. 网络请求是否成功（Network 面板）

祝修改顺利！🎉
