# CLAUDE.md

此文件为 Claude Code (claude.ai/code) 在该代码库中工作时提供指导。

## 项目概述

EasyChat 是一个基于 Spring Boot 3.4.2 的即时通讯系统后端，使用 Java 21 开发。

**核心技术栈：**
- Spring Boot 3.4.2 + Spring Web
- Spring WebSocket（实时通讯）
- MyBatis-Plus 3.5.14（数据库 ORM）
- Sa-Token 1.37.0（认证授权框架）
- MySQL 8.0.33
- Redis（会话管理）
- Lombok（代码简化）

**核心功能：**
- ✅ 用户认证与授权（Sa-Token）
- ✅ WebSocket 实时通讯（单聊、群聊）
- ✅ 消息持久化与历史记录
- ✅ 会话管理与未读消息提醒
- ✅ 好友与群组管理

## 常用开发命令

### 启动应用
```bash
# 使用 Maven 启动（开发环境）
mvn spring-boot:run

# 打包
mvn clean package

# 跳过测试打包（pom.xml 已配置默认跳过测试）
mvn clean package -DskipTests
```

### 运行测试
```bash
# 注意：pom.xml 配置了跳过单元测试，需要运行测试时：
mvn test -DskipTests=false

# 运行单个测试类
mvn test -Dtest=UserServiceTest

# 运行单个测试方法
mvn test -Dtest=UserServiceTest#testRegister
```

### 环境配置
- 默认激活 `dev` 环境（application.yml）
- 开发环境端口：8091
- 数据库和 Redis 配置见 `application-dev.yml`
- 生产环境配置见 `application-prod.yml`

## 代码架构

### 分层架构
采用标准的 Spring Boot 三层架构：

```
Controller（控制器层）
    ↓
Service（业务逻辑层）
    ↓
Mapper（数据访问层）
```

### 核心包结构

- **`controller/`** - REST API 控制器
  - `UserController`：用户管理（登录、注册）
  - `FriendController`：好友管理
  - `GroupController`：群组管理
  - `MessageController`：消息历史查询（分页）
  - `ConversationController`：会话列表、标记活跃会话、置顶、免打扰

- **`service/`** 和 `service/impl/` - 业务逻辑层
  - Service 接口定义业务方法
  - impl 包含具体实现
  - **MessageService**：消息发送、历史查询、删除、撤回
  - **ConversationService**：会话列表查询、置顶、免打扰
  - **UnreadService**：基于 Redis 的未读消息管理、活跃会话管理

- **`mapper/`** - MyBatis-Plus 数据访问层
  - 继承 `BaseMapper<T>` 获得 CRUD 基础方法
  - 自定义 SQL 在 Mapper 接口中定义

- **`model/`** - 数据模型
  - `entity/`：数据库实体类
    - User, Message, Friend, Group, GroupMember, Conversation
  - `dto/`：数据传输对象（请求参数）
    - `SendMessageDTO`：发送消息请求
    - `MessageHistoryDTO`：历史消息查询请求
    - `ActiveChatDTO`：设置活跃会话请求
  - `vo/`：视图对象（响应数据）
    - `MessageVO`：消息详情（含发送者信息、会话ID）
    - `ConversationVO`：会话详情（含最后消息预览、未读数、置顶、免打扰）

- **`config/`** - 配置类
  - `SaTokenConfig`：认证拦截器配置
  - `WebConfig`：Web 相关配置
  - `WebSocketConfig`：WebSocket 配置（连接路径：`/ws/chat`）

- **`handler/`** - 处理器
  - `GlobalExceptionHandler`：全局异常处理
  - `MyMetaObjectHandler`：MyBatis-Plus 自动填充时间戳
  - `ChatWebSocketHandler`：WebSocket 消息处理（单聊、群聊推送）
  - `WebSocketHandshakeInterceptor`：WebSocket 握手拦截（Sa-Token 认证）

