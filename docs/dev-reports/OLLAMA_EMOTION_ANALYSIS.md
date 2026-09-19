# 情绪分析功能 - Ollama AI 集成说明

## 概述

已将原来的远程 HTTP 调用（`http://100.107.179.128:8090/api/ai/chart`）替换为本地 Ollama AI 服务进行情绪分析。

## 架构变更

### 之前
```
UserController → RestTemplate → 远程AI服务 (100.107.179.128:8090)
```

### 现在
```
UserController → OllamaChatService → Ollama (localhost:11434) → qwen3.5:9b 模型
```

## 修改的文件

### 1. 新增文件

#### EmotionAnalysisResponse.java
- 路径: `api/src/main/java/com/emotion/api/dto/EmotionAnalysisResponse.java`
- 作用: Ollama AI 返回的结构化响应 DTO
- 字段:
  - `score`: Integer (1-100) - 情绪分数
  - `suggestion`: String - AI 建议或安慰语

#### emotion-analysis-response.json
- 路径: `api/src/main/resources/schema/emotion-analysis-response.json`
- 作用: JSON Schema 定义，描述返回值结构

### 2. 修改文件

#### OllamaChatService.java
- 新增方法: `analyzeEmotion(String text)`
- 功能: 使用 Ollama AI 进行情绪分析，返回结构化数据
- Prompt 设计:
  ```
  请分析以下文本的情绪，并给出一个1-100的情绪分数和一句简短的建议或安慰语。
  分数定义：1-10绝望，11-20痛苦，21-30愤怒，31-40沮丧，41-50平静，51-60好奇，61-70满足，71-80开心，81-90兴奋，91-100狂喜
  文本内容：{用户输入}
  
  请以JSON格式返回，包含score(整数1-100)和suggestion(字符串)两个字段。
  ```

#### UserController.java
- 注入: `OllamaChatService`
- 修改位置: `/api/v1/submitText` 接口（约第243行）
- 变更:
  - 移除: `restTemplate.getForObject()` 远程调用
  - 新增: `ollamaChatService.analyzeEmotion()` 本地调用
  - 添加异常处理和日志记录

## JSON Schema 定义

```json
{
  "type": "object",
  "properties": {
    "score": {
      "type": "integer",
      "minimum": 1,
      "maximum": 100
    },
    "suggestion": {
      "type": "string",
      "minLength": 1,
      "maxLength": 500
    }
  },
  "required": ["score", "suggestion"]
}
```

## 情绪分数映射表

| 分数范围 | 情绪标签 | 说明 |
|---------|---------|------|
| 1-10    | 绝望    | 完全失去希望，情绪崩溃 |
| 11-20   | 痛苦    | 强烈的不适感，难以承受 |
| 21-30   | 愤怒    | 感到被冒犯，情绪激动 |
| 31-40   | 沮丧    | 失望、低落，缺乏动力 |
| 41-50   | 平静    | 情绪稳定，内心安宁 |
| 51-60   | 好奇    | 对未知充满兴趣，想要探索 |
| 61-70   | 满足    | 感到舒适，小确幸 |
| 71-80   | 开心    | 明显的愉悦感，心情愉快 |
| 81-90   | 兴奋    | 情绪高涨，充满期待 |
| 91-100  | 狂喜    | 极度快乐，情绪巅峰 |

## 配置要求

### application-dev.yml
```yaml
spring:
  ai:
    ollama:
      base-url: http://localhost:11434
      chat:
        model: qwen3.5:9b
        options:
          temperature: 0.7
```

### 前置条件
1. 安装 Ollama: https://ollama.ai
2. 拉取模型: `ollama pull qwen3.5:9b`
3. 启动 Ollama 服务: `ollama serve`

## API 响应示例

### 成功响应
```json
{
  "code": 200,
  "data": {
    "emotion": "开心",
    "emotionRatio": 75,
    "reminder": "今天心情很好呢，保持这份快乐！"
  }
}
```

### 错误响应
```json
{
  "code": 500,
  "message": "AI服务异常"
}
```

## 优势

1. **本地化**: 不再依赖远程服务，降低网络延迟
2. **可控性**: 可以自定义模型和参数
3. **隐私性**: 数据在本地处理，不上传到外部服务器
4. **结构化输出**: 使用 JSON Schema 确保返回格式一致
5. **异常处理**: 完善的错误处理和日志记录

## 注意事项

1. 确保 Ollama 服务正在运行
2. 首次使用需要下载模型（约几GB）
3. 根据服务器性能选择合适的模型大小
4. 生产环境建议使用更稳定的模型版本
