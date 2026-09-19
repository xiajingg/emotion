# Emotion · 情绪急救小程序

> 输入烦恼 → AI 情绪分析 → 解压建议 + 高情商回复。
> 一个完整的**微信小程序 + Java 后端**全栈项目，从产品调研到线上运行。

面向「压力大、内耗重、想找人说说但不想真的找人」的年轻用户。用户输入一段烦恼文字（或手绘涂鸦），
服务端调用大模型做情绪识别与共情回复，流式返回结果，并沉淀为可回顾的情绪记录、周报和分享卡。

---

## 项目概览

| | |
|---|---|
| **形态** | 微信小程序（原生，非框架） + Spring Boot 后端 |
| **后端规模** | 273 个 Java 源文件 · 36 个测试 · 21 个 Controller · 70 个 Service · 22 个 MyBatis XML · 20 个 SQL 脚本 |
| **小程序规模** | 15 个页面 · 自定义组件 · `wx-charts` 图表 |
| **后端技术** | Java 17 · Spring Boot 3.2.5 · MyBatis-Plus · MySQL · Redis · Spring Security(JWT) |
| **AI 能力** | Ollama（Spring AI `ChatClient` + OkHttp 直连双通道）· JSON Schema 结构化输出 · SSE 流式推送 |
| **基础设施** | Druid · RustFS/MinIO 对象存储 · 微信支付 · DingTalk 告警 |

---

## 架构

```
┌──────────────────────────┐
│   微信小程序 (原生)        │  miniprogram/
│   15 页面 · 自定义组件     │
└────────────┬─────────────┘
             │ HTTPS / SSE 流式
┌────────────▼─────────────┐
│   Spring Boot 3 (api)     │  backend/api/
│  ┌────────────────────┐  │
│  │ Controller 21 个    │  │
│  ├────────────────────┤  │
│  │ Service 层          │  │
│  ├────────────────────┤  │
│  │ MyBatis-Plus + Druid│  │
│  └────────────────────┘  │
└──┬────┬────┬────┬────┬───┘
   │    │    │    │    │
 MySQL Redis Ollama RustFS 微信API
```

分层说明与包结构见 [`backend/CLAUDE.md`](backend/CLAUDE.md)。

---

## 目录结构

```
emotion/
├── backend/                    # Java 后端
│   ├── pom.xml                 # 聚合 POM
│   ├── api/                    # 业务模块（Spring Boot 启动模块）
│   ├── README.md               # 后端说明
│   └── CLAUDE.md               # 架构与约定
├── miniprogram/                # 微信小程序（原生）
├── docs/                       # 全部文档
│   ├── product/                # 产品调研、需求排期、项目分析
│   ├── dev-reports/            # 51 份开发报告（问题定位与修复记录）
│   ├── ops/                    # 运维与工具手册
│   └── archive/                # 历史快照
├── scripts/                    # 开发/运维脚本
└── README.md
```

> `docs/dev-reports/` 里的 51 份报告是这个项目最有信息量的部分之一 —— 它们记录了
> 从 SSE 断流、Ollama JSON 解析失败、分享卡权限问题到星座运势计算的一系列真实排障过程。
> 索引见 [`docs/README.md`](docs/README.md)。

---

## 主要功能

| 模块 | 说明 |
|---|---|
| **情绪分析** | 文字 / 涂鸦输入 → 大模型情绪识别 → 情绪值、情绪配比、共情回复；SSE 流式输出，首字延迟显著低于整包等待 |
| **情绪记录与回顾** | 历史情绪时间线、情绪趋势图、情绪评分圆环 |
| **周报推送** | 每周情绪汇总生成 + 微信订阅消息定时推送 |
| **分享卡** | 把某条情绪记录生成为可保存/分享的卡片图（Canvas 绘制） |
| **答案之书** | 1000 条预设答案 + AI 个性化解读 |
| **星座运势** | Astronomy Engine 真实天文计算（10 个天体位置）→ 规则引擎 → AI 生成 12 星座日运 |
| **好友互动** | 好友绑定、双人情绪互动、关系分析 |
| **每日激励 / 签到 / 喝水打卡** | 习惯养成类轻功能 |
| **英语学习** | 单词、句子结构、全文翻译等子模块 |

---

## 快速开始

### 后端

```bash
cd backend

# 1. 准备配置（dev/test/prod 配置不入库，从 example 复制）
cp api/src/main/resources/application-example.yml \
   api/src/main/resources/application-dev.yml
# 编辑填入 MySQL / Redis / Ollama / RustFS 连接信息

# 2. 编译
mvn clean package -DskipTests

# 3. 启动（application.yml 默认 profile 是 prod，本地必须显式指定）
mvn -pl api spring-boot:run -Dspring-boot.run.profiles=dev
```

外部依赖：MySQL 8、Redis、Ollama（`ollama serve` + `ollama pull qwen3.5:9b`）、
RustFS 或任意 MinIO 兼容存储。详细步骤见 [`backend/README.md`](backend/README.md)。

### 小程序

用微信开发者工具打开 `miniprogram/` 目录即可。

后端地址硬编码在 `miniprogram/utils/request.js` 的 `baseURL`（当前指向作者的服务地址），
另有 `subpage1/pages/reminisce/index.js`、`detail.js` 两处重复定义。改成自己的服务地址即可。

---

## 工程要点

几处值得一提的实现：

**SSE 流式输出与连接生命周期管理**
用 `SseEmitter` 把大模型逐字返回的结果实时推给小程序。真实网络下会遇到 `Broken pipe`、
`IllegalStateException: ResponseBodyEmitter has already completed` 等竞态问题，
这里做了 emitter 完成态的原子判断与异常兜底。
详见 [`docs/dev-reports/SSE_FINAL_FIX.md`](docs/dev-reports/SSE_FINAL_FIX.md)。

**大模型 JSON 输出的可靠性**
模型经常在 JSON 外面包一层 ` ```json ` 或加解释性文字，导致反序列化失败。
这里用 Spring AI 的 JSON Schema 约束 + 多级降级解析（完整 JSON → 提取代码块 → 正则兜底）。
详见 [`docs/dev-reports/OLLAMA_JSON_FINAL_SOLUTION.md`](docs/dev-reports/OLLAMA_JSON_FINAL_SOLUTION.md)。

**真实天文计算而非硬编码**
星座运势没有用查表或编造，而是引入 Astronomy Engine 计算指定日期的行星位置，
再用规则引擎推导占星要素，最后交给大模型润色成文。

**JWT + Redis 的有状态会话**
Spring Security 无状态鉴权，但 token 状态存 Redis（`JWT_<token>`），
支持主动失效。自定义 `@CurrentUser` 参数解析器注入 `UserPrincipal`。

---

## 文档

| 目录 | 内容 |
|---|---|
| [`docs/product/`](docs/product/) | 产品调研、需求排期、项目分析 |
| [`docs/dev-reports/`](docs/dev-reports/) | 51 份开发/修复报告 |
| [`docs/ops/`](docs/ops/) | RustFS 运维、小程序压缩工具、流式接口指南 |
| [`docs/README.md`](docs/README.md) | 文档索引 |

---

## 说明

本项目为个人独立开发。仓库中不包含任何真实环境配置、数据库密码或第三方密钥 ——
`application-dev/test/prod.yml`、`*.p12` 证书、小程序私有配置均已 gitignore，
`application-example.yml` 仅提供占位模板。

运行时请在 `api/src/main/resources/` 下自行创建配置文件，或通过环境变量注入。
