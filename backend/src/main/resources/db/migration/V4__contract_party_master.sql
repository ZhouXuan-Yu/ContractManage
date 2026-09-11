CREATE TABLE IF NOT EXISTS contract_party_master (
    id VARCHAR(64) PRIMARY KEY,
    party_name VARCHAR(255) NOT NULL,
    relation_type VARCHAR(64) NOT NULL,
    source_type VARCHAR(64) NOT NULL,
    nature VARCHAR(64) NOT NULL,
    credit_code VARCHAR(128),
    contact_name VARCHAR(128),
    contact_phone VARCHAR(64),
    enabled TINYINT NOT NULL DEFAULT 1,
    remark VARCHAR(500),
    created_at VARCHAR(40) NOT NULL,
    updated_at VARCHAR(40) NOT NULL
);

INSERT INTO contract_party_master (id, party_name, relation_type, source_type, nature, enabled, remark, created_at, updated_at)
SELECT 'EMP-001', '张明（员工）', '员工', 'HR', '个人', 1, '开发环境初始化', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM contract_party_master WHERE id = 'EMP-001');
INSERT INTO contract_party_master (id, party_name, relation_type, source_type, nature, enabled, remark, created_at, updated_at)
SELECT 'CUS-001', '华景科技有限公司', '客户', '交易对方系统', '企业', 1, '开发环境初始化', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM contract_party_master WHERE id = 'CUS-001');
INSERT INTO contract_party_master (id, party_name, relation_type, source_type, nature, enabled, remark, created_at, updated_at)
SELECT 'SUP-001', '远景供应链有限公司', '供应商', '交易对方系统', '企业', 1, '开发环境初始化', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM contract_party_master WHERE id = 'SUP-001');
