# Ollama V2 Prompt 单元测试说明

## 📦 交付内容

### 1. 单元测试类
**文件：** [OllamaV2PromptUnitTest.java](../../backend/api/src/test/java/com/emotion/api/service/OllamaV2PromptUnitTest.java)

包含 **8 个测试方法**：

| 测试方法 | 测试内容 | 验证点 |
|---------|---------|--------|
| `testTiredState` | 疲惫状态 | emotion 字段、suggestion 长度、无 AI 味 |
| `testHolidayExpectation` | 期待放假 | 高分（≥60）、正面情绪标签 |
| `testWorkPressure` | 工作压力 | 低分（≤40）、负面情绪标签 |
| `testSmallHappiness` | 小确幸 | 中等分数（50-80） |
| `testSocialFatigue` | 社交疲惫 | 复杂情绪识别、非简单词汇 |
| `testCompareV1AndV2` | V1 vs V2 对比 | V2 有 emotion、建议更长 |
| `testVeryShortText` | 极短文本边界 | 正确处理短文本 |
| `testLongText` | 较长文本边界 | 长期疲惫识别（≤40） |

---

### 2. 自动化测试脚本
**文件：** [run-ollama-v2-test.sh](../../scripts/run-ollama-v2-test.sh)

**功能：**
- ✅ 自动检查 Ollama 服务
- ✅ 自动检查模型是否下载
- ✅ 提供交互式测试选择
- ✅ 显示测试结果

---

## 🚀 快速开始

### 方法一：使用自动化脚本（推荐）

```bash
cd /Users/xiajing/emotion
./scripts/run-ollama-v2-test.sh
```

脚本会提示选择测试模式：
```
请选择测试模式：
  1. 运行所有测试
  2. 运行单个测试（疲惫状态）
  3. 运行对比测试（V1 vs V2）
```

---

### 方法二：直接使用 Maven 命令

#### 运行所有测试
```bash
cd /Users/xiajing/emotion/backend/api
mvn test -Dtest=OllamaV2PromptUnitTest
```

#### 运行单个测试
```bash
# 疲惫状态测试
mvn test -Dtest=OllamaV2PromptUnitTest#testTiredState_ShouldReturnEmotionLabelAndProperLength

# 对比测试
mvn test -Dtest=OllamaV2PromptUnitTest#testCompareV1AndV2_V2ShouldHaveEmotionAndLongerSuggestion
```

---

### 方法三：在 IDE 中运行

1. 打开 `OllamaV2PromptUnitTest.java`
2. 右键点击类名或测试方法
3. 选择 "Run Tests"

---

## 📊 测试验证点

### 1. 基本验证

```java
// 响应不为空
assertNotNull(response);

// 所有字段都有值
assertNotNull(response.getScore());
assertNotNull(response.getEmotion());
assertNotNull(response.getSuggestion());
```

---

### 2. 分数范围验证

```java
// 分数在 1-100 之间
assertTrue(response.getScore() >= 1 && response.getScore() <= 100);

// 负面情绪分数低
assertTrue(response.getScore() <= 40);

// 正面情绪分数高
assertTrue(response.getScore() >= 60);
```

---

### 3. 情绪标签验证

```java
// 情绪标签不为空
assertNotNull(response.getEmotion());

// 不是普通词
assertNotEquals("开心", response.getEmotion());
assertNotEquals("难过", response.getEmotion());

// 有画面感
assertFalse(emotion.equals("开心") || emotion.equals("累"));
```

---

### 4. 建议长度验证

```java
// V2 版本：80-150 字
int length = response.getSuggestion().length();
assertTrue(length >= 80 && length <= 150);
```

---

### 5. AI 味检测

```java
// 不应包含 AI 味重的表达
assertFalse(response.getSuggestion().contains("根据你的描述"));
assertFalse(response.getSuggestion().contains("建议调整心态"));
assertFalse(response.getSuggestion().contains("请保持积极"));
```

---

### 6. V1 vs V2 对比

```java
// V2 必须包含 emotion 字段
assertNotNull(v2Response.getEmotion());

// V2 建议长度 >= 80
assertTrue(v2Length >= 80);

// V2 建议长度 >= V1
assertTrue(v2Length >= v1Length);
```

---

## 🔍 预期输出示例

### 测试案例1：疲惫状态

