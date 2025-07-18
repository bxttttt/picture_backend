CREATE TABLE user (
                      id                  BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',

                      userAccount         VARCHAR(256) NOT NULL UNIQUE COMMENT '账号',
                      password            VARCHAR(512) NOT NULL COMMENT '密码',
                      userName            VARCHAR(256) NOT NULL DEFAULT 'momo' COMMENT '用户名',
                      userAvatar          VARCHAR(1024) COMMENT '用户头像',
                      userProfile         VARCHAR(512) COMMENT '用户简介',

                      userRole            VARCHAR(64) DEFAULT 'user' COMMENT '角色（user/admin）',

                      userStatus          TINYINT DEFAULT 0 COMMENT '状态（0-正常，1-被限制）',
                      restrictedExpireTime DATETIME COMMENT '限制过期时间（仅当userStatus=1时有效）',

                      isVip               TINYINT DEFAULT 0 COMMENT '是否会员（0-否，1-是）',
                      vipExpireTime       DATETIME COMMENT '会员过期时间',
                      vipCode             VARCHAR(128) COMMENT '会员开通兑换码（记录来源）',

                      editTime            DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '编辑时间（用户主动操作）',
                      createTime          DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                      updateTime          DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                      isDeleted           TINYINT DEFAULT 0 NOT NULL COMMENT '逻辑删除（0-未删除，1-已删除）'
);

CREATE TABLE userBlacklist (
                               id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',

                               userId          BIGINT NOT NULL COMMENT '发起拉黑的用户ID',
                               blackUserId     BIGINT NOT NULL COMMENT '被拉黑的用户ID',
                               reason          VARCHAR(512) COMMENT '拉黑理由（可选）',

                               createTime      DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '拉黑时间',
                               isDeleted       TINYINT DEFAULT 0 NOT NULL COMMENT '逻辑删除（0-正常，1-已删除）',

                               UNIQUE KEY uq_user_black (userId, blackUserId)
);

CREATE TABLE userFollow (
                             id            BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',

                             userId        BIGINT NOT NULL COMMENT '发起关注的用户ID',
                             followUserId  BIGINT NOT NULL COMMENT '被关注的用户ID',

                             createTime    DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '关注时间',
                             isDeleted     TINYINT DEFAULT 0 COMMENT '逻辑删除（0-未删除，1-取消关注）',

                             UNIQUE KEY uq_user_follow (userId, followUserId)
);