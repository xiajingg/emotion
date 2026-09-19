# 保存心情卡片功能 - 问题排查指南

## 🔍 已修复的问题

### 1. Canvas ID 属性错误
**问题**：使用了 `canvas-id` 而不是 `id`
```xml
<!-- 错误 -->
<canvas canvas-id="shareCanvas" type="2d"></canvas>

<!-- 正确 -->
<canvas id="shareCanvas" type="2d"></canvas>
```

**原因**：当使用 `type="2d"` 时，必须使用 `id` 属性来选择 Canvas 节点

### 2. CSS 尺寸不匹配
**问题**：CSS 中高度是 1200px，但代码中使用 1334px
```css
/* 修改前 */
height: 1200px;

/* 修改后 */
height: 1334px;
```

### 3. 添加详细日志
在关键步骤添加了 console.log，方便定位问题：
- ✅ 检查分析数据是否存在
- ✅ Canvas 节点查询结果
- ✅ 设备 DPR 信息
- ✅ Canvas 尺寸设置
- ✅ 每个绘制阶段
- ✅ 转换和保存结果

## 🧪 测试步骤

### 第一步：打开调试控制台
1. 在微信开发者工具中打开项目
2. 点击顶部菜单 "调试器" → "Console"
3. 确保能看到 console.log 输出

### 第二步：执行完整流程
1. **输入文本**：在首页输入框输入至少5个字
   ```
   例如：今天心情不太好，感觉有点孤独
   ```

2. **点击分析**：点击"开始感知情绪"按钮

3. **等待结果**：等待 AI 分析完成（约3-5秒）

4. **查看结果**：确认结果显示正常
   - 圆形进度条动画
   - 情绪分数
   - 情感建议文字

5. **点击保存**：点击"保存心情卡片"按钮

### 第三步：观察控制台日志

#### 正常情况的日志输出：
```
=== 开始生成心情卡片 ===
分析数据: {text: "...", score: 37, emotion: "平静", advice: "..."}
Canvas查询结果: [{node: Canvas, width: 750, height: 1334}]
设备信息 - DPR: 2 (或 3)
Canvas尺寸设置完成: 1500 x 2668 (DPR=2时)
背景绘制完成
开始绘制卡片内容, 尺寸: 750 x 1334
使用的数据: {text: "...", score: 37, ...}
内容绘制完成
Canvas转图片成功: http://tmp/xxx.png
保存到相册成功
```

#### 可能的错误情况及解决方案：

##### 错误1：没有分析数据
```
错误：没有分析数据
提示：请先进行情绪分析
```
**解决**：必须先点击"开始感知情绪"并等待分析完成

##### 错误2：Canvas节点未找到
```
错误：Canvas节点未找到
提示：生成失败
```
**解决**：
- 检查 WXML 中是否有 `<canvas id="shareCanvas">`
- 确认 `id` 拼写正确
- 重启小程序

##### 错误3：Canvas节点无效
```
错误：Canvas节点无效
提示：Canvas初始化失败
```
**解决**：
- 确认 Canvas 有 `type="2d"` 属性
- 检查是否在页面卸载后调用

##### 错误4：Canvas转图片失败
```
Canvas转图片失败: {errMsg: "..."}
提示：生成失败: [具体错误信息]
```
**常见原因**：
- Canvas 内容为空
- 绘制过程中出错
- 内存不足

**解决**：
- 查看前面的日志，确认"内容绘制完成"是否出现
- 检查 drawShareContent 是否有异常
- 尝试减小画布尺寸

##### 错误5：保存到相册失败
```
保存到相册失败: {errMsg: "saveImageToPhotosAlbum:fail auth deny"}
```
**解决**：
- 点击弹窗中的"去设置"
- 在系统设置中允许访问相册
- 重新点击保存按钮

## 🐛 常见问题排查

### Q1: 点击按钮没有任何反应？
**检查**：
1. 控制台是否有 "=== 开始生成心情卡片 ===" 日志？
2. 如果没有，检查按钮绑定是否正确
3. 查看 WXML 中 `bindtap="saveMoodCard"`

### Q2: 显示"生成失败"但没有详细信息？
**检查**：
1. 查看控制台完整的错误日志
2. 确认在哪一步失败的
3. 根据上面的错误类型对照解决