- **`common/`** - 通用组件
  - `Result<T>`：统一响应结果封装

- **`enums/`** - 枚举类
  - `ResultCodeEnum`：响应状态码
  - `ConversationTypeEnum`：会话类型（单聊、群聊）
  - `MessageTypeEnum`：消息类型（文本、图片、语音、视频等）
  - `MessageStatusEnum`：消息状态（正常、已撤回、已删除）
  - `UserStatusEnum`、`FriendStatusEnum` 等业务枚举

- **`exception/`** - 异常定义
  - `BusinessException`：业务异常

- **`utils/`** - 工具类
  - `UserContextUtil`：获取当前登录用户信息
  - `ConversationIdUtil`：会话ID生成与解析（单聊、群聊）
  - `RedisKeyConstants`：Redis键管理（未读数、活跃会话）

### 关键设计模式

**1. 认证授权机制（Sa-Token）**
- 所有 API（除登录/注册）需要登录认证
- Token 名称为 `Authorization`（Header 传递）
- 不支持同账号并发登录（新登录会挤掉旧登录）
- Token 永不过期（`timeout: -1`）
- 详见 `SaTokenConfig.java`

**2. 统一响应格式**
- 所有接口返回 `Result<T>` 对象
- 包含 `code`（状态码）、`message`（消息）、`data`（数据）
- 成功：`Result.success(data)`
- 失败：`Result.error(message)` 或 `Result.error(ResultCodeEnum)`

**3. 自动时间戳填充**
- 实体类的 `createdAt` 和 `updatedAt` 字段使用 `@TableField(fill = ...)` 注解
- `MyMetaObjectHandler` 自动填充时间戳：
  - 插入时填充 `createdAt` 和 `updatedAt`
  - 更新时填充 `updatedAt`

**4. 全局异常处理**
- `GlobalExceptionHandler` 统一捕获和处理异常
- `BusinessException` 用于业务逻辑异常

**5. WebSocket 实时通讯**
- **连接地址**：`ws://localhost:8091/ws/chat?token=YOUR_TOKEN`
- **认证方式**：通过 URL 参数传递 Sa-Token
- **握手拦截**：`WebSocketHandshakeInterceptor` 验证 token 并提取用户ID
- **消息处理**：`ChatWebSocketHandler` 处理消息接收和推送
- **在线管理**：内存 Map 维护在线用户（userId → WebSocketSession）
- **消息路由**：
  - 单聊：根据会话ID提取接收者ID，查找目标用户的 WebSocket 会话并推送
  - 群聊：查询群成员列表，遍历推送给所有在线成员（发送者除外）
- **未读数控制**：
  - 检查接收者的活跃会话ID（Redis，60秒过期）
  - 活跃会话匹配：续期活跃状态，推送消息，不增加未读数
  - 活跃会话不匹配或离线：增加未读数（Redis increment）

**6. 会话ID设计（业务主键）**
- **单聊**：`s_{min}_{max}` 格式（min 和 max 是对话双方的用户ID，确保唯一性）
  - 示例：用户1和用户3的会话ID为 `s_1_3`
- **群聊**：`g_{groupId}` 格式
  - 示例：群组5的会话ID为 `g_5`
- **工具类**：`ConversationIdUtil` 提供生成和解析方法
  - `generateSingleChatId(userId1, userId2)` - 生成单聊会话ID
  - `generateGroupChatId(groupId)` - 生成群聊会话ID
  - `extractTargetId(conversationId, currentUserId)` - 从单聊会话ID提取对方用户ID
  - `extractGroupIdFromGroupChat(conversationId)` - 从群聊会话ID提取群组ID
  - `isValid(conversationId)` - 验证会话ID格式

