# Emotion Backend

情绪分析小程序的后端服务 —— Spring Boot 3 + MyBatis-Plus + Spring AI(Ollama)。

## 技术栈

| 层次 | 选型 |
|---|---|
| 运行时 | Java 17 |
| 框架 | Spring Boot 3.2.5 / Spring Security（无状态 JWT） |
| 持久层 | MyBatis-Plus 3.5.5 + Druid 连接池 + MySQL 8 |
| 缓存 | Redis（Lettuce），用于 JWT 会话与分布式锁 |
| AI | Ollama（Spring AI `ChatClient` + OkHttp 直连双通道）；另有智谱 AI、百度千帆工具类 |
| 对象存储 | RustFS / MinIO（MinIO Java SDK） |
| 支付 | 微信支付 Java SDK |
| 其他 | JWT(jjwt 0.12.5)、Hutool、DingTalk 告警 |

## 模块结构

```
backend/
├── pom.xml                 # 聚合 POM（packaging=pom）
└── api/                    # 唯一的业务模块，Spring Boot 启动模块
    ├── src/main/java/com/emotion/api/
    │   ├── EmotionApplication.java     # 启动类（@EnableScheduling / @EnableAsync）
    │   ├── controller/                 # REST 接口
    │   ├── service/ + service/impl/    # 业务逻辑
    │   ├── repository/                 # dao / mapper / po / config
    │   ├── entity/  dto/  vo/          # 数据模型
    │   ├── config/                     # Security / JWT / Redis / Ollama 配置
    │   ├── task/                       # @Scheduled 定时任务
    │   └── util/  payment/  fanyi/     # 集成工具
    ├── src/main/resources/
    │   ├── mybatis/mapper/rds/         # MyBatis XML
    │   └── application*.yml            # 配置（仅 example 入库）
    └── sql/                            # 建表与初始化脚本
```

## 快速开始

### 1. 准备配置

`application-dev.yml` / `application-test.yml` / `application-prod.yml` **不在仓库里**（已 gitignore）。
以 `application-example.yml` 为模板，复制一份并填入你自己的连接信息：

```bash
cp api/src/main/resources/application-example.yml \
   api/src/main/resources/application-dev.yml
```

需要准备的外部依赖：

- MySQL 8（建库后执行 `api/sql/` 下的脚本）
- Redis
- Ollama：`ollama serve`，并 `ollama pull qwen3.5:9b`
- RustFS / MinIO（本地启动方式见 [`../docs/ops/RUSTFS_OPERATION.md`](../docs/ops/RUSTFS_OPERATION.md)）

### 2. 构建与运行

```bash
# 编译（跳过测试）
mvn clean package -DskipTests

# 本地启动，显式指定 profile（application.yml 默认是 prod）
mvn -pl api spring-boot:run -Dspring-boot.run.profiles=dev

# 或直接跑打包产物
java -jar api/target/emotion-api-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev
```

服务默认监听 `8080`。

### 3. 测试

```bash
mvn -pl api test                                      # 全部
mvn -pl api -Dtest=StreamEmotionTest test             # 单个类
mvn -pl api -Dtest=StreamEmotionTest#methodName test  # 单个方法
```

> ⚠️ 部分集成测试会真实调用 Ollama 与数据库，耗时长。跑全量前先确认依赖已就绪。

## 环境变量

`application-example.yml` 中的外部依赖都通过环境变量注入，带默认值：

| 变量 | 用途 | 默认 |
|---|---|---|
| `EMOTION_DB_URL` / `_USERNAME` / `_PASSWORD` | 主库连接 | `localhost:3306` |
| `JWT_SECRET` | JWT 签名密钥（Base64 32 字节） | 必填 |
| `ZHIPU_API_KEY` | 智谱 AI Key | 必填 |
| `REDIS_HOST` / `REDIS_PORT` | Redis | `localhost:6379` |
| `OLLAMA_BASE_URL` / `OLLAMA_MODEL` | Ollama 地址与模型 | `localhost:11434` / `qwen3.5:9b` |
| `RUSTFS_ENDPOINT` / `_ACCESS_KEY` / `_SECRET_KEY` / `_BUCKET` | 对象存储 | `127.0.0.1:9000` / `emotion` |
| `WECHAT_APP_ID` / `_APP_SECRET` | 微信小程序 | — |
| `WECHAT_WEEKLY_REPORT_TEMPLATE_ID` | 周报订阅消息模板 | — |

## 主要业务域

- **情绪分析**：`EmotionController`、`UserController` 的 SSE 流式接口、`OllamaChatService`、`OllamaDirectService`
- **微信小程序链路**：登录换 openId、签发/校验 JWT、订阅消息、二维码
- **星座运势**：Astronomy Engine 天文计算 + 规则引擎 + 定时生成推送
- **周报与通知**：情绪周报生成、订阅、定时推送
- **好友互动**：绑定、双人互动、关系分析
- **其他**：答案之书、每日激励、签到、喝水打卡、英语学习、文件上传

> 支付相关代码存在（微信支付 + 支付流水），但 `PaymentController` 当前处于注释停用状态。

## 已知事项

- 编译目标为 **Java 17**（由 Spring Boot 父 POM 的 `maven.compiler.release=17` 决定）。
  早期 `api/pom.xml` 里曾写死 `maven-compiler-plugin` 的 `source/target 8`，
  但那两行一直被父 POM 覆盖、从未生效，且会让人误以为只能写 Java 8 语法，**已移除**。
  使用 Java 17 语言特性是安全的。
- 密钥不在源码里。JWT 签名密钥、微信 AppSecret、钉钉机器人凭据、第三方 API Key
  均来自 `application-secret.yml`（已 gitignore，由 `application.yml` 通过
  `spring.config.import` 引入），或对应的环境变量。
- 定时任务默认开启（`@EnableScheduling`），本地启动会真实执行任务，注意别误写生产数据。

## 文档

- [`../docs/`](../docs/) —— 产品文档、开发报告、运维手册索引
- [`CLAUDE.md`](CLAUDE.md) —— 给 AI 编码助手的项目说明
