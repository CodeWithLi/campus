-- MySQL 8.0
-- 功能：初始化微信端用户表（1 张）+ 后台 RBAC（5 张）
-- 要求：
-- 1) 超级管理员账号固定为 superamdin / superadmin（初始化示例数据）
-- 2) 系统中只能存在一个超级管理员账号（通过唯一标记约束）
-- 3) “管理员管理”“模型配置”权限仅超级管理员可用
-- 4) 密码统一使用 BCrypt 密文存储，禁止明文存储
-- 5) RBAC 仅用于后台管理端（ADMIN），微信小程序端（WX）不做同级严格 RBAC

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- =========================
-- 微信用户端（1 张表）
-- =========================

DROP TABLE IF EXISTS wx_user;

CREATE TABLE wx_user (
    id           BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    open_id      VARCHAR(64) NOT NULL COMMENT '微信 OpenID（唯一）',
    union_id     VARCHAR(64) DEFAULT NULL COMMENT '微信 UnionID（可空）',
    username     VARCHAR(64) DEFAULT NULL COMMENT '系统用户名（可选）',
    password     VARCHAR(255) DEFAULT NULL COMMENT '密码（可选，BCrypt 密文）',
    nickname     VARCHAR(64) DEFAULT NULL COMMENT '用户昵称',
    avatar_url   VARCHAR(255) DEFAULT NULL COMMENT '头像地址',
    phone        VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    status       TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1=正常，0=禁用',
    created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    CONSTRAINT uq_wx_user_open_id UNIQUE (open_id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='微信小程序用户表（轻量认证，不参与后台 RBAC）';

-- 初始化微信用户（示例数据）
INSERT INTO wx_user (open_id, username, nickname, status) VALUES
    ('wx_openid_demo_001', 'wx_user_001', '微信用户001', 1);

-- =========================
-- 后台模型配置（1 张表）
-- =========================

DROP TABLE IF EXISTS ai_model_config;

CREATE TABLE ai_model_config (
    id                BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    model_code        VARCHAR(64) NOT NULL COMMENT '模型编码（系统内唯一）',
    model_name        VARCHAR(128) NOT NULL COMMENT '模型展示名称',
    invoke_type       TINYINT NOT NULL COMMENT '调用方式：1=springai，2=http',
    provider          VARCHAR(32) DEFAULT NULL COMMENT '模型提供商（如 qwen/xunfei/deepseek）',
    api_base_url      VARCHAR(255) DEFAULT NULL COMMENT '调用地址或baseUrl',
    api_key           VARCHAR(255) DEFAULT NULL COMMENT '调用密钥（建议后续加密存储）',
    model_name_remote VARCHAR(128) DEFAULT NULL COMMENT '远程模型名（如 qwen-plus/spark-x）',
    temperature       DECIMAL(4,2) DEFAULT NULL COMMENT '温度参数',
    max_tokens        INT DEFAULT NULL COMMENT '最大输出token数',
    enabled           TINYINT NOT NULL DEFAULT 1 COMMENT '启用状态：1=启用，0=禁用',
    sort_no           INT NOT NULL DEFAULT 100 COMMENT '排序值（越小越靠前）',
    remark            VARCHAR(500) DEFAULT NULL COMMENT '备注',
    created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    CONSTRAINT uq_ai_model_config_code UNIQUE (model_code),
    CONSTRAINT ck_ai_model_invoke_type CHECK (invoke_type IN (1, 2))
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='后台模型配置表（仅后台管理使用）';

INSERT INTO ai_model_config (model_code, model_name, invoke_type, provider, api_base_url, api_key, model_name_remote,
                             temperature, max_tokens, enabled, sort_no, remark)
VALUES ('qwen_plus', '通义千问Plus', 1, 'qwen', 'https://dashscope.aliyuncs.com/compatible-mode', '', 'qwen-plus',
        0.70, 4096, 1, 10, 'Spring AI方式调用'),
       ('Spark X2', '讯飞星火Spark X2模型', 2, 'xunfei', 'https://spark-api-open.xf-yun.com/x2/chat/completions', 'gdcDNSvCzHUeBWydOuFS:HEQZSIXfvtxLUCuDZBKK', 'Spark X2',
        0.70, 8192, 1, 20, 'HTTP方式调用');

-- =========================
-- RBAC（5 张表）
-- =========================

DROP TABLE IF EXISTS rbac_role_permission;
DROP TABLE IF EXISTS rbac_user_role;
DROP TABLE IF EXISTS rbac_permission;
DROP TABLE IF EXISTS rbac_role;
DROP TABLE IF EXISTS rbac_user;

CREATE TABLE rbac_user (
    id                 BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    username           VARCHAR(64) NOT NULL COMMENT '登录用户名',
    password           VARCHAR(255) NOT NULL COMMENT '登录密码（BCrypt 密文）',
    nickname           VARCHAR(64) DEFAULT NULL COMMENT '用户昵称',
    status             TINYINT NOT NULL DEFAULT 1 COMMENT '账号状态：1=正常，0=禁用',
    super_admin_marker CHAR(1) DEFAULT NULL COMMENT '超级管理员唯一标记：仅允许一个 Y',
    created_at         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    CONSTRAINT uq_rbac_user_username UNIQUE (username),
    CONSTRAINT uq_rbac_super_admin_marker UNIQUE (super_admin_marker)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='RBAC 用户表';

CREATE TABLE rbac_role (
    id         BIGINT NOT NULL AUTO_INCREMENT COMMENT '角色ID',
    role_code  VARCHAR(64) NOT NULL COMMENT '角色编码（如 SUPER_ADMIN）',
    role_name  VARCHAR(64) NOT NULL COMMENT '角色名称',
    built_in   TINYINT NOT NULL DEFAULT 1 COMMENT '是否内置角色：1=是，0=否',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    CONSTRAINT uq_rbac_role_code UNIQUE (role_code)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='RBAC 角色表';

CREATE TABLE rbac_permission (
    id          BIGINT NOT NULL AUTO_INCREMENT COMMENT '权限ID',
    perm_code   VARCHAR(128) NOT NULL COMMENT '权限编码（如 model:config）',
    perm_name   VARCHAR(128) NOT NULL COMMENT '权限名称',
    client_scope VARCHAR(16) NOT NULL DEFAULT 'ADMIN' COMMENT '权限作用端：ADMIN=后台管理端，WX=微信小程序端',
    api_pattern VARCHAR(255) DEFAULT NULL COMMENT '接口匹配路径（如 /admin/models/**）',
    http_method VARCHAR(16) DEFAULT NULL COMMENT 'HTTP方法（GET/POST/PUT/DELETE），NULL 表示不限',
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    CONSTRAINT uq_rbac_perm_code UNIQUE (perm_code)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='RBAC 权限表';

CREATE TABLE rbac_user_role (
    id         BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_id    BIGINT NOT NULL COMMENT '用户ID，关联 rbac_user.id',
    role_id    BIGINT NOT NULL COMMENT '角色ID，关联 rbac_role.id',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    CONSTRAINT fk_rbac_user_role_user FOREIGN KEY (user_id) REFERENCES rbac_user (id) ON DELETE CASCADE,
    CONSTRAINT fk_rbac_user_role_role FOREIGN KEY (role_id) REFERENCES rbac_role (id) ON DELETE CASCADE,
    CONSTRAINT uq_rbac_user_role UNIQUE (user_id, role_id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='RBAC 用户角色关联表';

CREATE TABLE rbac_role_permission (
    id            BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    role_id       BIGINT NOT NULL COMMENT '角色ID，关联 rbac_role.id',
    permission_id BIGINT NOT NULL COMMENT '权限ID，关联 rbac_permission.id',
    created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    CONSTRAINT fk_rbac_role_perm_role FOREIGN KEY (role_id) REFERENCES rbac_role (id) ON DELETE CASCADE,
    CONSTRAINT fk_rbac_role_perm_perm FOREIGN KEY (permission_id) REFERENCES rbac_permission (id) ON DELETE CASCADE,
    CONSTRAINT uq_rbac_role_permission UNIQUE (role_id, permission_id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='RBAC 角色权限关联表';

-- 初始化角色
INSERT INTO rbac_role (role_code, role_name, built_in) VALUES
    ('SUPER_ADMIN', '超级管理员', 1),
    ('ADMIN', '管理员', 1);

-- 绑定用户角色
INSERT INTO rbac_user_role (user_id, role_id)
SELECT u.id, r.id
FROM rbac_user u
JOIN rbac_role r ON r.role_code = 'SUPER_ADMIN'
WHERE u.username = 'superamdin';

INSERT INTO rbac_user_role (user_id, role_id)
SELECT u.id, r.id
FROM rbac_user u
JOIN rbac_role r ON r.role_code = 'ADMIN'
WHERE u.username = 'admin';

-- 初始化权限（重点：admin:manage、model:config 仅超级管理员）
-- 约束：当前初始化数据全部为后台管理端权限（client_scope='ADMIN'）
INSERT INTO rbac_permission (perm_code, perm_name, client_scope, api_pattern, http_method) VALUES
    ('dashboard:view', '查看控制台', 'ADMIN', '/admin/dashboard/**', 'GET'),
    ('user:view', '查看用户', 'ADMIN', '/admin/users/**', 'GET'),
    ('user:edit', '编辑用户', 'ADMIN', '/admin/users/**', 'PUT'),
    ('admin:manage', '管理员管理', 'ADMIN', '/admin/admin-users/**', NULL),
    ('model:config', '模型配置管理', 'ADMIN', '/admin/models/**', NULL);

-- 超级管理员：拥有全部权限
INSERT INTO rbac_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM rbac_role r
JOIN rbac_permission p ON 1 = 1
WHERE r.role_code = 'SUPER_ADMIN';

-- 普通管理员：拥有除“管理员管理、模型配置管理”之外的权限
INSERT INTO rbac_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM rbac_role r
JOIN rbac_permission p ON p.perm_code NOT IN ('admin:manage', 'model:config')
WHERE r.role_code = 'ADMIN';

SET FOREIGN_KEY_CHECKS = 1;