**7. 消息与会话管理**
- **消息发送流程**：
  1. 客户端通过 WebSocket 发送消息（含会话类型、接收者/群组ID）
  2. 服务器生成会话ID（ConversationIdUtil）
  3. 保存消息到数据库（自动生成消息ID，关联会话ID）
  4. 更新会话表（last_message_id, last_message_time）
  5. 推送消息给接收者（根据活跃会话状态控制未读数）
- **会话自动创建**：首次发送消息时自动创建会话记录
  - 单聊：为双方各创建一个会话记录（相同 conversation_id）
  - 群聊：为所有群成员各创建一个会话记录（相同 conversation_id）
- **会话表主键**：使用联合主键 `(user_id, conversation_id)`，无自增ID
- **未读消息处理**（基于 Redis）：
  - **活跃会话管理**：用户进入聊天页时调用 `POST /conversation/active`，设置当前活跃会话ID（60秒过期）
  - **接收消息时**：
    - 检查接收者的活跃会话ID是否匹配当前会话
    - 匹配：续期活跃状态（重置60秒），不增加未读数
    - 不匹配或离线：增加未读数（Redis increment）
  - **查看消息时**：调用 `GET /message/history` 时自动清除该会话的未读数（Redis delete）
  - **会话列表**：批量查询所有会话的未读数（Redis MGET）
- **历史消息查询**：
  - 支持普通分页（基于页码）
  - 支持游标分页（基于 lastMessageId，推荐用于滚动加载）
  - 批量查询发送者信息，避免 N+1 问题
  - 查询时自动清除未读数（Redis delete）

**8. Redis 未读数架构**
- **Redis Key 设计**（通过 `RedisKeyConstants` 管理）：
  - 未读数：`unread:{userId}:{conversationId}` - 存储该用户在该会话的未读消息数（整数）
  - 活跃会话：`active_chat:{userId}` - 存储用户当前活跃的会话ID（60秒过期）
- **核心操作**（`UnreadService`）：
  - `setActiveChat(userId, conversationId)` - 设置活跃会话（60秒TTL）
  - `getActiveChat(userId)` - 获取活跃会话ID
  - `renewActiveChat(userId)` - 续期活跃会话（重置60秒TTL）
  - `incrementUnread(userId, conversationId)` - 未读数+1（Redis INCR）
  - `clearUnread(userId, conversationId)` - 清除未读数（Redis DEL）
  - `getUnreadCount(userId, conversationId)` - 获取单个会话未读数
  - `batchGetUnreadCounts(userId, conversationIds)` - 批量获取未读数（Redis MGET）
- **滑动窗口机制**：活跃会话每次收到消息时续期，保持用户在聊天页的状态

**9. 性能优化点**
- **批量查询**：会话列表、历史消息等场景使用批量查询用户信息（`listByIds`）
- **批量查询未读数**：使用 Redis MGET 一次性查询所有会话未读数
- **游标分页**：聊天历史使用游标分页，避免数据偏移问题
- **自动填充**：时间戳字段自动填充，减少代码重复
- **活跃会话缓存**：60秒过期，减少 Redis 存储压力

## 数据库说明

### 核心实体表
- **users**：用户表（账号、密码、昵称、头像等）
- **friends**：好友关系表
- **groups**：群组表
- **group_members**：群成员表
- **messages**：消息表（存储所有聊天消息）
- **conversations**：会话表（单聊/群聊会话管理）

具体sql参考/docs/easy_chat.sql

### 表结构设计说明

**会话表（conversations）设计要点**：
- ✅ **无自增ID**：使用 `conversation_id`（业务主键）作为主标识
- ✅ **联合主键**：`PRIMARY KEY (user_id, conversation_id)` - 每个用户对每个会话有一条记录
- ✅ **会话ID格式**：
  - 单聊：`s_{min}_{max}`（min 和 max 是对话双方的用户ID，小的在前）
  - 群聊：`g_{groupId}`
