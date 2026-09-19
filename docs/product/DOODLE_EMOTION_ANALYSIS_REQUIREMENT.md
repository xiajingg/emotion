# 涂鸦情绪分析需求与实现说明

## 1. 功能目标

在小程序首页增加“画一下”输入方式。用户可以在画板上随手涂鸦，提交后系统将涂鸦图片上传到 RustFS，再由后端调用支持图片输入的 Ollama 多模态模型生成情绪解读卡。

功能输出复用现有情绪解压卡结构：

- 情绪标签
- AI 涂鸦解读
- 3 条即时解压动作
- 4 种表达话术
- 答案之书短句
- 可分享解压卡
- 写入情绪记录和周报数据源

## 2. 用户使用路径

1. 进入小程序首页。
2. 在输入区选择“画一下”。
3. 选择颜色、笔刷粗细，在画板上随便画几笔。
4. 可选当前感受：压力、烦躁、委屈、空空的、累了。
5. 点击“解读我的涂鸦”。
6. 小程序导出画布图片并上传到 RustFS。
7. 后端读取 RustFS 图片，调用 Ollama 图片模型分析。
8. 页面展示情绪解压卡结果。
9. 用户可以分享解压卡，记录也会进入历史情绪数据。

## 3. 产品边界

这个功能不是心理测验，也不是心理诊断。

AI Prompt 明确要求：

- 颜色、线条、构图、留白、密度只能作为表达线索。
- 禁止输出“你一定是”“说明你有心理问题”等绝对化判断。
- 使用“像是”“可能”“这幅图给人的感觉是”等温和表达。
- 不承诺治疗效果。
- 如识别到危机表达，只建议联系可信任的人或当地紧急帮助渠道。

## 4. 前端实现

主要文件：

- `miniprogram/pages/index/index.wxml`
- `miniprogram/pages/index/index.js`
- `miniprogram/pages/index/index.wxss`
- `miniprogram/utils/request.js`

### 4.1 页面结构

首页输入区新增两个模式：

- `写一句`
- `画一下`

`画一下`模式包含：

- 颜色选择
- 笔刷粗细选择
- 撤销
- 清空
- 画布
- 当前感受标签

### 4.2 绘制数据

前端记录每一笔：

- `color`：颜色
- `width`：笔刷粗细
- `points`：触摸点位

同时记录：

- 绘制开始时间
- 总笔数
- 点位数量
- 涂鸦覆盖范围
- 粗线条比例
- 用户选择的当前感受

这些统计信息会作为辅助文本传给后端，但真正分析依据是上传后的图片。

### 4.3 图片上传

提交时前端执行：

1. `wx.canvasToTempFilePath` 导出画布为 JPEG。
2. `wx.uploadFile` 上传到 `/files/upload`。
3. 上传成功后拿到 `uploadFileId`。
4. 调用 `/user/api/v1/submitDrawing`。

请求体示例：

```json
{
  "uploadFileId": 123,
  "selectedMood": "压力",
  "drawingMeta": "用户没有输入文字，而是画了一张情绪涂鸦。以下是小程序记录的绘制过程信息...",
  "drawingShareText": "我画了一张情绪涂鸦 · 压力，用了8笔把这一刻放下来。"
}
```

## 5. 后端实现

主要文件：

- `backend/api/src/main/java/com/emotion/api/controller/UserController.java`
- `backend/api/src/main/java/com/emotion/api/dto/SubmitDrawingDTO.java`
- `backend/api/src/main/java/com/emotion/api/service/OllamaDirectService.java`
- `backend/api/src/main/java/com/emotion/api/util/RustFsUtil.java`
- `backend/api/src/main/resources/application-example.yml`

### 5.1 全局 RustFS 服务

RustFS 是本机全局图片存储服务，不属于单个项目目录。

当前全局服务约定：

- API 地址：`http://127.0.0.1:9000`
- 控制台地址：`http://127.0.0.1:9001`
- 数据目录：`/opt/homebrew/var/rustfs`
- bucket：`emotion`
- accessKey：`rustfs_your_access_key_here`

本项目只通过配置连接这个全局服务，不在项目目录内保存 RustFS 数据。

### 5.2 上传图片

已有接口：

```http
POST /files/upload
```

入参：

- multipart `file`

处理：

1. `RustFsUtil.uploadFile` 上传文件到 RustFS。
2. 写入 `image_upload_record`。
3. 返回图片记录 ID。

### 5.3 提交涂鸦分析

新增接口：

```http
POST /user/api/v1/submitDrawing
```

处理逻辑：

1. 校验 `uploadFileId` 是否存在。
2. 校验图片是否属于当前登录用户。
3. 根据 `imageUrl` 从 RustFS 读取图片 bytes。
4. 调用 `OllamaDirectService.analyzeDrawingEmotion(drawingMeta, imageBytes)`。
5. Ollama 请求中使用 `images` 字段传入 base64 图片。
6. 返回现有情绪解压卡结构。
7. 复用 `userService.saveSubmit` 写入情绪记录。
8. 通过 `uploadFileId` 写入 `image_text` 关联表。

