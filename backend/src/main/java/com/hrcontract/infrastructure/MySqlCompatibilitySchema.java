package com.hrcontract.infrastructure;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Keeps the existing development APIs operational during the staged MySQL cutover.
 * The compatibility tables are not the authority: triggers mirror core contract data
 * into the DOCX-defined t_contract_* tables created by the V1 baseline.
 */
@Component
@Profile("mysql")
public class MySqlCompatibilitySchema {
    private final JdbcTemplate jdbc;

    public MySqlCompatibilitySchema(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @PostConstruct
    void initialize() {
        createCompatibilityTables();
        createCoreMirrors();
        seedReferenceData();
    }

    private void createCompatibilityTables() {
        execute("CREATE TABLE IF NOT EXISTS contract_draft (id BIGINT PRIMARY KEY, contract_no VARCHAR(64) NOT NULL UNIQUE, name VARCHAR(255) NOT NULL, type_id BIGINT NOT NULL, party_id VARCHAR(128) NOT NULL, remark VARCHAR(500), version INT NOT NULL, updated_at VARCHAR(40) NOT NULL, status VARCHAR(32) NOT NULL DEFAULT 'DRAFT', contract_status TINYINT NOT NULL DEFAULT 0, approval_status TINYINT NOT NULL DEFAULT 0, sign_date VARCHAR(16), effective_date VARCHAR(16), signed_file_id BIGINT, current_content_version_id BIGINT, template_version_id BIGINT, total_amount DECIMAL(18,2), currency VARCHAR(8) NOT NULL DEFAULT 'CNY', payment_direction TINYINT, expire_date VARCHAR(16), source_type_code TINYINT NOT NULL DEFAULT 3, applicant_id VARCHAR(64) NOT NULL DEFAULT '10001', applicant_org_id VARCHAR(64) NOT NULL DEFAULT '100', applicant_department_id VARCHAR(64) NOT NULL DEFAULT '101')");
        addColumn("contract_draft", "template_version_id", "BIGINT");
        addColumn("contract_draft", "archived", "TINYINT NOT NULL DEFAULT 0");
        addColumn("contract_draft", "archived_at", "VARCHAR(40)");
        execute("CREATE TABLE IF NOT EXISTS contract_party_ref (contract_id BIGINT NOT NULL, party_id VARCHAR(128) NOT NULL, relation_type VARCHAR(64) NOT NULL, source_type VARCHAR(64) NOT NULL, party_name VARCHAR(255) NOT NULL, PRIMARY KEY(contract_id,party_id))");
        execute("CREATE TABLE IF NOT EXISTS contract_operation_log (id BIGINT AUTO_INCREMENT PRIMARY KEY, contract_id BIGINT NOT NULL, action VARCHAR(64) NOT NULL, description VARCHAR(500) NOT NULL, operated_at VARCHAR(40) NOT NULL)");
        execute("CREATE TABLE IF NOT EXISTS contract_content_version (id BIGINT PRIMARY KEY, contract_id BIGINT NOT NULL, version_no INT NOT NULL, content MEDIUMTEXT NOT NULL, content_hash VARCHAR(64) NOT NULL, source VARCHAR(64) NOT NULL, is_current TINYINT NOT NULL DEFAULT 1, created_at VARCHAR(40) NOT NULL, UNIQUE KEY uk_contract_content_version(contract_id,version_no))");
        execute("CREATE TABLE IF NOT EXISTS contract_attachment (id BIGINT PRIMARY KEY, contract_id BIGINT NOT NULL, file_name VARCHAR(255) NOT NULL, file_size BIGINT NOT NULL, file_ref VARCHAR(255) NOT NULL UNIQUE, storage_path VARCHAR(512) NOT NULL, content_version_id BIGINT, biz_attach_type TINYINT, attach_type TINYINT, storage_object_id VARCHAR(255), file_hash VARCHAR(128), mime_type VARCHAR(128), is_main_file TINYINT NOT NULL DEFAULT 0, uploaded_at VARCHAR(40) NOT NULL)");
        execute("CREATE TABLE IF NOT EXISTS ai_task (id BIGINT PRIMARY KEY, contract_id BIGINT NOT NULL, task_id VARCHAR(128) NOT NULL UNIQUE, task_type VARCHAR(64) NOT NULL, status VARCHAR(32) NOT NULL, content_version_id BIGINT NOT NULL, summary TEXT, created_at VARCHAR(40) NOT NULL, finished_at VARCHAR(40))");
        execute("CREATE TABLE IF NOT EXISTS ai_review (id BIGINT PRIMARY KEY, contract_id BIGINT NOT NULL, task_id BIGINT NOT NULL, review_view VARCHAR(32) NOT NULL, overall_level VARCHAR(32) NOT NULL, summary TEXT NOT NULL, risks_json MEDIUMTEXT NOT NULL, confirmed TINYINT NOT NULL DEFAULT 0, created_at VARCHAR(40) NOT NULL)");
        addColumn("ai_task", "provider", "VARCHAR(32)");
        addColumn("ai_task", "raw_response", "MEDIUMTEXT");
        addColumn("ai_review", "provider", "VARCHAR(32)");
        addColumn("ai_review", "raw_response", "MEDIUMTEXT");
        execute("CREATE TABLE IF NOT EXISTS approval_mock (id BIGINT PRIMARY KEY, contract_id BIGINT NOT NULL UNIQUE, approval_no VARCHAR(64) NOT NULL UNIQUE, status VARCHAR(32) NOT NULL, result VARCHAR(32), content_version_id BIGINT, content_hash VARCHAR(64), approval_generation INT NOT NULL DEFAULT 1, updated_at VARCHAR(40) NOT NULL)");
        addColumn("approval_mock", "content_version_id", "BIGINT");
        addColumn("approval_mock", "content_hash", "VARCHAR(64)");
        addColumn("approval_mock", "approval_generation", "INT NOT NULL DEFAULT 1");
        execute("CREATE TABLE IF NOT EXISTS contract_payment_plan (id BIGINT PRIMARY KEY, contract_id BIGINT NOT NULL, item_name VARCHAR(255) NOT NULL, direction VARCHAR(16) NOT NULL, amount DECIMAL(18,2) NOT NULL, due_date VARCHAR(16) NOT NULL, status VARCHAR(32) NOT NULL, paid_at VARCHAR(40), remark VARCHAR(500))");
        execute("CREATE TABLE IF NOT EXISTS contract_payment_record (id BIGINT PRIMARY KEY, payment_plan_id BIGINT NOT NULL, amount DECIMAL(18,2) NOT NULL, payment_date VARCHAR(16) NOT NULL, status VARCHAR(32) NOT NULL, voucher_attachment_id BIGINT, remark VARCHAR(500), created_at VARCHAR(40) NOT NULL)");
        execute("CREATE TABLE IF NOT EXISTS contract_fulfillment_milestone (id BIGINT PRIMARY KEY, contract_id BIGINT NOT NULL, name VARCHAR(255) NOT NULL, due_date VARCHAR(16) NOT NULL, status VARCHAR(32) NOT NULL, completed_at VARCHAR(40), remark VARCHAR(500))");
        addColumn("contract_payment_plan", "phase_no", "INT NOT NULL DEFAULT 1");
        addColumn("contract_payment_plan", "trigger_condition", "VARCHAR(500)");
        addColumn("contract_payment_plan", "payment_condition_type", "TINYINT");
        addColumn("contract_payment_plan", "paid_amount", "DECIMAL(18,2) NOT NULL DEFAULT 0");
        addColumn("contract_payment_plan", "paid_date", "VARCHAR(16)");
        addColumn("contract_payment_plan", "source_extract_task_id", "BIGINT");
        addColumn("contract_payment_plan", "is_ai_confirmed", "TINYINT NOT NULL DEFAULT 0");
        addColumn("contract_fulfillment_milestone", "item_description", "TEXT");
        addColumn("contract_fulfillment_milestone", "item_type", "TINYINT NOT NULL DEFAULT 4");
        addColumn("contract_fulfillment_milestone", "responsible_user_id", "BIGINT");
        addColumn("contract_fulfillment_milestone", "evidence_attachment_id", "BIGINT");
        addColumn("contract_fulfillment_milestone", "actual_finish_date", "VARCHAR(16)");
        addColumn("contract_fulfillment_milestone", "perform_status", "TINYINT NOT NULL DEFAULT 0");
        addColumn("contract_fulfillment_milestone", "source_extract_task_id", "BIGINT");
        addColumn("contract_fulfillment_milestone", "is_ai_confirmed", "TINYINT NOT NULL DEFAULT 0");
        execute("CREATE TABLE IF NOT EXISTS contract_change (id BIGINT PRIMARY KEY, contract_id BIGINT NOT NULL, change_no VARCHAR(64) NOT NULL UNIQUE, change_type TINYINT NOT NULL, change_title VARCHAR(255) NOT NULL, change_reason VARCHAR(500), before_content_version_id BIGINT NOT NULL, after_content_version_id BIGINT NOT NULL, effective_content_version_id BIGINT, sign_date VARCHAR(16), effect_date VARCHAR(16), approval_status TINYINT NOT NULL DEFAULT 0, approval_no VARCHAR(64), updated_at VARCHAR(40) NOT NULL)");
        execute("CREATE TABLE IF NOT EXISTS contract_template (id BIGINT PRIMARY KEY, name VARCHAR(255) NOT NULL, type_id BIGINT NOT NULL, status VARCHAR(32) NOT NULL, current_version_no INT NOT NULL DEFAULT 0, updated_at VARCHAR(40) NOT NULL)");
        execute("CREATE TABLE IF NOT EXISTS contract_template_version (id BIGINT PRIMARY KEY, template_id BIGINT NOT NULL, version_no INT NOT NULL, content MEDIUMTEXT NOT NULL, created_at VARCHAR(40) NOT NULL, UNIQUE KEY uk_template_version(template_id,version_no))");
        execute("CREATE TABLE IF NOT EXISTS sys_contract_role_permission (role_code VARCHAR(64) PRIMARY KEY, role_name VARCHAR(128) NOT NULL, description VARCHAR(500), permissions VARCHAR(500) NOT NULL, data_scope VARCHAR(64) NOT NULL, type_ids VARCHAR(255) NOT NULL, sensitive_field_access TINYINT NOT NULL DEFAULT 0, updated_at VARCHAR(40) NOT NULL)");
        execute("CREATE TABLE IF NOT EXISTS sys_contract_audit_log (id BIGINT PRIMARY KEY, module VARCHAR(64) NOT NULL, action VARCHAR(128) NOT NULL, result VARCHAR(32) NOT NULL, operator_name VARCHAR(128) NOT NULL, description VARCHAR(500), operated_at VARCHAR(40) NOT NULL)");
        execute("CREATE TABLE IF NOT EXISTS t_external_connection (id BIGINT PRIMARY KEY, connection_type VARCHAR(64) NOT NULL UNIQUE, connection_name VARCHAR(128) NOT NULL, provider VARCHAR(128), base_url VARCHAR(512), secret TEXT, status VARCHAR(32) NOT NULL, last_test_at VARCHAR(40), last_error VARCHAR(500), create_time VARCHAR(40) NOT NULL, update_time VARCHAR(40) NOT NULL)");
        execute("CREATE TABLE IF NOT EXISTS t_external_connection_log (id BIGINT PRIMARY KEY, connection_id BIGINT NOT NULL, action VARCHAR(128) NOT NULL, result VARCHAR(32) NOT NULL, message VARCHAR(500), operated_at VARCHAR(40) NOT NULL)");
    }

    private void createCoreMirrors() {
        execute("DROP TRIGGER IF EXISTS trg_compat_contract_insert");
        execute("DROP TRIGGER IF EXISTS trg_compat_contract_update");
        execute("DROP TRIGGER IF EXISTS trg_compat_party_insert");
        execute("CREATE TRIGGER trg_compat_contract_insert AFTER INSERT ON contract_draft FOR EACH ROW INSERT INTO t_contract_main(id,contract_no,contract_name,category_type_id,category_version_id,contract_status,approval_status,payment_direction,total_amount,currency,sign_date,effective_date,expire_date,source_type,signed_file_id,current_content_version_id,applicant_id,applicant_org_id,remark,create_by,create_time,update_time) VALUES(NEW.id,NEW.contract_no,NEW.name,NEW.type_id,1,NEW.contract_status,NEW.approval_status,COALESCE(NEW.payment_direction,1),NEW.total_amount,NEW.currency,NULLIF(NEW.sign_date,''),NULLIF(NEW.effective_date,''),NULLIF(NEW.expire_date,''),NEW.source_type_code,NEW.signed_file_id,COALESCE(NEW.current_content_version_id,0),CAST(NEW.applicant_id AS UNSIGNED),CAST(NEW.applicant_org_id AS UNSIGNED),NEW.remark,CAST(NEW.applicant_id AS UNSIGNED),NOW(),NOW())");
        execute("CREATE TRIGGER trg_compat_contract_update AFTER UPDATE ON contract_draft FOR EACH ROW UPDATE t_contract_main SET contract_no=NEW.contract_no,contract_name=NEW.name,category_type_id=NEW.type_id,contract_status=NEW.contract_status,approval_status=NEW.approval_status,payment_direction=COALESCE(NEW.payment_direction,1),total_amount=NEW.total_amount,currency=NEW.currency,sign_date=NULLIF(NEW.sign_date,''),effective_date=NULLIF(NEW.effective_date,''),expire_date=NULLIF(NEW.expire_date,''),source_type=NEW.source_type_code,signed_file_id=NEW.signed_file_id,current_content_version_id=COALESCE(NEW.current_content_version_id,0),remark=NEW.remark,update_time=NOW() WHERE id=NEW.id");
        execute("CREATE TRIGGER trg_compat_party_insert AFTER INSERT ON contract_party_ref FOR EACH ROW INSERT INTO t_contract_party(id,contract_id,party_source_type,external_party_id,party_role,is_our_party,display_name,party_nature,sort_num,create_by,create_time) VALUES(NEW.contract_id*100+1,NEW.contract_id,3,NEW.party_id,NEW.relation_type,0,NEW.party_name,2,1,10001,NOW()) ON DUPLICATE KEY UPDATE external_party_id=VALUES(external_party_id),party_role=VALUES(party_role),display_name=VALUES(display_name)");
        execute("DROP TRIGGER IF EXISTS trg_compat_content_insert");
        execute("CREATE TRIGGER trg_compat_content_insert AFTER INSERT ON contract_content_version FOR EACH ROW INSERT INTO t_contract_content_version(id,contract_id,version_no,content,content_hash,version_source,is_current,create_by,create_time,deleted) VALUES(NEW.id,NEW.contract_id,NEW.version_no,NEW.content,NEW.content_hash,3,NEW.is_current,10001,NOW(),0) ON DUPLICATE KEY UPDATE content=VALUES(content),content_hash=VALUES(content_hash),is_current=VALUES(is_current),update_time=NOW()");
        execute("DROP TRIGGER IF EXISTS trg_compat_content_update");
        execute("CREATE TRIGGER trg_compat_content_update AFTER UPDATE ON contract_content_version FOR EACH ROW UPDATE t_contract_content_version SET content=NEW.content,content_hash=NEW.content_hash,is_current=NEW.is_current,update_time=NOW() WHERE id=NEW.id");
        execute("DROP TRIGGER IF EXISTS trg_compat_attachment_insert");
        execute("CREATE TRIGGER trg_compat_attachment_insert AFTER INSERT ON contract_attachment FOR EACH ROW INSERT INTO t_contract_attachment(id,contract_id,attach_type,biz_attach_type,content_version_id,file_name,file_url,storage_object_id,file_hash,file_size,mime_type,is_main_file,create_by,create_time,deleted) VALUES(NEW.id,NEW.contract_id,COALESCE(NEW.attach_type,3),NEW.biz_attach_type,NEW.content_version_id,NEW.file_name,NEW.storage_path,COALESCE(NULLIF(NEW.storage_object_id,''),NEW.file_ref),COALESCE(NULLIF(NEW.file_hash,''),SHA2(CONCAT(NEW.file_ref,NEW.file_name),256)),NEW.file_size,NEW.mime_type,NEW.is_main_file,10001,NOW(),0) ON DUPLICATE KEY UPDATE file_name=VALUES(file_name),file_url=VALUES(file_url),storage_object_id=VALUES(storage_object_id),file_hash=VALUES(file_hash),file_size=VALUES(file_size),mime_type=VALUES(mime_type),is_main_file=VALUES(is_main_file),update_time=NOW()");
        execute("DROP TRIGGER IF EXISTS trg_compat_payment_insert");
        execute("CREATE TRIGGER trg_compat_payment_insert AFTER INSERT ON contract_payment_plan FOR EACH ROW INSERT INTO t_contract_payment_plan(id,contract_id,phase_no,plan_name,payment_direction,plan_amount,trigger_condition,plan_due_date,paid_amount,paid_date,pay_status,source_extract_task_id,is_confirmed,remark,create_by,create_time,deleted) VALUES(NEW.id,NEW.contract_id,NEW.phase_no,NEW.item_name,IF(NEW.direction='RECEIVE',1,2),NEW.amount,NEW.trigger_condition,NULLIF(NEW.due_date,''),COALESCE(NEW.paid_amount,0),NULLIF(NEW.paid_date,''),CASE NEW.status WHEN 'PAID' THEN 2 WHEN 'CANCELLED' THEN 3 ELSE 0 END,NEW.source_extract_task_id,NEW.is_ai_confirmed,NEW.remark,10001,NOW(),0) ON DUPLICATE KEY UPDATE plan_name=VALUES(plan_name),payment_direction=VALUES(payment_direction),plan_amount=VALUES(plan_amount),trigger_condition=VALUES(trigger_condition),plan_due_date=VALUES(plan_due_date),paid_amount=VALUES(paid_amount),paid_date=VALUES(paid_date),pay_status=VALUES(pay_status),is_confirmed=VALUES(is_confirmed),remark=VALUES(remark),update_time=NOW()");
        execute("DROP TRIGGER IF EXISTS trg_compat_payment_update");
        execute("CREATE TRIGGER trg_compat_payment_update AFTER UPDATE ON contract_payment_plan FOR EACH ROW UPDATE t_contract_payment_plan SET phase_no=NEW.phase_no,plan_name=NEW.item_name,payment_direction=IF(NEW.direction='RECEIVE',1,2),plan_amount=NEW.amount,trigger_condition=NEW.trigger_condition,plan_due_date=NULLIF(NEW.due_date,''),paid_amount=COALESCE(NEW.paid_amount,0),paid_date=NULLIF(NEW.paid_date,''),pay_status=CASE NEW.status WHEN 'PAID' THEN 2 WHEN 'CANCELLED' THEN 3 ELSE 0 END,is_confirmed=NEW.is_ai_confirmed,remark=NEW.remark,update_time=NOW() WHERE id=NEW.id");
        execute("DROP TRIGGER IF EXISTS trg_compat_payment_record_insert");
        execute("CREATE TRIGGER trg_compat_payment_record_insert AFTER INSERT ON contract_payment_record FOR EACH ROW INSERT INTO t_contract_payment_record(id,payment_plan_id,payment_amount,payment_date,payment_status,voucher_file_id,remark,create_by,create_time,deleted) VALUES(NEW.id,NEW.payment_plan_id,NEW.amount,NULLIF(NEW.payment_date,''),CASE NEW.status WHEN 'CONFIRMED' THEN 1 ELSE 0 END,NEW.voucher_attachment_id,NEW.remark,10001,NOW(),0) ON DUPLICATE KEY UPDATE payment_amount=VALUES(payment_amount),payment_date=VALUES(payment_date),payment_status=VALUES(payment_status),voucher_file_id=VALUES(voucher_file_id),remark=VALUES(remark),update_time=NOW()");
        execute("DROP TRIGGER IF EXISTS trg_compat_perform_insert");
        execute("CREATE TRIGGER trg_compat_perform_insert AFTER INSERT ON contract_fulfillment_milestone FOR EACH ROW INSERT INTO t_contract_perform_item(id,contract_id,content_version_id,item_name,item_description,item_type,responsible_user_id,evidence_attachment_id,plan_finish_date,actual_finish_date,perform_status,remark,create_by,create_time,deleted) SELECT NEW.id,NEW.contract_id,COALESCE((SELECT current_content_version_id FROM t_contract_main WHERE id=NEW.contract_id),0),NEW.name,NEW.item_description,NEW.item_type,NEW.responsible_user_id,NEW.evidence_attachment_id,NULLIF(NEW.due_date,''),NULLIF(NEW.actual_finish_date,''),CASE NEW.status WHEN 'COMPLETED' THEN 2 WHEN 'CANCELLED' THEN 3 ELSE 0 END,NEW.remark,10001,NOW(),0 ON DUPLICATE KEY UPDATE item_name=VALUES(item_name),item_description=VALUES(item_description),plan_finish_date=VALUES(plan_finish_date),actual_finish_date=VALUES(actual_finish_date),perform_status=VALUES(perform_status),remark=VALUES(remark),update_time=NOW()");
        execute("DROP TRIGGER IF EXISTS trg_compat_perform_update");
        execute("CREATE TRIGGER trg_compat_perform_update AFTER UPDATE ON contract_fulfillment_milestone FOR EACH ROW UPDATE t_contract_perform_item SET item_name=NEW.name,item_description=NEW.item_description,plan_finish_date=NULLIF(NEW.due_date,''),actual_finish_date=NULLIF(NEW.actual_finish_date,''),perform_status=CASE NEW.status WHEN 'COMPLETED' THEN 2 WHEN 'CANCELLED' THEN 3 ELSE 0 END,remark=NEW.remark,update_time=NOW() WHERE id=NEW.id");
        execute("DROP TRIGGER IF EXISTS trg_compat_change_insert");
        execute("CREATE TRIGGER trg_compat_change_insert AFTER INSERT ON contract_change FOR EACH ROW INSERT INTO t_contract_change(id,contract_id,change_no,change_type,change_title,change_reason,before_content_version_id,after_content_version_id,effective_content_version_id,sign_date,effect_date,approval_status,approval_no,create_by,create_time,deleted) VALUES(NEW.id,NEW.contract_id,NEW.change_no,NEW.change_type,NEW.change_title,NEW.change_reason,NEW.before_content_version_id,NEW.after_content_version_id,NEW.effective_content_version_id,NULLIF(NEW.sign_date,''),NULLIF(NEW.effect_date,''),NEW.approval_status,NEW.approval_no,10001,NOW(),0) ON DUPLICATE KEY UPDATE effective_content_version_id=VALUES(effective_content_version_id),sign_date=VALUES(sign_date),effect_date=VALUES(effect_date),approval_status=VALUES(approval_status),approval_no=VALUES(approval_no),update_time=NOW()");
        execute("DROP TRIGGER IF EXISTS trg_compat_change_update");
        execute("CREATE TRIGGER trg_compat_change_update AFTER UPDATE ON contract_change FOR EACH ROW UPDATE t_contract_change SET effective_content_version_id=NEW.effective_content_version_id,sign_date=NULLIF(NEW.sign_date,''),effect_date=NULLIF(NEW.effect_date,''),approval_status=NEW.approval_status,approval_no=NEW.approval_no,update_time=NOW() WHERE id=NEW.id");
        execute("DROP TRIGGER IF EXISTS trg_compat_template_insert");
        execute("CREATE TRIGGER trg_compat_template_insert AFTER INSERT ON contract_template FOR EACH ROW INSERT INTO t_contract_template(id,template_no,template_name,template_category_id,content_format,is_enable,sort_num,create_by,create_time,deleted) VALUES(NEW.id,CONCAT('TPL-',NEW.id),NEW.name,NEW.type_id,1,IF(NEW.status='DISABLED',0,1),0,10001,NOW(),0) ON DUPLICATE KEY UPDATE template_name=VALUES(template_name),template_category_id=VALUES(template_category_id),is_enable=VALUES(is_enable),update_time=NOW()");
        execute("DROP TRIGGER IF EXISTS trg_compat_template_version_insert");
        execute("CREATE TRIGGER trg_compat_template_version_insert AFTER INSERT ON contract_template_version FOR EACH ROW INSERT INTO t_contract_template_version(id,template_id,version_no,content_format,content,status,create_by,create_time,deleted) VALUES(NEW.id,NEW.template_id,CAST(NEW.version_no AS CHAR),1,NEW.content,1,10001,NOW(),0) ON DUPLICATE KEY UPDATE content=VALUES(content),update_time=NOW()");
        execute("DROP TRIGGER IF EXISTS trg_compat_ai_task_insert");
        execute("CREATE TRIGGER trg_compat_ai_task_insert AFTER INSERT ON ai_task FOR EACH ROW INSERT INTO t_contract_ai_extract_task(id,contract_id,task_id,request_version,content_hash,task_status,schema_version,result_confirm_status,task_provider,request_time,finish_time,raw_result_json,create_by,create_time,deleted) VALUES(NEW.id,NEW.contract_id,NEW.task_id,1,SHA2(COALESCE(NEW.raw_response,NEW.summary,''),256),CASE NEW.status WHEN 'COMPLETED' THEN 2 WHEN 'FAILED' THEN 3 ELSE 1 END,'extract-v1',0,NEW.provider,NOW(),IF(NEW.finished_at IS NULL,NULL,NOW()),NEW.raw_response,10001,NOW(),0) ON DUPLICATE KEY UPDATE task_status=VALUES(task_status),task_provider=VALUES(task_provider),raw_result_json=VALUES(raw_result_json),finish_time=VALUES(finish_time),update_time=NOW()");
        execute("DROP TRIGGER IF EXISTS trg_compat_approval_insert");
        execute("CREATE TRIGGER trg_compat_approval_insert AFTER INSERT ON approval_mock FOR EACH ROW INSERT INTO sys_approval_instance(id,approval_no,process_definition_key,biz_type,biz_id,title,applicant_id,applicant_name,status,result,apply_time,update_time) VALUES(NEW.id,NEW.approval_no,'contract','contract',NEW.contract_id,'合同审批',10001,'系统操作人',CASE NEW.status WHEN 'APPROVED' THEN 2 WHEN 'REJECTED' THEN 3 WHEN 'WITHDRAWN' THEN 4 ELSE 1 END,CASE NEW.result WHEN 'APPROVED' THEN 1 WHEN 'REJECTED' THEN 2 ELSE NULL END,NOW(),NOW()) ON DUPLICATE KEY UPDATE status=VALUES(status),result=VALUES(result),update_time=NOW()");
        execute("DROP TRIGGER IF EXISTS trg_compat_approval_update");
        execute("CREATE TRIGGER trg_compat_approval_update AFTER UPDATE ON approval_mock FOR EACH ROW UPDATE sys_approval_instance SET status=CASE NEW.status WHEN 'APPROVED' THEN 2 WHEN 'REJECTED' THEN 3 WHEN 'WITHDRAWN' THEN 4 ELSE 1 END,result=CASE NEW.result WHEN 'APPROVED' THEN 1 WHEN 'REJECTED' THEN 2 ELSE NULL END,update_time=NOW() WHERE id=NEW.id");
        execute("DROP TRIGGER IF EXISTS trg_compat_ai_review_insert");
        execute("CREATE TRIGGER trg_compat_ai_review_insert AFTER INSERT ON ai_review FOR EACH ROW INSERT INTO t_contract_ai_review(id,contract_id,ai_task_id,review_version,review_view,task_status,request_version,request_content_version_id,request_hash,schema_version,task_provider,raw_result_json,overall_risk_level,risk_items_json,review_summary,confirmed_status,create_by,create_time,deleted) SELECT NEW.id,NEW.contract_id,CAST(NEW.task_id AS CHAR),1,IF(NEW.review_view='OUR_SIDE',1,2),2,1,COALESCE((SELECT current_content_version_id FROM t_contract_main WHERE id=NEW.contract_id),0),SHA2(COALESCE(NEW.raw_response,NEW.summary,''),256),'review-v1',NEW.provider,NEW.raw_response,CASE NEW.overall_level WHEN 'HIGH' THEN 3 WHEN 'MEDIUM' THEN 2 WHEN 'LOW' THEN 1 ELSE 0 END,NEW.risks_json,NEW.summary,NEW.confirmed,10001,NOW(),0 ON DUPLICATE KEY UPDATE overall_risk_level=VALUES(overall_risk_level),risk_items_json=VALUES(risk_items_json),review_summary=VALUES(review_summary),raw_result_json=VALUES(raw_result_json),confirmed_status=VALUES(confirmed_status),update_time=NOW()");
    }

    private void seedReferenceData() {
        if (count("sys_contract_role_permission") == 0) {
            String now = Instant.now().toString();
            role("ADMIN", "Administrator", "VIEW,EDIT,APPROVE,FULFILL,TEMPLATE,EXPORT,SENSITIVE", "ALL", "1,2,3,4,5,6", 1, now);
            role("LEGAL", "Legal", "VIEW,EDIT,APPROVE,TEMPLATE,EXPORT,SENSITIVE", "ORGANIZATION", "1,2,3,4,5,6", 1, now);
            role("BUSINESS", "Business", "VIEW,EDIT,FULFILL,EXPORT", "SELF_AND_PARTICIPANTS", "3,4,5,6", 0, now);
            role("VIEWER", "Viewer", "VIEW", "NAMED_CONTRACTS", "", 0, now);
        }
        if (count("t_contract_category") == 0) {
            String now = Instant.now().toString();
            category(1, null, "EMPLOYMENT", "Employment and personnel", 1, now);
            category(2, 1L, "LABOR", "Labor contract", 2, now);
            category(3, null, "TRADE", "Trade and cooperation", 1, now);
            category(4, 3L, "SERVICE", "Service contract", 2, now);
            for (long id = 1; id <= 4; id++) jdbc.update("INSERT INTO t_contract_category_version(id,category_id,version_no,version_status,create_by,create_time) VALUES(?,?,?,?,?,NOW())", id + 10000, id, 1, 2, 10001);
        }
    }

    private void category(long id, Long parentId, String code, String name, int level, String now) { jdbc.update("INSERT INTO t_contract_category(id,parent_id,category_code,category_name,category_level,is_enable,description,create_by,create_time,update_time,deleted) VALUES(?,?,?,?,?,?,?,?,NOW(),NOW(),0)", id, parentId, code, name, level, 1, "Development reference category", 10001); }
    private void role(String code, String name, String permissions, String scope, String typeIds, int sensitive, String now) { jdbc.update("INSERT INTO sys_contract_role_permission(role_code,role_name,description,permissions,data_scope,type_ids,sensitive_field_access,updated_at) VALUES(?,?,?,?,?,?,?,?)", code, name, "Development role", permissions, scope, typeIds, sensitive, now); }
    private int count(String table) { return jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class); }
    private void execute(String sql) { jdbc.execute(sql); }
    private void addColumn(String table, String column, String definition) { try { jdbc.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition); } catch (RuntimeException ignored) { } }
}
