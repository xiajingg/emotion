# 微信小程序源码压缩与性能优化方案

## 📊 当前项目分析

### 项目结构概览
```
miniprogram/
├── components/      (72K)   - 组件目录
├── pages/           (68K)   - 主包页面（首页、记录页）
├── subpage1/        (224K)  - 分包页面（回顾、排行、答案之书等）
├── utils/           (16K)   - 工具函数
└── node_modules/    (?)     - 第三方依赖
```

### 已配置的优化项 ✅
根据 `project.config.json`，以下优化已开启：
- ✅ `minified: true` - 代码压缩
- ✅ `minifyWXSS: true` - WXSS 压缩
- ✅ `minifyWXML: true` - WXML 压缩
- ✅ `es6: true` - ES6 转 ES5
- ✅ `postcss: true` - PostCSS 处理

---

## 🎯 优化策略总览

### 一、构建层面优化（立即生效）

#### 1.1 启用更高级的压缩选项

**当前配置：**
```json
{
  "setting": {
    "minified": true,
    "minifyWXSS": true,
    "minifyWXML": true,
    "es6": true
  }
}
```

**建议增强：**
```json
{
  "setting": {
    // 现有配置
    "minified": true,
    "minifyWXSS": true,
    "minifyWXML": true,
    "es6": true,
    
    // 新增优化项
    "codeProtect": false,        // ❌ 关闭代码保护（会增加体积）
    "uploadWithSourceMap": false, // ❌ 生产环境不上传 SourceMap
    "autoAudits": true,          // ✅ 开启自动审计
    "enhance": true              // ✅ 开启增强编译
  }
}
```

**预期效果：**
- 关闭 SourceMap：减少约 30-50% 的上传体积
- 关闭代码保护：避免额外的混淆代码增加体积

---

#### 1.2 使用 miniprogram-ci 自动化构建

**优势：**
- 统一的构建流程
- 可配置更细粒度的压缩选项
- 支持 CI/CD 集成

**实施步骤：**

**Step 1: 安装 miniprogram-ci**
```bash
cd /Users/xiajing/emotion/miniprogram
npm install miniprogram-ci --save-dev
```

**Step 2: 创建构建脚本 `build.js`**
```javascript
const ci = require('miniprogram-ci');
const path = require('path');

// 项目配置
const project = new ci.Project({
  appid: 'wxc5dd3169f9790fa3',
  type: 'miniProgram',
  projectPath: path.resolve(__dirname),
  privateKeyPath: path.resolve(__dirname, './private.key'), // 从微信公众平台下载
  ignores: [
    'node_modules/**/*',
    '*.md',
    '.git/**/*',
    'backups/**/*'
  ]
});

// 上传配置
async function upload() {
  const version = '1.0.' + Date.now(); // 版本号
  
  try {
    const result = await ci.upload({
      project,
      version,
      desc: `Automated build ${new Date().toLocaleString()}`,
      setting: {
        // 压缩配置
        minifyJS: true,        // 压缩 JS
        minifyWXML: true,      // 压缩 WXML
        minifyWXSS: true,      // 压缩 WXSS
        minify: true,          // 全局压缩
        
        // 编译配置
        es6: true,             // ES6 转 ES5
        es7: true,             // 增强编译
        autoPrefixWXSS: true,  // 样式自动补全
        
        // 性能配置
        codeProtect: false,    // 关闭代码保护
        disableUseStrict: false,
        
        // 其他
        uploadWithSourceMap: false, // 不上传 SourceMap
      },
      onProgressUpdate: console.log
    });
    
    console.log('✅ 上传成功！', result);
  } catch (error) {
    console.error('❌ 上传失败:', error);
    process.exit(1);
  }
}

upload();
```

**Step 3: 添加到 package.json**
```json
{
  "scripts": {
    "build": "node build.js",
    "preview": "node preview.js"  // 可选：预览脚本
  }
}
```

