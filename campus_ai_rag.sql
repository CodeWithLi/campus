/*
 Navicat Premium Data Transfer

 Source Server         : 118.145.69.241
 Source Server Type    : MySQL
 Source Server Version : 80031 (8.0.31)
 Source Host           : localhost:3062
 Source Schema         : campus_ai_rag

 Target Server Type    : MySQL
 Target Server Version : 80031 (8.0.31)
 File Encoding         : 65001

 Date: 21/04/2026 16:47:47
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for ai_model_config
-- ----------------------------
DROP TABLE IF EXISTS `ai_model_config`;
CREATE TABLE `ai_model_config`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `model_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '模型编码（系统内唯一）',
  `model_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '模型展示名称',
  `invoke_type` int NOT NULL COMMENT '调用方式：1=springai，2=xf_http(讯飞星火)，3=generic_http(通用HTTP)',
  `provider` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '模型提供商（如 qwen/xunfei/deepseek）',
  `api_base_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '调用地址或baseUrl',
  `api_key` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '调用密钥（建议后续加密存储）',
  `model_name_remote` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '远程模型名（如 qwen-plus/spark-x）',
  `temperature` decimal(4, 2) NULL DEFAULT NULL COMMENT '温度参数',
  `max_tokens` int NULL DEFAULT NULL COMMENT '最大输出token数',
  `generic_req_type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '通用HTTP请求体类型：messages/prompt/input',
  `generic_req_field` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '通用HTTP请求体字段名（实际发送字段名）',
  `generic_resp_path` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '通用HTTP响应提取路径，支持 a.b[0].c；为空则返回原始响应文本',
  `generic_headers_json` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '通用HTTP额外请求头JSON对象，例如 {\"Authorization\":\"Bearer xxx\"}',
  `enabled` tinyint NOT NULL DEFAULT 1 COMMENT '启用状态：1=启用，0=禁用',
  `supports_image_chat` tinyint NOT NULL DEFAULT 0 COMMENT '是否支持图片对话：1支持，0不支持',
  `sort_no` int NOT NULL DEFAULT 100 COMMENT '排序值（越小越靠前）',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uq_ai_model_config_code`(`model_code` ASC) USING BTREE,
  CONSTRAINT `ck_ai_model_invoke_type` CHECK (`invoke_type` in (1,2))
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '后台模型配置表（仅后台管理使用）' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for ai_system_prompt
-- ----------------------------
DROP TABLE IF EXISTS `ai_system_prompt`;
CREATE TABLE `ai_system_prompt`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `prompt_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '提示词编码（唯一，用于程序引用）',
  `prompt_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '提示词名称（后台展示）',
  `scene` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '适用场景：CHAT/RAG/BOTH',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '系统提示词正文（system role 内容）',
  `enabled` tinyint NOT NULL DEFAULT 1 COMMENT '启用状态：1=启用，0=禁用',
  `priority` int NOT NULL DEFAULT 100 COMMENT '优先级（越小越优先，便于未来支持叠加多条）',
  `is_default` tinyint NOT NULL DEFAULT 0 COMMENT '是否默认：1=默认，0=否（同scene建议仅一条默认）',
  `version` int NOT NULL DEFAULT 1 COMMENT '版本号（每次修改递增，便于审计/回滚）',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `created_by` bigint NULL DEFAULT NULL COMMENT '创建人ID（rbac_user.id）',
  `updated_by` bigint NULL DEFAULT NULL COMMENT '更新人ID（rbac_user.id）',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_prompt_code`(`prompt_code` ASC) USING BTREE,
  INDEX `idx_scene_enabled`(`scene` ASC, `enabled` ASC) USING BTREE,
  INDEX `idx_scene_default`(`scene` ASC, `is_default` ASC) USING BTREE,
  INDEX `idx_priority`(`priority` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'AI系统提示词表（后台管理端，仅管理员配置）' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for ai_token_billing_log
-- ----------------------------
DROP TABLE IF EXISTS `ai_token_billing_log`;
CREATE TABLE `ai_token_billing_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID，管理员测试固定-1',
  `scene` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '场景：CHAT/RAG/INTERNAL',
  `model_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '模型编码',
  `invoke_type` tinyint NOT NULL COMMENT '调用方式：1=springai，2=xf_http，3=generic_http',
  `billing_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '计费类型：TOKEN_API/ESTIMATE_CHAR',
  `prompt_tokens` int NOT NULL DEFAULT 0 COMMENT '输入token',
  `completion_tokens` int NOT NULL DEFAULT 0 COMMENT '输出token',
  `total_tokens` int NOT NULL DEFAULT 0 COMMENT '总token',
  `request_chars` int NOT NULL DEFAULT 0 COMMENT '输入字符数',
  `response_chars` int NOT NULL DEFAULT 0 COMMENT '输出字符数',
  `consumed_points` decimal(10, 2) NOT NULL DEFAULT 0.00 COMMENT '本次扣减算力点',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：1成功 0失败',
  `error_message` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '失败原因',
  `ext_json` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '扩展字段JSON（原始usage等）',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_ai_token_billing_user_created`(`user_id` ASC, `created_at` ASC) USING BTREE,
  INDEX `idx_ai_token_billing_model_created`(`model_code` ASC, `created_at` ASC) USING BTREE,
  INDEX `idx_ai_token_billing_scene_created`(`scene` ASC, `created_at` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 74 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '模型token计费流水' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for forum_notice_agent_record
-- ----------------------------
DROP TABLE IF EXISTS `forum_notice_agent_record`;
CREATE TABLE `forum_notice_agent_record`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `file_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '上传文件名',
  `file_ext` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '文件扩展名',
  `route` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '解析路由：IMAGE_OCR/PDF_PAGE_OCR/PDF_API_OCR',
  `raw_text` mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT 'OCR原始文本',
  `summary` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '结构化摘要',
  `items_json` mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '提醒候选JSON',
  `item_count` int NOT NULL DEFAULT 0 COMMENT '提醒候选数量',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_created`(`user_id` ASC, `created_at` ASC) USING BTREE,
  INDEX `idx_user_deleted_created`(`user_id` ASC, `deleted` ASC, `created_at` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 13 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '通知智能体解析记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for forum_post
-- ----------------------------
DROP TABLE IF EXISTS `forum_post`;
CREATE TABLE `forum_post`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT 'wx_user.id',
  `title` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `content` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `image_urls` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '图片URL数组(JSON)',
  `is_anonymous` tinyint NOT NULL DEFAULT 0 COMMENT '是否匿名发布：1匿名，0非匿名',
  `text_compliance_skipped` tinyint NOT NULL DEFAULT 0 COMMENT '1=发布时跳过文本合规（与 AI 草稿一致）',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_created`(`user_id` ASC, `created_at` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '论坛帖子' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for forum_post_comment
-- ----------------------------
DROP TABLE IF EXISTS `forum_post_comment`;
CREATE TABLE `forum_post_comment`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `post_id` bigint NOT NULL COMMENT 'forum_post.id',
  `user_id` bigint NOT NULL COMMENT 'wx_user.id',
  `content` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '评论内容',
  `parent_comment_id` bigint NULL DEFAULT NULL COMMENT '父评论ID（多级评论树）',
  `reply_to_comment_id` bigint NULL DEFAULT NULL COMMENT '回复目标评论ID（显示“回复谁”）',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '0=正常 1=删除',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_post_parent_created`(`post_id` ASC, `parent_comment_id` ASC, `created_at` ASC) USING BTREE,
  INDEX `idx_post_created`(`post_id` ASC, `created_at` ASC) USING BTREE,
  INDEX `idx_user_created`(`user_id` ASC, `created_at` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 9 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '帖子评论' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for forum_post_favorite
-- ----------------------------
DROP TABLE IF EXISTS `forum_post_favorite`;
CREATE TABLE `forum_post_favorite`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `post_id` bigint NOT NULL COMMENT '帖子ID',
  `user_id` bigint NOT NULL COMMENT '收藏用户ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_post_user`(`post_id` ASC, `user_id` ASC) USING BTREE,
  INDEX `idx_user_created`(`user_id` ASC, `created_at` ASC) USING BTREE,
  INDEX `idx_post`(`post_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '帖子收藏关系表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for forum_post_interaction_notice
-- ----------------------------
DROP TABLE IF EXISTS `forum_post_interaction_notice`;
CREATE TABLE `forum_post_interaction_notice`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `receiver_user_id` bigint NOT NULL COMMENT '接收互动通知的用户ID',
  `actor_user_id` bigint NOT NULL COMMENT '触发互动的用户ID',
  `post_id` bigint NOT NULL COMMENT '关联帖子ID',
  `comment_id` bigint NULL DEFAULT NULL COMMENT '关联评论ID',
  `interaction_type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'LIKE/COMMENT',
  `content_snapshot` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '互动内容摘要',
  `is_read` tinyint NOT NULL DEFAULT 0 COMMENT '是否已读：0未读，1已读',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_receiver_created`(`receiver_user_id` ASC, `created_at` ASC) USING BTREE,
  INDEX `idx_post`(`post_id` ASC) USING BTREE,
  INDEX `idx_actor`(`actor_user_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '帖子互动通知表（点赞/评论）' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for forum_post_like
-- ----------------------------
DROP TABLE IF EXISTS `forum_post_like`;
CREATE TABLE `forum_post_like`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `post_id` bigint NOT NULL COMMENT 'forum_post.id',
  `user_id` bigint NOT NULL COMMENT 'wx_user.id',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uq_post_user`(`post_id` ASC, `user_id` ASC) USING BTREE,
  INDEX `idx_post_created`(`post_id` ASC, `created_at` ASC) USING BTREE,
  INDEX `idx_user_created`(`user_id` ASC, `created_at` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '帖子点赞' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for forum_schedule_notice
-- ----------------------------
DROP TABLE IF EXISTS `forum_schedule_notice`;
CREATE TABLE `forum_schedule_notice`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT 'wx_user.id',
  `reminder_id` bigint NOT NULL COMMENT 'forum_schedule_reminder.id',
  `title` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '提醒标题',
  `remind_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'exam/ddl/activity',
  `content` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '通知内容',
  `is_read` tinyint NOT NULL DEFAULT 0 COMMENT '0=未读 1=已读',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_read_created`(`user_id` ASC, `is_read` ASC, `created_at` ASC) USING BTREE,
  INDEX `idx_reminder_created`(`reminder_id` ASC, `created_at` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '提醒通知' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for forum_schedule_reminder
-- ----------------------------
DROP TABLE IF EXISTS `forum_schedule_reminder`;
CREATE TABLE `forum_schedule_reminder`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT 'wx_user.id',
  `title` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '提醒标题',
  `remind_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'exam/ddl/activity',
  `remind_at` datetime NOT NULL COMMENT '提醒目标时间',
  `advance_minutes` int NOT NULL DEFAULT 30 COMMENT '提前提醒分钟数',
  `notified` tinyint NOT NULL DEFAULT 0 COMMENT '0=未通知 1=已通知',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '0=正常 1=删除',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_remind_time`(`user_id` ASC, `remind_at` ASC) USING BTREE,
  INDEX `idx_due_scan`(`deleted` ASC, `notified` ASC, `remind_at` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '日程提醒' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for kb_category
-- ----------------------------
DROP TABLE IF EXISTS `kb_category`;
CREATE TABLE `kb_category`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `category_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '分类编码（唯一）',
  `category_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '分类名称',
  `enabled` tinyint NOT NULL DEFAULT 1 COMMENT '是否启用：1启用，0禁用',
  `sort_no` int NOT NULL DEFAULT 100 COMMENT '排序号（越小越靠前）',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_kb_category_code`(`category_code` ASC) USING BTREE,
  INDEX `idx_kb_category_enabled_sort`(`enabled` ASC, `sort_no` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '知识库分类表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for kb_document
-- ----------------------------
DROP TABLE IF EXISTS `kb_document`;
CREATE TABLE `kb_document`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `category_id` bigint NOT NULL COMMENT '分类ID（关联 kb_category.id）',
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '文档标题',
  `original_file_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '原始文件名',
  `file_ext` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '文件后缀',
  `content_type` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '文件MIME类型',
  `file_size` bigint NOT NULL DEFAULT 0 COMMENT '文件大小（字节）',
  `storage_path` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '文件存储路径',
  `process_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'PENDING' COMMENT '处理状态：PENDING/PROCESSING/SUCCESS/FAILED',
  `fail_reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '失败原因',
  `retry_count` int NOT NULL DEFAULT 0 COMMENT '重试次数',
  `embedding_model` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'text-embedding-v4' COMMENT '向量模型（固定千问 text-embedding-v4）',
  `chunk_count` int NULL DEFAULT NULL COMMENT '切片数量',
  `parsed_content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '解析后的纯文本内容（用于后续切片和向量化）',
  `image_understand_text` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '图片理解结果文本',
  `processed_at` datetime NULL DEFAULT NULL COMMENT '处理完成时间',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_kb_document_category`(`category_id` ASC) USING BTREE,
  INDEX `idx_kb_document_status`(`process_status` ASC) USING BTREE,
  INDEX `idx_kb_document_created_at`(`created_at` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 12 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '知识库文档表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for rbac_permission
-- ----------------------------
DROP TABLE IF EXISTS `rbac_permission`;
CREATE TABLE `rbac_permission`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '权限ID',
  `perm_code` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '权限编码（如 model:config）',
  `perm_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '权限名称',
  `client_scope` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'ADMIN' COMMENT '权限作用端：ADMIN=后台管理端，WX=微信小程序端',
  `api_pattern` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '接口匹配路径（如 /admin/models/**）',
  `http_method` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'HTTP方法（GET/POST/PUT/DELETE），NULL 表示不限',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uq_rbac_perm_code`(`perm_code` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 8 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'RBAC 权限表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for rbac_role
-- ----------------------------
DROP TABLE IF EXISTS `rbac_role`;
CREATE TABLE `rbac_role`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '角色ID',
  `role_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '角色编码（如 SUPER_ADMIN）',
  `role_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '角色名称',
  `built_in` tinyint NOT NULL DEFAULT 1 COMMENT '是否内置角色：1=是，0=否',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uq_rbac_role_code`(`role_code` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'RBAC 角色表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for rbac_role_permission
-- ----------------------------
DROP TABLE IF EXISTS `rbac_role_permission`;
CREATE TABLE `rbac_role_permission`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `role_id` bigint NOT NULL COMMENT '角色ID，关联 rbac_role.id',
  `permission_id` bigint NOT NULL COMMENT '权限ID，关联 rbac_permission.id',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uq_rbac_role_permission`(`role_id` ASC, `permission_id` ASC) USING BTREE,
  INDEX `fk_rbac_role_perm_perm`(`permission_id` ASC) USING BTREE,
  CONSTRAINT `fk_rbac_role_perm_perm` FOREIGN KEY (`permission_id`) REFERENCES `rbac_permission` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_rbac_role_perm_role` FOREIGN KEY (`role_id`) REFERENCES `rbac_role` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 13 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'RBAC 角色权限关联表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for rbac_user
-- ----------------------------
DROP TABLE IF EXISTS `rbac_user`;
CREATE TABLE `rbac_user`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '登录用户名',
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '登录密码（BCrypt 密文）',
  `nickname` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '用户昵称',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '账号状态：1=正常，0=禁用',
  `super_admin_marker` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '超级管理员唯一标记：仅允许一个 Y',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uq_rbac_user_username`(`username` ASC) USING BTREE,
  UNIQUE INDEX `uq_rbac_super_admin_marker`(`super_admin_marker` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 17 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'RBAC 用户表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for rbac_user_role
-- ----------------------------
DROP TABLE IF EXISTS `rbac_user_role`;
CREATE TABLE `rbac_user_role`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID，关联 rbac_user.id',
  `role_id` bigint NOT NULL COMMENT '角色ID，关联 rbac_role.id',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uq_rbac_user_role`(`user_id` ASC, `role_id` ASC) USING BTREE,
  INDEX `fk_rbac_user_role_role`(`role_id` ASC) USING BTREE,
  CONSTRAINT `fk_rbac_user_role_role` FOREIGN KEY (`role_id`) REFERENCES `rbac_role` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_rbac_user_role_user` FOREIGN KEY (`user_id`) REFERENCES `rbac_user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 15 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'RBAC 用户角色关联表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for wx_conversation
-- ----------------------------
DROP TABLE IF EXISTS `wx_conversation`;
CREATE TABLE `wx_conversation`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '微信用户ID',
  `conversation_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '会话ID',
  `scene` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '场景：CHAT/RAG',
  `title` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '会话标题',
  `summary` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '会话摘要',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uq_wx_conv_user_scene_conv`(`user_id` ASC, `scene` ASC, `conversation_id` ASC) USING BTREE,
  INDEX `idx_wx_conv_user_scene_updated`(`user_id` ASC, `scene` ASC, `updated_at` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '微信会话元数据' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for wx_kb_document
-- ----------------------------
DROP TABLE IF EXISTS `wx_kb_document`;
CREATE TABLE `wx_kb_document`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '归属用户ID',
  `category_id` bigint NULL DEFAULT NULL COMMENT '知识库分类ID',
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '文档标题',
  `original_file_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '原始文件名',
  `file_ext` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '文件扩展名',
  `content_type` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '内容类型',
  `file_size` bigint NULL DEFAULT NULL COMMENT '文件大小字节',
  `storage_path` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '文件存储路径',
  `process_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'PENDING' COMMENT '处理状态：PENDING/PROCESSING/SUCCESS/FAILED',
  `fail_reason` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '失败原因',
  `retry_count` int NOT NULL DEFAULT 0 COMMENT '重试次数',
  `embedding_model` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'text-embedding-v4' COMMENT '向量模型',
  `chunk_count` int NOT NULL DEFAULT 0 COMMENT '切片数量',
  `parsed_content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '解析后的纯文本',
  `processed_at` datetime NULL DEFAULT NULL COMMENT '处理完成时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_wx_kb_doc_user_created`(`user_id` ASC, `created_at` ASC) USING BTREE,
  INDEX `idx_wx_kb_doc_user_status`(`user_id` ASC, `process_status` ASC) USING BTREE,
  INDEX `idx_wx_kb_document_user_category`(`user_id` ASC, `category_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户个人知识库文档' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for wx_power_point_credit_log
-- ----------------------------
DROP TABLE IF EXISTS `wx_power_point_credit_log`;
CREATE TABLE `wx_power_point_credit_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '微信用户ID',
  `order_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '充值订单号（唯一，用于幂等）',
  `channel` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'ALIPAY_WAP' COMMENT '渠道',
  `points` decimal(10, 2) NOT NULL COMMENT '本次到账算力点',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_order_no`(`order_no` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '算力点到账流水（用于幂等防重复加点）' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for wx_recharge_order
-- ----------------------------
DROP TABLE IF EXISTS `wx_recharge_order`;
CREATE TABLE `wx_recharge_order`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `order_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '商户订单号',
  `user_id` bigint NOT NULL COMMENT '微信用户ID',
  `channel` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'ALIPAY_WAP' COMMENT '支付渠道',
  `amount_yuan` decimal(10, 2) NOT NULL COMMENT '充值金额（元）',
  `points` decimal(10, 2) NOT NULL COMMENT '到账算力点',
  `subject` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '订单标题',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'CREATED' COMMENT 'CREATED/PAID/CLOSED/FAILED',
  `trade_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '支付宝交易号',
  `buyer_logon_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '买家账号',
  `paid_at` datetime NULL DEFAULT NULL COMMENT '支付完成时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_order_no`(`order_no` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 13 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '微信端充值订单' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for wx_resume_generate_record
-- ----------------------------
DROP TABLE IF EXISTS `wx_resume_generate_record`;
CREATE TABLE `wx_resume_generate_record`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '微信用户ID',
  `sid` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '星火返回sid',
  `template_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'CAMPUS' COMMENT '模板类型：CAMPUS/EXPERIENCED/INTERN',
  `job_direction` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '岗位方向',
  `resume_text` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '提交给生成接口的简历文本',
  `links_json` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '生成链接JSON（原样存储）',
  `link_count` int NOT NULL DEFAULT 0 COMMENT '链接数量',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_created`(`user_id` ASC, `created_at` ASC) USING BTREE,
  INDEX `idx_sid`(`sid` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '微信端简历生成记录' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for wx_user
-- ----------------------------
DROP TABLE IF EXISTS `wx_user`;
CREATE TABLE `wx_user`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户名（平台内唯一）',
  `email` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '邮箱（唯一）',
  `nickname` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '昵称',
  `avatar_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '头像URL',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '用户状态：1=正常，0=禁用',
  `register_source` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'PASSWORD' COMMENT '注册来源：PASSWORD/WX',
  `power_points` decimal(10, 2) NOT NULL DEFAULT 0.00 COMMENT '剩余算力点',
  `last_login_at` datetime NULL DEFAULT NULL COMMENT '最后登录时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_wx_user_username`(`username` ASC) USING BTREE,
  UNIQUE INDEX `uk_wx_user_email`(`email` ASC) USING BTREE,
  INDEX `idx_wx_user_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '微信端用户主表（账号主体）' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for wx_user_auth
-- ----------------------------
DROP TABLE IF EXISTS `wx_user_auth`;
CREATE TABLE `wx_user_auth`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID，关联 wx_user.id',
  `auth_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '认证类型：PASSWORD/WX_OPENID',
  `auth_key` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '认证标识：PASSWORD=用户名；WX_OPENID=openid',
  `auth_secret` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '认证密钥：PASSWORD=BCrypt密码；WX_OPENID可为空',
  `provider` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '认证提供方：WECHAT 等',
  `enabled` tinyint NOT NULL DEFAULT 1 COMMENT '凭据状态：1=启用，0=禁用',
  `allow_login` tinyint NOT NULL DEFAULT 1 COMMENT '是否允许该凭据登录：1=允许，0=禁止',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_auth_type_key`(`auth_type` ASC, `auth_key` ASC) USING BTREE,
  INDEX `idx_wx_user_auth_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_wx_user_auth_status`(`enabled` ASC, `allow_login` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '微信端用户登录凭据表（支持多登录方式绑定）' ROW_FORMAT = DYNAMIC;

SET FOREIGN_KEY_CHECKS = 1;
