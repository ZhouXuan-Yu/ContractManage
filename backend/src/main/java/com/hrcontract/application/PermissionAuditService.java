package com.hrcontract.application;

import com.hrcontract.application.ContractModels.GlobalAuditEntry;
import com.hrcontract.application.ContractModels.PermissionRole;
import com.hrcontract.application.ContractModels.UpdatePermissionRoleRequest;
import jakarta.annotation.PostConstruct;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Profile;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

@Service
public class PermissionAuditService {
    private final JdbcTemplate jdbc;
    private static final Set<String> VALID_PERMISSIONS = Set.of("VIEW", "EDIT", "APPROVE", "FULFILL", "TEMPLATE", "EXPORT", "SENSITIVE");
    private static final Set<String> VALID_SCOPES = Set.of("ALL", "ORGANIZATION", "DEPARTMENT", "SELF_AND_PARTICIPANTS", "NAMED_CONTRACTS");

    public PermissionAuditService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @PostConstruct
    @Profile("!mysql")
    void initialize() {
        if (mysqlProfile()) return;
        jdbc.execute("CREATE TABLE IF NOT EXISTS sys_contract_role_permission (role_code TEXT PRIMARY KEY, role_name TEXT NOT NULL, description TEXT, permissions TEXT NOT NULL, data_scope TEXT NOT NULL, type_ids TEXT NOT NULL DEFAULT '', sensitive_field_access INTEGER NOT NULL DEFAULT 0, updated_at TEXT NOT NULL)");
        jdbc.execute("CREATE TABLE IF NOT EXISTS sys_contract_audit_log (id INTEGER PRIMARY KEY, module TEXT NOT NULL, action TEXT NOT NULL, result TEXT NOT NULL, operator_name TEXT NOT NULL, description TEXT, operated_at TEXT NOT NULL)");
        seed();
    }

    public List<PermissionRole> roles() {
        return jdbc.query("SELECT role_code,role_name,description,permissions,data_scope,type_ids,sensitive_field_access FROM sys_contract_role_permission ORDER BY role_code", (rs, row) -> new PermissionRole(
                rs.getString("role_code"), rs.getString("role_name"), rs.getString("description"), split(rs.getString("permissions")),
                rs.getString("data_scope"), splitIds(rs.getString("type_ids")), rs.getInt("sensitive_field_access") == 1));
    }

    public PermissionRole role(String code) { return roles().stream().filter(item -> item.code().equals(code)).findFirst().orElseThrow(() -> bad("角色不存在")); }

    public PermissionRole update(String code, UpdatePermissionRoleRequest request, String operator) {
        if (request == null || request.permissions() == null || request.permissions().stream().anyMatch(item -> !VALID_PERMISSIONS.contains(item))) throw bad("权限配置不合法");
        if (!VALID_SCOPES.contains(request.dataScope())) throw bad("数据范围不合法");
        role(code);
        String permissions = String.join(",", request.permissions());
        String typeIds = request.typeIds() == null ? "" : request.typeIds().stream().map(String::valueOf).reduce((left, right) -> left + "," + right).orElse("");
        jdbc.update("UPDATE sys_contract_role_permission SET permissions=?,data_scope=?,type_ids=?,sensitive_field_access=?,updated_at=? WHERE role_code=?", permissions, request.dataScope(), typeIds, request.sensitiveFieldAccess() ? 1 : 0, Instant.now().toString(), code);
        audit("权限与审计", "更新角色权限", "SUCCESS", operator, "已更新角色 " + code + " 的权限与数据范围");
        return role(code);
    }

    public List<GlobalAuditEntry> audit(String module, String keyword) {
        String query = "SELECT id,module,action,result,operator_name,description,operated_at FROM sys_contract_audit_log";
        List<GlobalAuditEntry> configured = jdbc.query(query + " ORDER BY operated_at DESC", (rs, row) -> new GlobalAuditEntry(rs.getLong("id"), rs.getString("module"), rs.getString("action"), rs.getString("result"), rs.getString("operator_name"), rs.getString("description"), Instant.parse(rs.getString("operated_at"))));
        List<GlobalAuditEntry> contracts = jdbc.query("SELECT id,action,description,operated_at FROM contract_operation_log ORDER BY operated_at DESC", (rs, row) -> new GlobalAuditEntry(1000000L + rs.getLong("id"), "合同", rs.getString("action"), "SUCCESS", "系统操作人", rs.getString("description"), Instant.parse(rs.getString("operated_at"))));
        return java.util.stream.Stream.concat(configured.stream(), contracts.stream()).filter(item -> (module == null || module.isBlank() || "ALL".equals(module) || item.module().equals(module)) && (keyword == null || keyword.isBlank() || (item.action() + item.description() + item.operator()).toLowerCase().contains(keyword.toLowerCase()))).sorted((left, right) -> right.operatedAt().compareTo(left.operatedAt())).limit(200).toList();
    }

    public void audit(String module, String action, String result, String operator, String description) {
        long id = jdbc.queryForObject("SELECT COALESCE(MAX(id),0)+1 FROM sys_contract_audit_log", Long.class);
        jdbc.update("INSERT INTO sys_contract_audit_log(id,module,action,result,operator_name,description,operated_at) VALUES(?,?,?,?,?,?,?)", id, module, action, result, operator, description, Instant.now().toString());
    }

    private void seed() {
        if (jdbc.queryForObject("SELECT COUNT(*) FROM sys_contract_role_permission", Integer.class) > 0) return;
        String now = Instant.now().toString();
        insert("ADMIN", "系统管理员", "系统配置和全量合同管理", "VIEW,EDIT,APPROVE,FULFILL,TEMPLATE,EXPORT,SENSITIVE", "ALL", "1,2,3,4", 1, now);
        insert("LEGAL", "法务人员", "合同审查、审批和模板维护", "VIEW,EDIT,APPROVE,TEMPLATE,EXPORT,SENSITIVE", "ORGANIZATION", "1,2,3,4", 1, now);
        insert("BUSINESS", "业务人员", "本人及部门合同处理", "VIEW,EDIT,FULFILL,EXPORT", "SELF_AND_PARTICIPANTS", "3,4", 0, now);
        insert("VIEWER", "只读查看者", "只读查看被授权合同", "VIEW", "NAMED_CONTRACTS", "", 0, now);
    }

    private void insert(String code, String name, String description, String permissions, String scope, String typeIds, int sensitive, String now) { jdbc.update("INSERT INTO sys_contract_role_permission(role_code,role_name,description,permissions,data_scope,type_ids,sensitive_field_access,updated_at) VALUES(?,?,?,?,?,?,?,?)", code, name, description, permissions, scope, typeIds, sensitive, now); }
    private List<String> split(String value) { return value == null || value.isBlank() ? List.of() : Arrays.asList(value.split(",")); }
    private List<Long> splitIds(String value) { return value == null || value.isBlank() ? List.of() : Arrays.stream(value.split(",")).map(Long::valueOf).toList(); }
    private ResponseStatusException bad(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
    private boolean mysqlProfile() { String profiles = System.getenv("SPRING_PROFILES_ACTIVE"); return profiles != null && profiles.contains("mysql"); }
}
