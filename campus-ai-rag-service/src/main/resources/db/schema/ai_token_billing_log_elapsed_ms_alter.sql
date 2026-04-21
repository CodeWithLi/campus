ALTER TABLE ai_token_billing_log
    ADD COLUMN elapsed_ms BIGINT NULL COMMENT '请求耗时（毫秒）' AFTER consumed_points;
