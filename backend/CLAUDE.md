# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

This is a Maven multi-module Spring Boot project. There is no Maven wrapper in this directory, so use the system `mvn`.

```bash
# Build all modules without tests
mvn clean package -DskipTests

# Run all tests
mvn test

# Run tests for the API module only
mvn -pl api test

# Run one test class
mvn -pl api -Dtest=StreamEmotionTest test

# Run one test method
mvn -pl api -Dtest=StreamEmotionTest#methodName test

# Start the API locally with an explicit profile
mvn -pl api spring-boot:run -Dspring-boot.run.profiles=dev

# Run the packaged jar
java -jar api/target/emotion-api-0.0.1-SNAPSHOT.jar
```

There is no dedicated lint/format command configured in the Maven POMs.

Useful manual scripts (all live in the repo-level `scripts/` directory):

```bash
# Tests the local SSE emotion analysis endpoint; edit JWT_TOKEN first
../scripts/test_stream_api.sh

# Tests the answer-book AI explanation endpoint; this script targets https://www.onekey-ai.top
../scripts/test_ai_explanation.sh
```

For local AI-backed flows, Ollama is expected at `http://localhost:11434` with the default model `qwen3.5:9b`:

```bash
curl http://localhost:11434/api/tags
ollama serve
ollama pull qwen3.5:9b
```

## Project structure

- Root `pom.xml` is an aggregator (`packaging=pom`) with one module: `api`.
- `api` is the Spring Boot application. Entry point: `api/src/main/java/com/emotion/api/EmotionApplication.java`.
- Main application annotations enable scheduling and async execution, and scan MyBatis mappers in `com.emotion.api.repository.dao`, `com.emotion.api.repository.mapper`, and `com.emotion.api.mapper`.
- SQL/schema helper files live in `api/sql/`.
- MyBatis XML mapper files live in `api/src/main/resources/mybatis/mapper/rds/`.
- Application config is split across `application.yml`, `application-dev.yml`, `application-test.yml`, `application-prod.yml`, and `application-example.yml`. `application.yml` defaults to the `prod` profile, so pass an explicit profile for local work.

## Architecture overview

The backend uses a traditional Spring layered architecture:

- `controller/`: REST controllers. Most endpoints return the shared `BaseResult<T>` response wrapper.
- `service/` and `service/impl/`: business logic. Many services have `I*Service` interfaces with implementations, but some AI/client services are concrete classes only.
- `repository/dao/rds/`, `repository/mapper/`, and `mapper/`: MyBatis/MyBatis-Plus mapper interfaces. Keep mapper package placement consistent with the existing feature being changed.
- `repository/po/` and `entity/`: persistence objects/entities. Both contain MyBatis-Plus annotated database models; check nearby code before adding a new model because the separation is not strict.
- `dto/` and `vo/`: request/response transport objects.
- `config/`: Spring Security, JWT, Redis, Ollama, web MVC, and shared response/config utilities.
- `repository/config/`: datasource and MyBatis-Plus configuration, including RDS datasource setup and pagination.
- `task/`: scheduled jobs run by Spring `@Scheduled`.
- `util/`, `payment/`, and `fanyi/`: integration utilities for Redis locks, RustFS/MinIO, DingTalk, WeChat Pay, AI providers, and Baidu translation.

## Main domains and integrations

Core product areas include:

- Emotion analysis / "emotion relief card": `EmotionController`, `UserController` streaming endpoints, `OllamaChatService`, `OllamaDirectService`, emotion reports, quotas, and interaction persistence.
- WeChat mini-program user flow: login/openId, JWT issuing/validation, WeChat access tokens, subscribe/template messages, and QR code generation.
- Horoscope/astrology: daily horoscope endpoints, Astronomy Engine calculations, rule-based interpretation, scheduled generation, and notifications.
- Weekly reports and notifications: weekly emotion report generation, subscriptions, scheduled pushes, and user notification records.
- Friend/social features: friend binding, paired interactions, friend link analysis, and related scheduled analysis.
- Answer Book, daily motivation, sign-in, drink tracking, English learning, file/video/image upload and processing.
- Payment code exists for WeChat Pay and payment records, but `PaymentController` is currently commented out/disabled.

Key external systems:

- MySQL via Druid and MyBatis-Plus. RDS is the active datasource family; PG/STATS scaffolding is present but commented/inactive.
- Redis via Spring Data Redis/Lettuce for JWT session lookup, token caches, and distributed locks.
- Ollama via both Spring AI `ChatClient` and direct OkHttp calls to `/api/chat`; some flows stream SSE with `SseEmitter`.
- RustFS/MinIO-compatible object storage through the MinIO Java SDK.
  - **本地开发环境如何启动 RustFS：见 `../docs/ops/RUSTFS_OPERATION.md`。数据目录固定为 `/opt/homebrew/var/rustfs`，不要用 `~/rustfs-data`。**
- WeChat Mini Program APIs and WeChat Pay SDK.
- DingTalk alerting from the global exception path.
- Zhipu AI and Baidu Qianfan utilities exist alongside Ollama.

## Security and request context

- Spring Security is stateless and JWT-based.
- The WeChat login flow obtains an openId, issues a JWT, and stores token state in Redis with keys like `JWT_<token>`.
- `JwtRequestFilter` reads `Authorization: Bearer <token>`, validates JWT, checks Redis, and populates the Spring Security context.
- Some endpoints are public in `SecurityConfig`; most API work should assume authenticated requests.
- Controllers can use the custom `@CurrentUser` argument resolver to receive the current `UserPrincipal`.

## Configuration notes

- Prefer `application-example.yml` and environment variables when documenting or adding configuration. The dev/test/prod config files contain environment-specific values and are gitignored — never commit them.
- The build targets **Java 17**, via the Spring Boot parent POM's `maven.compiler.release=17`. The `api` POM previously set `maven-compiler-plugin` source/target to 8, but that was always overridden and never took effect — it has been removed. Java 17 language features are safe to use.
- Secrets (JWT signing key, WeChat AppSecret, DingTalk robot credentials, Zhipu AI key) are NOT in the source tree. They come from `application-secret.yml` (gitignored, imported via `spring.config.import`) or environment variables. Never hardcode them again.
- Scheduled tasks are enabled by default through `@EnableScheduling`; running the app locally may execute jobs unless disabled via profile/config changes.
