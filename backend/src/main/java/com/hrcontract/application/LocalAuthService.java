package com.hrcontract.application;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import java.util.List;
import java.time.Instant;

@Service
public class LocalAuthService {
    private final JdbcTemplate jdbc;
    public LocalAuthService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @PostConstruct
    void init() {
        jdbc.execute("CREATE TABLE IF NOT EXISTS local_user (id BIGINT PRIMARY KEY, login_name VARCHAR(128) NOT NULL UNIQUE, display_name VARCHAR(128) NOT NULL, password_hash VARCHAR(128) NOT NULL, role_code VARCHAR(32) NOT NULL, org_id VARCHAR(64) NOT NULL, department_id VARCHAR(64) NOT NULL, enabled TINYINT NOT NULL DEFAULT 1)");
        // 本地开发种子：确保默认管理员 admin/admin123 始终可用。
        // 历史数据库可能残留旧密码的 admin 行（COUNT>0 导致不再重灌），这里做幂等校正：
        // 缺失则插入；存在但哈希非空且不等于默认值则重置为 admin123（本地 mock 专用，重启会复位）。
        String defaultHash = hash("admin123");
        List<Map<String, Object>> admins = jdbc.query("SELECT password_hash FROM local_user WHERE login_name='admin'", (rs, row) -> Map.of("password_hash", rs.getString("password_hash")));
        if (admins.isEmpty()) {
            jdbc.update("INSERT INTO local_user(id,login_name,display_name,password_hash,role_code,org_id,department_id,enabled) VALUES(?,?,?,?,?,?,?,1)", 10001, "admin", "本地管理员", defaultHash, "ADMIN", "100", "101");
        } else {
            String stored = String.valueOf(admins.get(0).get("password_hash"));
            if (stored == null || stored.isBlank() || !defaultHash.equals(stored)) {
                jdbc.update("UPDATE local_user SET password_hash=?, enabled=1 WHERE login_name='admin'", defaultHash);
            }
        }
    }

    public Map<String, Object> login(String account, String password, HttpSession session) {
        var users = jdbc.query("SELECT id,login_name,display_name,role_code,org_id,department_id FROM local_user WHERE login_name=? AND password_hash=? AND enabled=1", (rs, row) -> Map.<String,Object>of("userId", String.valueOf(rs.getLong("id")), "account", rs.getString("login_name"), "displayName", rs.getString("display_name"), "role", rs.getString("role_code"), "orgId", rs.getString("org_id"), "departmentId", rs.getString("department_id")), account, hash(password));
        if (users.isEmpty()) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "账号或密码不正确");
        Map<String, Object> user = users.get(0); user.forEach(session::setAttribute); session.setMaxInactiveInterval(8 * 60 * 60); return user;
    }
    public Map<String, Object> session(HttpSession session) {
        Object userId = session.getAttribute("userId"); if (userId == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "未登录");
        return Map.of("userId", userId, "account", session.getAttribute("account"), "displayName", session.getAttribute("displayName"), "role", session.getAttribute("role"), "orgId", session.getAttribute("orgId"), "departmentId", session.getAttribute("departmentId"));
    }
    public void logout(HttpSession session) { session.invalidate(); }
    public List<Map<String, Object>> users() { return jdbc.query("SELECT id,login_name,display_name,role_code,org_id,department_id,enabled FROM local_user ORDER BY id", (rs, row) -> Map.of("id", String.valueOf(rs.getLong("id")), "account", rs.getString("login_name"), "displayName", rs.getString("display_name"), "role", rs.getString("role_code"), "orgId", rs.getString("org_id"), "departmentId", rs.getString("department_id"), "enabled", rs.getInt("enabled") == 1)); }
    public Map<String, Object> update(String id, Map<String, Object> body, String operator) { String role = body.getOrDefault("role", "VIEWER").toString().toUpperCase(); if (!List.of("ADMIN", "LEGAL", "BUSINESS", "VIEWER").contains(role)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "角色无效"); int changed = jdbc.update("UPDATE local_user SET display_name=?,role_code=?,org_id=?,department_id=?,enabled=? WHERE id=?", body.getOrDefault("displayName", "").toString().trim(), role, body.getOrDefault("orgId", "100").toString(), body.getOrDefault("departmentId", "101").toString(), Boolean.TRUE.equals(body.get("enabled")) ? 1 : 0, id); if (changed == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在"); audit(operator, "UPDATE_USER", "更新用户 " + id); return users().stream().filter(item -> item.get("id").equals(id)).findFirst().orElseThrow(); }
    public void resetPassword(String id, String password, String operator) { if (password == null || password.length() < 6) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "密码至少 6 位"); if (jdbc.update("UPDATE local_user SET password_hash=? WHERE id=?", hash(password), id) == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在"); audit(operator, "RESET_PASSWORD", "重置用户 " + id + " 密码"); }
    private void audit(String operator, String action, String description) { jdbc.update("INSERT INTO sys_contract_audit_log(id,module,action,result,operator_name,description,operated_at) VALUES((SELECT COALESCE(MAX(id),0)+1 FROM sys_contract_audit_log),?,?,?,?,?,?)", "系统管理", action, "SUCCESS", operator, description, Instant.now().toString()); }
    private String hash(String value) { try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); } catch (Exception e) { throw new IllegalStateException(e); } }
}
