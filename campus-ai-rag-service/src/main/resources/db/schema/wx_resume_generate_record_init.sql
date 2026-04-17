-- 微信端：简历生成结果落库
CREATE TABLE IF NOT EXISTS wx_resume_generate_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '微信用户ID',
    sid VARCHAR(128) NOT NULL DEFAULT '' COMMENT '星火返回sid',
    template_type VARCHAR(32) NOT NULL DEFAULT 'CAMPUS' COMMENT '模板类型：CAMPUS/EXPERIENCED/INTERN',
    job_direction VARCHAR(128) NULL COMMENT '岗位方向',
    resume_text TEXT NOT NULL COMMENT '提交给生成接口的简历文本',
    links_json TEXT NULL COMMENT '生成链接JSON（原样存储）',
    link_count INT NOT NULL DEFAULT 0 COMMENT '链接数量',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_user_created (user_id, created_at),
    KEY idx_sid (sid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='微信端简历生成记录';
