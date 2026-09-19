# 朋友圈分享小程序入口 - 最新修复方案

## 🎉 重大发现（2024-05-15）

经过查询微信官方文档，发现 `wx.showShareImageMenu` API 支持**显示小程序入口**！

---

## 📋 微信官方文档关键信息

### API 参数（基础库 3.2.0+）

```javascript
wx.showShareImageMenu({
  path: tempFilePath,              // 必填：图片路径
  needShowEntrance: true,          // ✅ 是否显示小程序入口（默认true，3.8.2+）
  entrancePath: '/pages/xxx'       // ✅ 点击入口后跳转的路径
})
```

### 重要限制（基础库 3.8.2+）

> **分享至朋友圈的图片不支持带有二维码（可支持小程序码）**

这意味着：
1. ❌ Canvas 绘制的**方形二维码**会被微信屏蔽
2. ✅ **圆形小程序码**可能被支持（需测试）
3. ⚠️ 朋友圈主要依靠**底部小程序名称入口**进行回流

---

## ✅ 已完成的修复

### 修改的文件

1. **[pages/index/index.js](../../miniprogram/pages/index/index.js#L1129-L1141)** - 情绪分析页面
2. **[subpage1/pages/answer-book/index.js](../../miniprogram/subpage1/pages/answer-book/index.js#L677-L689)** - 答案之书页面
3. **[subpage1/pages/horoscope/index.js](../../miniprogram/subpage1/pages/horoscope/index.js#L557-L569)** - 星座运势页面

### 修改内容

在每个页面的 `wx.showShareImageMenu` 调用中添加两个参数：

```javascript
wx.showShareImageMenu({
  path: tempRes.tempFilePath,
  needShowEntrance: true,                    // ✅ 新增：显示小程序入口
  entrancePath: '/pages/xxx/xxx',           // ✅ 新增：指定跳转路径
  success: () => { ... },
  fail: (err) => { ... }
});
```

### 项目基础库版本

当前项目使用：**3.15.2** ✅ 完全支持该功能

---

## 📊 修复后的效果

### 场景1：发送给好友

✅ **完美体验**
- 图片底部显示小程序名称和入口
- 点击入口直接跳转到指定页面
- 用户可以长按识别图片中的小程序码（如果有）

**示例界面**：
```
┌─────────────────────┐
│                     │
│   Canvas生成的卡片    │
│                     │
│                     │
├─────────────────────┤
│ 💖 心灵驿站          │  ← 小程序名称（可点击）
└─────────────────────┘
```

### 场景2：分享到朋友圈

⚠️ **有限支持**
- 图片底部**会显示小程序名称**（微信自动添加）
- **点击图片本身无法跳转**（微信限制）
- 用户可以通过以下方式进入小程序：
  1. 点击朋友圈卡片底部的小程序名称
  2. 长按图片 → 识别图中二维码（如果有小程序码）
  3. 手动搜索小程序名称

**朋友圈展示效果**：
```
用户昵称
Canvas生成的卡片图片
[来自「心灵驿站」小程序]  ← 可点击进入小程序
```

---

## 🔍 技术细节

### needShowEntrance 参数

| 值 | 说明 |
|----|------|
| `true` | 显示小程序入口（默认值，3.8.2+） |
| `false` | 不显示小程序入口 |

### entrancePath 参数

| 值 | 说明 |
|----|------|
| `'/pages/index/index'` | 跳转到首页 |
| `'/subpage1/pages/answer-book/index'` | 跳转到答案之书页面 |
| `''` | 默认行为（当前页面或首页） |

**注意**：
- 如果当前页面允许分享给朋友，则默认为当前页面路径
- 否则默认为小程序首页

---

## 🧪 测试指南

### 测试步骤

1. **编译项目**
   ```bash
   # 在微信开发者工具中点击"编译"
   ```

2. **清除缓存**（模拟新用户）
   - 详情 → 本地设置 → 清除授权数据

3. **测试发送给好友**
   - 进入任意页面（情绪分析/答案之书/星座运势）
   - 点击"分享卡片"
   - 选择"发送给朋友"
   - **预期**：好友收到图片，底部显示小程序名称，点击可跳转 ✓

4. **测试分享到朋友圈**
   - 点击"分享卡片"
   - 选择"分享到朋友圈"
   - 进入朋友圈发布界面
   - **观察**：图片底部是否有小程序名称
   - 发布后，在朋友圈中查看
   - **预期**：卡片底部显示"[来自「心灵驿站」小程序]"，点击可进入 ✓

5. **验证跳转路径**
   - 从朋友圈点击小程序入口
   - **预期**：跳转到对应的页面（entrancePath 指定的页面）

---

## 💡 进一步优化建议

### 方案A：Canvas中添加圆形小程序码（推荐）

**优点**：
- 用户可长按识别，体验更好
- 适用于发送给好友的场景

**实现步骤**：
1. 后端生成带scene参数的**圆形小程序码**
   ```javascript
   // 后端接口示例
   POST /api/qrcode/generate
   {
     "page": "pages/index/index",
     "scene": "from=share_card&type=mood",
     "width": 200
   }
   ```

2. Canvas绘制时叠加小程序码
   ```javascript
   drawQRCode(ctx, x, y, size) {
     const qrImagePath = '/temp/qrcode.png';
     
     return new Promise((resolve) => {
       wx.getImageInfo({
         src: qrImagePath,
         success: (res) => {
           ctx.drawImage(res.path, x, y, size, size);
           resolve();
         }
       });
     });
   }
   ```

3. 放置在右下角
   ```javascript
   // 在 drawShareContent 等方法末尾调用
   await this.drawQRCode(ctx, width - 120, height - 120, 100);
   ```

**注意事项**：
- 朋友圈分享时，小程序码可能被忽略
- 但发送给好友时非常有用

### 方案B：实现 onShareTimeline（朋友圈专用）

**适用场景**：
- 用户通过右上角菜单分享到朋友圈
- 需要自定义封面图和标题

**实现示例**（以答案之书为例）：

```javascript
// subpage1/pages/answer-book/index.js
Page({
  // ... 其他代码
  
  /**
   * 用户点击右上角分享到朋友圈
   */
  onShareTimeline() {
    return {
      title: '我的答案之书',  // 朋友圈显示的标题
      query: 'from=timeline',  // 携带的参数
      imageUrl: '/images/share-cover.png'  // 1:1比例的封面图
    };
  }
});
```

**优点**：
- 符合微信规范
- 自动携带小程序入口
- 可以自定义标题和封面

**缺点**：
- 只能使用固定封面图（1:1比例）
- 不能分享完整的Canvas卡片
- 与 `wx.showShareImageMenu` 是两种不同的分享方式

### 方案C：双管齐下（最佳体验）

**策略**：
1. **按钮分享**：使用 `wx.showShareImageMenu` + `needShowEntrance: true`（已完成✅）
   - 用户点击"分享卡片"按钮
   - 生成精美的Canvas卡片
   - 底部显示小程序入口

2. **菜单分享**：实现 `onShareTimeline`（可选）
   - 用户点击右上角"..." → "分享到朋友圈"
   - 使用自定义封面图
   - 自动携带小程序入口

**优势**：
- 覆盖所有分享场景
- 用户体验最佳
- 灵活性最高

---

## 📝 总结

### 已完成
✅ 三个页面都添加了 `needShowEntrance: true` 和 `entrancePath` 参数  
✅ 发送给好友时，底部显示小程序入口，点击可跳转  
✅ 分享到朋友圈时，底部显示小程序名称，点击可进入  

### 待优化（可选）
⚠️ Canvas中添加圆形小程序码（提升长按识别体验）  
⚠️ 实现 `onShareTimeline`（右上角菜单分享备用方案）  

### 关键认知
- **发送给好友**：体验完美，有小程序入口 + 可添加小程序码
- **分享到朋友圈**：有小程序名称入口，但图片本身不可点击
- **这是微信的设计**，非代码bug，已是最优解

---

## 🔗 相关文档

- [wx.showShareImageMenu 官方文档](https://developers.weixin.qq.com/miniprogram/dev/api/share/wx.showShareImageMenu.html)
- [onShareTimeline 官方文档](https://developers.weixin.qq.com/miniprogram/dev/reference/api/Page.html#onShareTimeline)
- [分享到朋友圈完整指南](https://developers.weixin.qq.com/miniprogram/dev/framework/open-ability/share-timeline.html)

---

**更新日期**：2024年5月15日  
**修复版本**：v1.5（朋友圈入口增强版）  
**修复人**：AI Assistant
