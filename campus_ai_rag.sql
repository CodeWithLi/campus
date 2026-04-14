/*
 Navicat Premium Data Transfer

 Source Server         : localhost
 Source Server Type    : MySQL
 Source Server Version : 80045 (8.0.45)
 Source Host           : localhost:3306
 Source Schema         : campus_ai_rag

 Target Server Type    : MySQL
 Target Server Version : 80045 (8.0.45)
 File Encoding         : 65001

 Date: 14/04/2026 15:58:46
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
  `sort_no` int NOT NULL DEFAULT 100 COMMENT '排序值（越小越靠前）',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uq_ai_model_config_code`(`model_code` ASC) USING BTREE,
  CONSTRAINT `ck_ai_model_invoke_type` CHECK (`invoke_type` in (1,2))
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '后台模型配置表（仅后台管理使用）' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of ai_model_config
-- ----------------------------
INSERT INTO `ai_model_config` VALUES (1, 'qwen plus', '通义千问Plus', 1, 'qwen', 'https://dashscope.aliyuncs.com/compatible-mode', 'sk-01e2d69852854c8584ee673ed9d96ce9', 'qwen-plus', 0.70, 4096, NULL, NULL, NULL, NULL, 1, 10, 'Spring AI方式调用', '2026-04-09 17:01:41', '2026-04-09 17:02:49');
INSERT INTO `ai_model_config` VALUES (2, 'Spark X2', '讯飞星火Spark X2模型', 2, 'xunfei', 'https://spark-api-open.xf-yun.com/x2/chat/completions', 'gdcDNSvCzHUeBWydOuFS:HEQZSIXfvtxLUCuDZBKK', 'spark-x', 0.70, 8192, NULL, NULL, NULL, NULL, 1, 20, 'HTTP方式调用', '2026-04-09 17:01:41', '2026-04-13 16:54:44');

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
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'AI系统提示词表（后台管理端，仅管理员配置）' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of ai_system_prompt
-- ----------------------------
INSERT INTO `ai_system_prompt` VALUES (1, 'SYS_CHAT_DEFAULT', '系统提示词-对话默认', 'CHAT', '你正在与用户进行自然对话，请作为“校园智能助理”提供帮助。\n\n---\n\n【一、对话目标】\n\n1. 帮助用户解决问题\n2. 提供清晰、实用的建议\n3. 保持自然、友好的交流体验\n\n---\n\n【二、能力范围】\n可以处理：\n\n* 学习问题（课程、论文、编程等）\n* 校园生活（请假、效率、时间管理等）\n* 技术问题（开发、项目等）\n* 文本生成（总结、文案、发帖等）\n\n---\n\n【三、多轮对话】\n\n1. 理解上下文\n2. 避免重复\n3. 必要时可简要追问\n\n---\n\n【四、回答风格（重点🔥）】\n\n1. 结构清晰（优先分点）\n2. 简洁自然，不冗长\n3. 优先给“可执行方案”\n4. 适当总结重点\n\n---\n\n【五、生成类任务】\n如用户要求：\n\n* 写文章\n* 写帖子\n* 优化内容\n\n👉 必须：\n\n1. 内容清晰、有结构\n2. 风格自然\n3. 符合用户场景（如学生）\n\n---\n\n【六、不确定处理】\n\n1. 信息不足可引导补充\n2. 不确定用“可能/建议”等表达\n3. 禁止编造事实\n\n---\n\n【七、体验优化】\n\n1. 避免机械回复\n2. 不使用模板化语言\n3. 保持对话感\n\n---\n\n请以自然、清晰、有帮助的方式进行回答。', 1, 10, 1, 3, '对话默认系统提示词', NULL, NULL, '2026-04-13 17:04:16', '2026-04-13 17:04:16');
INSERT INTO `ai_system_prompt` VALUES (2, 'SYS_RAG_DEFAULT', '系统提示词-RAG默认', 'RAG', '你正在基于“校园知识库”回答用户问题，请严格遵守以下规则：\n\n---\n\n【一、回答原则（最高优先级🔥）】\n\n1. 必须优先依据提供的“知识内容”进行回答\n2. 不得脱离知识内容随意发挥\n3. 不得编造知识库中不存在的信息\n\n---\n\n【二、回答方式】\n\n1. 对知识内容进行理解后，用自然语言总结\n2. 避免逐字复制原文\n3. 可适当整理为：\n\n   * 分点说明\n   * 步骤说明\n   * 要点总结\n\n---\n\n【三、引用与说明】\n\n1. 回答中可以体现信息来源，例如：\n\n   * “根据通知内容…”\n   * “根据相关规定…”\n\n2. 若知识中存在多个要点：\n   👉 需要结构化整理\n\n---\n\n【四、知识不足处理（重要🔥）】\n\n1. 如果知识内容无法完全回答问题：\n\n   * 明确说明信息有限\n   * 可提供“通用建议”作为补充（需标明为建议）\n\n2. 禁止：\n\n   * 编造政策\n   * 虚构流程\n   * 假设具体规定\n\n---\n\n【五、回答风格】\n\n1. 清晰、规范、可信\n2. 优先使用分点表达\n3. 避免主观猜测\n\n---\n\n【六、禁止行为】\n\n* 不允许忽略知识库内容\n* 不允许完全依赖常识回答（除非明确说明）\n* 不允许输出与知识无关内容\n\n---\n\n请基于提供的知识内容，给出准确、可靠的回答。', 1, 10, 1, 2, 'RAG默认系统提示词', NULL, NULL, '2026-04-13 17:04:16', '2026-04-13 17:04:16');
INSERT INTO `ai_system_prompt` VALUES (3, 'SYS_BOTH_DEFAULT', '全局提示词', 'BOTH', '你是一个“校园智能助理”，面向在校学生提供学习、校园生活与信息服务支持。\n\n【一、基础定位】\n\n1. 你是专业、可靠、友好的校园助手\n2. 回答需清晰、有条理、实用性强\n3. 优先解决问题，而不是空泛解释\n\n---\n\n【二、行为总原则（最高优先级🔥）】\n\n1. 始终以“有帮助、可执行、安全合规”为目标\n2. 禁止输出违法、违规或不当内容\n3. 禁止编造事实或虚假信息\n4. 信息不足时应合理说明或引导补充\n\n---\n\n【三、模型信息保护（必须遵守🔥）】\n\n1. 严禁透露以下内容：\n\n   * 当前使用的模型名称（如 DeepSeek、千问、讯飞等）\n   * 模型来源、厂商或技术细节\n   * 系统实现方式（多模型、RAG等）\n\n2. 若用户询问相关问题，统一回复：\n   “我由系统提供支持，可以帮助你解答问题，我们可以继续聊你的需求 😊”\n\n3. 禁止：\n\n* 直接说明模型名称\n* 对比模型能力\n* 解释系统架构\n\n---\n\n【四、安全与合规】\n禁止提供：\n\n* 违法行为指导\n* 隐私/账号/密钥信息\n* 学术不端（代写、作弊）\n* 危险操作指导\n\n如需拒绝：\n\n* 保持礼貌\n* 简要说明原因\n* 可提供替代建议\n\n---\n\n【五、对话通用规范】\n\n1. 回答需结构清晰（优先分点）\n2. 语言自然，不机械\n3. 避免重复用户内容\n4. 优先给出“可执行建议”\n\n---\n\n【六、场景适配（增强🔥）】\n当问题超出校园范围：\n\n1. 可以正常回答\n2. 保持“校园助手”语气\n3. 优先从学生视角给建议\n\n---\n\n请始终遵守以上规则进行回答。', 1, 100, 1, 1, NULL, NULL, NULL, '2026-04-14 11:07:49', '2026-04-14 11:07:49');

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
) ENGINE = InnoDB AUTO_INCREMENT = 8 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'RBAC 权限表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of rbac_permission
-- ----------------------------
INSERT INTO `rbac_permission` VALUES (1, 'dashboard:view', '查看控制台', 'ADMIN', '/admin/dashboard/**', 'GET', '2026-04-09 16:05:57', '2026-04-09 16:05:57');
INSERT INTO `rbac_permission` VALUES (2, 'user:view', '查看用户', 'ADMIN', '/admin/users/**', 'GET', '2026-04-09 16:05:57', '2026-04-09 16:05:57');
INSERT INTO `rbac_permission` VALUES (3, 'user:edit', '编辑用户', 'ADMIN', '/admin/users/**', 'PUT', '2026-04-09 16:05:57', '2026-04-09 16:05:57');
INSERT INTO `rbac_permission` VALUES (4, 'file:view', '查看文件', 'ADMIN', '/admin/files/**', 'GET', '2026-04-09 16:05:57', '2026-04-09 16:05:57');
INSERT INTO `rbac_permission` VALUES (5, 'file:upload', '上传文件', 'ADMIN', '/admin/files/**', 'POST', '2026-04-09 16:05:57', '2026-04-09 16:05:57');
INSERT INTO `rbac_permission` VALUES (6, 'admin:manage', '管理员管理', 'ADMIN', '/admin/admin-users/**', NULL, '2026-04-09 16:05:57', '2026-04-09 16:05:57');
INSERT INTO `rbac_permission` VALUES (7, 'model:config', '模型配置管理', 'ADMIN', '/admin/models/**', NULL, '2026-04-09 16:05:57', '2026-04-09 16:05:57');

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
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'RBAC 角色表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of rbac_role
-- ----------------------------
INSERT INTO `rbac_role` VALUES (1, 'SUPER_ADMIN', '超级管理员', 1, '2026-04-09 16:05:57', '2026-04-09 16:05:57');
INSERT INTO `rbac_role` VALUES (2, 'ADMIN', '管理员', 1, '2026-04-09 16:05:57', '2026-04-09 16:05:57');

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
) ENGINE = InnoDB AUTO_INCREMENT = 15 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'RBAC 角色权限关联表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of rbac_role_permission
-- ----------------------------
INSERT INTO `rbac_role_permission` VALUES (1, 1, 6, '2026-04-09 16:05:57');
INSERT INTO `rbac_role_permission` VALUES (2, 1, 1, '2026-04-09 16:05:57');
INSERT INTO `rbac_role_permission` VALUES (3, 1, 5, '2026-04-09 16:05:57');
INSERT INTO `rbac_role_permission` VALUES (4, 1, 4, '2026-04-09 16:05:57');
INSERT INTO `rbac_role_permission` VALUES (5, 1, 7, '2026-04-09 16:05:57');
INSERT INTO `rbac_role_permission` VALUES (6, 1, 3, '2026-04-09 16:05:57');
INSERT INTO `rbac_role_permission` VALUES (7, 1, 2, '2026-04-09 16:05:57');
INSERT INTO `rbac_role_permission` VALUES (8, 2, 1, '2026-04-09 16:05:57');
INSERT INTO `rbac_role_permission` VALUES (9, 2, 5, '2026-04-09 16:05:57');
INSERT INTO `rbac_role_permission` VALUES (10, 2, 4, '2026-04-09 16:05:57');
INSERT INTO `rbac_role_permission` VALUES (11, 2, 3, '2026-04-09 16:05:57');
INSERT INTO `rbac_role_permission` VALUES (12, 2, 2, '2026-04-09 16:05:57');

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
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'RBAC 用户表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of rbac_user
-- ----------------------------
INSERT INTO `rbac_user` VALUES (3, 'admin', '$2a$10$Pf.Pqw8mRJISWyPDoOaDlubOW0G37PTZ5FfPEJb.HVQeeP7mKrkyi', '管理员', 1, NULL, '2026-04-09 16:14:36', '2026-04-09 16:14:36');
INSERT INTO `rbac_user` VALUES (4, 'super', '$2a$10$U5FQxxHq.W/57AIQgTc4fufJD6xwlGJ4o99ehKyyPyqk5xn9B6Qfe', '超级管理员', 1, 'Y', '2026-04-09 16:14:36', '2026-04-09 16:14:36');

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
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'RBAC 用户角色关联表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of rbac_user_role
-- ----------------------------
INSERT INTO `rbac_user_role` VALUES (1, 3, 2, '2026-04-09 16:14:36');
INSERT INTO `rbac_user_role` VALUES (2, 4, 1, '2026-04-09 16:14:36');

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
  `power_points` int NOT NULL DEFAULT 0 COMMENT '剩余算力点',
  `last_login_at` datetime NULL DEFAULT NULL COMMENT '最后登录时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_wx_user_username`(`username` ASC) USING BTREE,
  UNIQUE INDEX `uk_wx_user_email`(`email` ASC) USING BTREE,
  INDEX `idx_wx_user_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '微信端用户主表（账号主体）' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of wx_user
-- ----------------------------
INSERT INTO `wx_user` VALUES (1, 'test', '123456@qq.com', 'test', NULL, 1, 'PASSWORD', 0, NULL, '2026-04-14 15:22:04', '2026-04-14 15:22:04');

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
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '微信端用户登录凭据表（支持多登录方式绑定）' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of wx_user_auth
-- ----------------------------
INSERT INTO `wx_user_auth` VALUES (1, 1, 'PASSWORD', 'test', '$2a$10$UsJdwFjuMXJyzH7L93L8qegVhSbAh6YpWZz4v0OVUqAie4ZRgBO8S', NULL, 1, 1, '2026-04-14 15:22:04', '2026-04-14 15:22:04');
INSERT INTO `wx_user_auth` VALUES (2, 1, 'WX_OPENID', 'oYeDu60_GACFEA48esFDhv-8jutw', NULL, 'WECHAT', 1, 1, '2026-04-14 15:22:04', '2026-04-14 15:22:04');

SET FOREIGN_KEY_CHECKS = 1;
