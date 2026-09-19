#!/bin/bash

# 微信小程序源码压缩优化脚本
# 适用于 miniprogram 项目

set -e  # 遇到错误立即退出

echo "=========================================="
echo "  微信小程序源码压缩优化工具"
echo "  项目: miniprogram"
echo "  日期: $(date '+%Y-%m-%d %H:%M:%S')"
echo "=========================================="
echo ""

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 项目根目录
PROJECT_DIR="/Users/xiajing/emotion/miniprogram"

cd "$PROJECT_DIR"

echo -e "${YELLOW}Step 1: 检查项目结构...${NC}"
if [ ! -f "app.json" ]; then
    echo -e "${RED}❌ 错误: 未找到 app.json，请确认在项目根目录执行${NC}"
    exit 1
fi
echo -e "${GREEN}✅ 项目结构正常${NC}"
echo ""

echo -e "${YELLOW}Step 2: 分析当前包大小...${NC}"
echo "主包和分包大小："
du -sh pages/ components/ utils/ subpage1/ 2>/dev/null || true
echo ""

TOTAL_SIZE=$(du -sh . | awk '{print $1}')
echo -e "当前项目总大小: ${GREEN}$TOTAL_SIZE${NC}"
echo ""

echo -e "${YELLOW}Step 3: 检查无用文件...${NC}"
echo -e "${YELLOW}提示: 请在微信开发者工具中执行以下操作：${NC}"
echo "  1. 打开微信开发者工具"
echo "  2. 点击「工具」→「代码依赖分析」"
echo "  3. 查看「无依赖文件」列表"
echo "  4. 删除未使用的文件"
echo ""
read -p "按回车键继续..."

echo -e "${YELLOW}Step 4: 清理 node_modules 中的示例文件...${NC}"
if [ -d "node_modules/echarts-for-weixin/examples" ]; then
    echo "发现 echarts 示例文件，这些不需要打包到小程序中"
    read -p "是否删除示例文件？(y/n) " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        rm -rf node_modules/echarts-for-weixin/examples
        echo -e "${GREEN}✅ 已删除示例文件${NC}"
    fi
fi
echo ""

echo -e "${YELLOW}Step 5: 检查图片资源...${NC}"
IMAGE_COUNT=$(find . -path ./node_modules -prune -o \( -name "*.png" -o -name "*.jpg" -o -name "*.jpeg" -o -name "*.gif" \) -print | wc -l)
if [ "$IMAGE_COUNT" -gt 0 ]; then
    echo -e "发现 $IMAGE_COUNT 个图片文件"
    echo "建议："
    echo "  1. 使用 TinyPNG (https://tinypng.com/) 压缩图片"
    echo "  2. 将非 TabBar 图标上传到 CDN"
    find . -path ./node_modules -prune -o \( -name "*.png" -o -name "*.jpg" \) -print | head -10
else
    echo -e "${GREEN}✅ 未发现本地图片资源（良好实践）${NC}"
fi
echo ""

echo -e "${YELLOW}Step 6: 检查 Console 日志...${NC}"
CONSOLE_COUNT=$(grep -r "console\.log" pages/ components/ utils/ --include="*.js" 2>/dev/null | wc -l)
if [ "$CONSOLE_COUNT" -gt 0 ]; then
    echo -e "${YELLOW}⚠️  发现 $CONSOLE_COUNT 处 console.log${NC}"
    echo "建议在生产环境移除或封装日志"
    echo ""
    echo "示例代码："
    cat << 'EOF'
// utils/logger.js
const isDev = __wxConfig.envVersion === 'develop';

module.exports = {
  log: (...args) => isDev && console.log(...args),
  warn: (...args) => isDev && console.warn(...args),
  error: (...args) => console.error(...args)
};
EOF
else
    echo -e "${GREEN}✅ 未发现 console.log${NC}"
fi
echo ""

echo -e "${YELLOW}Step 7: 生成优化报告...${NC}"
REPORT_FILE="$PROJECT_DIR/../MINIPROGRAM_OPTIMIZATION_REPORT_$(date '+%Y%m%d_%H%M%S').md"

cat > "$REPORT_FILE" << EOF
# 微信小程序优化报告

**生成时间：** $(date '+%Y-%m-%d %H:%M:%S')
**项目路径：** $PROJECT_DIR

## 当前状态

### 包大小分析
\`\`\`
$(du -sh pages/ components/ utils/ subpage1/ 2>/dev/null || echo "无法获取")
\`\`\`

**总大小：** $TOTAL_SIZE

### 发现的问题

1. **Console 日志：** $CONSOLE_COUNT 处
2. **图片资源：** $IMAGE_COUNT 个
3. **无用文件：** 需在开发者工具中检查

## 建议的优化措施

### P0 - 立即执行
- [ ] 关闭 SourceMap（修改 project.config.json）
- [ ] 清理无用文件
- [ ] 移除 Console 日志

### P1 - 短期执行
- [ ] 配置 miniprogram-ci
- [ ] 图片优化
- [ ] 依赖优化

### P2 - 长期执行
- [ ] 分包进一步优化
- [ ] 组件异步化
- [ ] 建立性能监控

## 参考文档

详细优化方案请参考：
- [MINIPROGRAM_OPTIMIZATION_GUIDE.md](./MINIPROGRAM_OPTIMIZATION_GUIDE.md)

EOF

echo -e "${GREEN}✅ 优化报告已生成：$REPORT_FILE${NC}"
echo ""

echo "=========================================="
echo -e "${GREEN}  优化检查完成！${NC}"
echo "=========================================="
echo ""
echo "下一步操作："
echo "  1. 查看详细优化指南: MINIPROGRAM_OPTIMIZATION_GUIDE.md"
echo "  2. 查看优化报告: $REPORT_FILE"
echo "  3. 在微信开发者工具中执行「代码依赖分析」"
echo "  4. 修改 project.config.json 关闭 SourceMap"
echo "  5. 重新编译并上传"
echo ""
echo -e "${YELLOW}祝优化顺利！🚀${NC}"
