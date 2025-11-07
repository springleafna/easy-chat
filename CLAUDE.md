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
- ✅ 好友申请与审批流程
- ✅ 好友与群组管理
- ✅ 群组邀请机制（类似微信，无需审批）

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
  - `UserController`：用户管理（登录、注册、搜索用户）
  - `FriendController`：好友管理（好友申请、同意/拒绝、撤回、好友列表）
  - `GroupController`：群组管理
  - `MessageController`：消息历史查询（分页）
  - `ConversationController`：会话列表、标记活跃会话、置顶、免打扰

- **`service/`** 和 `service/impl/` - 业务逻辑层
  - Service 接口定义业务方法
  - impl 包含具体实现
  - **UserService**：用户注册、登录、信息更新、用户搜索
  - **FriendService**：好友关系管理（删除好友、好友列表）
  - **FriendRequestService**：好友申请管理（发送、处理、撤回、查询）
  - **MessageService**：消息发送、历史查询、删除、撤回
  - **ConversationService**：会话列表查询、置顶、免打扰
  - **UnreadService**：基于 Redis 的未读消息管理、活跃会话管理

- **`mapper/`** - MyBatis-Plus 数据访问层
  - 继承 `BaseMapper<T>` 获得 CRUD 基础方法
  - 自定义 SQL 在 Mapper 接口中定义

- **`model/`** - 数据模型
  - `entity/`：数据库实体类
    - User, Message, Friend, FriendRequest, Group, GroupMember, Conversation
  - `dto/`：数据传输对象（请求参数）
    - `SendMessageDTO`：发送消息请求
    - `MessageHistoryDTO`：历史消息查询请求
    - `ActiveChatDTO`：设置活跃会话请求
    - `SendFriendRequestDTO`：发送好友申请请求
    - `HandleFriendRequestDTO`：处理好友申请请求
    - `SearchUserRequest`：搜索用户请求
  - `vo/`：视图对象（响应数据）
    - `MessageVO`：消息详情（含发送者信息、会话ID）
    - `ConversationVO`：会话详情（含最后消息预览、未读数、置顶、免打扰）
    - `FriendRequestVO`：好友申请详情（含申请人信息、状态）
    - `UserSearchVO`：用户搜索结果（含好友关系状态）

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
  - `FriendStatusEnum`：好友关系状态（正常、已删除、黑名单）
  - `FriendRequestStatusEnum`：好友申请状态（待处理、已同意、已拒绝、已撤回）
  - `UserStatusEnum`、`GroupMemberRoleEnum` 等业务枚举

- **`exception/`** - 异常定义
  - `BusinessException`：业务异常

- **`utils/`** - 工具类
  - `UserContextUtil`：获取当前登录用户信息
  - `ConversationIdUtil`：会话ID生成与解析（单聊、群聊）

- **`constants/`** - 静态常量管理
  - `RedisKeyConstants`：Redis键管理（未读数、活跃会话）

### 关键设计模式

**1. 认证授权机制（Sa-Token）**
- 所有 API（除登录/注册）需要登录认证
- Token 名称为 `Authorization`（Header 传递）
- 不支持同账号并发登录（新登录会挤掉旧登录）
- Token 一天（`timeout: 86400`）
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

**10. 好友申请机制（类似微信）**
- **申请流程**：发送申请 → 等待审批 → 同意/拒绝 → 成为好友
- **状态管理**：待处理、已同意、已拒绝、已撤回
- **防重复申请**：同一用户对同一目标的待处理申请只能有一条（业务逻辑控制）
- **已是好友检查**：发送申请前检查是否已经是好友关系
- **撤回机制**：申请人可以撤回待处理的申请
- **数据一致性**：同意申请时，自动在 `friends` 表创建双向好友关系
- **Service层**：`FriendRequestService` 提供完整的申请管理功能
  - `sendFriendRequest()`：发送好友申请
  - `handleFriendRequest()`：处理申请（同意创建双向好友关系，拒绝记录原因）
  - `withdrawFriendRequest()`：撤回申请
  - `getReceivedRequests()`：获取收到的申请列表
  - `getSentRequests()`：获取发出的申请列表
  - `getUnhandledCount()`：获取未处理申请数量

**11. 群组邀请机制（类似微信，无需审批）**
- **设计理念**：采用微信模式，群成员可以直接邀请好友进群，无需审批流程
- **inviter_id字段**：`group_members` 表包含 `inviter_id` 字段，记录邀请人ID
- **入群方式**：
  - 创建群组：群主创建群时直接拉好友进群（`inviter_id` 为群主ID）
  - 邀请进群：群成员直接邀请好友，无需申请表（直接插入 `group_members` 记录）
  - 扫码进群：通过二维码或邀请链接直接进群（`inviter_id` 可为 NULL）
- **无需申请表**：群组加入不使用 `friend_requests` 表，直接操作 `group_members` 表
- **权限控制**（可选）：可在 `groups` 表添加 `join_mode` 字段控制入群方式（预留扩展）

