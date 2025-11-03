# WebSocket 聊天功能使用说明

## 功能概述

已实现基于 WebSocket 的即时通讯功能，支持：
- ✅ 单聊（一对一聊天）
- ✅ 群聊（多人群组聊天）
- ✅ 实时消息推送
- ✅ 在线状态管理
- ✅ 自动会话创建和更新

## WebSocket 连接

### 连接地址
```
ws://localhost:8091/ws/chat?token=YOUR_TOKEN
```

### 参数说明
- `token`: 用户登录后获取的 Sa-Token，通过 URL 参数传递

### 连接示例（使用 JavaScript）
```javascript
// 假设你已经通过登录接口获得了 token
const token = "your_sa_token_here";
const ws = new WebSocket(`ws://localhost:8091/ws/chat?token=${token}`);

ws.onopen = function() {
    console.log("WebSocket 连接成功");
};

ws.onmessage = function(event) {
    const message = JSON.parse(event.data);
    console.log("收到消息:", message);
};

ws.onerror = function(error) {
    console.error("WebSocket 错误:", error);
};

ws.onclose = function() {
    console.log("WebSocket 连接关闭");
};
```

## 发送消息

### 单聊消息格式
```json
{
    "messageType": 1,
    "conversationType": 1,
    "receiverId": 2,
    "content": "你好，这是一条单聊消息"
}
```

### 群聊消息格式
```json
{
    "messageType": 1,
    "conversationType": 2,
    "groupId": 1,
    "content": "大家好，这是一条群聊消息"
}
```

### 发送示例（JavaScript）
```javascript
// 发送单聊消息
const privateMessage = {
    messageType: 1,        // 1-文本消息
    conversationType: 1,   // 1-单聊
    receiverId: 2,         // 接收者用户ID
    content: "你好"
};
ws.send(JSON.stringify(privateMessage));

// 发送群聊消息
const groupMessage = {
    messageType: 1,        // 1-文本消息
    conversationType: 2,   // 2-群聊
    groupId: 1,            // 群组ID
    content: "大家好"
};
ws.send(JSON.stringify(groupMessage));

// 发送图片消息
const imageMessage = {
    messageType: 2,        // 2-图片消息
    conversationType: 1,   // 1-单聊
    receiverId: 2,
    mediaUrl: "https://example.com/image.jpg"
};
ws.send(JSON.stringify(imageMessage));

// 发送文件消息
const fileMessage = {
    messageType: 5,        // 5-文件消息
    conversationType: 1,
    receiverId: 2,
    fileName: "document.pdf",
    fileSize: 1024000,
    mediaUrl: "https://example.com/files/document.pdf"
};
ws.send(JSON.stringify(fileMessage));
```

## 接收消息

### 消息格式（MessageVO）
```json
{
    "id": 1,
    "conversationId": 1,
    "senderId": 1,
    "senderNickname": "张三",
    "senderAvatar": "https://example.com/avatar.jpg",
    "receiverId": 2,
    "groupId": null,
    "messageType": 1,
    "conversationType": 1,
    "content": "你好",
    "mediaUrl": null,
    "fileName": null,
    "fileSize": null,
    "status": 1,
    "createdAt": "2025-11-03T10:30:00"
}
```

### 字段说明
- `id`: 消息ID
- `conversationId`: 会话ID
- `senderId`: 发送者ID
- `senderNickname`: 发送者昵称
- `senderAvatar`: 发送者头像
- `receiverId`: 接收者ID（单聊）
- `groupId`: 群组ID（群聊）
- `messageType`: 消息类型（1-文本，2-图片，3-语音，4-视频，5-文件，6-位置，7-系统消息）
- `conversationType`: 会话类型（1-单聊，2-群聊）
- `content`: 消息内容
- `mediaUrl`: 媒体文件URL
- `fileName`: 文件名
- `fileSize`: 文件大小（字节）
- `status`: 消息状态（0-已撤回，1-正常，2-已删除）
- `createdAt`: 创建时间

## 完整测试流程

### 1. 用户登录获取 Token
```bash
curl -X POST http://localhost:8091/user/login \
  -H "Content-Type: application/json" \
  -d '{
    "account": "user1",
    "password": "password123"
  }'