- ✅ **字段说明**：
  - `conversation_id`：会话ID（VARCHAR，业务主键）
  - `user_id`：用户ID（该会话所属用户）
  - `target_id`：目标ID（单聊时为对方用户ID，群聊时为群组ID）
  - `type`：会话类型（1-单聊，2-群聊）
  - `last_message_id`：最后一条消息ID
  - `last_message_time`：最后消息时间
  - `pinned`：是否置顶（BOOLEAN）
  - `muted`：是否免打扰（BOOLEAN）
  - `status`：会话状态（1-正常，2-已删除）

**消息表（messages）设计要点**：
- ✅ **自增ID**：`id BIGINT AUTO_INCREMENT PRIMARY KEY`
- ✅ **会话关联**：通过 `conversation_id` 关联会话（VARCHAR）
- ✅ **移除字段**：不再有 `receiver_id` 和 `group_id`（统一使用 conversation_id）
- ✅ **查询模式**：
  - 单聊消息：通过 `conversation_id = 's_{min}_{max}'` 查询双方对话
  - 群聊消息：通过 `conversation_id = 'g_{groupId}'` 查询群聊消息
- ✅ **消息状态**：status 字段（1-正常，2-已撤回，3-已删除）

**MyBatis-Plus 使用约束**：
- ⚠️ **conversations 表无 @TableId 注解字段**，因此不能使用 `updateById()`、`deleteById()` 等方法
- ✅ **正确更新方式**：使用 `update(entity, wrapper)` 配合 LambdaQueryWrapper
  ```java
  LambdaQueryWrapper<Conversation> wrapper = new LambdaQueryWrapper<>();
  wrapper.eq(Conversation::getUserId, userId)
         .eq(Conversation::getConversationId, conversationId);
  conversationMapper.update(conversation, wrapper);
  ```
- ✅ **messages 表有自增ID**，可以正常使用 `updateById()` 等方法

### 时区配置
- Jackson 时区设置为 GMT+8
- MySQL 连接时区为 Asia/Shanghai
- 实体类使用 `LocalDateTime` 和 `LocalDate` 处理时间

## 核心 API 接口

### WebSocket 接口
- **连接地址**：`ws://localhost:8091/ws/chat?token=YOUR_TOKEN`
- **发送消息格式**（JSON）：
  ```json
  {
    "messageType": 1,         // 1-文本，2-图片，3-语音等
    "conversationType": 1,    // 1-单聊，2-群聊
    "receiverId": 2,          // 单聊时的接收者ID
    "groupId": null,          // 群聊时的群组ID
    "content": "你好"
  }
  ```

### REST API 接口
- **GET /conversation/list** - 获取会话列表（含未读数、最后消息预览、置顶、免打扰状态）
- **POST /conversation/active** - 设置活跃会话（用户进入聊天页时调用）
  - 请求体：`{"conversationId": "s_1_3"}`
  - 作用：设置当前活跃会话ID，60秒内收到该会话消息不增加未读数
- **PUT /conversation/pin/{conversationId}** - 切换会话置顶状态
- **PUT /conversation/mute/{conversationId}** - 切换会话免打扰状态
- **GET /message/history** - 分页查询历史消息（自动清除未读数）
  - 参数：`conversationId`, `page`, `size`（普通分页）
  - 参数：`conversationId`, `lastMessageId`, `size`（游标分页，推荐）
- **DELETE /message/{messageId}** - 删除消息（仅发送者可删除）
- **PUT /message/recall/{messageId}** - 撤回消息（仅发送者可撤回）

### 用户交互流程
```
1. 用户登录 → 获取 Token
2. 建立 WebSocket 连接 (携带 Token)
3. 获取会话列表 (GET /conversation/list)
   → 显示未读数、最后消息预览、置顶/免打扰状态
4. 点击某个会话：
   a. 调用 POST /conversation/active 设置活跃会话
   b. 加载历史消息 (GET /message/history)
   → 后端自动清除该会话未读数
5. 发送消息 (通过 WebSocket)
   → 消息保存到数据库 → 推送给接收者
   → 根据接收者活跃会话状态决定是否增加未读数
6. 接收消息 (通过 WebSocket)
   → 如果当前在该会话页：不增加未读数（活跃会话续期）
   → 如果不在该会话页：未读数 +1
7. 离开会话页：活跃会话60秒后自动过期
```

