package com.hrcontract.application;

import com.hrcontract.application.ContractModels.ConnectionLog;
import com.hrcontract.application.ContractModels.CreateConnectionRequest;
import com.hrcontract.application.ContractModels.ExternalConnection;
import com.hrcontract.application.ContractModels.UpdateConnectionRequest;
import jakarta.annotation.PostConstruct;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Profile;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

@Service
public class ConnectionService {
    private final JdbcTemplate jdbc;

    public ConnectionService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @PostConstruct
    @Profile("!mysql")
    void initialize() {
        if (mysqlProfile()) return;
        jdbc.execute("CREATE TABLE IF NOT EXISTS t_external_connection (id INTEGER PRIMARY KEY, connection_type TEXT NOT NULL, connection_name TEXT NOT NULL, provider TEXT, base_url TEXT, secret TEXT, status TEXT NOT NULL, last_test_at TEXT, last_error TEXT, create_time TEXT NOT NULL, update_time TEXT NOT NULL, UNIQUE(connection_type))");
        jdbc.execute("CREATE TABLE IF NOT EXISTS t_external_connection_log (id INTEGER PRIMARY KEY, connection_id INTEGER NOT NULL, action TEXT NOT NULL, result TEXT NOT NULL, message TEXT, operated_at TEXT NOT NULL)");
        seed();
    }

    public List<ExternalConnection> list() {
        return jdbc.query("SELECT id,connection_type,connection_name,provider,base_url,status,secret,last_test_at,last_error,update_time FROM t_external_connection ORDER BY id", (rs, row) -> new ExternalConnection(
                rs.getLong("id"), rs.getString("connection_type"), rs.getString("connection_name"), rs.getString("provider"),
                rs.getString("base_url"), rs.getString("status"), rs.getString("secret") != null && !rs.getString("secret").isBlank(),
                rs.getString("last_test_at"), rs.getString("last_error"), Instant.parse(rs.getString("update_time"))));
    }

    public ExternalConnection create(CreateConnectionRequest request) {
        if (request == null || blank(request.connectionType()) || blank(request.name())) throw bad("连接类型和名称不能为空");
        Instant now = Instant.now();
        long id = nextId();
        jdbc.update("INSERT INTO t_external_connection(id,connection_type,connection_name,provider,base_url,secret,status,create_time,update_time) VALUES(?,?,?,?,?,?,?, ?,?)",
                id, request.connectionType().trim(), request.name().trim(), request.provider(), request.baseUrl(), request.secret(), "DISABLED", now.toString(), now.toString());
        log(id, "创建连接", "SUCCESS", "已创建连接配置");
        return find(id);
    }

    public ExternalConnection update(Long id, UpdateConnectionRequest request) {
        ExternalConnection current = find(id);
        if (request == null || blank(request.name())) throw bad("连接名称不能为空");
        String secret = blank(request.secret()) ? null : request.secret();
        if (secret == null) jdbc.update("UPDATE t_external_connection SET connection_name=?,provider=?,base_url=?,update_time=? WHERE id=?", request.name().trim(), request.provider(), request.baseUrl(), Instant.now().toString(), id);
        else jdbc.update("UPDATE t_external_connection SET connection_name=?,provider=?,base_url=?,secret=?,update_time=? WHERE id=?", request.name().trim(), request.provider(), request.baseUrl(), secret, Instant.now().toString(), id);
        log(current.id(), "更新配置", "SUCCESS", "已更新连接配置");
        return find(current.id());
    }

    public ExternalConnection toggle(Long id) {
        ExternalConnection current = find(id);
        String status = "ENABLED".equals(current.status()) ? "DISABLED" : "ENABLED";
        jdbc.update("UPDATE t_external_connection SET status=?,update_time=? WHERE id=?", status, Instant.now().toString(), id);
        log(id, "启用/停用", "SUCCESS", "ENABLED".equals(status) ? "连接已启用" : "连接已停用");
        return find(id);
    }

    public ExternalConnection test(Long id) {
        ExternalConnection current = find(id);
        Instant now = Instant.now();
        boolean success = !blank(current.baseUrl()) && !blank(current.provider());
        String status = success ? ("DISABLED".equals(current.status()) ? "DISABLED" : "ENABLED") : "FAILED";
        String message = success ? "适配器握手成功" : "请先填写服务地址和 Provider";
        jdbc.update("UPDATE t_external_connection SET status=?,last_test_at=?,last_error=?,update_time=? WHERE id=?", status, now.toString(), success ? null : message, now.toString(), id);
        log(id, "测试连接", success ? "SUCCESS" : "FAILED", message);
        return find(id);
    }

    public List<ConnectionLog> logs(Long id) {
        find(id);
        return jdbc.query("SELECT id,action,result,message,operated_at FROM t_external_connection_log WHERE connection_id=? ORDER BY id DESC", (rs, row) -> new ConnectionLog(rs.getLong("id"), rs.getString("action"), rs.getString("result"), rs.getString("message"), Instant.parse(rs.getString("operated_at"))), id);
    }

    private ExternalConnection find(Long id) { return list().stream().filter(item -> item.id().equals(id)).findFirst().orElseThrow(() -> bad("连接不存在")); }
    private ExternalConnection findByType(String type) { return list().stream().filter(item -> item.connectionType().equals(type)).findFirst().orElseThrow(); }
    private void log(Long id, String action, String result, String message) { jdbc.update("INSERT INTO t_external_connection_log(id,connection_id,action,result,message,operated_at) VALUES(?,?,?,?,?,?)", nextLogId(), id, action, result, message, Instant.now().toString()); }
    private long nextId() { return jdbc.queryForObject("SELECT COALESCE(MAX(id),0)+1 FROM t_external_connection", Long.class); }
    private long nextLogId() { return jdbc.queryForObject("SELECT COALESCE(MAX(id),0)+1 FROM t_external_connection_log", Long.class); }
    private boolean blank(String value) { return value == null || value.isBlank(); }
    private ResponseStatusException bad(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }

    private void seed() {
        if (jdbc.queryForObject("SELECT COUNT(*) FROM t_external_connection", Integer.class) > 0) return;
        String now = Instant.now().toString();
        String[][] data = {{"HR_ORG", "HR / 组织系统", "HR Adapter", "http://hr.internal/api"}, {"TRADE_PARTY", "交易对方主数据", "Master Data Adapter", "http://master-data.internal/api"}, {"LEGAL_ENTITY", "我方主体系统", "Legal Entity Adapter", "http://legal-entity.internal/api"}, {"FILE_SERVICE", "文件服务", "Object Storage Adapter", "http://file.internal/api"}, {"APPROVAL", "外部审批系统", "Approval Adapter", "http://approval.internal/api"}, {"AI_PROVIDER", "AI Provider", "Mock AI Adapter", "http://ai.internal/api"}};
        long id = 1;
        for (String[] item : data) jdbc.update("INSERT INTO t_external_connection(id,connection_type,connection_name,provider,base_url,status,create_time,update_time) VALUES(?,?,?,?,?,?,?,?)", id++, item[0], item[1], item[2], item[3], "ENABLED", now, now);
    }
    private boolean mysqlProfile() { String profiles = System.getenv("SPRING_PROFILES_ACTIVE"); return profiles != null && profiles.contains("mysql"); }
}
