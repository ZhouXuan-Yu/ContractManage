package com.hrcontract.application;

import com.hrcontract.application.ContractModels.ContractType;
import com.hrcontract.application.ContractModels.CreateTemplateRequest;
import com.hrcontract.application.ContractModels.CreateTemplateVersionRequest;
import com.hrcontract.application.ContractModels.TemplateDetail;
import com.hrcontract.application.ContractModels.TemplateSummary;
import jakarta.annotation.PostConstruct;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Profile;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TemplateService {
    private static final Pattern VARIABLE = Pattern.compile("\\{\\{\\s*([a-zA-Z0-9_.-]+)\\s*}} ".trim());
    private final JdbcTemplate jdbc;
    private final ContractService contracts;

    public TemplateService(JdbcTemplate jdbc, ContractService contracts) {
        this.jdbc = jdbc;
        this.contracts = contracts;
    }

    @PostConstruct
    @Profile("!mysql")
    void initializeSchema() {
        if (mysqlProfile()) return;
        jdbc.execute("CREATE TABLE IF NOT EXISTS contract_template (id INTEGER PRIMARY KEY, name TEXT NOT NULL, type_id INTEGER NOT NULL, status TEXT NOT NULL, current_version_no INTEGER NOT NULL DEFAULT 0, updated_at TEXT NOT NULL)");
        jdbc.execute("CREATE TABLE IF NOT EXISTS contract_template_version (id INTEGER PRIMARY KEY, template_id INTEGER NOT NULL, version_no INTEGER NOT NULL, content TEXT NOT NULL, created_at TEXT NOT NULL, UNIQUE(template_id, version_no))");
    }

    public List<TemplateSummary> list() {
        return jdbc.query("SELECT t.id,t.name,t.type_id,t.status,t.current_version_no,t.updated_at FROM contract_template t ORDER BY t.updated_at DESC", (rs, row) -> {
            long typeId = rs.getLong("type_id");
            ContractType type = contracts.listTypes().stream().filter(item -> item.id().equals(typeId)).findFirst().orElse(null);
            return new TemplateSummary(rs.getLong("id"), rs.getString("name"), typeId, type == null ? "" : type.name(),
                    rs.getString("status"), rs.getInt("current_version_no"), Instant.parse(rs.getString("updated_at")));
        });
    }

    public TemplateDetail get(Long id) {
        try {
            return jdbc.queryForObject("SELECT t.id,t.name,t.type_id,t.status,t.current_version_no,v.content,t.updated_at FROM contract_template t LEFT JOIN contract_template_version v ON v.template_id=t.id AND v.version_no=t.current_version_no WHERE t.id=?", (rs, row) -> {
                long typeId = rs.getLong("type_id");
                ContractType type = contracts.listTypes().stream().filter(item -> item.id().equals(typeId)).findFirst().orElse(null);
                String content = rs.getString("content");
                return new TemplateDetail(rs.getLong("id"), rs.getString("name"), typeId, type == null ? "" : type.name(),
                        rs.getString("status"), rs.getInt("current_version_no"), content,
                        variables(content), Instant.parse(rs.getString("updated_at")));
            }, id);
        } catch (EmptyResultDataAccessException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Template not found");
        }
    }

    public TemplateDetail create(CreateTemplateRequest request) {
        validate(request.name(), request.typeId(), request.content());
        long id = jdbc.queryForObject("SELECT COALESCE(MAX(id),7000)+1 FROM contract_template", Long.class);
        Instant now = Instant.now();
        jdbc.update("INSERT INTO contract_template(id,name,type_id,status,current_version_no,updated_at) VALUES(?,?,?,?,?,?)", id, request.name().trim(), request.typeId(), "DRAFT", 1, now.toString());
        jdbc.update("INSERT INTO contract_template_version(id,template_id,version_no,content,created_at) VALUES(?,?,?,?,?)", id + 100000, id, 1, request.content(), now.toString());
        return get(id);
    }

    public TemplateDetail addVersion(Long id, CreateTemplateVersionRequest request) {
        get(id);
        if (request == null || request.content() == null || request.content().isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Template content is required");
        int next = jdbc.queryForObject("SELECT COALESCE(MAX(version_no),0)+1 FROM contract_template_version WHERE template_id=?", Integer.class, id);
        long versionId = jdbc.queryForObject("SELECT COALESCE(MAX(id),8000)+1 FROM contract_template_version", Long.class);
        Instant now = Instant.now();
        jdbc.update("INSERT INTO contract_template_version(id,template_id,version_no,content,created_at) VALUES(?,?,?,?,?)", versionId, id, next, request.content(), now.toString());
        jdbc.update("UPDATE contract_template SET current_version_no=?,status='DRAFT',updated_at=? WHERE id=?", next, now.toString(), id);
        return get(id);
    }

    public TemplateDetail publish(Long id) {
        TemplateDetail template = get(id);
        if (template.content() == null || template.content().isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Template has no content");
        jdbc.update("UPDATE contract_template SET status='PUBLISHED',updated_at=? WHERE id=?", Instant.now().toString(), id);
        return get(id);
    }

    public TemplateDetail toggle(Long id) {
        TemplateDetail template = get(id);
        String next = "DISABLED".equals(template.status()) ? "DRAFT" : "DISABLED";
        jdbc.update("UPDATE contract_template SET status=?,updated_at=? WHERE id=?", next, Instant.now().toString(), id);
        return get(id);
    }

    public List<TemplateDetail> versions(Long id) {
        get(id);
        return jdbc.query("SELECT t.id,t.name,t.type_id,t.status,v.version_no,v.content,t.updated_at FROM contract_template t JOIN contract_template_version v ON v.template_id=t.id WHERE t.id=? ORDER BY v.version_no DESC", (rs, row) -> {
            long typeId = rs.getLong("type_id");
            ContractType type = contracts.listTypes().stream().filter(item -> item.id().equals(typeId)).findFirst().orElse(null);
            return new TemplateDetail(rs.getLong("id"), rs.getString("name"), typeId, type == null ? "" : type.name(), rs.getString("status"), rs.getInt("version_no"), rs.getString("content"), variables(rs.getString("content")), Instant.parse(rs.getString("updated_at")));
        }, id);
    }

    public String render(Long id, String contractName, String contractNo, String partyName) {
        TemplateDetail template = get(id);
        if (!"PUBLISHED".equals(template.status())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only published templates can be used");
        String content = template.content();
        return content.replace("{{contract.name}}", value(contractName)).replace("{{contract.no}}", value(contractNo)).replace("{{party.name}}", value(partyName));
    }

    public Long currentVersionId(Long templateId) {
        TemplateDetail template = get(templateId);
        return jdbc.query("SELECT id FROM contract_template_version WHERE template_id=? AND version_no=?", (rs, row) -> rs.getLong(1), templateId, template.versionNo())
                .stream().findFirst().orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "模板当前版本不存在"));
    }

    private void validate(String name, Long typeId, String content) {
        if (name == null || name.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Template name is required");
        if (typeId == null || contracts.listTypes().stream().noneMatch(item -> item.id().equals(typeId))) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Contract type is invalid");
        if (content == null || content.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Template content is required");
    }

    private List<String> variables(String content) {
        if (content == null) return List.of();
        Matcher matcher = VARIABLE.matcher(content);
        List<String> result = new ArrayList<>();
        while (matcher.find() && !result.contains(matcher.group(1))) result.add(matcher.group(1));
        return result;
    }

    private String value(String value) { return value == null ? "" : value; }
    private boolean mysqlProfile() { String profiles = System.getenv("SPRING_PROFILES_ACTIVE"); return profiles != null && profiles.contains("mysql"); }
}
