-- MySQL：用户个人知识库文档（仅本人可用）
CREATE TABLE IF NOT EXISTS wx_kb_document (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '归属用户ID',
    title VARCHAR(255) NOT NULL COMMENT '文档标题',
    original_file_name VARCHAR(255) NOT NULL COMMENT '原始文件名',
    file_ext VARCHAR(32) NOT NULL COMMENT '文件扩展名',
    content_type VARCHAR(128) DEFAULT NULL COMMENT '内容类型',
    file_size BIGINT DEFAULT NULL COMMENT '文件大小字节',
    storage_path VARCHAR(500) NOT NULL COMMENT '文件存储路径',
    process_status VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT '处理状态：PENDING/PROCESSING/SUCCESS/FAILED',
    fail_reason VARCHAR(1000) DEFAULT NULL COMMENT '失败原因',
    retry_count INT NOT NULL DEFAULT 0 COMMENT '重试次数',
    embedding_model VARCHAR(64) NOT NULL DEFAULT 'text-embedding-v4' COMMENT '向量模型',
    chunk_count INT NOT NULL DEFAULT 0 COMMENT '切片数量',
    parsed_content LONGTEXT DEFAULT NULL COMMENT '解析后的纯文本',
    processed_at DATETIME DEFAULT NULL COMMENT '处理完成时间',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_wx_kb_doc_user_created (user_id, created_at),
    KEY idx_wx_kb_doc_user_status (user_id, process_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户个人知识库文档';

