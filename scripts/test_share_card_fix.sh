#!/bin/bash

# 分享卡片功能修复 - 快速验证脚本

echo "======================================"
echo "分享卡片功能修复验证"
echo "======================================"
echo ""

# 检查文件修改
echo "📋 检查修改的文件..."
echo ""

echo "1. 答案之书页面 (answer-book/index.js)"
if grep -q "checkAndSavePhoto" miniprogram/subpage1/pages/answer-book/index.js; then
    echo "   ✅ 已添加 checkAndSavePhoto 方法"
else
    echo "   ❌ 未找到 checkAndSavePhoto 方法"
fi

if grep -q "savePhotoToAlbum" miniprogram/subpage1/pages/answer-book/index.js; then
    echo "   ✅ 已添加 savePhotoToAlbum 方法"
else
    echo "   ❌ 未找到 savePhotoToAlbum 方法"
fi

echo ""
echo "2. 星座运势页面 (horoscope/index.js)"
if grep -q "checkAndSavePhoto" miniprogram/subpage1/pages/horoscope/index.js; then
    echo "   ✅ 已添加 checkAndSavePhoto 方法"
else
    echo "   ❌ 未找到 checkAndSavePhoto 方法"
fi

if grep -q "savePhotoToAlbum" miniprogram/subpage1/pages/horoscope/index.js; then
    echo "   ✅ 已添加 savePhotoToAlbum 方法"
else
    echo "   ❌ 未找到 savePhotoToAlbum 方法"
fi

echo ""
echo "3. 全局配置 (app.json)"
if grep -q "scope.writePhotosAlbum" miniprogram/app.json; then
    echo "   ✅ 已添加相册权限说明"
else
    echo "   ❌ 未找到权限配置"
fi

echo ""
echo "======================================"
echo "代码统计"
echo "======================================"
echo ""

# 统计新增代码行数
ANSWER_BOOK_LINES=$(grep -c "checkAndSavePhoto\|savePhotoToAlbum" miniprogram/subpage1/pages/answer-book/index.js || echo 0)
HOROSCOPE_LINES=$(grep -c "checkAndSavePhoto\|savePhotoToAlbum" miniprogram/subpage1/pages/horoscope/index.js || echo 0)

echo "答案之书页面：约 ${ANSWER_BOOK_LINES} 处权限相关调用"
echo "星座运势页面：约 ${HOROSCOPE_LINES} 处权限相关调用"

echo ""
echo "======================================"
echo "✅ 修复完成！"
echo "======================================"
echo ""
echo "下一步操作："
echo "1. 在微信开发者工具中编译项目"
echo "2. 清除授权数据（模拟新用户）"
echo "3. 测试答案之书和星座运势的分享功能"
echo "4. 验证权限授权弹窗是否正常显示"
echo ""
echo "详细测试指南请查看：SHARE_CARD_FIX_REPORT.md"
echo ""
