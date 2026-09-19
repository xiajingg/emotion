#!/bin/bash

# 心灵花园流式输出 - 自动修改脚本
# 此脚本会备份原文件并生成修改后的版本

FRONTEND_DIR="/Users/xiajing/WebstormProjects/miniprogram"
BACKUP_DIR="${FRONTEND_DIR}/backup_$(date +%Y%m%d_%H%M%S)"

echo "========================================="
echo "心灵花园流式输出 - 自动修改工具"
echo "========================================="
echo ""

# 检查前端目录是否存在
if [ ! -d "$FRONTEND_DIR" ]; then
    echo "❌ 错误: 前端项目目录不存在: $FRONTEND_DIR"
    exit 1
fi

# 创建备份目录
mkdir -p "$BACKUP_DIR"
echo "✅ 创建备份目录: $BACKUP_DIR"

# 备份原文件
cp "${FRONTEND_DIR}/pages/index/index.js" "$BACKUP_DIR/"
cp "${FRONTEND_DIR}/pages/index/index.wxml" "$BACKUP_DIR/"
cp "${FRONTEND_DIR}/pages/index/index.wxss" "$BACKUP_DIR/"
echo "✅ 已备份原文件"
echo ""

echo "📝 请按照以下步骤手动修改文件："
echo ""
echo "1️⃣  修改 index.js:"
echo "   文件路径: ${FRONTEND_DIR}/pages/index/index.js"
echo "   - 在 data 中添加 streamContent, displayReminder, analysisComplete 字段"
echo "   - 替换 submitText() 方法为流式版本"
echo "   - 添加 parseSSEData() 和 handleSSEEvent() 方法"
echo ""
echo "2️⃣  修改 index.wxml:"
echo "   文件路径: ${FRONTEND_DIR}/pages/index/index.wxml"
echo "   - 修改结果展示部分的渲染逻辑（第96-117行）"
echo ""
echo "3️⃣  修改 index.wxss:"
echo "   文件路径: ${FRONTEND_DIR}/pages/index/index.wxss"
echo "   - 在末尾添加流式文本样式"
echo ""

echo "📖 详细修改指南请参考:"
echo "   /Users/xiajing/emotion/backend/FRONTEND_MODIFICATION_GUIDE.md"
echo ""
echo "📄 代码补丁文件:"
echo "   /Users/xiajing/emotion/backend/frontend_patch.js"
echo ""

echo "========================================="
echo "备份位置: $BACKUP_DIR"
echo "========================================="
echo ""
echo "如需恢复原文件，执行:"
echo "cp $BACKUP_DIR/* ${FRONTEND_DIR}/pages/index/"
echo ""
echo "修改完成后，请在微信开发者工具中测试功能！"
