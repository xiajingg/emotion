#!/bin/bash

# Ollama V2 Prompt 单元测试运行脚本

set -e

echo "=========================================="
echo "  Ollama V2 Prompt 单元测试"
echo "=========================================="
echo ""

PROJECT_DIR="/Users/xiajing/emotion/backend/api"

cd "$PROJECT_DIR"

# 颜色定义
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${YELLOW}Step 1: 检查 Ollama 服务...${NC}"

# 检查 Ollama 是否运行
if ! curl -s http://localhost:11434/api/tags > /dev/null 2>&1; then
    echo -e "${RED}❌ Ollama 服务未运行${NC}"
    echo "请先启动 Ollama："
    echo "  ollama serve"
    exit 1
else
    echo -e "${GREEN}✅ Ollama 服务正在运行${NC}"
fi

echo ""

echo -e "${YELLOW}Step 2: 检查模型...${NC}"

# 检查模型是否存在
if ! curl -s http://localhost:11434/api/tags | grep -q "qwen3.5:9b"; then
    echo -e "${YELLOW}⚠️  模型 qwen3.5:9b 未下载，正在下载...${NC}"
    ollama pull qwen3.5:9b
    echo -e "${GREEN}✅ 模型下载完成${NC}"
else
    echo -e "${GREEN}✅ 模型 qwen3.5:9b 已存在${NC}"
fi

echo ""

echo -e "${YELLOW}Step 3: 运行单元测试...${NC}"
echo ""

# 选择测试模式
echo "请选择测试模式："
echo "  1. 运行所有测试"
echo "  2. 运行单个测试（疲惫状态）"
echo "  3. 运行对比测试（V1 vs V2）"
echo ""
read -p "请输入选项 (1/2/3): " choice

case $choice in
    1)
        echo ""
        echo -e "${YELLOW}运行所有测试...${NC}"
        mvn test -Dtest=OllamaV2PromptUnitTest
        ;;
    2)
        echo ""
        echo -e "${YELLOW}运行单个测试：疲惫状态...${NC}"
        mvn test -Dtest=OllamaV2PromptUnitTest#testTiredState_ShouldReturnEmotionLabelAndProperLength
        ;;
    3)
        echo ""
        echo -e "${YELLOW}运行对比测试：V1 vs V2...${NC}"
        mvn test -Dtest=OllamaV2PromptUnitTest#testCompareV1AndV2_V2ShouldHaveEmotionAndLongerSuggestion
        ;;
    *)
        echo -e "${RED}无效选项${NC}"
        exit 1
        ;;
esac

echo ""
echo "=========================================="
echo -e "${GREEN}  ✅ 测试完成！${NC}"
echo "=========================================="
echo ""
echo "查看测试结果："
echo "  - 控制台输出包含详细的日志"
echo "  - 测试报告位于: target/surefire-reports/"
echo ""
