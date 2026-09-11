CREATE TABLE IF NOT EXISTS ai_call_log (
    id BIGINT PRIMARY KEY,
    contract_id BIGINT NULL,
    capability VARCHAR(64) NOT NULL,
    provider VARCHAR(64) NOT NULL,
    model_or_workflow VARCHAR(128) NULL,
    status VARCHAR(32) NOT NULL,
    error_summary VARCHAR(500) NULL,
    created_at VARCHAR(40) NOT NULL
);