**Step 4: 配置 IP 白名单**
1. 登录 [微信公众平台](https://mp.weixin.qq.com/)
2. 进入「开发」→「开发设置」
3. 下载代码上传密钥（保存为 `private.key`）
4. 配置 IP 白名单（添加你的服务器 IP）

**预期效果：**
- 自动化构建，减少人为失误
- 更细粒度的压缩控制
- 可集成到 CI/CD 流程

---

### 二、代码层面优化

#### 2.1 Tree Shaking（树摇优化）

**原理：** 移除未使用的代码

**当前问题：** 
原生小程序不支持自动 Tree Shaking，需要手动清理。

**优化措施：**

**1. 清理未使用的组件**
检查每个页面的 `.json` 文件：
```json
{
  "usingComponents": {
    // ❌ 删除未使用的组件声明
    // "unused-component": "/components/unused/index"
  }
}
```

**2. 清理未使用的工具函数**
检查 `utils/` 目录：
```javascript
// utils/util.js
// ❌ 删除未导出的函数
// function unusedHelper() { ... }

// ✅ 只保留实际使用的函数
module.exports = {
  formatTime,
  withAntiDoubleClick,
  // ... 其他使用的函数
};
```

**3. 使用微信开发者工具的「代码依赖分析」**
- 打开微信开发者工具
- 点击「工具」→「代码依赖分析」
- 查看无依赖文件（未被引用的文件）
- 删除这些文件

**预期效果：** 可减少 5-15% 的代码体积

---

#### 2.2 代码分割与懒加载

**当前配置：**
已在 `app.json` 中配置了分包：
```json
{
  "subpackages": [
    {
      "root": "subpage1",
      "pages": [
        "pages/reminisce/index",
        "pages/ranking/index",
        // ... 其他页面
      ]
    }
  ]
}
```

**进一步优化：**

**1. 将非核心功能拆分到独立分包**
```json
{
  "subpackages": [
    {
      "root": "subpage1",
      "name": "main-sub",
      "pages": [
        "pages/reminisce/index",
        "pages/ranking/index"
      ]
    },
    {
      "root": "subpage2",
      "name": "feature-sub",
      "pages": [
        "pages/answer-book/index",
        "pages/answer-book/history",
        "pages/horoscope/index"
      ],
      "independent": false  // 普通分包
    }
  ]
}
```

**2. 使用分包预下载**
在 `app.json` 中添加：
```json
{
  "preloadRule": {
    "pages/home/index": {
      "network": "all",
      "packages": ["subpage1"]
    }
  }
}
```

**预期效果：**
- 主包体积减少 30-50%
- 首屏加载速度提升 40-60%

---

#### 2.3 移除 Console 日志

**问题：** 生产环境的 `console.log` 会增加代码体积并影响性能

**解决方案：**

**方法 1：使用构建工具移除**
如果使用 miniprogram-ci，可以配置 babel 插件：
```javascript
// babel.config.js
module.exports = {
  plugins: [
    ['transform-remove-console', { exclude: ['error', 'warn'] }]
  ]
};
```

**方法 2：手动封装日志工具**
```javascript
// utils/logger.js
const isDev = __wxConfig.envVersion === 'develop';

module.exports = {
  log: (...args) => isDev && console.log(...args),
  warn: (...args) => isDev && console.warn(...args),
  error: (...args) => console.error(...args) // 错误始终保留
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

**预期效果：** 减少 2-5% 的代码体积，提升运行时性能

---

### 三、资源层面优化

#### 3.1 图片优化

**当前状态：**
项目中没有本地图片（除了 node_modules 中的示例图片），这是好的实践！

**建议：**

**1. TabBar 图标优化**
如果使用了 TabBar，确保图标：
- 格式：PNG 或 WebP
- 尺寸：81px × 81px（推荐）
- 大小：每个 < 40KB
- 使用 TinyPNG 压缩：https://tinypng.com/

**2. 其他图片全部上云**
```javascript
// ❌ 不要这样做
<image src="/images/bg.png" />

// ✅ 应该这样做
<image src="https://cdn.example.com/images/bg.png" />
```

**推荐 CDN 服务：**
- 微信云存储（免费额度充足）
- 阿里云 OSS
- 腾讯云 COS

**预期效果：** 每个图片可减少 50-80% 的体积

---

#### 3.2 字体文件优化

**当前状态：** 项目未使用自定义字体，无需优化。

**如果有自定义字体：**
```css
/* ❌ 不要引入完整字体文件（通常 2-5MB） */
@font-face {
  font-family: 'CustomFont';
  src: url('/fonts/custom.ttf');
}

/* ✅ 使用字体子集化工具 */
// 使用 font-spider 提取实际使用的字符
npm install font-spider -g
font-spider ./pages/**/*.html
```

---

### 四、依赖层面优化

#### 4.1 检查第三方库

**当前依赖：**
```bash
cd /Users/xiajing/emotion/miniprogram
ls node_modules/
```

**建议：**

**1. 移除未使用的依赖**
```bash
# 检查哪些依赖被实际使用
npm ls --depth=0

# 移除未使用的依赖
npm uninstall <unused-package>
```

**2. 使用轻量级替代方案**
```javascript
// ❌ moment.js (20KB+)
import moment from 'moment';

// ✅ day.js (2KB)
import dayjs from 'dayjs';

// ❌ lodash (70KB+)
import _ from 'lodash';

// ✅ lodash-es（按需引入）
import debounce from 'lodash-es/debounce';
```

**预期效果：** 每个重型库可减少 10-50KB

---

#### 4.2 NPM 包优化

**当前配置：**
```json
{
  "setting": {
    "packNpmManually": false,
    "nodeModules": false
  }
}
```

**建议：**
如果使用了 NPM 包，启用手动打包：
```json
{
  "setting": {
    "packNpmManually": true,
    "packNpmRelationList": [
      {
        "packageJsonPath": "./package.json",
        "miniprogramNpmDistDir": "./miniprogram_npm"
      }
    ]
  }
}
```

然后执行：
```bash
# 在微信开发者工具中
工具 → 构建 npm
```

**预期效果：** NPM 包体积可减少 30-50%

---

### 五、架构层面优化

#### 5.1 主包与分包规划

**当前主包内容：**
- `pages/home/index` - 首页
- `pages/index/index` - 记录页
- `components/tab-bar/` - 底部导航栏

**优化建议：**

**原则：**
- 主包只包含：TabBar 页面 + 核心组件
- 其他所有页面放入分包

**当前已符合最佳实践** ✅

---

#### 5.2 组件异步化

**对于仅在分包中使用的组件：**

**1. 将组件移到分包目录**
```
subpage1/
├── components/
│   └── custom-chart/  # 仅在该分包中使用
└── pages/
```

**2. 使用分包异步化**
```json
{
  "componentPlaceholder": {
    "custom-chart": "view"  // 占位符
  }
}
```

**预期效果：** 主包体积进一步减少 10-20%

---

### 六、性能监控与分析

#### 6.1 使用微信开发者工具分析

**步骤：**
1. 打开微信开发者工具
2. 点击「工具」→「分析包大小」
3. 查看可视化树状图
4. 定位占用空间最大的文件

**关键指标：**
- 主包大小：< 2MB（硬性限制）
- 单个分包：< 2MB
- 总包大小：< 20MB（普通账号）/ 30MB（服务商）

---

#### 6.2 性能评分

**在微信开发者工具中：**
1. 点击「工具」→「性能评分」
2. 查看各项评分
3. 根据建议优化

**关键指标：**
- 启动性能
- 渲染性能
- 脚本性能
- 网络性能

---

## 📋 实施清单（按优先级排序）

### P0 - 立即执行（高收益，低成本）

- [ ] **关闭 SourceMap**（修改 `project.config.json`）
  ```json
  "uploadWithSourceMap": false
  ```
  **预期收益：** 减少 30-50% 上传体积

- [ ] **清理无用文件**（使用代码依赖分析工具）
  - 删除未使用的组件
  - 删除未使用的工具函数
  - 删除注释掉的代码
  
  **预期收益：** 减少 5-15% 代码体积

- [ ] **移除 Console 日志**（生产环境）
  - 使用日志工具封装
  - 或使用 babel 插件自动移除
  
  **预期收益：** 减少 2-5% 体积，提升性能

---

### P1 - 短期执行（中等收益，中等成本）

- [ ] **配置 miniprogram-ci**
  - 安装依赖
  - 创建构建脚本
  - 配置 IP 白名单
  
  **预期收益：** 自动化构建，统一压缩标准

- [ ] **图片优化**
  - TabBar 图标压缩（TinyPNG）
  - 其他图片全部上云
  
  **预期收益：** 每个图片减少 50-80% 体积

- [ ] **依赖优化**
  - 移除未使用的 NPM 包
  - 使用轻量级替代方案（day.js 替代 moment.js）
  
  **预期收益：** 每个重型库减少 10-50KB

---

### P2 - 长期执行（低收益，高成本）

- [ ] **分包进一步优化**
  - 将非核心功能拆分为独立分包
  - 配置分包预下载
  
  **预期收益：** 首屏加载速度提升 40-60%

- [ ] **组件异步化**
  - 将仅分包使用的组件移到分包
  - 使用组件占位符
  
  **预期收益：** 主包体积减少 10-20%

- [ ] **建立性能监控体系**
  - 定期使用「分析包大小」工具
  - 监控性能评分
  - 建立优化基线
  
  **预期收益：** 持续保持高性能

---

## 🎯 预期总体效果

| 优化项 | 预计减少体积 | 实施难度 | 优先级 |
|--------|-------------|---------|--------|
| 关闭 SourceMap | 30-50% 上传体积 | ⭐ | P0 |
| 清理无用文件 | 5-15% 代码体积 | ⭐⭐ | P0 |
| 移除 Console | 2-5% 代码体积 | ⭐ | P0 |
| 图片优化 | 50-80% 图片体积 | ⭐⭐ | P1 |
| 依赖优化 | 10-50KB/库 | ⭐⭐ | P1 |
| miniprogram-ci | 自动化构建 | ⭐⭐⭐ | P1 |
| 分包优化 | 40-60% 首屏加载 | ⭐⭐⭐⭐ | P2 |
| 组件异步化 | 10-20% 主包体积 | ⭐⭐⭐⭐ | P2 |

**综合预期：**
- **代码体积减少：** 15-25%
- **首屏加载速度提升：** 30-50%
- **运行时性能提升：** 10-20%

---

## 🔧 快速开始（5分钟）

### Step 1: 修改 project.config.json
```json
{
  "setting": {
    "uploadWithSourceMap": false,  // ← 改为 false
    "codeProtect": false           // ← 确保为 false
  }
}
```

### Step 2: 清理无用文件
```bash
# 在微信开发者工具中
工具 → 代码依赖分析 → 查看无依赖文件 → 删除
```

### Step 3: 重新编译上传
```bash
# 在微信开发者工具中
编译 → 上传
```

**完成！** 你已经完成了最基础的优化。

---

## 📚 参考资源

### 官方文档
- [微信小程序代码包限制](https://developers.weixin.qq.com/miniprogram/dev/framework/subpackages.html)
- [miniprogram-ci 官方文档](https://developers.weixin.qq.com/miniprogram/dev/devtools/ci.html)
- [小程序性能优化指南](https://developers.weixin.qq.com/miniprogram/dev/framework/performance/)

### 工具推荐
- **图片压缩：** [TinyPNG](https://tinypng.com/)
- **字体子集化：** [font-spider](https://github.com/aui/font-spider)
- **代码分析：** 微信开发者工具内置
- **自动化构建：** [miniprogram-ci](https://www.npmjs.com/package/miniprogram-ci)

### 社区资源
- [awesome-wechat-weapp](https://github.com/justjavac/awesome-wechat-weapp)
- [微信小程序优化最佳实践](https://developers.weixin.qq.com/community/develop/doc/00040e5a0846706e893dcc24256009)

---

## 💡 总结

### 核心原则
1. **能分包就分包** - 这是最有效的优化手段
2. **能上云就上云** - 图片、音频等资源不要放在代码包中
3. **能删就删** - 定期清理无用代码和资源
4. **能压缩就压缩** - 开启所有可用的压缩选项

### 持续优化
- 每次发布前运行「代码依赖分析」
- 每月检查一次性能评分
- 每季度审查第三方依赖

### 注意事项
- ⚠️ 不要过度优化导致代码可读性下降
- ⚠️ 优先优化对用户感知明显的部分（首屏加载）
- ⚠️ 保持备份，优化前做好版本控制

---

**最后更新：** 2026-05-10  
**适用项目：** miniprogram  
**优化目标：** 降低源码大小，提高性能
