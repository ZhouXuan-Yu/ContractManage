package com.hrcontract.application;

import com.hrcontract.application.ContractModels.CategoryNode;
import com.hrcontract.application.ContractModels.CreateCategoryRequest;
import com.hrcontract.application.ContractModels.UpdateCategoryRequest;
import jakarta.annotation.PostConstruct;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Profile;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

@Service
public class CategoryService {
    private final JdbcTemplate jdbc;
    public CategoryService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @PostConstruct
    @Profile("!mysql")
    void initializeSchema() {
        if (mysqlProfile()) return;
        jdbc.execute("CREATE TABLE IF NOT EXISTS t_contract_category (id INTEGER PRIMARY KEY, parent_id INTEGER, category_code TEXT NOT NULL UNIQUE, category_name TEXT NOT NULL, category_level INTEGER NOT NULL, is_enable INTEGER NOT NULL DEFAULT 1, description TEXT, create_time TEXT NOT NULL, update_time TEXT NOT NULL, deleted INTEGER NOT NULL DEFAULT 0)");
        jdbc.execute("CREATE TABLE IF NOT EXISTS t_contract_category_version (id INTEGER PRIMARY KEY, category_id INTEGER NOT NULL, version_no INTEGER NOT NULL, version_status INTEGER NOT NULL DEFAULT 0, change_summary TEXT, published_time TEXT, create_time TEXT NOT NULL, UNIQUE(category_id,version_no))");
        seedSafe();
    }

    public List<CategoryNode> list() {
        return jdbc.query("SELECT c.id,c.parent_id,c.category_code,c.category_name,c.category_level,c.is_enable,c.description,COALESCE(MAX(v.version_no),1) version_no,COALESCE(MAX(v.version_status),0) version_status,c.update_time FROM t_contract_category c LEFT JOIN t_contract_category_version v ON v.category_id=c.id WHERE c.deleted=0 GROUP BY c.id ORDER BY c.category_level,c.parent_id,c.id", (rs, row) -> new CategoryNode(rs.getLong("id"), (Long) rs.getObject("parent_id"), rs.getString("category_code"), rs.getString("category_name"), rs.getInt("category_level"), rs.getInt("is_enable") == 1, rs.getInt("version_no"), versionName(rs.getInt("version_status")), rs.getString("description"), Instant.parse(rs.getString("update_time"))));
    }

    public CategoryNode create(CreateCategoryRequest request) {
        if (request == null || blank(request.code()) || blank(request.name()) || request.level() < 1 || request.level() > 3) throw bad("分类编码、名称和层级不能为空");
        if (request.level() > 1 && request.parentId() == null) throw bad("类型和子类型必须选择上级分类");
        long id = nextId(); Instant now = Instant.now();
        jdbc.update("INSERT INTO t_contract_category(id,parent_id,category_code,category_name,category_level,is_enable,description,create_time,update_time) VALUES(?,?,?,?,?,1,?,?,?)", id, request.parentId(), request.code().trim(), request.name().trim(), request.level(), request.description(), now.toString(), now.toString());
        jdbc.update("INSERT INTO t_contract_category_version(id,category_id,version_no,version_status,create_time) VALUES(?,?,?,?,?)", id + 10000, id, 1, 0, now.toString());
        return list().stream().filter(item -> item.id().equals(id)).findFirst().orElseThrow();
    }

