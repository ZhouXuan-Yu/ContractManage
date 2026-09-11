CREATE TABLE IF NOT EXISTS contract_ai_config (
    id BIGINT PRIMARY KEY,
    config_name VARCHAR(128) NOT NULL,
    provider VARCHAR(64) NOT NULL,
    base_url VARCHAR(512),
    model VARCHAR(128),
    secret TEXT,
    draft_workflow_id VARCHAR(128),
    extract_workflow_id VARCHAR(128),
    review_workflow_id VARCHAR(128),
    fulfillment_workflow_id VARCHAR(128),
    enabled TINYINT NOT NULL DEFAULT 0,
    last_test_at VARCHAR(40),
    last_error VARCHAR(500),
    updated_at VARCHAR(40) NOT NULL
);
INSERT INTO contract_ai_config (id, config_name, provider, enabled, updated_at)
SELECT 1, '合同 AI 服务', 'mock', 0, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM contract_ai_config WHERE id = 1);
