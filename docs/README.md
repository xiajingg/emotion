# 文档索引

本目录收纳项目全部文档。开发过程中的 51 份排障报告集中在 `dev-reports/` —— 它们是这个项目里
信息密度最高的部分，记录了真实问题的定位过程，而不只是最终结论。

---

## 📁 product/ — 产品与需求

| 文档 | 内容 |
|---|---|
| [产品调研文档.md](product/产品调研文档.md) | 目标用户、竞品、功能取舍 |
| [PROJECT_ANALYSIS.md](product/PROJECT_ANALYSIS.md) | 全项目接口与业务链路分析（逐模块） |
| [需求排期/](product/需求排期/) | 迭代需求清单 |
| [DOODLE_EMOTION_ANALYSIS_REQUIREMENT.md](product/DOODLE_EMOTION_ANALYSIS_REQUIREMENT.md) | 涂鸦情绪分析需求与设计 |
| [WEEKLY_REPORT_IMPLEMENTATION_COMPLETE.md](product/WEEKLY_REPORT_IMPLEMENTATION_COMPLETE.md) | 情绪周报功能实现 |
| [DAILY_MOTIVATION_MODIFICATION_REPORT.md](product/DAILY_MOTIVATION_MODIFICATION_REPORT.md) | 每日激励改造 |

---

## 📁 dev-reports/ — 开发与排障报告

### SSE 流式输出（6 篇）

后端用 `SseEmitter` 把大模型结果实时推给小程序，这一路踩坑最多。

| 文档 | 内容 |
|---|---|
| [SSE_STREAMING_IMPLEMENTATION.md](dev-reports/SSE_STREAMING_IMPLEMENTATION.md) | 流式方案选型与落地 |
| [SSE_BROKEN_PIPE_FIX.md](dev-reports/SSE_BROKEN_PIPE_FIX.md) | `Broken pipe`：客户端提前断开导致的写异常 |
| [SSE_ILLEGAL_STATE_FIX.md](dev-reports/SSE_ILLEGAL_STATE_FIX.md) | `ResponseBodyEmitter has already completed` 竞态 |
| [SSE_SECURITY_FIX.md](dev-reports/SSE_SECURITY_FIX.md) | 流式接口的鉴权与越权问题 |
| [SSE_FINAL_FIX.md](dev-reports/SSE_FINAL_FIX.md) | 最终稳定版方案 |
| [SSE_TESTING_GUIDE.md](dev-reports/SSE_TESTING_GUIDE.md) | 流式接口测试方法 |

### Ollama / Spring AI（11 篇）

大模型接入与输出可靠性。

| 文档 | 内容 |
|---|---|
| [OLLAMA_CHATCLIENT_FIX_REPORT.md](dev-reports/OLLAMA_CHATCLIENT_FIX_REPORT.md) | Spring AI `ChatClient` 接入问题 |
| [OLLAMA_JSON_FINAL_SOLUTION.md](dev-reports/OLLAMA_JSON_FINAL_SOLUTION.md) | **JSON 输出多级降级解析**（含代码块剥离） |
| [OLLAMA_JSON_FIX.md](dev-reports/OLLAMA_JSON_FIX.md) / [OLLAMA_JSON_MODE_FIX.md](dev-reports/OLLAMA_JSON_MODE_FIX.md) | JSON 模式调优过程 |
| [SPRING_AI_JSON_SCHEMA_USAGE.md](dev-reports/SPRING_AI_JSON_SCHEMA_USAGE.md) | 用 JSON Schema 约束模型输出 |
| [OLLAMA_PERFORMANCE_FIX.md](dev-reports/OLLAMA_PERFORMANCE_FIX.md) / [OLLAMA_PERFORMANCE_OPTIMIZATION.md](dev-reports/OLLAMA_PERFORMANCE_OPTIMIZATION.md) | 响应延迟优化 |
| [OLLAMA_MODEL_SWITCH.md](dev-reports/OLLAMA_MODEL_SWITCH.md) | 模型切换与对比 |
| [SPRING_AI_THINK_MODE_RESEARCH.md](dev-reports/SPRING_AI_THINK_MODE_RESEARCH.md) | 思维链模式调研 |
| [SPRING_AI_STREAM_FIX.md](dev-reports/SPRING_AI_STREAM_FIX.md) | Spring AI 流式修复 |
| [OLLAMA_EMOTION_ANALYSIS.md](dev-reports/OLLAMA_EMOTION_ANALYSIS.md) | 情绪分析提示词设计 |
| [OLLAMA_FINAL_SOLUTION.md](dev-reports/OLLAMA_FINAL_SOLUTION.md) / [OLLAMA_V2_*.md](dev-reports/) | 最终方案与 V2 部署/测试 |

