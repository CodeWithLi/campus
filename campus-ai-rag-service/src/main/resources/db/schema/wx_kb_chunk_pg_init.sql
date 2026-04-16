-- PostgreSQL + pgvector：用户个人知识库切片表（与系统 kb_chunk 分表存储）
-- 依赖：CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE IF NOT EXISTS wx_kb_chunk (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    document_id BIGINT NOT NULL,
    chunk_index INT NOT NULL,
    content TEXT NOT NULL,
    embedding vector(1024) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_wx_kb_chunk_user_doc ON wx_kb_chunk(user_id, document_id);
CREATE INDEX IF NOT EXISTS idx_wx_kb_chunk_user ON wx_kb_chunk(user_id);

-- 可选：ivfflat 索引（需要数据量足够大且已 ANALYZE）
-- CREATE INDEX IF NOT EXISTS idx_wx_kb_chunk_embedding_ivfflat
--   ON wx_kb_chunk USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);

