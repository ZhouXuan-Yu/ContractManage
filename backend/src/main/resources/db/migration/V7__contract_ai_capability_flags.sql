ALTER TABLE contract_ai_config ADD COLUMN draft_enabled TINYINT NOT NULL DEFAULT 1;
ALTER TABLE contract_ai_config ADD COLUMN extract_enabled TINYINT NOT NULL DEFAULT 1;
ALTER TABLE contract_ai_config ADD COLUMN review_enabled TINYINT NOT NULL DEFAULT 1;
ALTER TABLE contract_ai_config ADD COLUMN fulfillment_enabled TINYINT NOT NULL DEFAULT 1;
