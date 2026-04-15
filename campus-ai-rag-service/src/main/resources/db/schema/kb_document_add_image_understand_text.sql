-- MySQL：为 kb_document 增加图片理解结果字段
-- 用于图片理解预览回显与后续一键向量化增强

ALTER TABLE kb_document
    ADD COLUMN image_understand_text LONGTEXT NULL COMMENT '图片理解结果文本' AFTER parsed_content;

