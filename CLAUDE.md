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
  - `ConversationController`：会话列表、标记已读

- **`service/`** 和 `service/impl/` - 业务逻辑层
  - Service 接口定义业务方法
  - impl 包含具体实现
  - **MessageService**：消息发送、历史查询（自动标记已读）
  - **ConversationService**：会话列表查询、已读管理

- **`mapper/`** - MyBatis-Plus 数据访问层
  - 继承 `BaseMapper<T>` 获得 CRUD 基础方法
  - 自定义 SQL 在 Mapper 接口中定义

- **`model/`** - 数据模型
  - `entity/`：数据库实体类
    - User, Message, Friend, Group, GroupMember, Conversation
  - `dto/`：数据传输对象（请求参数）
    - `SendMessageDTO`：发送消息请求
    - `MessageHistoryDTO`：历史消息查询请求
  - `vo/`：视图对象（响应数据）
    - `MessageVO`：消息详情（含发送者信息）
    - `ConversationVO`：会话详情（含最后消息预览）

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

### 关键设计模式

**1. 认证授权机制（Sa-Token）**
- 所有 API（除登录/注册）需要登录认证
- Token 名称为 `Authorization`（Header 传递）
- 不支持同账号并发登录（新登录会挤掉旧登录）
- Token 永不过期（`timeout: -1`）
- 详见 `SaTokenConfig.java:16-25`

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
  - 单聊：根据 `receiverId` 查找目标用户的 WebSocket 会话并推送
  - 群聊：查询群成员列表，遍历推送给所有在线成员
- **详细文档**：参见 `WEBSOCKET_TEST.md`

**6. 消息与会话管理**
- **消息发送流程**：
  1. 客户端通过 WebSocket 发送消息
  2. 服务器保存消息到数据库（自动生成ID）
  3. 更新会话表（last_message_id, last_message_time, unread_count）
  4. 推送消息给接收者（在线用户）
- **会话自动创建**：首次发送消息时自动创建双方的会话记录
- **未读消息处理**：
  - 接收消息时：自动增加 unread_count
  - 查看消息时：首次加载历史消息时自动标记已读（unread_count = 0）
- **历史消息查询**：
  - 支持普通分页（基于页码）
  - 支持游标分页（基于 lastMessageId，推荐用于滚动加载）
  - 批量查询发送者信息，避免 N+1 问题

**7. 性能优化点**
- **批量查询**：会话列表、历史消息等场景使用批量查询用户信息
- **游标分页**：聊天历史使用游标分页，避免数据偏移问题
- **自动填充**：时间戳字段自动填充，减少代码重复
- **索引建议**：
  ```sql
  -- 消息表索引（提升历史查询性能）
  CREATE INDEX idx_conversation_created ON messages(conversation_id, created_at DESC);
  CREATE INDEX idx_conversation_id ON messages(conversation_id, id DESC);
  ```

## 数据库说明

### 核心实体表
- **users**：用户表（账号、密码、昵称、头像等）
- **friends**：好友关系表
- **groups**：群组表
- **group_members**：群成员表
- **messages**：消息表（存储所有聊天消息）
  - `conversation_id`：关联会话
  - `sender_id`：发送者
  - `receiver_id`：接收者（单聊）
  - `group_id`：群组ID（群聊）
  - `message_type`：消息类型（文本、图片、语音等）
  - `content`：消息内容
  - `status`：消息状态（正常、已撤回、已删除）
- **conversations**：会话表（单聊/群聊会话管理）
  - `user_id`：会话所属用户
  - `target_id`：目标ID（单聊为对方user_id，群聊为group_id）
  - `type`：会话类型（1-单聊，2-群聊）
  - `unread_count`：未读消息数
  - `last_message_id`：最后一条消息ID
  - `last_message_time`：最后消息时间

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
- **GET /conversation/list** - 获取会话列表（含未读数、最后消息预览）
- **PUT /conversation/read/{id}** - 标记会话为已读（通常不需要手动调用）
- **GET /message/history** - 分页查询历史消息（首次加载时自动标记已读）
  - 参数：`conversationId`, `page`, `size`（普通分页）
  - 参数：`conversationId`, `lastMessageId`, `size`（游标分页，推荐）