```

响应：
```json
{
    "code": 200,
    "message": "登录成功",
    "data": {
        "token": "abc123...",
        "user": {...}
    }
}
```

### 2. 使用 Token 建立 WebSocket 连接
```javascript
const token = "abc123...";  // 从登录响应中获取
const ws = new WebSocket(`ws://localhost:8091/ws/chat?token=${token}`);
```

### 3. 发送和接收消息
```javascript
// 监听连接成功
ws.onopen = () => {
    console.log("连接成功");

    // 发送单聊消息
    ws.send(JSON.stringify({
        messageType: 1,
        conversationType: 1,
        receiverId: 2,
        content: "你好"
    }));
};

// 监听接收消息
ws.onmessage = (event) => {
    const message = JSON.parse(event.data);

    // 判断是否为错误消息
    if (message.error) {
        console.error("错误:", message.error);
        return;
    }

    // 处理正常消息
    console.log("收到消息:");
    console.log("发送者:", message.senderNickname);
    console.log("内容:", message.content);
    console.log("时间:", message.createdAt);
};
```

## 使用 Postman 测试（推荐）

### 1. 创建 WebSocket 请求
1. 打开 Postman，选择 "New" -> "WebSocket Request"
2. 输入连接地址：`ws://localhost:8091/ws/chat?token=YOUR_TOKEN`
3. 点击 "Connect"

### 2. 发送测试消息
在消息输入框中输入 JSON 格式的消息：
```json
{
    "messageType": 1,
    "conversationType": 1,
    "receiverId": 2,
    "content": "测试消息"
}
```

### 3. 查看响应
在 "Messages" 面板中查看接收到的消息。

## HTML 测试页面示例

创建一个简单的 HTML 文件进行测试：

```html
<!DOCTYPE html>
<html>
<head>
    <title>WebSocket 聊天测试</title>
    <meta charset="UTF-8">
</head>
<body>
    <h2>WebSocket 聊天测试</h2>

    <div>
        <label>Token: <input type="text" id="token" size="50"></label>
        <button onclick="connect()">连接</button>
        <button onclick="disconnect()">断开</button>
    </div>

    <div style="margin-top: 20px;">
        <h3>发送消息</h3>
        <label>接收者ID: <input type="number" id="receiverId"></label><br>
        <label>消息内容: <input type="text" id="content" size="50"></label><br>
        <button onclick="sendMessage()">发送单聊消息</button>
    </div>

    <div style="margin-top: 20px;">
        <h3>消息记录</h3>
        <div id="messages" style="border: 1px solid #ccc; height: 300px; overflow-y: scroll; padding: 10px;"></div>
    </div>

    <script>
        let ws = null;

        function connect() {
            const token = document.getElementById('token').value;
            if (!token) {
                alert('请输入 Token');
                return;
            }

            ws = new WebSocket(`ws://localhost:8091/ws/chat?token=${token}`);

            ws.onopen = () => {
                addMessage('系统', '连接成功');
            };

            ws.onmessage = (event) => {
                const message = JSON.parse(event.data);
                if (message.error) {
                    addMessage('错误', message.error);
                } else {
                    addMessage(message.senderNickname, message.content);
                }
            };

            ws.onerror = (error) => {
                addMessage('系统', '连接错误: ' + error);
            };

            ws.onclose = () => {
                addMessage('系统', '连接已关闭');
            };
        }

        function disconnect() {
            if (ws) {
                ws.close();
                ws = null;
            }
        }

        function sendMessage() {
            if (!ws || ws.readyState !== WebSocket.OPEN) {
                alert('未连接到服务器');
                return;
            }

            const receiverId = document.getElementById('receiverId').value;
            const content = document.getElementById('content').value;

            if (!receiverId || !content) {
                alert('请填写接收者ID和消息内容');
                return;
            }

            const message = {
                messageType: 1,
                conversationType: 1,
                receiverId: parseInt(receiverId),
                content: content
            };

            ws.send(JSON.stringify(message));
            document.getElementById('content').value = '';
        }

        function addMessage(sender, content) {
            const messagesDiv = document.getElementById('messages');
            const time = new Date().toLocaleTimeString();
            messagesDiv.innerHTML += `<div><strong>[${time}] ${sender}:</strong> ${content}</div>`;
            messagesDiv.scrollTop = messagesDiv.scrollHeight;
        }
    </script>