### 首页改版（10 篇）

`HOME_PAGE_*` —— 首页信息架构与交互重构的完整过程，从分析、设计决策、实现到视觉对比。

建议阅读顺序：[REFACTOR_ANALYSIS](dev-reports/HOME_PAGE_REFACTOR_ANALYSIS.md) →
[DESIGN_DECISIONS](dev-reports/HOME_PAGE_DESIGN_DECISIONS.md) →
[DEEP_REFACTOR_PLAN](dev-reports/HOME_PAGE_DEEP_REFACTOR_PLAN.md) →
[IMPLEMENTATION_COMPLETE](dev-reports/HOME_PAGE_IMPLEMENTATION_COMPLETE.md) →
[VISUAL_COMPARISON](dev-reports/HOME_PAGE_VISUAL_COMPARISON.md)

### 分享卡与心情卡（9 篇）

`SHARE_*` / `MOOD_CARD_*` / `SAVE_*` / `SCORE_CIRCLE_*` / `TIMELINE_*` ——
Canvas 绘制、相册权限、分享入口等小程序端具体问题。

### 其他

| 文档 | 内容 |
|---|---|
| [ANSWER_BOOK_AI_EMPTY_FIX_REPORT.md](dev-reports/ANSWER_BOOK_AI_EMPTY_FIX_REPORT.md) | 答案之书 AI 返回空值 |
| [NOTIFICATION_SYSTEM_OPTIMIZATION.md](dev-reports/NOTIFICATION_SYSTEM_OPTIMIZATION.md) | 通知系统优化 |
| [MINIPROGRAM_OPTIMIZATION_GUIDE.md](dev-reports/MINIPROGRAM_OPTIMIZATION_GUIDE.md) | 小程序性能优化 |
| [FRONTEND_MODIFICATION_GUIDE.md](dev-reports/FRONTEND_MODIFICATION_GUIDE.md) | 前端改造指南 |
| [SYNC_CALL_MIGRATION.md](dev-reports/SYNC_CALL_MIGRATION.md) | 同步调用迁移 |
| [STREAM_IMPLEMENTATION_SUMMARY.md](dev-reports/STREAM_IMPLEMENTATION_SUMMARY.md) | 流式实现总结 |
| [SEO_OPTIMIZATION_REPORT.md](dev-reports/SEO_OPTIMIZATION_REPORT.md) | SEO 优化 |

---

## 📁 ops/ — 运维与工具

| 文档 | 内容 |
|---|---|
| [RUSTFS_OPERATION.md](ops/RUSTFS_OPERATION.md) | RustFS 对象存储本地启动与运维（数据目录 `/opt/homebrew/var/rustfs`） |
| [QUICK_START.md](ops/QUICK_START.md) | 后端快速启动 |
| [STREAM_API_GUIDE.md](ops/STREAM_API_GUIDE.md) | 流式接口对接说明 |
| [WX_CODE_MINIFIER_GUIDE.md](ops/WX_CODE_MINIFIER_GUIDE.md) | 小程序代码压缩工具 |
| [COMPRESS_TOOL_README.md](ops/COMPRESS_TOOL_README.md) | 压缩脚本说明 |

---

## 📁 archive/ — 历史快照

| 目录 | 内容 |
|---|---|
| `home-page-backup-20260509/` | 首页重构前的页面代码快照 |
| `商业思维/` | 早期商业思路草图 |

---

## 脚本

仓库根目录的 `scripts/` 下：

| 脚本 | 用途 |
|---|---|
| `compress-miniprogram.sh` / `compress-with-wx-minifier.sh` / `optimize-miniprogram.sh` | 小程序打包优化 |
| `run-ollama-v2-test.sh` | Ollama V2 提示词测试 |
| `test_stream_api.sh` | 流式接口联调（需先填 JWT_TOKEN） |
| `test_ai_explanation.sh` | 答案之书接口测试 |
| `test_modifications.sh` / `test_share_card_fix.sh` / `test_timeline_entrance.sh` | 回归验证脚本 |
| `apply_frontend_patch.sh` + `patches/frontend_patch.js` | 一次性前端补丁 |
