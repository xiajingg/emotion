#!/bin/bash

# 流式接口测试脚本
# 使用方法: ./test_stream_api.sh

echo "========================================="
echo "心灵花园流式分析接口测试"
echo "========================================="
echo ""

# 配置
BASE_URL="http://localhost:8080"
ENDPOINT="/user/api/v1/submitTextStream"
JWT_TOKEN="YOUR_JWT_TOKEN_HERE"  # 替换为实际的 JWT token

# 测试用例 1: 正面情绪
echo "【测试 1】正面情绪分析"
echo "输入文本: 今天天气真好,心情很愉快!"
echo ""
echo "开始流式输出:"
echo "---"

curl -X POST "${BASE_URL}${ENDPOINT}" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${JWT_TOKEN}" \
  -d '{"text":"今天天气真好,心情很愉快!","type":0}' \
  -N 2>/dev/null | while IFS= read -r line; do
    echo "$line"
  done

echo ""
echo "---"
echo ""
sleep 2

# 测试用例 2: 负面情绪
echo "【测试 2】负面情绪分析"
echo "输入文本: 今天工作很不顺利,被老板批评了,感觉很沮丧。"
echo ""
echo "开始流式输出:"
echo "---"

curl -X POST "${BASE_URL}${ENDPOINT}" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${JWT_TOKEN}" \
  -d '{"text":"今天工作很不顺利,被老板批评了,感觉很沮丧。","type":0}' \
  -N 2>/dev/null | while IFS= read -r line; do
    echo "$line"
  done

echo ""
echo "---"
echo ""
sleep 2

# 测试用例 3: 签到提交
echo "【测试 3】签到提交"
echo "输入文本: 今天的感悟:保持积极心态很重要"
echo ""
echo "开始流式输出:"
echo "---"

curl -X POST "${BASE_URL}${ENDPOINT}" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${JWT_TOKEN}" \
  -d '{"text":"今天的感悟:保持积极心态很重要","type":1}' \
  -N 2>/dev/null | while IFS= read -r line; do
    echo "$line"
  done

echo ""
echo "---"
echo ""

echo ""
echo "========================================="
echo "测试完成!"
echo "========================================="
