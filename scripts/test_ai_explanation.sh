#!/bin/bash

# 测试 get-ai-explanation 接口

echo "========================================="
echo "测试 AI 解读接口"
echo "========================================="

# 首先登录获取 token
echo ""
echo "1. 登录获取 Token..."
LOGIN_RESPONSE=$(curl -s -X GET "https://www.onekey-ai.top/user/api/v2/getOpenId?code=test_code")
echo "登录响应: $LOGIN_RESPONSE"

# 提取 token（假设返回格式为 {"data":{"token":"xxx"}}）
TOKEN=$(echo $LOGIN_RESPONSE | grep -o '"token":"[^"]*"' | cut -d'"' -f4)

if [ -z "$TOKEN" ]; then
    echo "❌ 无法获取 Token，使用模拟数据测试"
    TOKEN="test_token_123456"
else
    echo "✓ Token: ${TOKEN:0:20}..."
fi

echo ""
echo "2. 调用 get-ai-explanation 接口..."

# 构造请求数据
REQUEST_DATA='{
  "id": 1,
  "question": "我应该换工作吗？",
  "randomAnswer": "勇敢一点"
}'

echo "请求数据: $REQUEST_DATA"
echo ""

# 调用接口
RESPONSE=$(curl -s -X POST "https://www.onekey-ai.top/answer-book/api/v1/get-ai-explanation" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d "$REQUEST_DATA")

echo "响应结果:"
echo "$RESPONSE" | python3 -m json.tool 2>/dev/null || echo "$RESPONSE"

echo ""
echo "========================================="
echo "检查 data 字段是否为空..."
DATA_FIELD=$(echo "$RESPONSE" | grep -o '"data":"[^"]*"' | head -1)

if [ -z "$DATA_FIELD" ] || [ "$DATA_FIELD" = '"data":""' ]; then
    echo "❌ data 字段为空！"
else
    echo "✓ data 字段有内容: $DATA_FIELD"
fi

echo "========================================="
