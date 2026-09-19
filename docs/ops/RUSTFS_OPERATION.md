# RustFS 运维手册（本地开发环境）

> **重要**：图片上传/查看依赖 RustFS 对象存储。启动方式**必须**按本文档来，不要随手 `rustfs server` 或指定其它数据目录，会导致后端读不到真实数据。

## 1. 关键参数（不能改）

| 项 | 值 | 说明 |
|---|---|---|
| 二进制 | `/opt/homebrew/bin/rustfs` | 通过 `brew install rustfs/tap/rustfs` 安装 |
| 监听地址 | `:9000` | Java 端 `application-*.yml` 中 `rustfs.endpoint=http://127.0.0.1:9000` |
| Access Key | `rustfs_your_access_key_here` | 与 `application-*.yml` 中 `rustfs.accessKey` 保持一致 |
| Secret Key | `rustfs_your_secret_key_here` | 与 `application-*.yml` 中 `rustfs.secretKey` 保持一致 |
| **数据目录** | `/opt/homebrew/var/rustfs` | ⚠️ 真实用户图片和 `emotion` bucket 都在这里 |
| Bucket | `emotion` | Java 端 `rustfs.bucketName=emotion` |
| 日志 | `/opt/homebrew/var/log/rustfs.log` | |

**⚠️ 错误的数据目录（历史遗留，不要用）**
- `~/rustfs-data`：4/30 跑单元测试 `RustFsUploadTest` 时留下的空壳，里面只有 3 个测试文件，**不是生产数据**。
- 任何其它路径也不要用；用错数据目录 rustfs 会启动一个空的对象存储，前端表现为「图片全丢了」。

## 2. 启动方式：launchd（推荐，开机自启+崩溃自愈）

已配置 launchd plist：`~/Library/LaunchAgents/homebrew.mxcl.rustfs.plist`

> 说明：`rustfs/tap` 的 Homebrew formula 没有声明 `service` 块，因此 **`brew services start rustfs` 会失败**（报 "has not implemented #plist, #service or provided a locatable service file"）。改用 launchd 直接托管，效果与 `brew services` 等价：开机自启、进程被 kill 会被拉起。

### 常用命令

```bash
# 启动（首次或停止后）
launchctl load ~/Library/LaunchAgents/homebrew.mxcl.rustfs.plist

# 停止
launchctl unload ~/Library/LaunchAgents/homebrew.mxcl.rustfs.plist

# 重启
launchctl unload ~/Library/LaunchAgents/homebrew.mxcl.rustfs.plist
launchctl load   ~/Library/LaunchAgents/homebrew.mxcl.rustfs.plist

# 状态
launchctl list | grep rustfs              # 有 PID 就是运行中
lsof -i:9000 -P -n                        # 应看到 rustfs LISTEN
tail -f /opt/homebrew/var/log/rustfs.log  # 看日志
```

### plist 内容（备份）

如果 `~/Library/LaunchAgents/homebrew.mxcl.rustfs.plist` 丢了，把下面这段写回去即可：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
  <key>Label</key>
  <string>homebrew.mxcl.rustfs</string>
  <key>ProgramArguments</key>
  <array>
    <string>/opt/homebrew/bin/rustfs</string>
    <string>server</string>
    <string>--address</string>
    <string>:9000</string>
    <string>--access-key</string>
    <string>rustfs_your_access_key_here</string>
    <string>--secret-key</string>
    <string>rustfs_your_secret_key_here</string>
    <string>/opt/homebrew/var/rustfs</string>
  </array>
  <key>RunAtLoad</key>
  <true/>
  <key>KeepAlive</key>
  <true/>
  <key>WorkingDirectory</key>
  <string>/opt/homebrew/var/rustfs</string>
  <key>StandardOutPath</key>
  <string>/opt/homebrew/var/log/rustfs.log</string>
  <key>StandardErrorPath</key>
  <string>/opt/homebrew/var/log/rustfs.log</string>
</dict>
</plist>
```

## 3. 应急启动方式：手动前台（仅调试用）

调试时才用，不要 `Ctrl+C` 后就走开，因为**这样启动没有 KeepAlive**：

```bash
# 先确保 launchd 没在跑，否则端口冲突
launchctl unload ~/Library/LaunchAgents/homebrew.mxcl.rustfs.plist

# 前台启动（用与 launchd 完全相同的参数）
/opt/homebrew/bin/rustfs server \
  --address :9000 \
  --access-key rustfs_your_access_key_here \
  --secret-key rustfs_your_secret_key_here \
  /opt/homebrew/var/rustfs
```

调试完记得再 `launchctl load` 把 launchd 托管加回去。

## 4. 健康检查

```bash
# 端口
lsof -i:9000 -P -n

# S3 端点（未签名请求应返回 403 AccessDenied XML，说明服务正常）
curl -sS -o /dev/null -w "%{http_code}\n" "http://127.0.0.1:9000/emotion/?list-type=2"
# 期望：403

# 通过 Java 后端验证端到端
# 从数据库 image_upload_record 挑一条 id，浏览器打开：
#   https://www.onekey-ai.top/files/view?id=<id>
# 应该正常返回图片
```

## 5. 排查手册

**症状：Java 端报 `Failed to connect to /127.0.0.1:9000` / OkHttp `connectSocket` 失败**
- 原因：rustfs 进程没在跑。
- 定位：`lsof -i:9000` 空、`launchctl list | grep rustfs` 无输出。
- 处理：`launchctl load ~/Library/LaunchAgents/homebrew.mxcl.rustfs.plist`。

**症状：能连上 rustfs，但用户历史图片全部 404 / 只剩几个测试文件**
- 原因：**数据目录用错了**（用了 `~/rustfs-data` 而不是 `/opt/homebrew/var/rustfs`）。
- 定位：`ps aux | grep rustfs`，看命令行末尾的数据目录路径。
- 处理：停掉当前进程，用本文档第 2 节的 plist 重新起，路径必须是 `/opt/homebrew/var/rustfs`。**不要动数据目录里的任何文件**。

**症状：`brew services start rustfs` 报错**
- 原因：formula 没定义 service。
- 处理：不要用 brew services，用 `launchctl load` 加载本项目的 plist。

**症状：rustfs 启动日志出现 `read_all_data_with_dmtime NotFound` warn**
- 原因：初次扫描 metacache 时的正常告警，不影响服务。忽略即可。

## 6. 数据目录说明

```
/opt/homebrew/var/rustfs/
├── .rustfs.sys/          # rustfs 元数据（不要动）
├── emotion/              # 本项目 bucket，用户上传的所有图片
│   └── <uuid>-<原始文件名>/xl.meta 及数据块
└── zhishen/              # 另一个项目（zhishen）的 bucket，与本项目无关
```

- 备份时整目录 `tar` 打包即可。
- 迁移到别的机器时把整个 `/opt/homebrew/var/rustfs` 拷过去，plist 里的路径改成新路径。

## 7. 后端配置对照

`backend/api/src/main/resources/application-dev.yml` 和 `application-prod.yml` 中：

```yaml
rustfs:
  endpoint: http://127.0.0.1:9000
  accessKey: rustfs_your_access_key_here
  secretKey: rustfs_your_secret_key_here
  bucketName: emotion
```

**修改任何一项都要同步改 plist 里的参数**，然后 unload + load 重启 rustfs。
