# 通知系统优化方案

## 📋 优化目标

1. **新用户自动初始化**：用户首次登录时自动创建通知记录
2. **数据去重优化**：每个用户每种类型只保留一条记录，每天更新而非插入
3. **前端主动查询**：登录后立即查询未读数，触发后端初始化逻辑

---

## 🔧 优化内容

### 1. 后端优化

#### 1.1 `UserNotificationServiceImpl.getUnreadCount()` - 自动初始化

**修改位置**: `api/src/main/java/com/emotion/api/service/impl/UserNotificationServiceImpl.java`

**核心逻辑**:
```java
@Override
public Map<String, Integer> getUnreadCount(String userId) {
    // ✅ 新增：如果用户不存在，自动初始化
    ensureUserNotificationsExist(userId);
    
    // ... 查询未读数逻辑
}

private void ensureUserNotificationsExist(String userId) {
    // 检查用户是否已有通知记录
    int existingCount = baseMapper.countUnreadByUserId(userId);
    
    // 如果没有任何记录，说明是新用户，需要初始化
    if (existingCount == 0) {
        log.info("检测到新用户，自动初始化通知记录: userId={}", userId);
        
        LocalDate today = LocalDate.now();
        List<UserNotification> notifications = new ArrayList<>();
        
        // 创建每日补给通知（默认为已读）
        UserNotification dailyBonus = new UserNotification();
        dailyBonus.setUserId(userId);
        dailyBonus.setNotificationType(1);
        dailyBonus.setIsRead(1); // 默认为已读，避免新用户看到红点
        dailyBonus.setTriggerDate(today);
        notifications.add(dailyBonus);
        
        // 创建星座运势通知（默认为已读）
        UserNotification horoscope = new UserNotification();
        horoscope.setUserId(userId);
        horoscope.setNotificationType(2);
        horoscope.setIsRead(1); // 默认为已读
        horoscope.setTriggerDate(today);
        notifications.add(horoscope);
        
        // 批量插入（使用 INSERT IGNORE 避免重复）
        baseMapper.batchInsertIgnore(notifications);
        log.info("用户通知记录初始化完成: userId={}", userId);
    }
}
```

**优势**:
- ✅ 新用户首次调用接口时自动创建通知记录
- ✅ 默认设置为"已读"，避免新用户看到不必要的红点
- ✅ 使用 `INSERT IGNORE` 避免并发问题

---

#### 1.2 `UserNotificationServiceImpl.batchCreateNotifications()` - 更新而非插入

**修改前的问题**:
- 每天为所有用户创建新记录，导致数据快速增长
- 同一个用户会有多条历史记录，浪费存储空间

**修改后的逻辑**:
```java
@Override
@Transactional(rollbackFor = Exception.class)
public int batchCreateNotifications(Integer notificationType, LocalDate triggerDate) {
    // 1. 获取目标用户列表
    List<String> userIds = getActiveUserIds(); // 或 getUserIdsWithConstellation()
    
    // 2. 先尝试批量插入（新用户）
    List<UserNotification> notifications = userIds.stream()
            .map(userId -> createNotification(userId, notificationType, triggerDate))
            .collect(Collectors.toList());
    
    int insertedCount = baseMapper.batchInsertIgnore(notifications);
    
    // 3. 对于已有记录的用户，更新 trigger_date 和 is_read=0
    int updatedCount = 0;
    if (insertedCount < userIds.size()) {
        updatedCount = updateUserNotificationDate(notificationType, triggerDate, userIds);
    }
    
    log.info("批量创建/更新通知完成, type={}, date={}, 用户数={}, 插入数={}, 更新数={}", 
            notificationType, triggerDate, userIds.size(), insertedCount, updatedCount);
    
    return insertedCount + updatedCount;
}

/**
 * 更新已有用户的通知日期（将旧日期的记录更新为新日期）
 */
private int updateUserNotificationDate(Integer notificationType, LocalDate newDate, List<String> userIds) {
    // 分批更新，避免一次性更新太多数据
    int batchSize = 500;
    int totalUpdated = 0;
    
    for (int i = 0; i < userIds.size(); i += batchSize) {
        int end = Math.min(i + batchSize, userIds.size());
        List<String> batchUserIds = userIds.subList(i, end);
        
        // 更新这些用户的通知记录：将旧日期的记录更新为新日期，并重置为未读
        LambdaQueryWrapper<UserNotification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserNotification::getNotificationType, notificationType)
               .in(UserNotification::getUserId, batchUserIds)
               .ne(UserNotification::getTriggerDate, newDate); // 只更新非今日的记录
        
        UserNotification updateEntity = new UserNotification();
        updateEntity.setTriggerDate(newDate);
        updateEntity.setIsRead(0); // 重置为未读
        updateEntity.setUpdateTime(java.time.LocalDateTime.now());
        
        int updated = this.getBaseMapper().update(updateEntity, wrapper);
        totalUpdated += updated;
    }
    
    return totalUpdated;
}
```

