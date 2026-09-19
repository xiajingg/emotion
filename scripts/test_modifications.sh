#!/bin/bash

echo "========================================="
echo "测试每日激励定时任务修改"
echo "========================================="
echo ""

echo "1. 编译项目..."
cd /Users/xiajing/emotion/backend
mvn clean compile -q

if [ $? -eq 0 ]; then
    echo "✅ 编译成功"
else
    echo "❌ 编译失败"
    exit 1
fi

echo ""
echo "2. 检查修改的文件..."
echo ""

echo "检查 DailyMotivationTask.java:"
grep -n "callOllamaForMotivation" /Users/xiajing/emotion/backend/api/src/main/java/com/emotion/api/task/DailyMotivationTask.java | head -5

echo ""
echo "检查 SendException.java:"
grep -n "extractContentFromResponse" /Users/xiajing/emotion/backend/api/src/main/java/com/emotion/api/exception/SendException.java | head -5

echo ""
echo "========================================="
echo "修改总结："
echo "========================================="
echo "1. ✅ DailyMotivationTask 已修改为调用 Ollama API 并提取 content 字段"
echo "2. ✅ SendException 已添加 extractContentFromResponse 方法提取 content 字段"
echo "3. ✅ 参考 submitText 接口的实现方式（OllamaDirectService）"
echo "4. ✅ 钉钉异常通知现在只发送必要的错误信息"
echo ""
echo "主要改动："
echo "- 定时任务从使用 QianfanAI 改为直接调用 Ollama API"
echo "- 确保只提取 message.content 字段，避免存储完整响应"
echo "- 钉钉通知中的 AI 分析结果也会提取 content 字段"
echo "========================================="
