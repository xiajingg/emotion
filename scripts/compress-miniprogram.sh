#!/bin/bash

# 微信小程序源码压缩工具
# 使用方法: ./compress-miniprogram.sh

set -e

echo "=========================================="
echo "  微信小程序源码压缩工具"
echo "=========================================="
echo ""

PROJECT_DIR="/Users/xiajing/emotion/miniprogram"
BACKUP_DIR="$PROJECT_DIR/../backups/compress-$(date '+%Y%m%d_%H%M%S')"

cd "$PROJECT_DIR"

# 颜色定义
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${YELLOW}Step 1: 备份当前代码...${NC}"
mkdir -p "$BACKUP_DIR"
cp -r pages/ components/ utils/ subpage1/ app.* "$BACKUP_DIR/" 2>/dev/null || true
echo -e "${GREEN}✅ 备份完成: $BACKUP_DIR${NC}"
echo ""

echo -e "${YELLOW}Step 2: 清理无用文件...${NC}"

# 删除 .DS_Store 文件
find . -name ".DS_Store" -delete 2>/dev/null || true
echo "  ✓ 删除 .DS_Store 文件"

# 删除备份文件
find . -name "*.bak" -o -name "*.old" -o -name "*~" | xargs rm -f 2>/dev/null || true
echo "  ✓ 删除备份文件"

# 删除 node_modules 中的示例和文档（保留核心文件）
if [ -d "node_modules/echarts-for-weixin/examples" ]; then
    rm -rf node_modules/echarts-for-weixin/examples
    echo "  ✓ 删除 echarts 示例文件"
fi

if [ -d "node_modules/echarts-for-weixin/test" ]; then
    rm -rf node_modules/echarts-for-weixin/test
    echo "  ✓ 删除 echarts 测试文件"
fi

# 删除 markdown 文档（除了根目录的）
find . -path ./node_modules -prune -o -name "*.md" -not -path "./README.md" -delete 2>/dev/null || true
echo "  ✓ 删除多余文档"

echo ""

echo -e "${YELLOW}Step 3: 压缩图片资源...${NC}"

# 检查是否有本地图片
IMAGE_COUNT=$(find . -path ./node_modules -prune -o \( -name "*.png" -o -name "*.jpg" -o -name "*.jpeg" \) -print 2>/dev/null | wc -l)

if [ "$IMAGE_COUNT" -gt 0 ]; then
    echo "发现 $IMAGE_COUNT 个图片文件"
    echo "建议使用 TinyPNG (https://tinypng.com/) 手动压缩后替换"
else
    echo -e "${GREEN}✅ 未发现本地图片资源${NC}"
fi

echo ""

echo -e "${YELLOW}Step 4: 优化 project.config.json...${NC}"

# 创建临时文件
TEMP_FILE=$(mktemp)

# 修改配置
cat project.config.json | python3 -c "
import sys, json
config = json.load(sys.stdin)
config['setting']['uploadWithSourceMap'] = False
config['setting']['codeProtect'] = False
config['setting']['minified'] = True
config['setting']['minifyWXSS'] = True
config['setting']['minifyWXML'] = True
json.dump(config, sys.stdout, indent=2, ensure_ascii=False)
" > "$TEMP_FILE"

mv "$TEMP_FILE" project.config.json
echo -e "${GREEN}✅ 已关闭 SourceMap 和代码保护${NC}"
echo ""

echo -e "${YELLOW}Step 5: 统计优化结果...${NC}"
echo ""
echo "优化前后对比："
echo "----------------------------------------"
echo "主包大小："
du -sh pages/ components/ utils/ 2>/dev/null || true
echo ""
echo "分包大小："
du -sh subpage1/ 2>/dev/null || true
echo ""
TOTAL_SIZE=$(du -sh . | awk '{print $1}')
echo -e "总大小: ${GREEN}$TOTAL_SIZE${NC}"
echo "----------------------------------------"
echo ""

echo "=========================================="
echo -e "${GREEN}  ✅ 压缩完成！${NC}"
echo "=========================================="
echo ""
echo "下一步操作："
echo "  1. 在微信开发者工具中重新编译"
echo "  2. 点击「上传」按钮"
echo "  3. 查看上传后的代码包大小"
echo ""
echo "备份位置: $BACKUP_DIR"
echo ""