**优势**:
- ✅ 每个用户每种类型只保留一条记录
- ✅ 定时任务执行时，新用户插入，老用户更新
- ✅ 大幅减少数据量，提高查询性能
- ✅ 分批更新，避免大批量操作导致的性能问题

---

### 2. 前端优化

#### 2.1 `pages/index/index.js` - 登录后主动查询

**修改位置**: `miniprogram/pages/index/index.js`

**核心逻辑**:
```javascript
// 先登录再执行回调
checkLoginThen(callback) {
  const token = wx.getStorageSync('token');
  if (!token) {
    loginWithWechat().then(() => {
      // ✅ 新增：登录成功后，主动查询未读消息数（会自动初始化新用户数据）
      this.loadUnreadCount();
      callback && callback();
    }).catch(err => {
      console.error('登录失败:', err);
    });
  } else {
    // ✅ 已有token，也查询一次未读数（确保数据最新）
    this.loadUnreadCount();
    callback && callback();
  }
},

// ✅ 加载未读消息数
loadUnreadCount() {
  request({
    url: '/user/api/v1/notification/unread-count',
    method: 'GET'
  }).then(res => {
    if (res.data) {
      console.log('[Index] 未读消息数:', res.data);
      // TabBar 组件会在 onShow 时自己加载
    }
  }).catch(err => {
    console.error('[Index] 获取未读数失败:', err);
  });
},
```

**优势**:
- ✅ 新用户登录后立即触发后端初始化逻辑
- ✅ 确保用户数据在首次使用时就已准备好
- ✅ 已有用户每次进入页面都会刷新未读数

---

### 3. 数据库优化

#### 3.1 添加唯一索引

**SQL脚本**: `api/sql/optimize_user_notification.sql`

```sql
-- 1. 添加唯一索引（确保每个用户每种类型只有一条记录）
ALTER TABLE `user_notification` 
ADD UNIQUE INDEX `uk_user_type` (`user_id`, `notification_type`) 
COMMENT '唯一索引：每个用户每种通知类型只保留一条记录';

-- 2. 清理历史重复数据（保留最新的记录）
DELETE n1 FROM user_notification n1
INNER JOIN user_notification n2 
WHERE n1.user_id = n2.user_id 
  AND n1.notification_type = n2.notification_type
  AND n1.id < n2.id;
```

**优势**:
- ✅ 从数据库层面保证数据唯一性
- ✅ 防止并发插入导致的数据重复
- ✅ 清理历史冗余数据

---

## 📊 优化效果对比

### 优化前
| 指标 | 数值 |
|------|------|
| 数据增长 | 每天 × 用户数 条新记录 |
| 1000用户30天 | 60,000 条记录（2种类型） |
| 查询性能 | 需要过滤大量历史数据 |
| 存储空间 | 持续增长 |

### 优化后
| 指标 | 数值 |
|------|------|
| 数据增长 | 稳定在 用户数 × 2 条记录 |
| 1000用户 | 2,000 条记录（固定） |
| 查询性能 | 直接查询，无需过滤 |
| 存储空间 | 基本不变 |

**数据量减少**: 96.7% (30天后)

---

## 🚀 部署步骤

### 1. 执行数据库迁移
```bash
# 连接到数据库
mysql -u root -p your_database

# 执行优化脚本
source /path/to/optimize_user_notification.sql
```

### 2. 部署后端代码
```bash
cd backend
mvn clean package
# 重启服务
```

### 3. 部署前端代码
```bash
# 编译小程序
# 上传到微信开发者工具
```

### 4. 验证功能
1. **新用户测试**:
   - 注册新账号
   - 登录后查看 `user_notification` 表，确认已自动创建2条记录
   - 确认两条记录的 `is_read` 都为 1

2. **老用户测试**:
   - 手动触发定时任务（调用 `/test/api/v1/notification/trigger-daily-bonus`）
   - 查看日志，确认有"插入数"和"更新数"
   - 检查数据库，确认每个用户只有1条每日补给记录

3. **定时任务测试**:
   - 等待第二天凌晨0点
   - 检查 `scheduled_task_log` 表，确认定时任务执行成功
   - 检查 `user_notification` 表，确认 `trigger_date` 已更新为新日期

---

## ⚠️ 注意事项

1. **唯一索引冲突**: 
   - 如果数据库中已有重复数据，必须先执行清理脚本
   - 否则添加唯一索引会失败

2. **并发安全**:
   - 使用 `INSERT IGNORE` 避免并发插入冲突
   - 定时任务已使用分布式锁，确保单实例执行

3. **向后兼容**:
   - 现有代码无需修改，接口保持不变
   - 前端调用方式不变

4. **监控建议**:
   - 观察定时任务日志中的"插入数"和"更新数"比例
   - 正常情况下，第一天插入数较多，后续以更新为主

---

## 📝 总结

本次优化实现了三个核心目标：

1. ✅ **新用户自动初始化**: 首次调用接口时自动创建通知记录
2. ✅ **数据去重**: 每个用户每种类型只保留一条记录，每天更新而非插入
3. ✅ **前端主动查询**: 登录后立即查询未读数，触发后端初始化

优化后，数据量减少约96%，查询性能显著提升，存储成本大幅降低。