```
========== 测试案例1：疲惫状态 ==========
输入文本: 今天正常上班，正常聊天，但回家后什么都不想做。
✅ 分数: 35
✅ 情绪标签: 情绪透支
✅ 建议: 你最近的累，不像是身体没休息好，更像是一直在硬撑。白天正常工作、正常聊天，但情绪其实已经偷偷消耗很久了。你不是懒，只是真的太久没有让自己放松过了。
✅ 建议长度: 86 字
=====================================
```

---

### 对比测试：V1 vs V2

```
========== 对比测试：V1 vs V2 ==========
输入文本: 今天工作很不顺心，被领导批评了。

----- V1 版本（原版）-----
分数: 28
情绪标签: null
建议: 抱抱你，工作中的挫折确实让人难受。记得照顾好自己，明天会更好！
建议长度: 32 字

----- V2 版本（优化版）-----
分数: 28
情绪标签: 委屈压抑
建议: 被批评的那一刻，心里肯定特别不好受。那种委屈和无力感，可能让你觉得自己已经很努力了却还是不够好。但其实，一次批评不代表你的全部，给自己一点时间消化情绪，你已经在做得很好了。
建议长度: 98 字

=====================================
✅ V2 优势验证通过：
   - V2 包含 emotion 字段: 委屈压抑
   - V2 建议长度: 98 字 (V1: 32 字)
```

---

## ⚠️ 前置条件

### 1. Ollama 服务必须运行

```bash
# 启动 Ollama
ollama serve
```

### 2. 模型必须已下载

```bash
# 下载模型
ollama pull qwen3.5:9b
```

### 3. 后端服务必须启动

确保 Spring Boot 应用正在运行，或者测试时会自动启动。

---

## 📝 测试报告

测试完成后，可以在以下位置查看详细报告：

```
backend/api/target/surefire-reports/
├── TEST-com.emotion.api.service.OllamaV2PromptUnitTest.xml
└── com.emotion.api.service.OllamaV2PromptUnitTest.txt
```

---

## 🎯 测试通过标准

所有测试必须满足以下条件才算通过：

1. ✅ **所有断言通过** - 无 AssertionError
2. ✅ **emotion 字段有值** - 不为 null 且不为空
3. ✅ **suggestion 长度符合** - 80-150 字
4. ✅ **分数合理** - 符合情绪状态预期
5. ✅ **无 AI 味表达** - 不包含禁用的短语
6. ✅ **V2 优于 V1** - 有 emotion 字段且建议更长

---

## 🐛 常见问题

### Q1: 测试超时怎么办？

**A:** Ollama 推理可能需要较长时间，可以调整超时设置：

```xml
<!-- pom.xml -->
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <configuration>
        <forkCount>1</forkCount>
        <reuseForks>false</reuseForks>
        <testFailureIgnore>false</testFailureIgnore>
    </configuration>
</plugin>
```

---

### Q2: 模型未找到怎么办？

**A:** 确保模型已下载：

```bash
ollama list  # 查看已安装的模型
ollama pull qwen3.5:9b  # 下载模型
```

---

### Q3: 测试失败但日志显示有结果？

**A:** 检查断言条件是否过于严格，可能需要调整：

```java
// 如果建议长度偶尔超出范围，可以放宽条件
assertTrue(suggestionLength >= 70 && suggestionLength <= 160);
```

---

### Q4: 如何只运行部分测试？

**A:** 使用 Maven 的测试过滤器：

```bash
# 运行包含 "Tired" 的测试
mvn test -Dtest=OllamaV2PromptUnitTest#*Tired*

# 运行排除 "Compare" 的测试
mvn test -Dtest=OllamaV2PromptUnitTest#!*Compare*
```

---

## 💡 测试技巧

### 1. 观察日志输出

测试执行时会输出详细日志，包括：
- 输入文本
- 分数
- 情绪标签
- 建议内容
- 建议长度

这些信息可以帮助判断 AI 的输出质量。

---

### 2. 手动验证输出

即使测试通过，也建议人工阅读几条输出，确认：
- 情绪标签是否有画面感
- 建议是否像真人对话
- 是否避免了 AI 味

---

### 3. 多次运行取平均

由于 LLM 的输出可能有波动，建议多次运行测试，观察输出的稳定性。

---

## 📈 后续优化

如果测试结果显示某些方面不理想，可以：

1. **调整 prompt** - 修改 `buildPromptV2()` 中的指令
2. **更换模型** - 尝试更大的模型或其他模型
3. **增加示例** - 在 prompt 中添加更多示例
4. **调整参数** - 修改 temperature、top_p 等参数

---

**最后更新：** 2026-05-10  
**测试文件：** OllamaV2PromptUnitTest.java  
**适用项目：** backend
