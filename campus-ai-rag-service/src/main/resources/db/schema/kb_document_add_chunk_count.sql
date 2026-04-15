-- MySQL：为 kb_document 补充切片数量字段（与 KbDocumentEntity.chunkCount 对齐）
-- 若已存在该列会报错，可忽略或先执行：SHOW COLUMNS FROM kb_document LIKE 'chunk_count';

ALTER TABLE kb_document
    ADD COLUMN chunk_count INT NULL DEFAULT NULL COMMENT '切片数量' AFTER embedding_model;
