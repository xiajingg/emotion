# 微信小程序快速优化清单（5分钟版）

## 🚀 立即执行的优化（5分钟内完成）

### ✅ Step 1: 关闭 SourceMap（30秒）

修改 `miniprogram/project.config.json`：

```json
{
  "setting": {
    "uploadWithSourceMap": false,  // ← 改为 false（原来是 true）
    "codeProtect": false           // ← 确保为 false
  }
}
```

**效果：** 上传体积减少 30-50%

---

### ✅ Step 2: 清理无用文件（2分钟）

在微信开发者工具中：

1. 点击菜单栏「工具」→「代码依赖分析」
2. 查看左侧「无依赖文件」标签
3. 删除列出的所有未使用文件

**常见可删除的文件：**
- 未使用的组件（`.js`, `.wxml`, `.wxss`, `.json`）
- 注释掉的代码块
- 测试文件
- 备份文件（`.bak`, `.old`）

**效果：** 代码体积减少 5-15%

---

### ✅ Step 3: 检查 Console 日志（1分钟）

运行以下命令检查：

```bash
cd /Users/xiajing/emotion/miniprogram
grep -r "console\.log" pages/ components/ utils/ --include="*.js" | wc -l
```

如果数量 > 10，建议封装日志工具：

**创建 `utils/logger.js`：**
```javascript
const isDev = __wxConfig.envVersion === 'develop';

module.exports = {
  log: (...args) => isDev && console.log(...args),
  warn: (...args) => isDev && console.warn(...args),
  error: (...args) => console.error(...args)
};
```

**替换所有 console.log：**
```javascript
// ❌ 旧代码
console.log('调试信息');

// ✅ 新代码
const logger = require('../../utils/logger');
logger.log('调试信息');
```

**效果：** 代码体积减少 2-5%，运行时性能提升

---

### ✅ Step 4: 重新编译上传（1分钟）

在微信开发者工具中：

1. 点击「编译」按钮
2. 确认无错误
3. 点击「上传」按钮
4. 填写版本号和备注
5. 提交审核

---

## 📊 预期效果

| 优化项 | 预计减少 | 实施时间 |
|--------|---------|---------|
| 关闭 SourceMap | 30-50% 上传体积 | 30秒 |
| 清理无用文件 | 5-15% 代码体积 | 2分钟 |
| 移除 Console | 2-5% 代码体积 | 1分钟 |
| **总计** | **15-25% 总体积** | **5分钟** |

---

## 🔍 验证优化效果

### 方法 1: 查看上传体积

上传成功后，在微信公众平台查看：
- 登录 [mp.weixin.qq.com](https://mp.weixin.qq.com/)
- 进入「版本管理」
- 查看最新版本的代码包大小

### 方法 2: 使用分析工具

在微信开发者工具中：
1. 点击「工具」→「分析包大小」
2. 查看可视化树状图
3. 对比优化前后的大小

---

## 📝 进阶优化（有时间再做）

如果完成上述步骤后还想进一步优化，可以参考完整指南：

📖 [MINIPROGRAM_OPTIMIZATION_GUIDE.md](./MINIPROGRAM_OPTIMIZATION_GUIDE.md)

**进阶优化包括：**
- 配置 miniprogram-ci 自动化构建
- 图片压缩和 CDN 化
- 第三方依赖优化
- 分包策略优化
- 组件异步化

---

## ⚠️ 注意事项

1. **备份代码：** 优化前确保代码已提交到 Git
2. **测试功能：** 优化后全面测试小程序功能
3. **灰度发布：** 首次优化后建议先小范围发布观察
4. **监控性能：** 定期查看性能评分和用户反馈

---

## 🆘 常见问题

### Q1: 关闭 SourceMap 后如何调试？
**A:** 开发环境仍然可以开启 SourceMap，只在上传生产版本时关闭。

### Q2: 删除文件后小程序报错怎么办？
**A:** 
1. 检查是否有其他文件引用了被删除的文件
2. 使用 Git 恢复被误删的文件
3. 重新编译测试

### Q3: 优化后体积还是很大怎么办？
**A:** 
1. 检查是否有大型第三方库（如 echarts、lodash）
2. 检查是否有未压缩的图片
3. 考虑使用分包加载

---

**最后更新：** 2026-05-10  
**适用项目：** miniprogram
