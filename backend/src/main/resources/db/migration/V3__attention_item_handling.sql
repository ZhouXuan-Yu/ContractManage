CREATE TABLE IF NOT EXISTS contract_attention_disposition (
    attention_key VARCHAR(255) NOT NULL PRIMARY KEY,
    contract_id BIGINT NOT NULL,
    handling_status VARCHAR(32) NOT NULL,
    handling_opinion TEXT,
    handled_by VARCHAR(128),
    handled_at DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS contract_attention_history (
    id BIGINT NOT NULL PRIMARY KEY,
    attention_key VARCHAR(255) NOT NULL,
    contract_id BIGINT NOT NULL,
    handling_status VARCHAR(32) NOT NULL,
    handling_opinion TEXT,
    handled_by VARCHAR(128) NOT NULL,
    handled_at DATETIME NOT NULL
);
