# CLAUDE.md

此文件为 Claude Code (claude.ai/code) 在该代码库中工作时提供指导。

## 项目概述

EasyChat 是一个基于 Spring Boot 3.4.2 的即时通讯系统后端，使用 Java 21 开发。

**核心技术栈：**
- Spring Boot 3.4.2 + Spring Web
- MyBatis-Plus 3.5.14（数据库 ORM）
- Sa-Token 1.37.0（认证授权框架）
- MySQL 8.0.33
- Redis（会话管理）
- Lombok（代码简化）

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
  - UserController：用户管理（登录、注册）
  - FriendController：好友管理
  - GroupController：群组管理
  - MessageController：消息处理
  - ConversationController：会话管理

- **`service/`** 和 `service/impl/` - 业务逻辑层
  - Service 接口定义业务方法
  - impl 包含具体实现

- **`mapper/`** - MyBatis-Plus 数据访问层
  - 继承 `BaseMapper<T>` 获得 CRUD 基础方法
  - 自定义 SQL 在 Mapper 接口中定义

- **`model/`** - 数据模型
  - `entity/`：数据库实体类（User, Message, Friend, Group, GroupMember, Conversation）
  - `dto/`：数据传输对象（请求参数）
  - `vo/`：视图对象（响应数据）

- **`config/`** - 配置类
  - `SaTokenConfig`：认证拦截器配置，拦截所有请求（除 `/user/login`, `/user/register`, `/error`）
  - `WebConfig`：Web 相关配置

- **`handler/`** - 处理器
  - `GlobalExceptionHandler`：全局异常处理
  - `MyMetaObjectHandler`：MyBatis-Plus 自动填充 `createdAt` 和 `updatedAt`

- **`common/`** - 通用组件
  - `Result<T>`：统一响应结果封装

- **`enums/`** - 枚举类
  - `ResultCodeEnum`：响应状态码
  - `UserStatusEnum`、`FriendStatusEnum`、`ConversationTypeEnum` 等业务枚举

- **`exception/`** - 异常定义
  - `BusinessException`：业务异常

- **`utils/`** - 工具类

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

## 数据库说明

### 核心实体表
- **users**：用户表（账号、密码、昵称、头像等）
- **friends**：好友关系表
- **groups**：群组表
- **group_members**：群成员表
- **messages**：消息表
- **conversations**：会话表（单聊/群聊）

### 时区配置
- Jackson 时区设置为 GMT+8
- MySQL 连接时区为 Asia/Shanghai
- 实体类使用 `LocalDateTime` 和 `LocalDate` 处理时间

## 开发注意事项

### 添加新的 API 端点
1. 在 Controller 中定义接口方法，使用 `@RestController` 和 `@RequestMapping`
2. 返回值统一使用 `Result<T>` 封装
3. 如果不需要认证，在 `SaTokenConfig.java:21-25` 添加到 `excludePathPatterns`
4. 使用 `@Validated` 和 DTO 进行参数校验

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
