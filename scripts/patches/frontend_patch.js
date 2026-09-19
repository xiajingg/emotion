/**
 * 心灵花园流式输出 - 代码补丁
 * 
 * 使用说明：
 * 1. 打开文件: /Users/xiajing/WebstormProjects/miniprogram/pages/index/index.js
 * 2. 按照下面的标记找到对应位置
 * 3. 替换或添加代码
 */

// ========================================
// 修改 1: data 字段（第6-22行附近）
// ========================================
/*
在 data 对象中添加以下三个字段：
*/

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
    streamContent: '',      // 流式输出的原始内容
    displayReminder: '',    // 打字机效果显示的建议文本
    analysisComplete: false // 分析是否完成
  },
  
  // ... 其他代码保持不变
});


// ========================================
// 修改 2: 替换 submitText 方法（第85-125行）
// ========================================
/*
完全删除原有的 submitText 方法，替换为以下代码：
*/

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


// ========================================
// 修改 3: 添加 SSE 解析方法（在 submitText 之后）
// ========================================
/*
在 submitText 方法后面添加以下两个新方法：
*/

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


// ========================================
// 修改 4: WXML 文件修改
// ========================================
/*
文件路径: /Users/xiajing/WebstormProjects/miniprogram/pages/index/index.wxml

找到第96-117行，将原有的结果展示部分替换为：
*/

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


// ========================================
// 修改 5: WXSS 样式添加
// ========================================
/*
文件路径: /Users/xiajing/WebstormProjects/miniprogram/pages/index/index.wxss

在文件末尾添加以下样式：
*/

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


// ========================================
// 完成！
// ========================================
/*
修改完成后：
1. 保存所有文件
2. 在微信开发者工具中编译
3. 测试流式输出功能
4. 查看 Console 日志确认数据接收正常
*/