</body>
</html>
```

## 注意事项

1. **Token 认证**：必须携带有效的 Sa-Token 才能建立连接
2. **消息持久化**：所有消息都会自动保存到数据库
3. **离线消息**：用户离线时收到的消息会存储到数据库，上线后可通过 REST API 查询
4. **会话管理**：首次发送消息时会自动创建会话
5. **群聊权限**：只有群成员才能发送群聊消息
6. **在线状态**：WebSocket 连接建立后，用户即为在线状态

## 错误处理

如果收到错误消息，格式如下：
```json
{
    "error": "错误描述"
}
```

常见错误：
- "无效的 token"：Token 认证失败
- "发送者不存在"：发送者用户不存在
- "接收者不存在"：接收者用户不存在
- "您不在该群组中"：尝试发送群聊消息但不是群成员
- "消息类型不能为空"：缺少必要参数

## REST API 补充

除了 WebSocket 实时通讯，还需要以下 REST API 配合使用：

### 1. 获取会话列表
```http
GET /conversation/list
Authorization: {your_token}
```

**响应示例：**
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "type": 1,
      "conversationName": "张三",
      "avatarUrl": "https://example.com/avatar.jpg",
      "unreadCount": 5,
      "lastMessageContent": "你好，在吗？",
      "lastMessageTime": "2025-11-03 14:30:00"
    },
    {
      "id": 2,
      "type": 2,
      "conversationName": "技术交流群",
      "avatarUrl": "https://example.com/group.jpg",
      "unreadCount": 0,
      "lastMessageContent": "[图片]",
      "lastMessageTime": "2025-11-03 14:25:00"
    }
  ]
}
```

### 2. 标记会话为已读（清除未读数）

**注意：通常不需要单独调用此接口！**

当用户首次加载聊天历史消息时（调用 `/message/history` 接口），系统会**自动**将会话标记为已读。只有在特殊场景下（例如：用户只想清除未读数但不打开聊天窗口）才需要单独调用此接口。

```http
PUT /conversation/read/{conversationId}
Authorization: {your_token}
```

**示例：**
```bash
curl -X PUT http://localhost:8091/conversation/read/1 \
  -H "Authorization: your_token_here"
```

**响应：**
```json
{
  "code": 200,
  "message": "操作成功",
  "data": null
}
```

### 3. 获取会话的历史消息（分页）

**重要提示：首次加载消息时，会自动标记会话为已读（清除未读数）！**

```http
GET /message/history?conversationId={conversationId}&page={page}&size={size}
Authorization: {your_token}
```

**自动标记已读的触发条件：**
- 首次加载消息（page=1 且没有 lastMessageId）
- 会话有未读消息（unreadCount > 0）

这意味着前端只需要调用获取历史消息的接口，无需额外调用标记已读接口。

#### 方式一：普通分页（基于页码）

**请求参数：**
- `conversationId`（必填）：会话ID
- `page`（可选）：页码，从1开始，默认1
- `size`（可选）：每页大小，默认20，最大100

**示例：**
```bash
# 获取第1页，每页20条
curl -X GET "http://localhost:8091/message/history?conversationId=1&page=1&size=20" \
  -H "Authorization: your_token_here"

# 获取第2页
curl -X GET "http://localhost:8091/message/history?conversationId=1&page=2&size=20" \
  -H "Authorization: your_token_here"
```

#### 方式二：游标分页（推荐，用于滚动加载）

**请求参数：**
- `conversationId`（必填）：会话ID
- `lastMessageId`（必填）：最后一条消息的ID（已加载的最早的一条消息）
- `size`（可选）：每次加载多少条，默认20

