CREATE DATABASE easy_chat IF NOT EXISTS;

CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    account VARCHAR(50) UNIQUE NOT NULL COMMENT '账号唯一',
    nickname VARCHAR(100) NOT NULL COMMENT '昵称',
    password VARCHAR(255) NOT NULL COMMENT '密码',
    phone VARCHAR(20) UNIQUE COMMENT '手机号',
    email VARCHAR(100) UNIQUE COMMENT '邮箱',
    avatar_url VARCHAR(500) COMMENT '头像URL',
    region VARCHAR(500) COMMENT '地区',
    gender TINYINT DEFAULT 0 COMMENT '性别：0-未知，1-男，2-女',
    birthday DATE COMMENT '生日',
    signature VARCHAR(200) DEFAULT '' COMMENT '个性签名',
    status TINYINT DEFAULT 1 COMMENT '用户状态：0-禁用，1-正常，2-注销',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_login_at TIMESTAMP NULL COMMENT '最后登录时间',

    INDEX idx_account (account),
    INDEX idx_phone (phone),
    INDEX idx_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

CREATE TABLE friends (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    friend_id BIGINT NOT NULL COMMENT '好友ID',
    remark_name VARCHAR(100) COMMENT '好友备注名',
    status TINYINT DEFAULT 1 COMMENT '关系状态：0-已删除，1-正常，2-黑名单',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY uk_user_friend (user_id, friend_id),
    INDEX idx_user_id (user_id),
    INDEX idx_friend_id (friend_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='好友表';

CREATE TABLE `groups` (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '群组ID',
    group_name VARCHAR(100) NOT NULL COMMENT '群名称',
    owner_id BIGINT NOT NULL COMMENT '群主ID',
    avatar_url VARCHAR(500) COMMENT '群头像URL',
    announcement TEXT COMMENT '群公告',
    max_members INT DEFAULT 500 COMMENT '最大成员数',
    status TINYINT DEFAULT 1 COMMENT '群状态：0-已解散，1-正常',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_owner_id (owner_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='群组表';

CREATE TABLE group_members (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    group_id BIGINT NOT NULL COMMENT '群组ID',
    user_id BIGINT NOT NULL COMMENT '成员ID',
    inviter_id BIGINT DEFAULT NULL COMMENT '邀请人ID（谁拉你进群的，创建群时为NULL）',
    nickname VARCHAR(100) COMMENT '群内昵称',
    role TINYINT DEFAULT 1 COMMENT '成员角色：1-普通成员，2-管理员，3-群主',
    status TINYINT DEFAULT 1 COMMENT '成员状态：0-已退出，1-正常',
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',

    UNIQUE KEY uk_group_user (group_id, user_id),
    INDEX idx_group_id (group_id),
    INDEX idx_user_id (user_id),
    INDEX idx_inviter (inviter_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='群组成员表';

CREATE TABLE conversations (
    conversation_id VARCHAR(64) NOT NULL COMMENT '逻辑会话ID：单聊 s_{min}_{max}，群聊 g_{group_id}',
    type TINYINT NOT NULL COMMENT '会话类型：1-单聊，2-群聊',
    user_id BIGINT NOT NULL COMMENT '当前用户ID',
    target_id BIGINT NOT NULL COMMENT '目标ID（单聊为对方user_id，群聊为group_id）',
    last_message_id BIGINT DEFAULT NULL COMMENT '最后一条消息ID',
    last_message_time TIMESTAMP NULL COMMENT '最后消息时间',
    status TINYINT DEFAULT 1 COMMENT '会话状态：0-已删除，1-正常',
    pinned BOOLEAN DEFAULT FALSE COMMENT '是否置顶',
    muted BOOLEAN DEFAULT FALSE COMMENT '是否免打扰',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    -- 联合主键：确保每个用户对每个会话只有一条记录
    PRIMARY KEY (user_id, conversation_id),

    -- 用于按时间倒序拉取会话列表
    INDEX idx_user_status_time (user_id, status, last_message_time DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会话表';

CREATE TABLE messages (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '消息ID',
    conversation_id VARCHAR(64) NOT NULL COMMENT '会话ID',
    sender_id BIGINT NOT NULL COMMENT '发送者ID',
    message_type TINYINT NOT NULL COMMENT '消息类型：1-文本，2-图片，3-语音，4-视频，5-文件，6-位置，7-系统消息',
    content TEXT COMMENT '消息内容',
    media_url VARCHAR(500) COMMENT '媒体文件URL（图片/语音/视频等）',
    file_name VARCHAR(200) COMMENT '文件名',
    file_size INT COMMENT '文件大小（字节）',
    status TINYINT DEFAULT 1 COMMENT '消息状态：0-已撤回，1-正常，2-已删除',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    -- 关键索引：按会话分页查询消息（最新在前）
    INDEX idx_conv_created (conversation_id, created_at DESC),

    -- 辅助索引：查某人发送的所有消息（用于个人中心等）
    INDEX idx_sender_created (sender_id, created_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息表';

CREATE TABLE friend_requests (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '申请ID',
    requester_id BIGINT NOT NULL COMMENT '申请人ID（发起好友申请的用户）',
    target_id BIGINT NOT NULL COMMENT '目标用户ID（被申请添加为好友的用户）',
    status TINYINT DEFAULT 0 COMMENT '状态：0-待处理，1-已同意，2-已拒绝，3-已撤回',
    apply_message VARCHAR(200) DEFAULT '' COMMENT '申请备注（如：我是xxx）',
    reject_reason VARCHAR(200) DEFAULT NULL COMMENT '拒绝原因（可选）',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '处理时间',

    -- 索引：查询"我收到的待处理好友申请"
    INDEX idx_target_status_time (target_id, status, created_at DESC),

    -- 索引：查询"我发出的好友申请记录"
    INDEX idx_requester_status_time (requester_id, status, created_at DESC),

    -- 复合索引：防止重复申请（配合业务逻辑检查）
    INDEX idx_requester_target (requester_id, target_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='好友申请表';
