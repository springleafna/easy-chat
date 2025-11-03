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
  "message": "标记已读成功",
  "data": null
}
```

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
4. 前端立即发送请求标记已读
   PUT /conversation/read/1
   ↓
5. 前端刷新会话列表或本地更新
   → 显示：张三 (0条未读) ✅
   ↓
6. 同时加载历史消息
   GET /message/history?conversationId=1
```

#### 前端实现示例（JavaScript）

```javascript
// 用户点击进入聊天窗口
function enterConversation(conversationId) {
  // 1. 如果有未读消息，立即标记为已读
  if (hasUnreadMessages(conversationId)) {
    markAsRead(conversationId);
  }

  // 2. 加载聊天界面和历史消息
  loadChatWindow(conversationId);
}

// 标记已读
async function markAsRead(conversationId) {
  try {
    const response = await fetch(`/conversation/read/${conversationId}`, {
      method: 'PUT',
      headers: {
        'Authorization': token
      }
    });

    if (response.ok) {
      // 更新本地会话列表的未读数
      updateLocalUnreadCount(conversationId, 0);
    }
  } catch (error) {
    console.error('标记已读失败', error);
  }
}
```

### 注意事项

1. **时机选择**：
   - ✅ 用户**点击进入**聊天窗口时调用
   - ❌ 不要等到用户发送消息时才调用
   - ❌ 不要在收到消息时自动调用（用户可能没看到）

2. **权限验证**：
   - 接口会验证会话是否属于当前用户
   - 防止用户标记别人的会话为已读

3. **幂等性**：
   - 如果未读数已经是0，不会更新数据库
   - 可以多次调用同一个会话的已读接口

4. **与WebSocket的配合**：
   - WebSocket：实时推送消息，增加未读数
   - HTTP API：用户主动标记已读，清除未读数
   - 两者独立工作，互不干扰

### 待实现的其他接口

以下接口可以根据需要实现：

- `GET /message/history?conversationId={id}&page={page}&size={size}` - 获取历史消息（分页）
- `DELETE /message/{id}` - 删除消息
- `PUT /message/{id}/recall` - 撤回消息
- `DELETE /conversation/{id}` - 删除会话