### 用户交互流程
```
1. 用户登录 → 获取 Token
2. 建立 WebSocket 连接 (携带 Token)
3. 获取会话列表 (GET /conversation/list)
4. 点击某个会话 → 加载历史消息 (GET /message/history)
   → 后端自动标记该会话为已读
5. 发送消息 (通过 WebSocket)
   → 消息保存到数据库 → 推送给接收者
6. 接收消息 (通过 WebSocket)
   → 会话列表未读数 +1
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
1. **消息保存顺序**：必须先保存消息（生成ID），再更新会话表的 `last_message_id`
2. **会话创建**：
   - 单聊：需要为双方各创建一个会话记录
   - 群聊：需要为所有群成员各创建一个会话记录
3. **未读数管理**：
   - 接收者的会话：`unread_count + 1`
   - 发送者的会话：不增加未读数
   - 查看消息时：自动清零（首次加载历史消息时）
4. **性能优化**：
   - 使用批量查询（`listByIds`）获取用户信息，避免 N+1 问题
   - 推荐使用游标分页查询历史消息（`lastMessageId`），避免数据偏移

### 添加新的 API 端点
1. 在 Controller 中定义接口方法，使用 `@RestController` 和 `@RequestMapping`
2. 返回值统一使用 `Result<T>` 封装
3. 如果不需要认证，在 `SaTokenConfig.java:21-25` 添加到 `excludePathPatterns`
4. 使用 `@Validated` 和 DTO 进行参数校验
5. WebSocket 路径无需添加到排除列表（通过握手拦截器处理认证）

### 数据库实体开发
1. 继承或参考现有实体类结构
2. 使用 `@TableName` 指定表名
3. ID 字段使用 `@TableId(type = IdType.AUTO)` 自增
4. 时间字段使用 `@TableField(fill = ...)` 自动填充
5. 使用 Lombok `@Data` 注解简化 getter/setter

### MyBatis-Plus Mapper
1. Mapper 接口继承 `BaseMapper<实体类>`
2. 基础 CRUD 方法已内置（insert、deleteById、updateById、selectById 等）
3. 复杂查询可使用 QueryWrapper 或自定义 SQL

### 日志级别
- Mapper 层和 MyBatis 日志级别为 DEBUG（仅开发环境）
- 可在 application-dev.yml 中调整日志级别

## 常见问题与解决方案

### 1. WebSocket 连接失败
- **问题**：WebSocket 握手失败，返回 403 或连接被拒绝
- **原因**：Token 无效或未提供
- **解决**：确保连接 URL 包含有效的 token 参数：`ws://localhost:8091/ws/chat?token=YOUR_TOKEN`

### 2. 消息未读数不更新
- **问题**：查看消息后未读数仍然显示
- **原因**：前端未刷新会话列表
- **解决**：加载历史消息后，前端应更新本地会话列表的 `unreadCount` 为 0，或重新获取会话列表

### 3. 会话表的 last_message_id 为 null
- **问题**：发送消息后，会话表的 `last_message_id` 字段是 null
- **原因**：更新会话时，消息还未保存到数据库（ID未生成）
- **解决**：必须先保存消息（`this.save(message)`），再更新会话表

### 4. 群聊消息只有发送者收到
- **问题**：发送群聊消息后，只有自己能看到
- **原因**：未查询群成员列表或推送逻辑有误
- **解决**：检查 `ChatWebSocketHandler.pushMessage()` 中的群聊推送逻辑，确保查询了群成员并遍历推送

### 5. 历史消息分页数据重复
- **问题**：滚动加载时，相同的消息出现多次
- **原因**：使用普通分页（page），新消息插入导致页码偏移
- **解决**：使用游标分页（`lastMessageId`），参考 `MessageHistoryDTO`

## 测试指南

### WebSocket 测试工具
1. **Postman**：支持 WebSocket 请求，推荐使用
2. **浏览器控制台**：使用 JavaScript `new WebSocket(url)` 测试
3. **在线工具**：websocket.org、websocket-test.com
4. **HTML 测试页面**：参考 `WEBSOCKET_TEST.md` 中的示例代码

### 测试流程
1. 启动应用：`mvn spring-boot:run`
2. 登录获取 token：`POST /user/login`
3. 建立 WebSocket 连接：`ws://localhost:8091/ws/chat?token=xxx`
4. 发送测试消息（JSON 格式）
5. 验证消息保存到数据库、会话更新、实时推送等功能

### 参考文档
- **WebSocket 使用文档**：`WEBSOCKET_TEST.md`（包含完整的测试示例和前端代码）
- **API 文档**：可使用 Swagger/Knife4j 生成（需添加依赖）
- **数据库脚本**：见 `sql/` 目录（如有）