## 数据库说明

### 核心实体表
- **users**：用户表（账号、密码、昵称、头像等）
- **friends**：好友关系表（双向关系）
- **friend_requests**：好友申请表（申请人、目标用户、状态、备注等）
- **groups**：群组表
- **group_members**：群成员表（新增 inviter_id 字段记录邀请人）
- **messages**：消息表（存储所有聊天消息）
- **conversations**：会话表（单聊/群聊会话管理）

具体sql参考/docs/easy_chat.sql

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

#### 用户相关
- **POST /user/register** - 用户注册
- **POST /user/login** - 用户登录
- **GET /user/info** - 获取当前用户信息
- **PUT /user/update** - 更新用户信息
- **POST /user/logout** - 退出登录
- **POST /user/search** - 搜索用户（支持手机号、账号、邮箱搜索）

#### 好友申请相关
- **POST /friend/request/send** - 发送好友申请
- **POST /friend/request/handle** - 处理好友申请（同意/拒绝）
- **DELETE /friend/request/withdraw/{requestId}** - 撤回好友申请
- **GET /friend/request/received** - 获取收到的好友申请列表
- **GET /friend/request/sent** - 获取发出的好友申请列表
- **GET /friend/request/unhandled/count** - 获取未处理的好友申请数量

#### 好友管理相关
- **GET /friend/list** - 获取好友列表
- **DELETE /friend/delete** - 删除好友
- **POST /friend/add** - ⚠️ 已废弃，请使用好友申请流程

#### 会话相关
- **GET /conversation/list** - 获取会话列表（含未读数、最后消息预览、置顶、免打扰状态）
- **POST /conversation/active** - 设置活跃会话（用户进入聊天页时调用）
  - 请求体：`{"conversationId": "s_1_3"}`
  - 作用：设置当前活跃会话ID，60秒内收到该会话消息不增加未读数
- **PUT /conversation/pin/{conversationId}** - 切换会话置顶状态
- **PUT /conversation/mute/{conversationId}** - 切换会话免打扰状态

#### 消息相关
- **GET /message/history** - 分页查询历史消息（自动清除未读数）
  - 参数：`conversationId`, `lastMessageId`, `size`（游标分页，推荐）
- **DELETE /message/{messageId}** - 删除消息（仅发送者可删除）
- **PUT /message/recall/{messageId}** - 撤回消息（仅发送者可撤回）

### 用户交互流程

#### 添加好友流程（申请审批模式）
```
1. 用户A 搜索用户 (POST /user/search，支持手机号/账号/邮箱)
   → 返回用户列表，含好友关系状态（是否已是好友）
2. 用户A 发送好友申请 (POST /friend/request/send)
   → 系统检查：不能向自己发申请、目标用户存在、未是好友、无待处理申请
   → 插入 friend_requests 表（status=0 待处理）
3. 用户B 查看收到的申请 (GET /friend/request/received)
   → 显示申请人信息、申请备注、申请时间等
4. 用户B 处理申请：
   a. 同意 (POST /friend/request/handle, accept=true)
      → 更新 friend_requests.status=1（已同意）
      → 在 friends 表创建双向好友关系
      → 可选填写好友备注名
   b. 拒绝 (POST /friend/request/handle, accept=false)
      → 更新 friend_requests.status=2（已拒绝）
      → 可选填写拒绝原因
5. 用户A 查看发出的申请状态 (GET /friend/request/sent)
   → 显示待处理/已同意/已拒绝状态
6. 用户A 可撤回待处理的申请 (DELETE /friend/request/withdraw/{id})
   → 更新 friend_requests.status=3（已撤回）
```

#### 聊天消息流程
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

#### 群组邀请流程（无需审批）
```
1. 创建群组：
   - 用户A 点击"创建群组"，选择好友列表中的多个好友（B, C, D）
   - 系统执行：
     a. 在 groups 表插入群组记录（owner_id=A）
     b. 在 group_members 表插入记录：
        - A（群主，role=3, inviter_id=NULL）
        - B（普通成员，role=1, inviter_id=A）
        - C（普通成员，role=1, inviter_id=A）
        - D（普通成员，role=1, inviter_id=A）
     c. 为所有成员创建群聊会话（conversations 表）
     d. 发送系统消息："用户A 创建了群聊"

2. 邀请进群（类似微信，无需审批）：
   - 用户A（群成员）点击"邀请好友进群"，选择好友B
   - 系统检查：用户B 是否已在群中、群是否已满员
   - 检查通过后：
     a. 直接在 group_members 表插入记录（user_id=B, inviter_id=A, status=1）
     b. 为用户B 创建群聊会话
     c. 发送系统消息："用户A 邀请 用户B 加入群聊"
   - ⚠️ 无需申请审批流程，直接加入
```

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

### 参考文档
- **数据库脚本**：见 `docs/easy_chat.sql` 