### Q3: Canvas 查询结果为空？
**可能原因**：
- Canvas 元素不存在
- id 拼写错误
- 页面未完全渲染

**解决**：
```javascript
// 在 onReady 中添加测试
onReady() {
  const query = wx.createSelectorQuery().in(this);
  query.select('#shareCanvas')
    .fields({ node: true, size: true })
    .exec((res) => {
      console.log('测试Canvas查询:', res);
    });
}
```

### Q4: 图片生成成功但保存失败？
**可能原因**：
- 用户拒绝了相册权限
- iOS 需要额外授权

**解决**：
1. 引导用户去设置开启权限
2. 使用 wx.getSetting 检查权限状态
3. 使用 wx.authorize 请求授权

### Q5: 真机上无法保存？
**注意**：
- 真机需要用户手动授权
- 开发工具可能自动授权
- 需要在 app.json 中配置权限

**检查 app.json**：
```json
{
  "permission": {
    "scope.writePhotosAlbum": {
      "desc": "用于保存心情卡片到相册"
    }
  }
}
```

## 📱 真机测试注意事项

### 1. 首次使用授权
- iOS 和 Android 都会弹出授权对话框
- 用户必须点击"允许"
- 拒绝后需要去系统设置中手动开启

### 2. 性能考虑
- 真机性能可能低于模拟器
- 大尺寸 Canvas 可能导致卡顿
- 可以考虑降低画布尺寸（如 600x1067）

### 3. 网络问题
- 保存图片不需要网络
- 但情绪分析需要网络
- 确保分析完成后再保存

## 🔧 调试技巧

### 1. 逐步注释法
如果还是失败，可以逐步注释代码来定位问题：

```javascript
// 先只绘制背景
ctx.fillRect(0, 0, width, height);
console.log('背景绘制完成');

// 然后添加文字
ctx.fillText('测试', 100, 100);
console.log('文字绘制完成');

// 最后添加复杂内容
this.drawShareContent(ctx, width, height);
```

### 2. 简化测试
创建一个最小化的测试函数：

```javascript
testCanvas() {
  const query = wx.createSelectorQuery().in(this);
  query.select('#shareCanvas')
    .fields({ node: true, size: true })
    .exec((res) => {
      if (!res[0].node) {
        console.error('Canvas不存在');
        return;
      }
      
      const canvas = res[0].node;
      const ctx = canvas.getContext('2d');
      
      // 简单绘制
      ctx.fillStyle = 'red';
      ctx.fillRect(0, 0, 100, 100);
      
      // 转换测试
      wx.canvasToTempFilePath({
        canvas: canvas,
        success: (res) => {
          console.log('测试成功:', res.tempFilePath);
        },
        fail: (err) => {
          console.error('测试失败:', err);
        }
      });
    });
}
```

### 3. 使用 setData 调试
在关键位置更新页面数据来确认执行流程：

```javascript
this.setData({ debugStep: 'background_drawn' });
this.setData({ debugStep: 'content_drawn' });
this.setData({ debugStep: 'image_generated' });
```

## 📊 日志分析清单

测试时请记录以下信息：

- [ ] 是否看到 "=== 开始生成心情卡片 ==="？
- [ ] 分析数据是否正常显示？
- [ ] Canvas 查询结果是否为数组且有内容？
- [ ] DPR 值是多少？（通常是 2 或 3）
- [ ] Canvas 尺寸设置是否正确？
- [ ] 是否看到 "背景绘制完成"？
- [ ] 是否看到 "开始绘制卡片内容"？
- [ ] 是否看到 "内容绘制完成"？
- [ ] Canvas 转图片是否成功？
- [ ] 保存到相册是否成功？
- [ ] 最终提示是什么？

## 🎯 下一步行动

1. **在开发者工具中测试**
   - 打开控制台
   - 执行完整流程
   - 复制所有日志

2. **如果仍然失败**
   - 将完整的日志发给我
   - 说明在哪一步失败的
   - 提供错误信息

3. **真机测试**
   - 预览模式扫描二维码
   - 在手机上测试
   - 注意授权提示

---

**更新日期**：2024年5月13日  
**版本**：v1.1（添加详细日志）