    public CategoryNode update(Long id, UpdateCategoryRequest request) { require(id); if (request == null || blank(request.name())) throw bad("分类名称不能为空"); jdbc.update("UPDATE t_contract_category SET category_name=?,description=?,update_time=? WHERE id=?", request.name().trim(), request.description(), Instant.now().toString(), id); return list().stream().filter(item -> item.id().equals(id)).findFirst().orElseThrow(); }
    public CategoryNode toggle(Long id) { CategoryNode node = list().stream().filter(item -> item.id().equals(id)).findFirst().orElseThrow(() -> bad("分类不存在")); jdbc.update("UPDATE t_contract_category SET is_enable=?,update_time=? WHERE id=?", node.enabled() ? 0 : 1, Instant.now().toString(), id); return list().stream().filter(item -> item.id().equals(id)).findFirst().orElseThrow(); }
    public CategoryNode publish(Long id) { CategoryNode node = list().stream().filter(item -> item.id().equals(id)).findFirst().orElseThrow(() -> bad("分类不存在")); Instant now = Instant.now(); jdbc.update("UPDATE t_contract_category_version SET version_status=2,published_time=? WHERE category_id=? AND version_no=?", now.toString(), id, node.versionNo()); return list().stream().filter(item -> item.id().equals(id)).findFirst().orElseThrow(); }
    private void seed() { if (jdbc.queryForObject("SELECT COUNT(*) FROM t_contract_category", Integer.class) > 0) return; Instant now = Instant.now(); jdbc.update("INSERT INTO t_contract_category(id,category_code,category_name,category_level,is_enable,description,create_time,update_time) VALUES(1,'EMPLOYMENT','用工与人员类',1,1,'劳动、劳务和顾问相关合同',?,?)", now.toString(), now.toString()); jdbc.update("INSERT INTO t_contract_category(id,parent_id,category_code,category_name,category_level,is_enable,description,create_time,update_time) VALUES(2,1,'LABOR','劳动合同',2,1,'员工劳动关系合同',?,?)", now.toString(), now.toString()); jdbc.update("INSERT INTO t_contract_category(id,parent_id,category_code,category_name,category_level,is_enable,description,create_time,update_time) VALUES(3,'TRADE','交易合作类',1,1,'采购、销售和服务相关合同',?,?)", now.toString(), now.toString()); jdbc.update("INSERT INTO t_contract_category(id,parent_id,category_code,category_name,category_level,is_enable,description,create_time,update_time) VALUES(4,3,'SERVICE','服务合同',2,1,'通用服务合作合同',?,?)", now.toString(), now.toString()); for (long id : List.of(1L,2L,3L,4L)) jdbc.update("INSERT INTO t_contract_category_version(id,category_id,version_no,version_status,create_time) VALUES(?,?,?,?,?)", id + 10000, id, 1, 2, now.toString()); }
    private void seedSafe() {
        if (jdbc.queryForObject("SELECT COUNT(*) FROM t_contract_category", Integer.class) > 0) return;
        String time = Instant.now().toString();
        String sql = "INSERT INTO t_contract_category(id,parent_id,category_code,category_name,category_level,is_enable,description,create_time,update_time) VALUES(?,?,?,?,?,?,?,?,?)";
        jdbc.update(sql, 1, null, "EMPLOYMENT", "用工与人员类", 1, 1, "劳动、劳务和顾问相关合同", time, time);
        jdbc.update(sql, 2, 1, "LABOR", "劳动合同", 2, 1, "员工劳动关系合同", time, time);
        jdbc.update(sql, 3, null, "TRADE", "交易合作类", 1, 1, "采购、销售和服务相关合同", time, time);
        jdbc.update(sql, 4, 3, "SERVICE", "服务合同", 2, 1, "通用服务合作合同", time, time);
        for (long id : List.of(1L, 2L, 3L, 4L)) jdbc.update("INSERT INTO t_contract_category_version(id,category_id,version_no,version_status,create_time) VALUES(?,?,?,?,?)", id + 10000, id, 1, 2, time);
    }

    private void require(Long id) { if (list().stream().noneMatch(item -> item.id().equals(id))) throw bad("分类不存在"); }
    private long nextId() { return jdbc.queryForObject("SELECT COALESCE(MAX(id),100)+1 FROM t_contract_category", Long.class); }
    private String versionName(int status) { return status == 2 ? "已发布" : status == 1 ? "草稿" : "未发布"; }
    private boolean blank(String value) { return value == null || value.isBlank(); }
    private ResponseStatusException bad(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
    private boolean mysqlProfile() { String profiles = System.getenv("SPRING_PROFILES_ACTIVE"); return profiles != null && profiles.contains("mysql"); }
}
