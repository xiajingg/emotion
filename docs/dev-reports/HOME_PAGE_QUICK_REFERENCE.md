# 首页重构 - 快速参考清单

## ✅ 已完成改动清单

### P0 优先级（已实施）

- [x] **移除虚假滑动效果**
  - [x] 删除 `pages/home/index.wxml` 中的 guide-animation 代码
  - [x] 删除 `pages/home/index.js` 中的 showGuide 字段
  - [x] 删除 `pages/home/index.wxss` 中的相关样式
  
- [x] **调整首页跳转目标**
  - [x] 修改 `pages/home/index.js` 的 goToMain 方法
  - [x] 从跳转到"我的"页面改为跳转到"记录"页面
  
- [x] **设置差异化页面标题**
  - [x] 首页：`心灵驿站 - 今日心语`
  - [x] 记录页：`心情记录 - AI情感分析`
  - [x] 回顾页：`心情回顾 - 情绪数据追踪`
  - [x] 运势页：`星座运势 - 每日专属指引`

---

## 📋 测试清单

### 功能测试
- [ ] 点击首页"开启心灵之旅"按钮，验证跳转到记录页面
- [ ] 检查底部导航栏在记录页面显示正确
- [ ] 验证各页面导航栏标题显示正确
- [ ] 测试分享功能，检查分享标题

### 兼容性测试
- [ ] iOS 微信客户端测试
- [ ] Android 微信客户端测试
- [ ] 不同屏幕尺寸测试（iPhone SE, iPhone 12, iPad）

### 性能测试
- [ ] 首页加载时间 < 1.5s
- [ ] 页面切换无卡顿
- [ ] 内存占用正常

---

## 🎯 P1 优先级待办（建议近期实施）

### 1. 增加新用户欢迎区
- [ ] 设计欢迎卡片 UI
- [ ] 实现新用户/老用户区分逻辑
- [ ] 添加核心功能展示（AI分析、趋势追踪、星座运势）
- [ ] 编写欢迎区样式

**预计工作量：** 2-3 小时  
**预期收益：** 新用户转化率 +25%

### 2. 优化分享配置
- [ ] 设计 3-5 张分享背景图（500x400px）
- [ ] 实现动态分享标题生成
- [ ] 实现分享图片选择逻辑
- [ ] 测试分享效果

**预计工作量：** 3-4 小时（含设计时间）  
**预期收益：** 分享率 +50%

### 3. 实现免登录试用心语
- [ ] 后端开发公开接口 `/api/v1/motivation/public`
- [ ] 前端支持无登录获取心语
- [ ] 实现心语缓存机制
- [ ] 测试离线场景

**预计工作量：** 4-6 小时（需后端配合）  
**预期收益：** 注册转化率 +20-30%

---

## 📊 关键指标监测清单

### 每日监测
- [ ] 首页访问量（PV/UV）
- [ ] 首页→记录页转化率
- [ ] 新用户注册数
- [ ] 首次记录完成率

### 每周分析
- [ ] 用户留存率（次日、7日、30日）
- [ ] 分享次数和分享转化率
- [ ] 搜索关键词排名变化
- [ ] 用户反馈和问题报告

### 每月复盘
- [ ] 整体业务指标对比
- [ ] A/B 测试结果分析
- [ ] 用户行为路径分析
- [ ] 下月优化计划制定

---

## 🔧 技术细节备忘

### 文件路径速查
```
首页相关文件：
- WXML: miniprogram/pages/home/index.wxml
- JS:   miniprogram/pages/home/index.js
- WXSS: miniprogram/pages/home/index.wxss
- JSON: miniprogram/pages/home/index.json

其他页面 JSON：
- 记录页: miniprogram/pages/index/index.json
- 回顾页: miniprogram/subpage1/pages/reminisce/index.json
- 运势页: miniprogram/subpage1/pages/horoscope/index.json
```

### 关键代码片段

#### 首页跳转逻辑
```javascript
goToMain() {
  // ✅ 跳转到记录页面，用户可立即使用核心功能
  wx.reLaunch({ url: '/pages/index/index' });
}
```

#### 分享配置模板
```javascript
onShareAppMessage() {
  return {
    title: `💭 ${this.data.motivation.substring(0, 30)}...`,
    path: `/pages/home/index?shareFrom=motivation&time=${Date.now()}`,
    imageUrl: this.getShareImageUrl()
  };
}
```

#### 新用户检测逻辑
```javascript
onLoad() {
  const userInfo = wx.getStorageSync('userInfo');
  const isFirstVisit = !userInfo || !userInfo.nickname;
  
  this.setData({ 
    isFirstVisit: isFirstVisit
  });
}
```

---

## 📚 相关文档索引

| 文档名称 | 用途 | 位置 |
|---------|------|------|
| 深度分析报告 | 完整的 UI/UX、获客、SEO 分析 | `HOME_PAGE_REFACTOR_ANALYSIS.md` |
| 实施报告 | 本次改动的详细说明和预期效果 | `HOME_PAGE_REFACTOR_IMPLEMENTATION_REPORT.md` |
| 改动对比 | Before/After 可视化对比 | `HOME_PAGE_CHANGES_COMPARISON.md` |
| 快速参考 | 本清单，日常查阅用 | `HOME_PAGE_QUICK_REFERENCE.md` |

---

## 🐛 常见问题排查

### Q1: 点击首页按钮后没有跳转？
**检查：**
1. 确认 `pages/home/index.js` 中 goToMain 方法已修改
2. 检查控制台是否有错误信息
3. 确认 `/pages/index/index` 路径是否正确

**解决：**
```javascript
// 确保路径以 / 开头
wx.reLaunch({ url: '/pages/index/index' });
```

### Q2: 页面标题没有变化？
**检查：**
1. 确认对应的 `.json` 文件已修改
2. 清除微信开发者工具缓存
3. 重新编译项目

**解决：**
- 重启微信开发者工具
- 删除本地缓存：`wx.clearStorageSync()`

### Q3: 分享标题还是旧的？
**检查：**
1. 确认 `onShareAppMessage` 方法已更新
2. 检查是否清除了分享缓存

**解决：**
- 完全关闭小程序后重新打开
- 或使用不同的微信号测试

### Q4: 样式显示异常？
**检查：**
1. 确认删除了 `.guide-animation` 相关样式
2. 检查是否有样式冲突

**解决：**
- 清除样式缓存
- 重新编译 WXSS

---

## 💡 优化建议备忘录

### 短期优化（1-2周）
- [ ] 收集用户反馈，重点关注新用户体验
- [ ] 分析首页跳出率变化
- [ ] 监测搜索关键词排名

### 中期优化（1个月）
- [ ] 实施 P1 级优化任务
- [ ] A/B 测试不同 CTA 文案
- [ ] 优化分享图片设计

### 长期优化（持续）
- [ ] 建立数据看板，实时监控关键指标
- [ ] 定期竞品分析，保持竞争优势
- [ ] 根据用户行为数据持续迭代

---

## 📞 联系方式

如有问题或建议，请联系：
- **项目负责人：** [填写姓名]
- **前端开发：** [填写姓名]
- **UI 设计：** [填写姓名]
- **数据分析：** [填写姓名]

---

## 🔄 更新日志

| 日期 | 版本 | 更新内容 | 负责人 |
|------|------|---------|--------|
| 2026-05-09 | v1.0 | 初始版本，完成 P0 级优化 | Lingma AI |

---

**最后更新：** 2026-05-09  
**下次审查：** 2026-05-16（一周后）
