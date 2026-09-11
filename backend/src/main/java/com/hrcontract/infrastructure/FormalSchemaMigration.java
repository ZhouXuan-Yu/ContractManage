package com.hrcontract.infrastructure;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.DependsOn;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Profile;

/** Builds the DOCX-aligned tables beside the current mock tables during the migration period. */
@Component
@Profile("!mysql")
@DependsOn({"contractService", "changeService", "aiWorkflowService", "fulfillmentService"})
public class FormalSchemaMigration {
    private final JdbcTemplate jdbc;

    public FormalSchemaMigration(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @PostConstruct
    void migrate() {
        createTables();
        migrateMain();
        migrateContent();
        migrateAttachments();
        migrateChanges();
        migrateApprovals();
        migrateFulfillment();
        migrateAi();
        createRuntimeTriggers();
        jdbc.update("INSERT OR IGNORE INTO contract_schema_migration(version,description) VALUES(?,?)", 1, "DOCX authority tables initialized and legacy mock data copied");
    }

    private void createTables() {
        jdbc.execute("CREATE TABLE IF NOT EXISTS contract_schema_migration (version INTEGER PRIMARY KEY, description TEXT NOT NULL, migrated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP)");
        jdbc.execute("CREATE TABLE IF NOT EXISTS t_contract_main (id INTEGER PRIMARY KEY, contract_no TEXT NOT NULL UNIQUE, contract_name TEXT NOT NULL, category_type_id INTEGER, category_version_id INTEGER, contract_status INTEGER NOT NULL DEFAULT 0, approval_status INTEGER NOT NULL DEFAULT 0, sign_date TEXT, effective_date TEXT, expire_date TEXT, total_amount NUMERIC, currency TEXT NOT NULL DEFAULT 'CNY', payment_direction INTEGER, source_type INTEGER NOT NULL DEFAULT 3, signed_file_id INTEGER, current_content_version_id INTEGER, remark TEXT, create_by INTEGER NOT NULL DEFAULT 0, create_time TEXT NOT NULL, update_time TEXT, deleted INTEGER NOT NULL DEFAULT 0)");
        ensureColumn("t_contract_main", "expire_date", "TEXT");
        ensureColumn("t_contract_main", "total_amount", "NUMERIC");
        ensureColumn("t_contract_main", "currency", "TEXT NOT NULL DEFAULT 'CNY'");
        ensureColumn("t_contract_main", "payment_direction", "INTEGER");
        ensureColumn("t_contract_main", "source_type", "INTEGER NOT NULL DEFAULT 3");
        ensureColumn("t_contract_main", "template_version_id", "INTEGER");
        jdbc.execute("CREATE TABLE IF NOT EXISTS t_contract_content_version (id INTEGER PRIMARY KEY, contract_id INTEGER NOT NULL, version_no INTEGER NOT NULL, content TEXT, content_hash TEXT NOT NULL, version_source INTEGER NOT NULL DEFAULT 2, is_current INTEGER NOT NULL DEFAULT 0, submit_approval_flag INTEGER NOT NULL DEFAULT 0, sign_flag INTEGER NOT NULL DEFAULT 0, create_time TEXT NOT NULL, UNIQUE(contract_id,version_no))");
        jdbc.execute("CREATE TABLE IF NOT EXISTS t_contract_attachment (id INTEGER PRIMARY KEY, contract_id INTEGER NOT NULL, change_id INTEGER, attach_type INTEGER NOT NULL DEFAULT 3, biz_attach_type INTEGER, file_name TEXT NOT NULL, storage_object_id TEXT NOT NULL, file_hash TEXT, file_size INTEGER, mime_type TEXT, is_main_file INTEGER NOT NULL DEFAULT 0, content_version_id INTEGER, create_time TEXT NOT NULL, deleted INTEGER NOT NULL DEFAULT 0)");
        jdbc.execute("CREATE TABLE IF NOT EXISTS t_contract_change (id INTEGER PRIMARY KEY, contract_id INTEGER NOT NULL, change_no TEXT NOT NULL UNIQUE, change_type INTEGER NOT NULL, change_title TEXT NOT NULL, change_reason TEXT, before_content_version_id INTEGER NOT NULL, after_content_version_id INTEGER NOT NULL, effective_content_version_id INTEGER, sign_date TEXT, effect_date TEXT, approval_status INTEGER NOT NULL DEFAULT 0, approval_no TEXT, create_time TEXT NOT NULL, update_time TEXT, deleted INTEGER NOT NULL DEFAULT 0)");
        jdbc.execute("CREATE TABLE IF NOT EXISTS sys_approval_instance (id INTEGER PRIMARY KEY, approval_no TEXT NOT NULL UNIQUE, process_definition_key TEXT NOT NULL DEFAULT 'contract', process_instance_id TEXT, biz_type TEXT NOT NULL DEFAULT 'contract', biz_id INTEGER NOT NULL, title TEXT NOT NULL, status INTEGER NOT NULL DEFAULT 0, result INTEGER, apply_time TEXT NOT NULL, update_time TEXT NOT NULL)");
        jdbc.execute("CREATE TABLE IF NOT EXISTS t_contract_approval_record (id INTEGER PRIMARY KEY, contract_id INTEGER NOT NULL, change_id INTEGER, approval_no TEXT NOT NULL, contract_content_version_id INTEGER, approval_generation INTEGER NOT NULL DEFAULT 1, approval_action INTEGER NOT NULL, approve_opinion TEXT, operate_time TEXT NOT NULL, create_by INTEGER NOT NULL DEFAULT 10001, create_time TEXT NOT NULL DEFAULT '', record_status INTEGER NOT NULL DEFAULT 1)");
        ensureColumn("t_contract_approval_record", "create_by", "INTEGER NOT NULL DEFAULT 10001");
        ensureColumn("t_contract_approval_record", "create_time", "TEXT NOT NULL DEFAULT ''");
        ensureColumn("t_contract_main", "party_id", "TEXT");
        ensureColumn("t_contract_main", "applicant_id", "TEXT");
        ensureColumn("t_contract_main", "applicant_org_id", "TEXT");
        ensureColumn("t_contract_main", "applicant_department_id", "TEXT");
        jdbc.execute("CREATE TABLE IF NOT EXISTS t_contract_payment_plan (id INTEGER PRIMARY KEY, contract_id INTEGER NOT NULL, phase_no INTEGER NOT NULL DEFAULT 1, plan_name TEXT, payment_direction INTEGER, plan_amount NUMERIC NOT NULL DEFAULT 0, plan_due_date TEXT, paid_amount NUMERIC DEFAULT 0, paid_date TEXT, pay_status INTEGER NOT NULL DEFAULT 0, remark TEXT, deleted INTEGER NOT NULL DEFAULT 0)");
        jdbc.execute("CREATE TABLE IF NOT EXISTS t_contract_payment_record (id INTEGER PRIMARY KEY, payment_plan_id INTEGER NOT NULL, payment_amount NUMERIC NOT NULL DEFAULT 0, payment_date TEXT, payment_status INTEGER NOT NULL DEFAULT 0, voucher_file_id INTEGER, remark TEXT, create_time TEXT NOT NULL, deleted INTEGER NOT NULL DEFAULT 0)");
        jdbc.execute("CREATE TABLE IF NOT EXISTS t_contract_perform_item (id INTEGER PRIMARY KEY, contract_id INTEGER NOT NULL, item_name TEXT NOT NULL, item_description TEXT, item_type INTEGER NOT NULL DEFAULT 4, plan_finish_date TEXT, actual_finish_date TEXT, perform_status INTEGER NOT NULL DEFAULT 0, remark TEXT, deleted INTEGER NOT NULL DEFAULT 0)");
        jdbc.execute("CREATE TABLE IF NOT EXISTS t_contract_ai_extract_task (id INTEGER PRIMARY KEY, contract_id INTEGER NOT NULL, task_id TEXT NOT NULL, task_status INTEGER NOT NULL DEFAULT 1, content_version_id INTEGER, summary TEXT, raw_result_json TEXT, create_time TEXT NOT NULL, finish_time TEXT, deleted INTEGER NOT NULL DEFAULT 0)");
        jdbc.execute("CREATE TABLE IF NOT EXISTS t_contract_ai_review (id INTEGER PRIMARY KEY, contract_id INTEGER NOT NULL, ai_task_id INTEGER NOT NULL, review_view INTEGER NOT NULL DEFAULT 1, task_status INTEGER NOT NULL DEFAULT 1, overall_risk_level INTEGER, review_summary TEXT, risk_items_json TEXT, confirmed_status INTEGER NOT NULL DEFAULT 0, create_time TEXT NOT NULL, deleted INTEGER NOT NULL DEFAULT 0)");
        jdbc.execute("CREATE TRIGGER IF NOT EXISTS trg_contract_draft_to_main_insert AFTER INSERT ON contract_draft BEGIN INSERT OR REPLACE INTO t_contract_main(id,contract_no,contract_name,category_type_id,contract_status,approval_status,sign_date,effective_date,expire_date,total_amount,currency,payment_direction,source_type,signed_file_id,current_content_version_id,remark,applicant_id,applicant_org_id,applicant_department_id,create_time,update_time) VALUES(NEW.id,NEW.contract_no,NEW.name,NEW.type_id,NEW.contract_status,NEW.approval_status,NEW.sign_date,NEW.effective_date,NEW.expire_date,NEW.total_amount,NEW.currency,NEW.payment_direction,NEW.source_type_code,NEW.signed_file_id,NEW.current_content_version_id,NEW.remark,NEW.applicant_id,NEW.applicant_org_id,NEW.applicant_department_id,NEW.updated_at,NEW.updated_at); END");
        jdbc.execute("CREATE TRIGGER IF NOT EXISTS trg_contract_draft_to_main_update AFTER UPDATE ON contract_draft BEGIN UPDATE t_contract_main SET contract_no=NEW.contract_no,contract_name=NEW.name,category_type_id=NEW.type_id,contract_status=NEW.contract_status,approval_status=NEW.approval_status,sign_date=NEW.sign_date,effective_date=NEW.effective_date,expire_date=NEW.expire_date,total_amount=NEW.total_amount,currency=NEW.currency,payment_direction=NEW.payment_direction,source_type=NEW.source_type_code,signed_file_id=NEW.signed_file_id,current_content_version_id=NEW.current_content_version_id,remark=NEW.remark,applicant_id=NEW.applicant_id,applicant_org_id=NEW.applicant_org_id,applicant_department_id=NEW.applicant_department_id,update_time=NEW.updated_at WHERE id=NEW.id; END");
    }

    private void migrateMain() {
        jdbc.update("INSERT OR REPLACE INTO t_contract_main(id,contract_no,contract_name,category_type_id,contract_status,approval_status,sign_date,effective_date,expire_date,total_amount,currency,payment_direction,source_type,signed_file_id,current_content_version_id,remark,party_id,applicant_id,applicant_org_id,applicant_department_id,create_time,update_time) SELECT id,contract_no,name,type_id,contract_status,approval_status,sign_date,effective_date,expire_date,total_amount,currency,payment_direction,source_type_code,signed_file_id,current_content_version_id,remark,party_id,applicant_id,applicant_org_id,applicant_department_id,updated_at,updated_at FROM contract_draft");
    }

    private void migrateContent() {
        jdbc.update("INSERT OR IGNORE INTO t_contract_content_version(id,contract_id,version_no,content,content_hash,version_source,is_current,create_time) SELECT id,contract_id,version_no,content,content_hash,2,is_current,created_at FROM contract_content_version");
    }

    private void migrateAttachments() {
        jdbc.update("INSERT OR IGNORE INTO t_contract_attachment(id,contract_id,attach_type,biz_attach_type,file_name,storage_object_id,file_hash,file_size,mime_type,is_main_file,content_version_id,create_time) SELECT id,contract_id,COALESCE(attach_type,3),biz_attach_type,file_name,COALESCE(storage_object_id,file_ref),file_hash,file_size,mime_type,COALESCE(is_main_file,0),content_version_id,uploaded_at FROM contract_attachment");
    }

    private void migrateChanges() {
        jdbc.update("INSERT OR IGNORE INTO t_contract_change(id,contract_id,change_no,change_type,change_title,change_reason,before_content_version_id,after_content_version_id,effective_content_version_id,sign_date,effect_date,approval_status,approval_no,create_time,update_time) SELECT id,contract_id,change_no,change_type,change_title,change_reason,before_content_version_id,after_content_version_id,effective_content_version_id,sign_date,effect_date,approval_status,approval_no,updated_at,updated_at FROM contract_change");
    }

    private void migrateApprovals() {
        jdbc.update("INSERT OR IGNORE INTO sys_approval_instance(id,approval_no,biz_id,title,status,result,apply_time,update_time) SELECT id,approval_no,contract_id,'合同审批',CASE status WHEN 'APPROVED' THEN 2 WHEN 'REJECTED' THEN 3 WHEN 'WITHDRAWN' THEN 4 ELSE 1 END,CASE result WHEN 'APPROVED' THEN 1 WHEN 'REJECTED' THEN 2 ELSE NULL END,updated_at,updated_at FROM approval_mock");
        jdbc.update("INSERT OR IGNORE INTO t_contract_approval_record(id,contract_id,approval_no,approval_action,operate_time) SELECT id,contract_id,approval_no,CASE status WHEN 'APPROVED' THEN 2 ELSE 1 END,updated_at FROM approval_mock");
    }

    private void migrateFulfillment() {
        jdbc.update("INSERT OR IGNORE INTO t_contract_payment_plan(id,contract_id,plan_name,payment_direction,plan_amount,plan_due_date,paid_amount,remark) SELECT id,contract_id,item_name,CASE direction WHEN 'RECEIVE' THEN 1 ELSE 2 END,amount,due_date,COALESCE(paid_amount,0),remark FROM contract_payment_plan");
        jdbc.update("INSERT OR IGNORE INTO t_contract_payment_record(id,payment_plan_id,payment_amount,payment_date,payment_status,voucher_file_id,remark,create_time) SELECT id,payment_plan_id,amount,payment_date,CASE status WHEN 'CONFIRMED' THEN 1 ELSE 0 END,voucher_attachment_id,remark,created_at FROM contract_payment_record");
        jdbc.update("INSERT OR IGNORE INTO t_contract_perform_item(id,contract_id,item_name,plan_finish_date,actual_finish_date,perform_status,remark) SELECT id,contract_id,name,due_date,completed_at,CASE status WHEN 'COMPLETED' THEN 2 ELSE 0 END,remark FROM contract_fulfillment_milestone");
    }

    private void migrateAi() {
        jdbc.update("INSERT OR IGNORE INTO t_contract_ai_extract_task(id,contract_id,task_id,task_status,content_version_id,summary,raw_result_json,create_time,finish_time) SELECT id,contract_id,task_id,CASE status WHEN 'COMPLETED' THEN 2 WHEN 'CONFIRMED' THEN 2 ELSE 1 END,content_version_id,summary,raw_response,created_at,finished_at FROM ai_task WHERE task_type='EXTRACT'");
        jdbc.update("INSERT OR IGNORE INTO t_contract_ai_review(id,contract_id,ai_task_id,review_view,task_status,overall_risk_level,review_summary,risk_items_json,confirmed_status,create_time) SELECT id,contract_id,task_id,CASE review_view WHEN 'OUR_SIDE' THEN 1 ELSE 0 END,2,CASE overall_level WHEN 'HIGH' THEN 3 WHEN 'MEDIUM' THEN 2 WHEN 'LOW' THEN 1 ELSE 0 END,summary,risks_json,confirmed,created_at FROM ai_review");
    }

    private void createRuntimeTriggers() {
        jdbc.execute("CREATE TRIGGER IF NOT EXISTS trg_payment_plan_to_contract_insert AFTER INSERT ON contract_payment_plan BEGIN INSERT OR REPLACE INTO t_contract_payment_plan(id,contract_id,plan_name,payment_direction,plan_amount,plan_due_date,paid_amount,pay_status,remark) VALUES(NEW.id,NEW.contract_id,NEW.item_name,CASE NEW.direction WHEN 'RECEIVE' THEN 1 ELSE 2 END,NEW.amount,NEW.due_date,COALESCE(NEW.paid_amount,0),CASE NEW.status WHEN 'PAID' THEN 2 WHEN 'CANCELLED' THEN 3 ELSE 0 END,NEW.remark); END");
        jdbc.execute("CREATE TRIGGER IF NOT EXISTS trg_payment_plan_to_contract_update AFTER UPDATE ON contract_payment_plan BEGIN UPDATE t_contract_payment_plan SET plan_name=NEW.item_name,payment_direction=CASE NEW.direction WHEN 'RECEIVE' THEN 1 ELSE 2 END,plan_amount=NEW.amount,plan_due_date=NEW.due_date,paid_amount=COALESCE(NEW.paid_amount,0),pay_status=CASE NEW.status WHEN 'PAID' THEN 2 WHEN 'CANCELLED' THEN 3 ELSE 0 END,remark=NEW.remark WHERE id=NEW.id; END");
        jdbc.execute("CREATE TRIGGER IF NOT EXISTS trg_payment_record_to_contract_insert AFTER INSERT ON contract_payment_record BEGIN INSERT OR REPLACE INTO t_contract_payment_record(id,payment_plan_id,payment_amount,payment_date,payment_status,voucher_file_id,remark,create_time) VALUES(NEW.id,NEW.payment_plan_id,NEW.amount,NEW.payment_date,CASE NEW.status WHEN 'CONFIRMED' THEN 1 ELSE 0 END,NEW.voucher_attachment_id,NEW.remark,NEW.created_at); END");
        jdbc.execute("CREATE TRIGGER IF NOT EXISTS trg_milestone_to_contract_insert AFTER INSERT ON contract_fulfillment_milestone BEGIN INSERT OR REPLACE INTO t_contract_perform_item(id,contract_id,item_name,plan_finish_date,actual_finish_date,perform_status,remark) VALUES(NEW.id,NEW.contract_id,NEW.name,NEW.due_date,NEW.completed_at,CASE NEW.status WHEN 'COMPLETED' THEN 2 WHEN 'CANCELLED' THEN 3 ELSE 0 END,NEW.remark); END");
        jdbc.execute("CREATE TRIGGER IF NOT EXISTS trg_milestone_to_contract_update AFTER UPDATE ON contract_fulfillment_milestone BEGIN UPDATE t_contract_perform_item SET item_name=NEW.name,plan_finish_date=NEW.due_date,actual_finish_date=NEW.completed_at,perform_status=CASE NEW.status WHEN 'COMPLETED' THEN 2 WHEN 'CANCELLED' THEN 3 ELSE 0 END,remark=NEW.remark WHERE id=NEW.id; END");
        jdbc.execute("CREATE TRIGGER IF NOT EXISTS trg_ai_task_to_contract_insert AFTER INSERT ON ai_task WHEN NEW.task_type='EXTRACT' BEGIN INSERT OR REPLACE INTO t_contract_ai_extract_task(id,contract_id,task_id,task_status,content_version_id,summary,raw_result_json,create_time,finish_time) VALUES(NEW.id,NEW.contract_id,NEW.task_id,CASE NEW.status WHEN 'COMPLETED' THEN 2 WHEN 'CONFIRMED' THEN 2 ELSE 1 END,NEW.content_version_id,NEW.summary,NEW.raw_response,NEW.created_at,NEW.finished_at); END");
        jdbc.execute("CREATE TRIGGER IF NOT EXISTS trg_ai_review_to_contract_insert AFTER INSERT ON ai_review BEGIN INSERT OR REPLACE INTO t_contract_ai_review(id,contract_id,ai_task_id,review_view,task_status,overall_risk_level,review_summary,risk_items_json,confirmed_status,create_time) VALUES(NEW.id,NEW.contract_id,NEW.task_id,CASE NEW.review_view WHEN 'OUR_SIDE' THEN 1 ELSE 0 END,2,CASE NEW.overall_level WHEN 'HIGH' THEN 3 WHEN 'MEDIUM' THEN 2 WHEN 'LOW' THEN 1 ELSE 0 END,NEW.summary,NEW.risks_json,NEW.confirmed,NEW.created_at); END");
        jdbc.execute("CREATE TRIGGER IF NOT EXISTS trg_ai_review_to_contract_update AFTER UPDATE ON ai_review BEGIN UPDATE t_contract_ai_review SET overall_risk_level=CASE NEW.overall_level WHEN 'HIGH' THEN 3 WHEN 'MEDIUM' THEN 2 WHEN 'LOW' THEN 1 ELSE 0 END,review_summary=NEW.summary,risk_items_json=NEW.risks_json,confirmed_status=NEW.confirmed WHERE id=NEW.id; END");
        jdbc.execute("CREATE TRIGGER IF NOT EXISTS trg_approval_mock_to_instance_insert AFTER INSERT ON approval_mock BEGIN INSERT OR REPLACE INTO sys_approval_instance(id,approval_no,process_definition_key,biz_type,biz_id,title,status,result,apply_time,update_time) VALUES(NEW.id,NEW.approval_no,'contract','contract',NEW.contract_id,'合同审批',CASE NEW.status WHEN 'APPROVED' THEN 2 WHEN 'REJECTED' THEN 3 WHEN 'WITHDRAWN' THEN 4 ELSE 1 END,CASE NEW.result WHEN 'APPROVED' THEN 1 WHEN 'REJECTED' THEN 2 ELSE NULL END,NEW.updated_at,NEW.updated_at); END");
        jdbc.execute("CREATE TRIGGER IF NOT EXISTS trg_approval_mock_to_instance_update AFTER UPDATE ON approval_mock BEGIN UPDATE sys_approval_instance SET status=CASE NEW.status WHEN 'APPROVED' THEN 2 WHEN 'REJECTED' THEN 3 WHEN 'WITHDRAWN' THEN 4 ELSE 1 END,result=CASE NEW.result WHEN 'APPROVED' THEN 1 WHEN 'REJECTED' THEN 2 ELSE NULL END,update_time=NEW.updated_at WHERE id=NEW.id; END");
    }

    private void ensureColumn(String table, String column, String definition) {
        try { jdbc.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition); }
        catch (Exception ignored) { }
    }
}