## 开发注意事项

### WebSocket 开发规范
1. **消息格式**：客户端发送的消息必须是 JSON 格式，包含必要字段（messageType, conversationType 等）
2. **认证机制**：WebSocket 连接必须携带有效的 Sa-Token（通过 URL 参数 `?token=xxx`）
3. **在线管理**：用户在线状态通过内存 Map 维护（`ONLINE_USERS`），服务重启后需要重新连接
4. **消息推送**：
   - 单聊消息只推送给接收者（如果在线）
   - 群聊消息推送给所有在线群成员（包括发送者自己，用于回显）
5. **离线消息**：离线用户收不到实时推送，但消息会保存到数据库，上线后可通过历史消息接口查询

### 消息与会话开发规范
1. **会话ID生成**：必须使用 `ConversationIdUtil` 工具类生成会话ID
   - 单聊：`ConversationIdUtil.generateSingleChatId(userId1, userId2)`
   - 群聊：`ConversationIdUtil.generateGroupChatId(groupId)`
2. **消息保存顺序**：必须先保存消息（生成ID），再更新会话表的 `last_message_id`
3. **会话创建**：
   - 单聊：需要为双方各创建一个会话记录（相同 conversation_id）
   - 群聊：需要为所有群成员各创建一个会话记录（相同 conversation_id）
4. **未读数管理**（Redis）：
   - 用户进入聊天页：调用 `unreadService.setActiveChat(userId, conversationId)`
   - 推送消息时：检查活跃会话，决定是否增加未读数
   - 查询历史消息：调用 `unreadService.clearUnread(userId, conversationId)`
   - 会话列表：调用 `unreadService.batchGetUnreadCounts(userId, conversationIds)`
5. **会话表更新**：由于无 @TableId 字段，不能使用 `updateById()`
   ```java
   // 错误：this.updateById(conversation);

   // 正确：使用 update(entity, wrapper)
   LambdaQueryWrapper<Conversation> wrapper = new LambdaQueryWrapper<>();
   wrapper.eq(Conversation::getUserId, userId)
          .eq(Conversation::getConversationId, conversationId);
   this.update(conversation, wrapper);
   ```
6. **性能优化**：
   - 使用批量查询（`listByIds`）获取用户信息，避免 N+1 问题
   - 推荐使用游标分页查询历史消息（`lastMessageId`），避免数据偏移
   - 使用 Redis MGET 批量查询未读数

### 添加新的 API 端点
1. 在 Controller 中定义接口方法，使用 `@RestController` 和 `@RequestMapping`
2. 返回值统一使用 `Result<T>` 封装
3. 如果不需要认证，在 `SaTokenConfig.java:32-36` 添加到 `excludePathPatterns`
4. 使用 `@Validated` 和 DTO 进行参数校验
5. WebSocket 路径无需添加到排除列表（通过握手拦截器处理认证）

### 数据库实体开发
1. 继承或参考现有实体类结构
2. 使用 `@TableName` 指定表名
3. ID 字段使用 `@TableId(type = IdType.AUTO)` 自增（如果有自增主键）
4. 对于无自增ID的表（如 conversations），不使用 `@TableId` 注解
5. 时间字段使用 `@TableField(fill = ...)` 自动填充
6. 使用 Lombok `@Data` 注解简化 getter/setter

