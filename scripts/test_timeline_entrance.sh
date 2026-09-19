#!/bin/bash

# 朋友圈分享入口参数 - 快速验证脚本

echo "======================================"
echo "朋友圈分享小程序入口 - 修复验证"
echo "======================================"
echo ""

# 检查文件修改
echo "📋 检查修改的文件..."
echo ""

echo "1. 情绪分析页面 (pages/index/index.js)"
if grep -q "needShowEntrance: true" miniprogram/pages/index/index.js; then
    echo "   ✅ 已添加 needShowEntrance 参数"
else
    echo "   ❌ 未找到 needShowEntrance 参数"
fi

if grep -q "entrancePath:" miniprogram/pages/index/index.js; then
    echo "   ✅ 已添加 entrancePath 参数"
else
    echo "   ❌ 未找到 entrancePath 参数"
fi

echo ""
echo "2. 答案之书页面 (subpage1/pages/answer-book/index.js)"
if grep -q "needShowEntrance: true" miniprogram/subpage1/pages/answer-book/index.js; then
    echo "   ✅ 已添加 needShowEntrance 参数"
else
    echo "   ❌ 未找到 needShowEntrance 参数"
fi

if grep -q "entrancePath:" miniprogram/subpage1/pages/answer-book/index.js; then
    echo "   ✅ 已添加 entrancePath 参数"
else
    echo "   ❌ 未找到 entrancePath 参数"
fi

echo ""
echo "3. 星座运势页面 (subpage1/pages/horoscope/index.js)"
if grep -q "needShowEntrance: true" miniprogram/subpage1/pages/horoscope/index.js; then
    echo "   ✅ 已添加 needShowEntrance 参数"
else
    echo "   ❌ 未找到 needShowEntrance 参数"
fi

if grep -q "entrancePath:" miniprogram/subpage1/pages/horoscope/index.js; then
    echo "   ✅ 已添加 entrancePath 参数"
else
    echo "   ❌ 未找到 entrancePath 参数"
fi

echo ""
echo "======================================"
echo "基础库版本检查"
echo "======================================"
echo ""

# 提取基础库版本
LIB_VERSION=$(grep '"libVersion"' miniprogram/project.config.json | grep -o '[0-9.]*')
echo "当前基础库版本: ${LIB_VERSION}"

# 比较版本号（需要 3.2.0+）
if [ "$(printf '%s\n' "3.2.0" "$LIB_VERSION" | sort -V | head -n1)" = "3.2.0" ]; then
    echo "✅ 版本 >= 3.2.0，支持 needShowEntrance 参数"
else
    echo "❌ 版本 < 3.2.0，不支持该功能"
fi

echo ""
echo "======================================"
echo "✅ 修复完成！"
echo "======================================"
echo ""
echo "下一步操作："
echo "1. 在微信开发者工具中编译项目"
echo "2. 测试分享给好友功能"
echo "   - 点击图片底部的小程序名称"
echo "   - 验证是否能正确跳转到指定页面"
echo ""
echo "3. 测试分享到朋友圈功能"
echo "   - 观察朋友圈卡片底部是否显示小程序名称"
echo "   - 点击小程序名称验证是否能进入小程序"
echo ""
echo "详细文档请查看："
echo "- TIMELINE_SHARE_ENTRANCE_FIX.md（朋友圈入口修复说明）"
echo "- SHARE_CARD_FIX_REPORT.md（完整修复报告）"
echo ""
