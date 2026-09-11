package com.hrcontract.application;

import com.hrcontract.application.ContractModels.ContractChange;
import com.hrcontract.application.ContractModels.ContentVersion;
import com.hrcontract.application.ContractModels.CreateContractChangeRequest;
import jakarta.annotation.PostConstruct;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Profile;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class ChangeService {
    private final JdbcTemplate jdbc;
    private final ContractService contracts;

    public ChangeService(JdbcTemplate jdbc, ContractService contracts) { this.jdbc = jdbc; this.contracts = contracts; }

    @PostConstruct
    @Profile("!mysql")
    void initializeSchema() {
        if (mysqlProfile()) return;
        jdbc.execute("CREATE TABLE IF NOT EXISTS contract_change (id INTEGER PRIMARY KEY, contract_id INTEGER NOT NULL, change_no TEXT NOT NULL UNIQUE, change_type INTEGER NOT NULL, change_title TEXT NOT NULL, change_reason TEXT, before_content_version_id INTEGER NOT NULL, after_content_version_id INTEGER NOT NULL, effective_content_version_id INTEGER, sign_date TEXT, effect_date TEXT, approval_status INTEGER NOT NULL DEFAULT 0, approval_no TEXT, updated_at TEXT NOT NULL)");
    }

    public List<ContractChange> list(Long contractId) {
        contracts.get(contractId);
        return jdbc.query("SELECT id,contract_id,change_no,change_type,change_title,change_reason,before_content_version_id,after_content_version_id,effective_content_version_id,sign_date,effect_date,approval_status,approval_no,updated_at FROM contract_change WHERE contract_id=? ORDER BY id DESC", (rs, row) -> new ContractChange(rs.getLong("id"), rs.getLong("contract_id"), rs.getString("change_no"), rs.getInt("change_type"), rs.getString("change_title"), rs.getString("change_reason"), rs.getLong("before_content_version_id"), rs.getLong("after_content_version_id"), (Long) rs.getObject("effective_content_version_id"), rs.getString("sign_date"), rs.getString("effect_date"), rs.getInt("approval_status"), rs.getString("approval_no"), Instant.parse(rs.getString("updated_at"))), contractId);
    }

    public ContractChange create(Long contractId, CreateContractChangeRequest request) {
        ContractModels.ContractDetail contract = contracts.get(contractId);
        if (request == null || request.changeType() < 1 || request.changeType() > 4 || blank(request.changeTitle()) || blank(request.content())) throw bad("变更信息和变更后正文不能为空");
        ContentVersion current = contract.contentVersions().stream().filter(ContentVersion::current).findFirst().orElseThrow(() -> bad("请先保存合同当前正文"));
        ContentVersion after = contracts.createPendingContent(contractId, request.content(), "变更待审批");
        long id = nextId(); Instant now = Instant.now();
        jdbc.update("INSERT INTO contract_change(id,contract_id,change_no,change_type,change_title,change_reason,before_content_version_id,after_content_version_id,approval_status,updated_at) VALUES(?,?,?,?,?,?,?,?,0,?)", id, contractId, "BG-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(), request.changeType(), request.changeTitle().trim(), request.changeReason(), current.id(), after.id(), now.toString());
        log(contractId, "CHANGE_CREATE", "发起合同变更 " + request.changeTitle().trim());
        return list(contractId).get(0);
    }

    public ContractChange submit(Long contractId, Long changeId) { return updateApproval(contractId, changeId, 1); }
    public ContractChange approve(Long contractId, Long changeId) { return updateApproval(contractId, changeId, 2); }
    public ContractChange reject(Long contractId, Long changeId) { return updateApproval(contractId, changeId, 3); }
    public ContractChange withdraw(Long contractId, Long changeId) { return updateApproval(contractId, changeId, 4); }

    public ContractChange effective(Long contractId, Long changeId, String signDate, String effectDate) {
        contracts.ensureEditable(contractId);
        ContractChange change = get(contractId, changeId);
        if (change.approvalStatus() != 2) throw bad("变更审批通过后才能生效");
        if (blank(signDate) || blank(effectDate)) throw bad("请填写签署日期和生效日期");
        Instant now = Instant.now();
        jdbc.update("UPDATE contract_change SET effective_content_version_id=?,sign_date=?,effect_date=?,updated_at=? WHERE id=? AND contract_id=?", change.afterContentVersionId(), signDate, effectDate, now.toString(), changeId, contractId);
        contracts.activateContentVersion(contractId, change.afterContentVersionId());
        log(contractId, "CHANGE_EFFECTIVE", "合同变更生效 " + change.changeTitle());
        return get(contractId, changeId);
    }

    private ContractChange updateApproval(Long contractId, Long changeId, int status) {
        contracts.ensureEditable(contractId);
        ContractChange change = get(contractId, changeId);
        if (status == 1 && change.approvalStatus() != 0) throw bad("当前变更不能重复提交审批");
        if ((status == 2 || status == 3 || status == 4) && change.approvalStatus() != 1) throw bad("当前变更不在审批中");
        jdbc.update("UPDATE contract_change SET approval_status=?,approval_no=COALESCE(approval_no,?),updated_at=? WHERE id=? AND contract_id=?", status, "AP-CG-" + change.id(), Instant.now().toString(), changeId, contractId);
        String action = switch (status) { case 1 -> "CHANGE_SUBMIT"; case 2 -> "CHANGE_APPROVE"; case 3 -> "CHANGE_REJECT"; default -> "CHANGE_WITHDRAW"; };
        String description = switch (status) { case 1 -> "提交变更审批"; case 2 -> "变更审批通过"; case 3 -> "变更审批驳回"; default -> "撤回变更审批"; };
        approvalRecord(contractId, change, status);
        log(contractId, action, description);
        return get(contractId, changeId);
    }

    private void approvalRecord(Long contractId, ContractChange change, int action) {
        long id = jdbc.queryForObject("SELECT COALESCE(MAX(id),8000)+1 FROM t_contract_approval_record", Long.class);
        String now = Instant.now().toString();
        jdbc.update("INSERT INTO t_contract_approval_record(id,contract_id,change_id,approval_no,contract_content_version_id,approval_action,operate_time,create_by,create_time) VALUES(?,?,?,?,?,?,?,?,?)",
                id, contractId, change.id(), "AP-CG-" + change.id(), change.afterContentVersionId(), action, now, 10001, now);
    }

    private ContractChange get(Long contractId, Long changeId) { return list(contractId).stream().filter(item -> item.id().equals(changeId)).findFirst().orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "变更记录不存在")); }
    private long nextId() { return jdbc.queryForObject("SELECT COALESCE(MAX(id),7000)+1 FROM contract_change", Long.class); }
    private void log(Long contractId, String action, String description) { jdbc.update("INSERT INTO contract_operation_log(contract_id,action,description,operated_at) VALUES(?,?,?,?)", contractId, action, description, Instant.now().toString()); }
    private boolean blank(String value) { return value == null || value.isBlank(); }
    private ResponseStatusException bad(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
    private boolean mysqlProfile() { String profiles = System.getenv("SPRING_PROFILES_ACTIVE"); return profiles != null && profiles.contains("mysql"); }
}
