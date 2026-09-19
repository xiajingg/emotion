# wx-code-minifier 使用指南

## 📦 工具介绍

**wx-code-minifier** 是一个专门用于微信小程序代码压缩的工具。

- **NPM**: https://www.npmjs.com/package/wx-code-minifier
- **GitHub**: https://github.com/LeeJim/wxml-minifier
- **压缩率**: 25-30%

---

## 🚀 快速开始

### 方法一：使用自动化脚本（推荐）

```bash
cd /Users/xiajing/emotion
./scripts/compress-with-wx-minifier.sh
```

脚本会自动完成：
1. ✅ 安装 wx-code-minifier
2. ✅ 备份代码
3. ✅ 创建配置文件
4. ✅ 执行压缩
5. ✅ 显示压缩结果

---

### 方法二：手动执行

#### Step 1: 安装工具

```bash
npm install -g wx-code-minifier
```

#### Step 2: 创建配置文件

在项目根目录创建 `wxmin.config.js`：

```javascript
module.exports = {
  // 源代码目录
  src: './',
  
  // 是否压缩各类文件
  wxjsMin: true,   // 压缩 JS 代码
  wxssMin: true,   // 压缩 WXSS 代码
  wxmlMin: true,   // 压缩 WXML 代码
  
  // JS 压缩配置
  wxjsMinConfig: {
    mangle: {
      toplevel: true  // 代码混淆
    },
    compress: {
      dead_code: true,      // 移除未引用的代码
      conditionals: true,   // 优化条件判断
      warnings: false,      // 不显示警告
      drop_console: true,   // 删除所有 console
      passes: 2             // 压缩次数
    }
  }
};
```

#### Step 3: 执行压缩

```bash
cd /Users/xiajing/emotion/miniprogram
wx-code-minifier --src ./
```

或者指定目录：

```bash
wx-code-minifier --src ./dist
```

---

## ⚙️ 配置说明

### 完整配置项

```javascript
module.exports = {
  // 源代码目录（必填）
  src: 'dist',
  
  // 是否压缩 wxjs 代码
  wxjsMin: true,
  
  // 是否压缩 wxss 代码
  wxssMin: true,
  
  // 是否压缩 wxml 代码
  wxmlMin: true,
  
  // wxjs 压缩配置
  wxjsMinConfig: {
    // 代码混淆配置
    mangle: {
      toplevel: true  // 混淆顶级作用域的变量名
    },
    
    // 压缩配置
    compress: {
      dead_code: true,      // 移除未被引用的代码
      conditionals: true,   // 优化 if 条件判断
      warnings: true,       // 显示警告信息
      drop_console: true,   // 删除所有 console.* 调用
      passes: 2             // 压缩遍数（越多压缩越彻底，但速度越慢）
    }
  }
};
```

---

## 📊 压缩效果

### 预期压缩率

| 文件类型 | 压缩率 | 说明 |
|---------|--------|------|
| JS 文件 | 30-40% | 移除空格、混淆变量、删除 console |
| WXML 文件 | 10-20% | 移除空格、注释 |
| WXSS 文件 | 15-25% | 移除空格、合并样式 |
| **总体** | **25-30%** | 综合压缩效果 |

---

## 🔍 工作原理

### JS 压缩
- 使用 UglifyJS/Terser 进行压缩
- 移除未使用的代码（Tree Shaking）
- 混淆变量名
- 删除 console.log

### WXML 压缩
- 移除多余空格和换行
- 删除 HTML 注释
- 保留 WXS 脚本

### WXSS 压缩
- 移除空格和注释
- 合并重复样式
- 优化选择器

---

## ⚠️ 注意事项

### 1. 备份代码
压缩前务必备份代码，因为压缩是**不可逆**的。

```bash
# 手动备份
cp -r miniprogram miniprogram-backup
```

### 2. 测试功能
压缩后需要全面测试小程序功能，确保没有破坏代码逻辑。

### 3. Console 删除
如果配置了 `drop_console: true`，所有的 `console.log` 都会被删除，包括错误日志。

**建议：** 开发环境不要开启此选项。

### 4. 代码混淆
如果开启了 `mangle.toplevel`，变量名会被混淆，可能导致调试困难。

**建议：** 生产环境开启，开发环境关闭。

---

## 🆘 常见问题

### Q1: 压缩后小程序报错怎么办？

**A:** 
1. 恢复备份
2. 检查配置文件，关闭某些压缩选项
3. 重新压缩

```bash
# 恢复备份
rm -rf pages/ components/ utils/ subpage1/
cp -r backups/minifier-时间戳/* .
```

### Q2: 如何只压缩特定类型的文件？

**A:** 修改配置文件：

```javascript
module.exports = {
  src: './',
  wxjsMin: true,   // 只压缩 JS
  wxssMin: false,  // 不压缩 WXSS
  wxmlMin: false   // 不压缩 WXML
};
```

### Q3: 压缩后体积没有明显变化？

**A:** 
1. 微信开发者工具上传时会自动压缩 JS 和 WXSS
2. wx-code-minifier 主要优势在于压缩 WXML
3. 可以查看开发者工具的「代码依赖分析」确认实际大小

### Q4: 可以和微信开发者工具的压缩同时使用吗？

**A:** 可以，而且推荐这样做：
1. 先用 wx-code-minifier 压缩
2. 再在开发者工具中开启自动压缩
3. 双重压缩效果更好

---

## 🔄 与其他工具对比

| 工具 | 压缩率 | 易用性 | 适用场景 |
|------|--------|--------|---------|
| **wx-code-minifier** | 25-30% | ⭐⭐⭐⭐⭐ | 通用压缩 |
| 微信开发者工具 | 10-15% | ⭐⭐⭐⭐⭐ | 基础压缩 |
| Gulp + 插件 | 20-35% | ⭐⭐⭐ | 自定义流程 |
| miniprogram-ci | 15-25% | ⭐⭐⭐⭐ | CI/CD 集成 |

---

## 💡 最佳实践

### 开发环境配置

```javascript
module.exports = {
  src: './',
  wxjsMin: false,  // 开发环境不压缩
  wxssMin: false,
  wxmlMin: false
};
```

### 生产环境配置

```javascript
module.exports = {
  src: './',
  wxjsMin: true,
  wxssMin: true,
  wxmlMin: true,
  wxjsMinConfig: {
    mangle: {
      toplevel: true
    },
    compress: {
      dead_code: true,
      drop_console: true,  // 删除 console
      passes: 2
    }
  }
};
```

---

## 📝 总结

**wx-code-minifier 的优势：**
- ✅ 专门针对微信小程序优化
- ✅ 支持 JS/WXML/WXSS 全量压缩
- ✅ 配置简单，一键执行
- ✅ 压缩率高（25-30%）

**适用场景：**
- 小程序包体积接近 2MB 限制
- 需要进一步优化加载速度
- 希望自动化压缩流程

**不适用场景：**
- 开发阶段（影响调试）
- 已经使用了其他构建工具（如 Taro、Uni-app）

---

**最后更新：** 2026-05-10  
**工具版本：** wx-code-minifier v1.0.1