### MyBatis-Plus Mapper
1. Mapper 接口继承 `BaseMapper<实体类>`
2. 基础 CRUD 方法已内置（insert、deleteById、updateById、selectById 等）
3. **重要**：如果实体类没有 `@TableId` 注解字段（如 Conversation），则不能使用 `updateById()`、`deleteById()` 等依赖ID的方法
4. 复杂查询可使用 LambdaQueryWrapper 或自定义 SQL
5. 更新无 @TableId 实体时，必须使用 `update(entity, wrapper)` 方式：
   ```java
   LambdaQueryWrapper<Conversation> wrapper = new LambdaQueryWrapper<>();
   wrapper.eq(Conversation::getUserId, userId)
          .eq(Conversation::getConversationId, conversationId);
   mapper.update(conversation, wrapper);
   ```

### 日志级别
- Mapper 层和 MyBatis 日志级别为 DEBUG（仅开发环境）
- 可在 application-dev.yml 中调整日志级别

## 常见问题与解决方案

### 1. WebSocket 连接失败
- **问题**：WebSocket 握手失败，返回 403 或连接被拒绝
- **原因**：Token 无效或未提供
- **解决**：确保连接 URL 包含有效的 token 参数：`ws://localhost:8091/ws/chat?token=YOUR_TOKEN`

### 2. 未读数不准确
- **问题**：查看消息后未读数仍然显示，或者在聊天页收到消息却增加了未读数
- **原因**：前端未正确调用活跃会话接口
- **解决**：
  - 进入聊天页时必须调用 `POST /conversation/active` 设置活跃会话
  - 离开聊天页时无需手动清除，60秒后自动过期
  - 确保加载历史消息时调用了 `GET /message/history`（会自动清除未读数）

### 3. 会话表更新失败
- **问题**：调用 `conversationService.updateById(conversation)` 报错或不生效
- **原因**：Conversation 实体类没有 `@TableId` 注解字段，MyBatis-Plus 无法识别主键
- **解决**：使用 `update(entity, wrapper)` 方式更新
  ```java
  LambdaQueryWrapper<Conversation> wrapper = new LambdaQueryWrapper<>();
  wrapper.eq(Conversation::getUserId, userId)
         .eq(Conversation::getConversationId, conversationId);
  conversationMapper.update(conversation, wrapper);
  ```

### 4. 会话ID格式错误
- **问题**：单聊会话ID不一致，导致查询不到对话
- **原因**：手动拼接会话ID时，用户ID顺序不一致
- **解决**：必须使用 `ConversationIdUtil` 工具类生成会话ID
  - 单聊：`ConversationIdUtil.generateSingleChatId(userId1, userId2)` - 自动处理ID排序
  - 群聊：`ConversationIdUtil.generateGroupChatId(groupId)`

### 5. 群聊消息只有发送者收到
- **问题**：发送群聊消息后，只有自己能看到
- **原因**：未查询群成员列表或推送逻辑有误
- **解决**：检查 `ChatWebSocketHandler.pushMessage()` 中的群聊推送逻辑，确保：
  - 查询了群成员列表
  - 遍历所有在线成员推送（发送者自己除外）
  - 正确处理活跃会话和未读数逻辑

### 6. 历史消息分页数据重复
- **问题**：滚动加载时，相同的消息出现多次
- **原因**：使用普通分页（page），新消息插入导致页码偏移
- **解决**：使用游标分页（`lastMessageId`），参考 `MessageHistoryDTO`
  ```java
  // 推荐：游标分页
  if (queryDTO.getLastMessageId() != null) {
      queryWrapper.lt(Message::getId, queryDTO.getLastMessageId());
  }
  ```

### 7. Redis 未读数不一致
- **问题**：Redis 中的未读数与实际不符
- **原因**：Redis 数据过期或被误删除
- **解决**：
  - Redis 未读数是临时数据，过期后会自动清零
  - 前端应以 Redis 数据为准，不要在本地维护未读数
  - 如需持久化未读数，可考虑定期同步到数据库

### 参考文档
- **数据库脚本**：见 `docs/easy_chat.sql` 
