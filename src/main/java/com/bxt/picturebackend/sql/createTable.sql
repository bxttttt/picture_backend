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
-- 图片表
create table if not exists picture
(
    id           bigint auto_increment comment 'id' primary key,
    url          varchar(512)                       not null comment '图片 url',
    name         varchar(128)                       not null comment '图片名称',
    introduction varchar(512)                       null comment '简介',
    category     varchar(64)                        null comment '分类',
    tags         varchar(512)                      null comment '标签（JSON 数组）',
    picSize      bigint                             null comment '图片体积',
    picWidth     int                                null comment '图片宽度',
    picHeight    int                                null comment '图片高度',
    picScale     double                             null comment '图片宽高比例',
    picFormat    varchar(32)                        null comment '图片格式',
    userId       bigint                             not null comment '创建用户 id',
    createTime   datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    editTime     datetime default CURRENT_TIMESTAMP not null comment '编辑时间',
    updateTime   datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDeleted     tinyint  default 0                 not null comment '是否删除',
    INDEX idx_name (name),                 -- 提升基于图片名称的查询性能
    INDEX idx_introduction (introduction), -- 用于模糊搜索图片简介
    INDEX idx_category (category),         -- 提升基于分类的查询性能
    INDEX idx_tags (tags),                 -- 提升基于标签的查询性能
    INDEX idx_userId (userId)              -- 提升基于用户 ID 的查询性能
) comment '图片' collate = utf8mb4_unicode_ci;
create table if not exists pictureLike
(
    id         bigint auto_increment comment 'id' primary key,
    userId     bigint not null comment '用户 id',
    pictureId  bigint not null comment '图片 id',
    createTime datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    isDeleted   tinyint  default 0                 not null comment '是否删除',
    UNIQUE KEY uq_user_picture (userId, pictureId), -- 确保每个用户对每张图片只能点赞一次
    INDEX idx_userId (userId),             -- 提升基于用户 ID 的查询性能
    INDEX idx_pictureId (pictureId)        -- 提升基于图片 ID 的查询性能
) comment '图片点赞' collate = utf8mb4_unicode_ci;
create table if not exists pictureComment
(
    id         bigint auto_increment comment 'id' primary key,
    userId     bigint not null comment '用户 id',
    pictureId  bigint not null comment '图片 id',
    content    text not null comment '评论内容',
    parentId   bigint null comment '父评论 id（用于回复）',
    createTime datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    isDeleted   tinyint  default 0                 not null comment '是否删除',
    INDEX idx_userId (userId),             -- 提升基于用户 ID 的查询性能
    INDEX idx_pictureId (pictureId),       -- 提升基于图片 ID 的查询性能
    INDEX idx_parentId (parentId)          -- 提升基于父评论 ID 的查询性能
) comment '图片评论' collate = utf8mb4_unicode_ci;
create table if not exists pictureCollection
(
    id         bigint auto_increment comment 'id' primary key,
    userId     bigint not null comment '用户 id',
    pictureId  bigint not null comment '图片 id',
    createTime datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    isDeleted   tinyint  default 0                 not null comment '是否删除',
    UNIQUE KEY uq_user_picture (userId, pictureId), -- 确保每个用户对每张图片只能收藏一次
    INDEX idx_userId (userId),             -- 提升基于用户 ID 的查询性能
    INDEX idx_pictureId (pictureId)        -- 提升基于图片 ID 的查询性能
) comment '图片收藏' collate = utf8mb4_unicode_ci;
create table if not exists pictureReport
(
    id         bigint auto_increment comment 'id' primary key,
    userId     bigint not null comment '用户 id',
    pictureId  bigint not null comment '图片 id',
    reason     varchar(512) not null comment '举报理由',
    createTime datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    isDeleted   tinyint  default 0                 not null comment '是否删除',
    INDEX idx_userId (userId),             -- 提升基于用户 ID 的查询性能
    INDEX idx_pictureId (pictureId)        -- 提升基于图片 ID 的查询性能
) comment '图片举报' collate = utf8mb4_unicode_ci;
create table if not exists pictureNotInterest
(
    id         bigint auto_increment comment 'id' primary key,
    userId     bigint not null comment '用户 id',
    pictureId  bigint not null comment '图片 id',
    createTime datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    isDeleted   tinyint  default 0                 not null comment '是否删除',
    UNIQUE KEY uq_user_picture (userId, pictureId), -- 确保每个用户对每张图片只能标记一次不感兴趣
    INDEX idx_userId (userId),             -- 提升基于用户 ID 的查询性能
    INDEX idx_pictureId (pictureId)        -- 提升基于图片 ID 的查询性能
) comment '图片不感兴趣' collate = utf8mb4_unicode_ci;

alter table picture
    add column reviewStatus tinyint default 0 not null comment '审核状态（0-待审核，1-审核通过，2-审核不通过）',
    add column reviewReason varchar(512) null comment '审核通过/不通过原因',
    add column reviewTime datetime null comment '审核时间',
    add column reviewUserId bigint null comment '审核用户 ID（审核通过/不通过的管理员）';
create index idx_reviewStatue on picture (reviewStatus);

alter table picture
    add column thumbnailUrl varchar(512) null comment '缩略图 URL';

create table if not exists pictureDownload
(
    id         bigint auto_increment comment 'id' primary key,
    userId     bigint not null comment '用户 id',
    pictureId  bigint not null comment '图片 id',
    createTime datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    isDeleted   tinyint  default 0                 not null comment '是否删除'

)comment "下载图片";

alter table picture add column picColor varchar(512) null comment '图片主颜色';