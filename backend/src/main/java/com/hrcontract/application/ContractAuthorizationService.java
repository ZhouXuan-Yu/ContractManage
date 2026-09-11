package com.hrcontract.application;

import jakarta.annotation.PostConstruct;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Profile;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;

/**
 * Contract-level authorization. The authority table is t_contract_object_auth (DOCX-aligned):
 * auth_target_type/auth_target_id replace the legacy target_user_id so the schema matches V1.
 * The public API keeps the targetUserId string for backward compatibility; internally it is
 * stored as auth_target_type=1 (user) + auth_target_id.
 */
@Service
public class ContractAuthorizationService {
    private static final int TARGET_USER = 1;
    private final JdbcTemplate jdbc;
    public ContractAuthorizationService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @PostConstruct @Profile("!mysql")
    void init() {
        String profiles = System.getenv("SPRING_PROFILES_ACTIVE");
        if (profiles != null && profiles.contains("mysql")) return;
        jdbc.execute("CREATE TABLE IF NOT EXISTS t_contract_object_auth (id INTEGER PRIMARY KEY, contract_id INTEGER NOT NULL, auth_target_type INTEGER NOT NULL, auth_target_id INTEGER NOT NULL, perm_view INTEGER NOT NULL DEFAULT 1, perm_edit INTEGER NOT NULL DEFAULT 0, perm_export INTEGER NOT NULL DEFAULT 0, perm_legal_confirm INTEGER NOT NULL DEFAULT 0, auth_status INTEGER NOT NULL DEFAULT 1, auth_start_time TEXT, auth_expire_time TEXT, revoked_by INTEGER, revoked_time TEXT, auth_remark TEXT, create_by INTEGER, create_time TEXT NOT NULL, update_by INTEGER, update_time TEXT, deleted INTEGER NOT NULL DEFAULT 0)");
        migrateLegacyAuth();
    }

    /** One-time migration for pre-existing SQLite tables that still use target_user_id. */
    private void migrateLegacyAuth() {
        if (!hasColumn("t_contract_object_auth", "target_user_id")) return;
        addColumn("t_contract_object_auth", "auth_target_type", "INTEGER NOT NULL DEFAULT 0");
        addColumn("t_contract_object_auth", "auth_target_id", "INTEGER NOT NULL DEFAULT 0");
        addColumn("t_contract_object_auth", "auth_start_time", "TEXT");
        addColumn("t_contract_object_auth", "revoked_by", "INTEGER");
        addColumn("t_contract_object_auth", "revoked_time", "TEXT");
        addColumn("t_contract_object_auth", "create_by", "INTEGER");
        addColumn("t_contract_object_auth", "update_by", "INTEGER");
        addColumn("t_contract_object_auth", "update_time", "TEXT");
        addColumn("t_contract_object_auth", "deleted", "INTEGER NOT NULL DEFAULT 0");
        jdbc.update("UPDATE t_contract_object_auth SET auth_target_type=1, auth_target_id=CAST(target_user_id AS INTEGER) WHERE target_user_id IS NOT NULL AND target_user_id <> ''");
    }

    public boolean allowed(Long contractId, String userId, String permission) {
        long targetId;
        try { targetId = Long.parseLong(userId); }
        catch (NumberFormatException exception) { return false; }
        String column = switch (permission) { case "EDIT" -> "perm_edit"; case "EXPORT" -> "perm_export"; case "APPROVE" -> "perm_legal_confirm"; default -> "perm_view"; };
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM t_contract_object_auth WHERE contract_id=? AND auth_target_type=? AND auth_target_id=? AND auth_status=1 AND deleted=0 AND " + column + "=1 AND (auth_expire_time IS NULL OR auth_expire_time>?)", Integer.class, contractId, TARGET_USER, targetId, dbNow());
        return count != null && count > 0;
    }

    public java.util.List<ContractModels.ContractAuthorization> list(Long contractId) {
        return jdbc.query("SELECT id,contract_id,auth_target_id,perm_view,perm_edit,perm_export,perm_legal_confirm,auth_status,auth_expire_time,auth_remark FROM t_contract_object_auth WHERE contract_id=? AND deleted=0 ORDER BY id DESC", (rs, row) -> new ContractModels.ContractAuthorization(rs.getLong(1), rs.getLong(2), String.valueOf(rs.getLong(3)), rs.getInt(4) == 1, rs.getInt(5) == 1, rs.getInt(6) == 1, rs.getInt(7) == 1, rs.getInt(8), rs.getString(9), rs.getString(10)), contractId);
    }

    public ContractModels.ContractAuthorization create(ContractModels.CreateContractAuthorizationRequest r) {
        if (r == null || r.contractId() == null || r.targetUserId() == null || r.targetUserId().isBlank() || (!r.view() && !r.edit() && !r.export() && !r.legalConfirm())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "授权信息不完整");
        long targetId;
        try { targetId = Long.parseLong(r.targetUserId().trim()); }
        catch (NumberFormatException exception) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "授权对象必须是有效的用户ID"); }
        long id = jdbc.queryForObject("SELECT COALESCE(MAX(id),0)+1 FROM t_contract_object_auth", Long.class);
        String now = dbNow();
        jdbc.update("INSERT INTO t_contract_object_auth(id,contract_id,auth_target_type,auth_target_id,perm_view,perm_edit,perm_export,perm_legal_confirm,auth_status,auth_start_time,auth_expire_time,auth_remark,create_by,create_time) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)", id, r.contractId(), TARGET_USER, targetId, r.view() ? 1 : 0, r.edit() ? 1 : 0, r.export() ? 1 : 0, r.legalConfirm() ? 1 : 0, 1, now, r.expireTime(), r.remark(), 10001L, now);
        return list(r.contractId()).stream().filter(a -> a.id().equals(id)).findFirst().orElseThrow();
    }

    public void revoke(Long id) {
        if (jdbc.update("UPDATE t_contract_object_auth SET auth_status=2,revoked_by=10001,revoked_time=? WHERE id=?", dbNow(), id) == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "授权不存在");
    }

    private String dbNow() {
        return java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(java.time.ZoneOffset.UTC).format(Instant.now());
    }

    private boolean hasColumn(String table, String column) {
        try { jdbc.queryForObject("SELECT " + column + " FROM " + table + " LIMIT 1", String.class); return true; }
        catch (RuntimeException exception) { return false; }
    }
    private void addColumn(String table, String column, String definition) {
        try { jdbc.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition); } catch (RuntimeException ignored) { }
    }
}