**示例：**
```bash
# 首次加载（不需要 lastMessageId）
curl -X GET "http://localhost:8091/message/history?conversationId=1&size=20" \
  -H "Authorization: your_token_here"

# 向上滚动加载更多（使用已加载的最早一条消息的ID）
curl -X GET "http://localhost:8091/message/history?conversationId=1&lastMessageId=100&size=20" \
  -H "Authorization: your_token_here"
```

**响应格式：**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [
      {
        "id": 123,
        "conversationId": 1,
        "senderId": 2,
        "senderNickname": "张三",
        "senderAvatar": "https://example.com/avatar.jpg",
        "receiverId": 1,
        "groupId": null,
        "messageType": 1,
        "conversationType": 1,
        "content": "你好，在吗？",
        "mediaUrl": null,
        "fileName": null,
        "fileSize": null,
        "status": 1,
        "createdAt": "2025-11-03 14:30:00"
      },
      {
        "id": 122,
        "conversationId": 1,
        "senderId": 1,
        "senderNickname": "我",
        "senderAvatar": "https://example.com/my-avatar.jpg",
        "receiverId": 2,
        "groupId": null,
        "messageType": 2,
        "conversationType": 1,
        "content": null,
        "mediaUrl": "https://example.com/image.jpg",
        "fileName": null,
        "fileSize": null,
        "status": 1,
        "createdAt": "2025-11-03 14:28:00"
      }
    ],
    "total": 150,
    "size": 20,
    "current": 1,
    "pages": 8
  }
}
```

**响应字段说明：**
- `records`：消息列表（按时间倒序，最新的在前）
- `total`：总消息数
- `size`：每页大小
- `current`：当前页码
- `pages`：总页数

#### 前端滚动加载实现示例

```javascript
class ChatWindow {
  constructor(conversationId) {
    this.conversationId = conversationId;
    this.messages = []; // 消息列表（按时间正序，最早的在前）
    this.loading = false;
    this.hasMore = true;
    this.pageSize = 20;
  }

  // 首次加载消息
  async loadInitialMessages() {
    const response = await fetch(
      `/message/history?conversationId=${this.conversationId}&size=${this.pageSize}`,
      {
        headers: { 'Authorization': token }
      }
    );
    const result = await response.json();

    if (result.code === 200) {
      // 接口返回的是倒序（最新的在前），需要反转成正序（最早的在前）
      this.messages = result.data.records.reverse();
      this.hasMore = result.data.current < result.data.pages;

      // 渲染消息并滚动到底部
      this.renderMessages();
      this.scrollToBottom();
    }
  }

  // 向上滚动加载更多历史消息
  async loadMoreMessages() {
    if (this.loading || !this.hasMore) return;

    this.loading = true;

    // 获取当前已加载的最早一条消息的ID
    const oldestMessageId = this.messages[0]?.id;

    if (!oldestMessageId) {
      this.loading = false;
      return;
    }

    const response = await fetch(
      `/message/history?conversationId=${this.conversationId}&lastMessageId=${oldestMessageId}&size=${this.pageSize}`,
      {
        headers: { 'Authorization': token }
      }
    );
    const result = await response.json();

    if (result.code === 200) {
      const newMessages = result.data.records.reverse();

      // 保存当前滚动位置
      const scrollHeight = this.chatContainer.scrollHeight;

      // 将新消息插入到数组前面
      this.messages = [...newMessages, ...this.messages];

      // 渲染消息
      this.renderMessages();

      // 保持滚动位置（避免跳动）
      const newScrollHeight = this.chatContainer.scrollHeight;
      this.chatContainer.scrollTop = newScrollHeight - scrollHeight;

      // 更新状态
      this.hasMore = result.data.current < result.data.pages;
    }

    this.loading = false;
  }

  // 监听滚动事件
  setupScrollListener() {
    this.chatContainer.addEventListener('scroll', () => {
      // 滚动到顶部时加载更多
      if (this.chatContainer.scrollTop < 100) {
        this.loadMoreMessages();
      }
    });
  }

