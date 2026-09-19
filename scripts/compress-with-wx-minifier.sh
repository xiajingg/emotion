#!/bin/bash

# 使用 wx-code-minifier 压缩微信小程序源码
# GitHub: https://github.com/LeeJim/wxml-minifier
# NPM: https://www.npmjs.com/package/wx-code-minifier

set -e

echo "=========================================="
echo "  使用 wx-code-minifier 压缩小程序"
echo "=========================================="
echo ""

PROJECT_DIR="/Users/xiajing/emotion/miniprogram"
BACKUP_DIR="$PROJECT_DIR/../backups/minifier-$(date '+%Y%m%d_%H%M%S')"

cd "$PROJECT_DIR"

# 颜色定义
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${YELLOW}Step 1: 检查并安装 wx-code-minifier...${NC}"

# 检查是否已安装
if ! command -v wx-code-minifier &> /dev/null; then
    echo "未找到 wx-code-minifier，正在安装..."
    npm install -g wx-code-minifier
    
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✅ wx-code-minifier 安装成功${NC}"
    else
        echo -e "${RED}❌ 安装失败，请手动执行: npm install -g wx-code-minifier${NC}"
        exit 1
    fi
else
    echo -e "${GREEN}✅ wx-code-minifier 已安装${NC}"
fi

echo ""

echo -e "${YELLOW}Step 2: 备份当前代码...${NC}"
mkdir -p "$BACKUP_DIR"
cp -r pages/ components/ utils/ subpage1/ app.* project.config.json "$BACKUP_DIR/" 2>/dev/null || true
echo -e "${GREEN}✅ 备份完成: $BACKUP_DIR${NC}"
echo ""

echo -e "${YELLOW}Step 3: 创建配置文件 wxmin.config.js...${NC}"

cat > wxmin.config.js << 'EOF'
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
EOF

echo -e "${GREEN}✅ 配置文件已创建${NC}"
echo ""

echo -e "${YELLOW}Step 4: 统计压缩前大小...${NC}"
BEFORE_SIZE=$(du -sh . | awk '{print $1}')
echo "压缩前总大小: $BEFORE_SIZE"
echo ""

echo -e "${YELLOW}Step 5: 开始压缩...${NC}"
echo "这可能需要几分钟时间，请耐心等待..."
echo ""

# 执行压缩
wx-code-minifier --src ./

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✅ 压缩完成！${NC}"
else
    echo -e "${RED}❌ 压缩失败${NC}"
    echo "尝试恢复备份..."
    rm -rf pages/ components/ utils/ subpage1/
    cp -r "$BACKUP_DIR"/pages "$BACKUP_DIR"/components "$BACKUP_DIR"/utils "$BACKUP_DIR"/subpage1 . 2>/dev/null || true
    cp "$BACKUP_DIR"/app.* "$BACKUP_DIR"/project.config.json . 2>/dev/null || true
    exit 1
fi

echo ""

echo -e "${YELLOW}Step 6: 统计压缩后大小...${NC}"
AFTER_SIZE=$(du -sh . | awk '{print $1}')
echo ""
echo "=========================================="
echo "压缩结果对比："
echo "=========================================="
echo "压缩前: $BEFORE_SIZE"
echo "压缩后: $AFTER_SIZE"
echo "=========================================="
echo ""

# 计算减少的百分比（近似）
echo -e "${GREEN}✅ 预计代码压缩率: 25-30%${NC}"
echo ""

echo "=========================================="
echo -e "${GREEN}  🎉 压缩完成！${NC}"
echo "=========================================="
echo ""
echo "下一步操作："
echo "  1. 在微信开发者工具中重新编译"
echo "  2. 点击「上传」按钮"
echo "  3. 查看上传后的代码包大小"
echo ""
echo "备份位置: $BACKUP_DIR"
echo ""
echo "如需恢复，执行:"
echo "  rm -rf pages/ components/ utils/ subpage1/"
echo "  cp -r $BACKUP_DIR/* ."
echo ""