### 5.4 Ollama 图片调用

Ollama 请求结构：

```json
{
  "model": "qwen3.5:9b",
  "stream": false,
  "think": false,
  "messages": [
    {
      "role": "user",
      "content": "涂鸦情绪解读 Prompt",
      "images": ["base64-image"]
    }
  ]
}
```

模型名配置：

```yaml
ollama:
  vision-model: ${OLLAMA_VISION_MODEL:${OLLAMA_MODEL:qwen3.5:9b}}
```

如果生产环境的图片模型名不是 `qwen3.5:9b`，设置环境变量：

```bash
export OLLAMA_VISION_MODEL=你的图片模型名
```

## 6. 数据库表

本功能没有新增数据库表，复用已有表。

### 6.1 `image_upload_record`

用途：保存上传到 RustFS 的图片记录。

关键字段：

- `id`：图片记录 ID
- `user_id`：上传用户 ID
- `image_url`：RustFS 文件 URL
- `upload_time`：上传时间

对应实体：

```java
ImageUploadRecord
```

### 6.2 `user_text_interactions`

用途：保存一次情绪分析记录。

涂鸦分析中：

- `input_text` 保存展示文案，例如“我画了一张情绪涂鸦 · 压力，用了8笔把这一刻放下来。”
- `ai_response` 保存情绪解压卡 JSON
- `ai_text` 保存兼容旧版周报/历史逻辑的 AI 文本
- `type = 0`，进入普通情绪记录

### 6.3 `image_text`

用途：关联情绪记录和图片。

字段：

- `text_id`：`user_text_interactions.id`
- `img_id`：`image_upload_record.id`

`userService.saveSubmit` 已经支持根据 `SubmitTextDTO.uploadFileId` 写入该关联。

## 7. 返回结构

`/user/api/v1/submitDrawing` 返回结构与 `/user/api/v1/submitText` 保持一致：

```json
{
  "emotion_tag": "情绪打结中",
  "ai_comment": "这幅图像是把很多没说出口的压力先放在纸面上了，先别急着解释自己。",
  "relief_actions": [
    "慢慢呼吸30秒",
    "把担心写成一句话",
    "先离开现场喝口水"
  ],
  "replies": {
    "high_eq": "...",
    "crazy": "...",
    "gentle": "...",
    "sarcastic": "..."
  },
  "answer_book": "先稳住自己"
}
```

## 8. 上线和验证

### 8.1 全局 RustFS

本地启动命令：

```bash
launchctl submit -l com.local.rustfs -- /opt/homebrew/bin/rustfs server \
  --address :9000 \
  --console-enable \
  --console-address :9001 \
  --access-key rustfs_your_access_key_here \
  --secret-key rustfs_your_secret_key_here \
  /opt/homebrew/var/rustfs
```

验证：

```bash
lsof -nP -iTCP:9000 -sTCP:LISTEN
lsof -nP -iTCP:9001 -sTCP:LISTEN
curl --max-time 3 -I http://127.0.0.1:9000
```

`curl` 返回 `HTTP/1.1 501 Not Implemented` 也代表 RustFS API 已响应；这是因为根路径的 `HEAD` 请求不是业务接口。

### 8.2 后端

需要重新编译并重启后端。

验证命令：

```bash
cd /Users/xiajing/emotion/backend
mvn -pl api -DskipTests compile
```

启动方式按当前环境执行，例如：

```bash
mvn -pl api spring-boot:run
```

如果生产环境图片模型名不同，需要配置：

```bash
OLLAMA_VISION_MODEL=你的图片模型名
```

### 8.3 前端

需要重新编译小程序：

1. 打开微信开发者工具。
2. 打开 `/Users/xiajing/emotion/miniprogram`。
3. 点击“编译”。
4. 首页应看到 `写一句 / 画一下`。
5. 点击 `画一下`，画几笔后点击 `解读我的涂鸦`。

### 8.4 预期结果

成功后应看到：

- 页面展示情绪解压卡。
- RustFS 中出现一张涂鸦图片。
- `image_upload_record` 新增一条图片记录。
- `user_text_interactions` 新增一条情绪分析记录。
- `image_text` 新增一条情绪记录和图片的关联。

## 9. 当前限制

- 图片上传依赖 RustFS 配置正确。
- Ollama 模型必须支持图片输入。
- 前端暂未做图片压缩尺寸控制，只导出当前画布 JPEG。
- 分享卡展示的是情绪结果卡，不直接嵌入原始涂鸦图片。
- 埋点尚未接入，后续应增加画板进入、开始绘制、提交、上传成功、分析成功、分享等事件。