  // 接收到新消息（通过WebSocket）
  onNewMessage(message) {
    // 将新消息添加到数组末尾
    this.messages.push(message);
    this.renderMessages();
    this.scrollToBottom();
  }

  renderMessages() {
    // 渲染消息到UI
    // ...
  }

  scrollToBottom() {
    this.chatContainer.scrollTop = this.chatContainer.scrollHeight;
  }
}

// 使用示例
const chatWindow = new ChatWindow(conversationId);
chatWindow.loadInitialMessages();
chatWindow.setupScrollListener();
```

#### 两种分页方式对比

| 特性 | 普通分页（page） | 游标分页（lastMessageId） |
|------|----------------|------------------------|
| **适用场景** | 查看历史记录、跳页浏览 | 聊天窗口滚动加载 |
| **优点** | 可以跳转到任意页 | 性能好，数据一致性强 |
| **缺点** | 数据插入时页码会偏移 | 不能跳页，只能连续加载 |
| **推荐度** | ⭐⭐⭐ | ⭐⭐⭐⭐⭐（强烈推荐） |

**为什么推荐游标分页？**

假设场景：你正在查看第2页的消息，此时新消息不断到来：

- **普通分页**：
  - 第1页：消息1-20
  - 第2页：消息21-40
  - *此时新消息插入*
  - 再次请求第2页：消息22-41（消息21被挤到第1页了）❌ 数据重复！

- **游标分页**：
  - 请求：`lastMessageId=40`
  - 返回：比40更早的消息（39, 38, 37...）
  - *无论新消息如何插入，都不影响*
  - 结果始终一致 ✅

### 未读消息处理流程

#### 完整的用户交互流程：

```
1. 用户A打开APP
   ↓
2. 建立WebSocket连接 + 获取会话列表
   GET /conversation/list
   → 显示：张三 (5条未读)
   ↓
3. 用户A点击"张三"的会话
   ↓
4. 前端加载历史消息
   GET /message/history?conversationId=1&size=20
   ↓
5. 后端自动标记会话为已读（unreadCount = 0）✅
   ↓
6. 前端收到消息列表 + 会话列表自动更新
   → 显示：张三 (0条未读) ✅
```

#### 前端实现示例（JavaScript）

```javascript
// 用户点击进入聊天窗口
async function enterConversation(conversationId) {
  // 只需要加载历史消息，后端会自动标记已读
  await loadChatWindow(conversationId);

  // 可选：主动刷新会话列表以更新未读数显示
  // 或者直接在本地更新该会话的 unreadCount = 0
  updateLocalUnreadCount(conversationId, 0);
}

// 加载聊天窗口和历史消息
async function loadChatWindow(conversationId) {
  try {
    const response = await fetch(
      `/message/history?conversationId=${conversationId}&size=20`,
      {
        method: 'GET',
        headers: {
          'Authorization': token
        }
      }
    );

    if (response.ok) {
      const result = await response.json();
      // 渲染消息列表
      renderMessages(result.data.records.reverse());
      // 滚动到底部
      scrollToBottom();

      // 此时后端已经自动将会话标记为已读
      // 前端可以更新本地的会话列表状态
    }
  } catch (error) {
    console.error('加载聊天失败', error);
  }
}
```

### 注意事项

1. **自动标记已读的逻辑**：
   - ✅ 首次加载聊天窗口时自动标记已读
   - ✅ 向上滚动加载更多历史消息时不会重复标记
   - ✅ 只在有未读消息时才更新数据库，避免不必要的操作

2. **权限验证**：
   - 接口会验证会话是否属于当前用户
   - 防止用户查看他人的聊天记录

3. **与WebSocket的配合**：
   - WebSocket：实时推送新消息，增加未读数
   - HTTP API：查看历史消息时自动清除未读数
   - 两者配合实现完整的未读消息管理

### 待实现的其他接口

以下接口可以根据需要实现：

- `GET /message/history?conversationId={id}&page={page}&size={size}` - 获取历史消息（分页）
- `DELETE /message/{id}` - 删除消息
- `PUT /message/{id}/recall` - 撤回消息
- `DELETE /conversation/{id}` - 删除会话
