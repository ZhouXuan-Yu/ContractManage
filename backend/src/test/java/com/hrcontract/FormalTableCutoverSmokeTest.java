package com.hrcontract;

import com.hrcontract.application.AiWorkflowService;
import com.hrcontract.application.ContractAuthorizationService;
import com.hrcontract.application.ContractModels;
import com.hrcontract.application.StatisticsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies the formal-table read cutover on the SQLite profile:
 * migration creates the AI tables + runtime triggers, and the triggers materialize
 * compatibility writes into the DOCX-aligned tables that authorization/approval/statistics now read.
 */
@SpringBootTest
class FormalTableCutoverSmokeTest {
    static Path dbFile;

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) throws Exception {
        dbFile = Files.createTempFile("contract-cutover-", ".db");
        registry.add("spring.datasource.url", () -> "jdbc:sqlite:" + dbFile.toAbsolutePath());
    }

    @Autowired JdbcTemplate jdbc;
    @Autowired AiWorkflowService aiWorkflowService;
    @Autowired ContractAuthorizationService authorizationService;
    @Autowired StatisticsService statisticsService;

    @Test
    void migrationRan() {
        Integer version = jdbc.queryForObject("SELECT version FROM contract_schema_migration WHERE version=1", Integer.class);
        assertEquals(1, version);
    }

    @Test
    void approvalTriggerFeedsApprovalRead() {
        jdbc.update("INSERT INTO approval_mock(id,contract_id,approval_no,status,result,updated_at) VALUES(1,10,'A-1','PENDING',NULL,'2026-09-03T00:00:00Z')");
        ContractModels.ApprovalInfo pending = aiWorkflowService.approval(10L);
        assertNotNull(pending);
        assertEquals("PENDING", pending.status());

        jdbc.update("UPDATE approval_mock SET status='APPROVED',result='APPROVED',updated_at='2026-09-03T01:00:00Z' WHERE id=1");
        ContractModels.ApprovalInfo approved = aiWorkflowService.approval(10L);
        assertNotNull(approved);
        assertEquals("APPROVED", approved.status());
    }

    @Test
    void paymentAndMilestoneTriggersFeedStatistics() {
        jdbc.update("INSERT INTO contract_payment_plan(id,contract_id,item_name,direction,amount,due_date,status,remark) VALUES(1,10,'首付款','RECEIVE',100.0,'2026-09-10','PENDING','r')");
        jdbc.update("INSERT INTO contract_fulfillment_milestone(id,contract_id,name,due_date,status,remark) VALUES(1,10,'交付节点','2026-09-12','PENDING','r')");
        assertEquals(0, (int) jdbc.queryForObject("SELECT pay_status FROM t_contract_payment_plan WHERE id=1", Integer.class));
        assertEquals(0, (int) jdbc.queryForObject("SELECT perform_status FROM t_contract_perform_item WHERE id=1", Integer.class));

        jdbc.update("UPDATE contract_payment_plan SET status='PAID' WHERE id=1");
        jdbc.update("UPDATE contract_fulfillment_milestone SET status='COMPLETED',completed_at='2026-09-12T00:00:00Z' WHERE id=1");
        assertEquals(2, (int) jdbc.queryForObject("SELECT pay_status FROM t_contract_payment_plan WHERE id=1", Integer.class));
        assertEquals(2, (int) jdbc.queryForObject("SELECT perform_status FROM t_contract_perform_item WHERE id=1", Integer.class));
    }

    @Test
    void aiTriggersFeedStatistics() {
        jdbc.update("INSERT INTO ai_task(id,contract_id,task_id,task_type,status,content_version_id,summary,created_at,finished_at) VALUES(1,10,'T-1','EXTRACT','COMPLETED',1,'s','2026-09-03T00:00:00Z','2026-09-03T00:00:00Z')");
        assertEquals(2, (int) jdbc.queryForObject("SELECT task_status FROM t_contract_ai_extract_task WHERE id=1", Integer.class));

        jdbc.update("INSERT INTO ai_review(id,contract_id,task_id,review_view,overall_level,summary,risks_json,confirmed,created_at) VALUES(1,10,1,'OUR_SIDE','HIGH','s','[]',0,'2026-09-03T00:00:00Z')");
        assertEquals(3, (int) jdbc.queryForObject("SELECT overall_risk_level FROM t_contract_ai_review WHERE id=1", Integer.class));
    }

    @Test
    void authorizationReadsFormalFields() {
        ContractModels.ContractAuthorization created = authorizationService.create(
                new ContractModels.CreateContractAuthorizationRequest(10L, "20001", true, false, false, false, null, null));
        assertNotNull(created);
        assertTrue(authorizationService.allowed(10L, "20001", "VIEW"));
        assertFalse(authorizationService.allowed(10L, "20001", "EDIT"));
    }

    @Test
    void statisticsReadsFormalTables() {
        jdbc.update("INSERT INTO t_contract_main(id,contract_no,contract_name,contract_status,create_time) VALUES(10,'C-10','测试合同',0,'2026-09-03T00:00:00Z')");
        ContractModels.StatisticsOverview overview = statisticsService.overview("ADMIN");
        assertNotNull(overview);
        assertEquals(1, overview.total());
        assertEquals(1, overview.draft());
    }
}
