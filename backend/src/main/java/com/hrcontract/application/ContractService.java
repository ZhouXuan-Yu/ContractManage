package com.hrcontract.application;

import com.hrcontract.application.ContractModels.AuditEntry;
import com.hrcontract.application.ContractModels.ContractDetail;
import com.hrcontract.application.ContractModels.ContractSummary;
import com.hrcontract.application.ContractModels.ContractType;
import com.hrcontract.application.ContractModels.CreateContractRequest;
import com.hrcontract.application.ContractModels.TradeParty;
import com.hrcontract.application.ContractModels.ConfigParty;
import com.hrcontract.application.ContractModels.UpdateDraftRequest;
import com.hrcontract.application.ContractModels.ContentVersion;
import com.hrcontract.application.ContractModels.Attachment;
import jakarta.annotation.PostConstruct;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Profile;
import org.springframework.web.server.ResponseStatusException;
import com.hrcontract.integration.FileStorageProvider;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.sql.ResultSet;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Service
public class ContractService {
    private final JdbcTemplate jdbc;
    private final FileStorageProvider fileStorage;
    private final DocumentTextExtractionService documentText;
    private final List<ContractType> types = List.of(
            new ContractType(1L, "用工与人员类", "劳动合同", "通用", true),
            new ContractType(2L, "用工与人员类", "劳务协议", "通用", true),
            new ContractType(3L, "用工与人员类", "顾问协议", "通用", true),
            new ContractType(4L, "交易合作类", "采购合同", "通用", true),
            new ContractType(5L, "交易合作类", "销售合同", "通用", true),
            new ContractType(6L, "交易合作类", "服务合同", "通用", true));
    private final List<TradeParty> parties = List.of(
            new TradeParty("EMP-001", "张明（员工）", "员工", "HR", "个人"),
            new TradeParty("CUS-001", "华辰科技有限公司", "客户", "交易对方系统", "企业"),
            new TradeParty("SUP-001", "远景供应链有限公司", "供应商", "交易对方系统", "企业"));

    public ContractService(JdbcTemplate jdbc, FileStorageProvider fileStorage, DocumentTextExtractionService documentText) {
        this.jdbc = jdbc;
        this.fileStorage = fileStorage;
        this.documentText = documentText;
    }

    @PostConstruct
    @Profile("!mysql")
    void initializeSchema() {
        if (mysqlProfile()) return;
        try {
            Files.createDirectories(Path.of("data"));
        } catch (Exception exception) {
            throw new IllegalStateException("无法创建本地数据库目录", exception);
        }
        jdbc.execute("CREATE TABLE IF NOT EXISTS contract_draft (" +
                "id INTEGER PRIMARY KEY, contract_no TEXT NOT NULL UNIQUE, name TEXT NOT NULL, " +
                "type_id INTEGER NOT NULL, party_id TEXT NOT NULL, remark TEXT, version INTEGER NOT NULL, " +
                "updated_at TEXT NOT NULL)");
        try { jdbc.execute("ALTER TABLE contract_draft ADD COLUMN status TEXT NOT NULL DEFAULT '草稿'"); } catch (Exception ignored) { }
        ensureColumn("contract_draft", "contract_status", "INTEGER NOT NULL DEFAULT 0");
        ensureColumn("contract_draft", "approval_status", "INTEGER NOT NULL DEFAULT 0");
        ensureColumn("contract_draft", "sign_date", "TEXT");
        ensureColumn("contract_draft", "effective_date", "TEXT");
        ensureColumn("contract_draft", "signed_file_id", "INTEGER");
        ensureColumn("contract_draft", "current_content_version_id", "INTEGER");
        ensureColumn("contract_draft", "template_version_id", "INTEGER");
        ensureColumn("contract_draft", "total_amount", "NUMERIC");
        ensureColumn("contract_draft", "currency", "TEXT NOT NULL DEFAULT 'CNY'");
        ensureColumn("contract_draft", "payment_direction", "INTEGER");
        ensureColumn("contract_draft", "expire_date", "TEXT");
        ensureColumn("contract_draft", "source_type_code", "INTEGER NOT NULL DEFAULT 3");
        ensureColumn("contract_draft", "applicant_id", "TEXT NOT NULL DEFAULT '10001'");
        ensureColumn("contract_draft", "applicant_org_id", "TEXT NOT NULL DEFAULT '100'");
        ensureColumn("contract_draft", "applicant_department_id", "TEXT NOT NULL DEFAULT '101'");
        ensureColumn("contract_draft", "archived", "INTEGER NOT NULL DEFAULT 0");
        ensureColumn("contract_draft", "archived_at", "TEXT");
        jdbc.update("UPDATE contract_draft SET contract_status=CASE status WHEN '待签署' THEN 2 WHEN '履行中' THEN 3 WHEN '已完成' THEN 4 WHEN '已终止' THEN 5 WHEN '已作废' THEN 6 ELSE 0 END");
        jdbc.execute("CREATE TABLE IF NOT EXISTS contract_party_ref (" +
                "contract_id INTEGER NOT NULL, party_id TEXT NOT NULL, relation_type TEXT NOT NULL, " +
                "source_type TEXT NOT NULL, party_name TEXT NOT NULL, PRIMARY KEY(contract_id, party_id))");
        jdbc.execute("CREATE TABLE IF NOT EXISTS contract_party_master (" +
                "id TEXT PRIMARY KEY, party_name TEXT NOT NULL, relation_type TEXT NOT NULL, " +
                "source_type TEXT NOT NULL, nature TEXT NOT NULL, credit_code TEXT, contact_name TEXT, " +
                "contact_phone TEXT, enabled INTEGER NOT NULL DEFAULT 1, remark TEXT, created_at TEXT NOT NULL, updated_at TEXT NOT NULL)");
        seedParties();
            jdbc.execute("CREATE TABLE IF NOT EXISTS contract_operation_log (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, contract_id INTEGER NOT NULL, action TEXT NOT NULL, " +
                "description TEXT NOT NULL, operated_at TEXT NOT NULL)");
        jdbc.execute("CREATE TABLE IF NOT EXISTS contract_content_version (" +
                "id INTEGER PRIMARY KEY, contract_id INTEGER NOT NULL, version_no INTEGER NOT NULL, " +
                "content TEXT NOT NULL, content_hash TEXT NOT NULL, source TEXT NOT NULL, " +
                "is_current INTEGER NOT NULL DEFAULT 1, created_at TEXT NOT NULL, UNIQUE(contract_id,version_no))");
        jdbc.execute("CREATE TABLE IF NOT EXISTS contract_attachment (" +
                "id INTEGER PRIMARY KEY, contract_id INTEGER NOT NULL, file_name TEXT NOT NULL, " +
                "file_size INTEGER NOT NULL, file_ref TEXT NOT NULL UNIQUE, storage_path TEXT NOT NULL, " +
                "content_version_id INTEGER, uploaded_at TEXT NOT NULL)");
        ensureColumn("contract_attachment", "attach_type", "INTEGER");
        ensureColumn("contract_attachment", "biz_attach_type", "INTEGER");
        ensureColumn("contract_attachment", "storage_object_id", "TEXT");
        ensureColumn("contract_attachment", "file_hash", "TEXT");
        ensureColumn("contract_attachment", "mime_type", "TEXT");
        ensureColumn("contract_attachment", "is_main_file", "INTEGER NOT NULL DEFAULT 0");
    }

    public List<ContractType> listTypes() {
        return types.stream().filter(ContractType::enabled).sorted(Comparator.comparing(ContractType::id)).toList();
    }

    public List<TradeParty> searchParties(String keyword) {
        String value = keyword == null ? "" : keyword.trim();
        try {
            return jdbc.query("SELECT id,party_name,relation_type,source_type,nature FROM contract_party_master WHERE enabled=1 AND (party_name LIKE ? OR id LIKE ? OR credit_code LIKE ?) ORDER BY party_name",
                    (rs, row) -> new TradeParty(rs.getString("id"), rs.getString("party_name"), rs.getString("relation_type"), rs.getString("source_type"), rs.getString("nature")),
                    "%" + value + "%", "%" + value + "%", "%" + value + "%");
        } catch (Exception ignored) {
            return parties.stream().filter(p -> value.isBlank() || p.name().contains(value) || p.id().contains(value)).sorted(Comparator.comparing(TradeParty::name)).toList();
        }
    }

    public List<ConfigParty> listPartyConfig(String keyword) {
        String value = keyword == null ? "" : keyword.trim();
        return jdbc.query("SELECT id,party_name,relation_type,source_type,nature,credit_code,contact_name,contact_phone,enabled,remark FROM contract_party_master WHERE party_name LIKE ? OR id LIKE ? OR credit_code LIKE ? ORDER BY updated_at DESC",
                (rs, row) -> new ConfigParty(rs.getString("id"), rs.getString("party_name"), rs.getString("relation_type"), rs.getString("source_type"), rs.getString("nature"), rs.getString("credit_code"), rs.getString("contact_name"), rs.getString("contact_phone"), rs.getInt("enabled") == 1, rs.getString("remark")),
                "%" + value + "%", "%" + value + "%", "%" + value + "%");
    }

    public ConfigParty createPartyConfig(ConfigParty request) {
        if (request == null || request.name() == null || request.name().isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "主体名称不能为空");
        String id = request.id() == null || request.id().isBlank() ? "LOCAL-" + System.currentTimeMillis() : request.id().trim();
        String now = Instant.now().toString();
        jdbc.update("INSERT INTO contract_party_master(id,party_name,relation_type,source_type,nature,credit_code,contact_name,contact_phone,enabled,remark,created_at,updated_at) VALUES(?,?,?,?,?,?,?,?,?,?,?,?)",
                id, request.name().trim(), request.relationType() == null ? "其他" : request.relationType(), request.sourceType() == null ? "本地配置" : request.sourceType(), request.nature() == null ? "企业" : request.nature(), request.creditCode(), request.contactName(), request.contactPhone(), request.enabled() ? 1 : 0, request.remark(), now, now);
        return listPartyConfig(id).get(0);
    }

    public ConfigParty togglePartyConfig(String id) {
        jdbc.update("UPDATE contract_party_master SET enabled=CASE enabled WHEN 1 THEN 0 ELSE 1 END,updated_at=? WHERE id=?", Instant.now().toString(), id);
        return listPartyConfig(id).get(0);
    }

    private void seedParties() {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM contract_party_master", Integer.class);
        if (count != null && count > 0) return;
        parties.forEach(p -> createPartyConfig(new ConfigParty(p.id(), p.name(), p.relationType(), p.sourceType(), p.nature(), null, null, null, true, "开发环境初始化")));
    }

    public List<ContractSummary> listContracts() {
        return jdbc.query("SELECT c.id,c.contract_no,c.name,c.type_id,c.status,p.party_name,c.updated_at " +
                        "FROM contract_draft c JOIN contract_party_ref p ON p.contract_id=c.id " +
                        "ORDER BY c.updated_at DESC", (rs, row) -> summary(rs));
    }

    public List<ContractSummary> listContracts(String scope, String userId, String orgId, String departmentId, List<Long> typeIds) {
        String filter = ""; java.util.ArrayList<Object> args = new java.util.ArrayList<>();
        if ("SELF_AND_PARTICIPANTS".equals(scope)) { filter += " AND c.applicant_id=?"; args.add(userId); }
        else if ("ORGANIZATION".equals(scope)) { filter += " AND c.applicant_org_id=?"; args.add(orgId); }
        else if ("DEPARTMENT".equals(scope)) { filter += " AND c.applicant_department_id=?"; args.add(departmentId); }
        else if ("NAMED_CONTRACTS".equals(scope)) { filter += " AND 1=0"; }
        if (typeIds != null && !typeIds.isEmpty()) { filter += " AND c.type_id IN (" + String.join(",", java.util.Collections.nCopies(typeIds.size(), "?")) + ")"; args.addAll(typeIds); }
        return jdbc.query("SELECT c.id,c.contract_no,c.name,c.type_id,c.status,p.party_name,c.updated_at FROM contract_draft c JOIN contract_party_ref p ON p.contract_id=c.id WHERE 1=1" + filter + " ORDER BY c.updated_at DESC", (rs, row) -> summary(rs), args.toArray());
    }

    private ContractSummary summary(ResultSet rs) throws java.sql.SQLException {
        Long typeId = jdbc.queryForObject("SELECT type_id FROM contract_draft WHERE id=?", Long.class, rs.getLong("id"));
        ContractType type = requireType(typeId);
        return new ContractSummary(rs.getLong("id"), rs.getString("contract_no"), rs.getString("name"),
                type.name(), rs.getString("party_name"), rs.getString("status"), Instant.parse(rs.getString("updated_at")));
    }

    public ContractDetail get(Long id) { return detail(id); }

    public boolean visible(Long id, String scope, String userId, String orgId, String departmentId, List<Long> typeIds) {
        if ("NAMED_CONTRACTS".equals(scope)) return false;
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM contract_draft c WHERE c.id=?");
        java.util.ArrayList<Object> args = new java.util.ArrayList<>(); args.add(id);
        if ("SELF_AND_PARTICIPANTS".equals(scope)) { sql.append(" AND c.applicant_id=?"); args.add(userId); }
        else if ("ORGANIZATION".equals(scope)) { sql.append(" AND c.applicant_org_id=?"); args.add(orgId); }
        else if ("DEPARTMENT".equals(scope)) { sql.append(" AND c.applicant_department_id=?"); args.add(departmentId); }
        if (typeIds != null && !typeIds.isEmpty()) { sql.append(" AND c.type_id IN (" + String.join(",", java.util.Collections.nCopies(typeIds.size(), "?")) + ")"); args.addAll(typeIds); }
        return jdbc.queryForObject(sql.toString(), Integer.class, args.toArray()) > 0;
    }

    public ContractDetail create(CreateContractRequest request) {
        ContractType type = requireType(request.typeId());
        TradeParty party = requireConfiguredParty(request.partyId());
        validateName(request.name());
        long id = jdbc.queryForObject("SELECT COALESCE(MAX(id),1000)+1 FROM contract_draft", Long.class);
        Instant now = Instant.now();
        validateFormal(request.totalAmount(), request.currency(), request.paymentDirection(), request.expireDate());
        jdbc.update("INSERT INTO contract_draft(id,contract_no,name,type_id,party_id,remark,total_amount,currency,payment_direction,expire_date,version,updated_at) VALUES(?,?,?,?,?,?,?,?,?,?,1,?)",
                id, "HT-" + id, request.name().trim(), type.id(), party.id(), request.remark(), request.totalAmount(), currency(request.currency()), request.paymentDirection(), request.expireDate(), now.toString());
        jdbc.update("INSERT INTO contract_party_ref(contract_id,party_id,relation_type,source_type,party_name) VALUES(?,?,?,?,?)",
                id, party.id(), party.relationType(), party.sourceType(), party.name());
        log(id, "CREATE", "创建合同草稿", now);
        return detail(id);
    }

    public ContractDetail createIntake(String path) {
        long id = jdbc.queryForObject("SELECT COALESCE(MAX(id),1000)+1 FROM contract_draft", Long.class);
        Instant now = Instant.now();
        String partyId = "PENDING-" + id;
        String name = "待解析合同-" + id;
        jdbc.update("INSERT INTO contract_draft(id,contract_no,name,type_id,party_id,remark,total_amount,currency,payment_direction,expire_date,version,updated_at) VALUES(?,?,?,?,?,?,?,?,?,?,1,?)",
                id, "HT-" + id, name, 6L, partyId, "文件导入：" + (path == null ? "待审合同" : path), null, "CNY", null, null, now.toString());
        jdbc.update("INSERT INTO contract_party_ref(contract_id,party_id,relation_type,source_type,party_name) VALUES(?,?,?,?,?)",
                id, partyId, "待确认", "AI候选", "待主数据确认");
        log(id, "CREATE_INTAKE", "创建文件解析草稿", now);
        return detail(id);
    }

    public ContractDetail update(Long id, UpdateDraftRequest request) {
        require(id);
        ensureEditable(id);
        validateName(request.name());
        validateFormal(request.totalAmount(), request.currency(), request.paymentDirection(), request.expireDate());
        int changed = jdbc.update("UPDATE contract_draft SET name=?,type_id=?,party_id=?,remark=?,total_amount=?,currency=?,payment_direction=?,expire_date=?,version=version+1,updated_at=? WHERE id=? AND version=?",
                request.name().trim(), requireType(request.typeId()).id(), requireConfiguredParty(request.partyId()).id(),
                request.remark(), request.totalAmount(), currency(request.currency()), request.paymentDirection(), request.expireDate(), Instant.now().toString(), id, request.version());
        if (changed == 0) throw new ResponseStatusException(HttpStatus.CONFLICT, "合同已被其他操作更新，请刷新后重试");
        TradeParty party = requireConfiguredParty(request.partyId());
        jdbc.update("UPDATE contract_party_ref SET party_id=?,relation_type=?,source_type=?,party_name=? WHERE contract_id=?",
                party.id(), party.relationType(), party.sourceType(), party.name(), id);
        log(id, "UPDATE", "编辑合同草稿", Instant.now());
        return detail(id);
    }

    private ContractDetail detail(Long id) {
        try {
            return jdbc.queryForObject("SELECT c.*,p.party_name,p.relation_type,p.source_type FROM contract_draft c " +
                            "JOIN contract_party_ref p ON p.contract_id=c.id WHERE c.id=?", (rs, row) -> {
                        ContractType type = requireType(rs.getLong("type_id"));
                        return new ContractDetail(rs.getLong("id"), rs.getString("contract_no"), rs.getString("name"),
                                type.id(), type.name(), rs.getString("party_id"), rs.getString("party_name"),
                                rs.getString("relation_type"), rs.getString("source_type"), rs.getString("status"), rs.getString("remark"),
                                rs.getInt("version"), Instant.parse(rs.getString("updated_at")),
                                decimal(rs.getObject("total_amount")), rs.getString("currency"),
                                rs.getObject("payment_direction") == null ? null : String.valueOf(rs.getInt("payment_direction")),
                                rs.getString("sign_date"), rs.getString("effective_date"), rs.getString("expire_date"),
                                rs.getObject("source_type_code") == null ? null : String.valueOf(rs.getInt("source_type_code")),
                                logs(rs.getLong("id")),
                                contentVersionsInternal(rs.getLong("id")), attachments(rs.getLong("id")), rs.getInt("archived") == 1);
                    }, id);
        } catch (EmptyResultDataAccessException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "合同不存在");
        }
    }

    private List<AuditEntry> logs(Long id) {
        return jdbc.query("SELECT action,description,operated_at FROM contract_operation_log WHERE contract_id=? ORDER BY id",
                (rs, row) -> new AuditEntry(rs.getString("action"), rs.getString("description"), Instant.parse(rs.getString("operated_at"))), id);
    }

    private static java.math.BigDecimal decimal(Object value) {
        if (value == null) return null;
        if (value instanceof java.math.BigDecimal decimal) return decimal;
        if (value instanceof Number number) return java.math.BigDecimal.valueOf(number.doubleValue());
        return new java.math.BigDecimal(value.toString());
    }

    public List<ContentVersion> contentVersions(Long contractId) {
        require(contractId);
        return contentVersionsInternal(contractId);
    }

    public List<AuditEntry> auditEntries(Long contractId) {
        require(contractId);
        return logs(contractId);
    }

    public ContentVersion saveContent(Long contractId, String content) {
        return saveContent(contractId, content, "人工编辑");
    }

    public ContentVersion saveContent(Long contractId, String content, String source) {
        require(contractId);
        ensureEditable(contractId);
        if (content == null || content.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "合同正文不能为空");
        }
        Integer next = jdbc.queryForObject("SELECT COALESCE(MAX(version_no),0)+1 FROM contract_content_version WHERE contract_id=?", Integer.class, contractId);
        long id = jdbc.queryForObject("SELECT COALESCE(MAX(id),2000)+1 FROM contract_content_version", Long.class);
        String hash = hash(content);
        Instant now = Instant.now();
        jdbc.update("UPDATE contract_content_version SET is_current=0 WHERE contract_id=?", contractId);
        jdbc.update("INSERT INTO contract_content_version(id,contract_id,version_no,content,content_hash,source,is_current,created_at) VALUES(?,?,?,?,?,?,1,?)",
                id, contractId, next, content, hash, source, now.toString());
        jdbc.update("UPDATE contract_draft SET current_content_version_id=?,updated_at=? WHERE id=?", id, now.toString(), contractId);
        log(contractId, "CONTENT_VERSION", "保存合同正文 v" + next, now);
        return contentVersionsInternal(contractId).get(0);
    }

    public ContentVersion createPendingContent(Long contractId, String content, String source) {
        require(contractId);
        ensureEditable(contractId);
        if (content == null || content.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "合同正文不能为空");
        Integer next = jdbc.queryForObject("SELECT COALESCE(MAX(version_no),0)+1 FROM contract_content_version WHERE contract_id=?", Integer.class, contractId);
        long id = jdbc.queryForObject("SELECT COALESCE(MAX(id),2000)+1 FROM contract_content_version", Long.class);
        Instant now = Instant.now();
        jdbc.update("INSERT INTO contract_content_version(id,contract_id,version_no,content,content_hash,source,is_current,created_at) VALUES(?,?,?,?,?,?,0,?)",
                id, contractId, next, content, hash(content), source, now.toString());
        log(contractId, "CHANGE_CONTENT_DRAFT", "保存待审批变更正文 v" + next, now);
        return contentVersionsInternal(contractId).stream().filter(item -> item.id().equals(id)).findFirst().orElseThrow();
    }

    public void activateContentVersion(Long contractId, Long contentVersionId) {
        require(contractId);
        ensureEditable(contractId);
        Integer exists = jdbc.queryForObject("SELECT COUNT(*) FROM contract_content_version WHERE id=? AND contract_id=?", Integer.class, contentVersionId, contractId);
        if (exists == null || exists == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "合同正文版本不存在");
        Instant now = Instant.now();
        jdbc.update("UPDATE contract_content_version SET is_current=0 WHERE contract_id=?", contractId);
        jdbc.update("UPDATE contract_content_version SET is_current=1 WHERE id=? AND contract_id=?", contentVersionId, contractId);
        jdbc.update("UPDATE contract_draft SET current_content_version_id=?,updated_at=? WHERE id=?", contentVersionId, now.toString(), contractId);
    }

    public void bindTemplateVersion(Long contractId, Long templateVersionId) {
        require(contractId);
        ensureEditable(contractId);
        jdbc.update("UPDATE contract_draft SET template_version_id=?,updated_at=? WHERE id=?", templateVersionId, Instant.now().toString(), contractId);
        try { jdbc.update("UPDATE t_contract_main SET template_version_id=? WHERE id=?", templateVersionId, contractId); }
        catch (Exception ignored) { }
        log(contractId, "TEMPLATE_BIND", "绑定模板版本 " + templateVersionId, Instant.now());
    }

    public Attachment saveAttachment(Long contractId, org.springframework.web.multipart.MultipartFile file, int bizAttachType) {
        require(contractId);
        ensureEditable(contractId);
        if (file == null || file.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请选择文件");
        if (bizAttachType < 1 || bizAttachType > 4) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "附件业务类型无效");
        try {
            byte[] content = file.getBytes();
            FileStorageProvider.StoredFile stored = fileStorage.store(file.getOriginalFilename(), content, file.getContentType());
            long id = jdbc.queryForObject("SELECT COALESCE(MAX(id),3000)+1 FROM contract_attachment", Long.class);
            Instant now = Instant.now();
            Long contentVersionId = currentContentVersionId(contractId);
            jdbc.update("INSERT INTO contract_attachment(id,contract_id,file_name,file_size,file_ref,storage_path,biz_attach_type,attach_type,storage_object_id,file_hash,mime_type,is_main_file,content_version_id,uploaded_at) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                    id, contractId, stored.fileName(), stored.size(), stored.reference(), stored.storagePath(), bizAttachType,
                    bizAttachType == 2 ? 1 : 3, stored.reference(), hash(content), file.getContentType(), bizAttachType == 1 ? 1 : 0, contentVersionId, now.toString());
            log(contractId, "ATTACHMENT", "上传附件 " + file.getOriginalFilename(), now);
            return attachments(contractId).stream().filter(a -> a.id().equals(id)).findFirst().orElseThrow();
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "文件保存失败", exception);
        }
    }

    public ContentVersion extractAttachmentText(Long contractId, Long attachmentId) {
        require(contractId);
        ensureEditable(contractId);
        var attachment = jdbc.query("SELECT file_name,storage_path FROM contract_attachment WHERE id=? AND contract_id=?", (rs, row) -> new Object[] { rs.getString("file_name"), rs.getString("storage_path") }, attachmentId, contractId).stream().findFirst().orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "合同附件不存在"));
        String text = documentText.extract(Path.of((String) attachment[1]), (String) attachment[0]);
        return saveContent(contractId, text, "文件文本提取");
    }

    public void confirmIntakeParty(Long contractId, String partyId) {
        if (partyId == null || partyId.isBlank()) return;
        TradeParty party = requireConfiguredParty(partyId);
        jdbc.update("UPDATE contract_draft SET party_id=?,updated_at=? WHERE id=?", party.id(), Instant.now().toString(), contractId);
        jdbc.update("UPDATE contract_party_ref SET party_id=?,relation_type=?,source_type=?,party_name=? WHERE contract_id=?", party.id(), party.relationType(), party.sourceType(), party.name(), contractId);
    }

    public void confirmIntakeType(Long contractId, Long typeId) {
        if (typeId == null) return;
        jdbc.update("UPDATE contract_draft SET type_id=?,updated_at=? WHERE id=?", requireType(typeId).id(), Instant.now().toString(), contractId);
    }

    public ContractDetail confirmEffective(Long contractId, Long signedFileId, String signDate, String effectiveDate) {
        ContractDetail current = detail(contractId);
        if (!"待签署".equals(current.status())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "只有待签署合同才能确认生效");
        Integer approved = jdbc.queryForObject("SELECT COUNT(*) FROM approval_mock WHERE contract_id=? AND status='APPROVED'", Integer.class, contractId);
        if (approved == null || approved == 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "合同尚未审批通过");
        Integer signed = jdbc.queryForObject("SELECT COUNT(*) FROM contract_attachment WHERE id=? AND contract_id=? AND biz_attach_type=2", Integer.class, signedFileId, contractId);
        if (signed == null || signed == 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请选择已上传的签署文件");
        Long submittedVersion = jdbc.query("SELECT contract_content_version_id FROM t_contract_approval_record WHERE contract_id=? ORDER BY id DESC LIMIT 1", (rs, row) -> (Long) rs.getObject(1), contractId).stream().findFirst().orElse(null);
        Long currentVersion = currentContentVersionId(contractId);
        Long signedVersion = jdbc.queryForObject("SELECT content_version_id FROM contract_attachment WHERE id=?", Long.class, signedFileId);
        if (submittedVersion == null || !submittedVersion.equals(currentVersion) || !submittedVersion.equals(signedVersion)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "签署文件和当前正文必须与审批版本一致");
        }
        try { LocalDate.parse(signDate); LocalDate.parse(effectiveDate); }
        catch (RuntimeException exception) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "签署日期或生效日期格式不正确"); }
        Instant now = Instant.now();
        jdbc.update("UPDATE contract_draft SET status='履行中',contract_status=3,signed_file_id=?,sign_date=?,effective_date=?,updated_at=? WHERE id=?",
                signedFileId, signDate, effectiveDate, now.toString(), contractId);
        log(contractId, "EFFECTIVE", "合同确认生效并进入履行中", now);
        return detail(contractId);
    }

    public ContractDetail registerSigned(Long contractId, Long signedFileId, String signDate, String effectiveDate) {
        ContractDetail current = detail(contractId);
        if (!"草稿".equals(current.status())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "登记已签合同只能从草稿开始");
        }
        Integer signed = jdbc.queryForObject("SELECT COUNT(*) FROM contract_attachment WHERE id=? AND contract_id=? AND biz_attach_type=1", Integer.class, signedFileId, contractId);
        if (signed == null || signed == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请选择当前合同下的合同原件");
        }
        try {
            LocalDate signedOn = LocalDate.parse(signDate);
            LocalDate effectiveOn = LocalDate.parse(effectiveDate);
            if (effectiveOn.isBefore(signedOn)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "生效日期不能早于签署日期");
            }
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "签署日期或生效日期格式不正确");
        }
        Instant now = Instant.now();
        jdbc.update("UPDATE contract_draft SET status='履行中',contract_status=3,signed_file_id=?,sign_date=?,effective_date=?,updated_at=? WHERE id=?",
                signedFileId, signDate, effectiveDate, now.toString(), contractId);
        log(contractId, "REGISTER_SIGNED", "登记已签合同并进入履约，不生成签前审批记录", now);
        return detail(contractId);
    }

    public ContractDetail changeStatus(Long contractId, String target) {
        ContractDetail current = detail(contractId);
        String status = current.status();
        boolean allowed = switch (target) {
            case "已作废" -> "草稿".equals(status) || "待签署".equals(status);
            case "履行中" -> "待签署".equals(status);
            case "已终止" -> "履行中".equals(status);
            case "已完成" -> "履行中".equals(status);
            default -> false;
        };
        if (!allowed) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "当前状态不允许执行该操作");
        if ("履行中".equals(target)) {
            Integer attachmentCount = jdbc.queryForObject("SELECT COUNT(*) FROM contract_attachment WHERE contract_id=? AND biz_attach_type=2", Integer.class, contractId);
            if (attachmentCount == null || attachmentCount == 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请先上传签署文件后再确认生效");
            }
        }
        Instant now = Instant.now();
        jdbc.update("UPDATE contract_draft SET status=?,updated_at=? WHERE id=?", target, now.toString(), contractId);
        jdbc.update("UPDATE contract_draft SET contract_status=? WHERE id=?", numericStatus(target), contractId);
        log(contractId, "STATUS", "合同状态变更为" + target, now);
        return detail(contractId);
    }

    public ContractDetail archive(Long contractId) {
        ContractDetail current = detail(contractId);
        if (current.archived()) throw new ResponseStatusException(HttpStatus.CONFLICT, "合同已归档");
        if (!"已完成".equals(current.status()) && !"已终止".equals(current.status())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "仅已完成或已终止的合同可以归档");
        }
        Instant now = Instant.now();
        jdbc.update("UPDATE contract_draft SET archived=1,archived_at=?,updated_at=? WHERE id=?", now.toString(), now.toString(), contractId);
        log(contractId, "ARCHIVE", "合同归档，后续仅可查询", now);
        return detail(contractId);
    }

    private List<ContentVersion> contentVersionsInternal(Long id) {
        return jdbc.query("SELECT id,version_no,source,content_hash,is_current,created_at FROM contract_content_version WHERE contract_id=? ORDER BY version_no DESC",
                (rs, row) -> new ContentVersion(rs.getLong("id"), rs.getInt("version_no"), rs.getString("source"),
                        rs.getString("content_hash"), rs.getInt("is_current") == 1, Instant.parse(rs.getString("created_at"))), id);
    }

    private List<Attachment> attachments(Long id) {
        return jdbc.query("SELECT id,file_name,file_size,file_ref,content_version_id,biz_attach_type,uploaded_at FROM contract_attachment WHERE contract_id=? ORDER BY id DESC",
                (rs, row) -> new Attachment(rs.getLong("id"), rs.getString("file_name"), rs.getLong("file_size"),
                        rs.getString("file_ref"), (Long) rs.getObject("content_version_id"), (Integer) rs.getObject("biz_attach_type"), Instant.parse(rs.getString("uploaded_at"))), id);
    }

    private String hash(String content) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content.getBytes(java.nio.charset.StandardCharsets.UTF_8))); }
        catch (Exception exception) { throw new IllegalStateException("正文摘要生成失败", exception); }
    }

    private String hash(byte[] content) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content)); }
        catch (Exception exception) { throw new IllegalStateException("文件摘要生成失败", exception); }
    }

    private void log(long id, String action, String description, Instant time) {
        jdbc.update("INSERT INTO contract_operation_log(contract_id,action,description,operated_at) VALUES(?,?,?,?)",
                id, action, description, time.toString());
    }

    public void ensureEditable(Long contractId) {
        Integer archived = jdbc.queryForObject("SELECT archived FROM contract_draft WHERE id=?", Integer.class, contractId);
        if (archived != null && archived == 1) throw new ResponseStatusException(HttpStatus.CONFLICT, "合同已归档，仅可查询");
        Integer status = jdbc.queryForObject("SELECT approval_status FROM contract_draft WHERE id=?", Integer.class, contractId);
        if (status != null && status == 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "合同审批中，不能修改关键内容");
        }
    }

    private Long currentContentVersionId(Long contractId) {
        return jdbc.query("SELECT current_content_version_id FROM contract_draft WHERE id=?", (rs, row) -> (Long) rs.getObject(1), contractId)
                .stream().findFirst().orElse(null);
    }

    private TradeParty requireConfiguredParty(String id) {
        return searchParties(id).stream().filter(p -> p.id().equals(id)).findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "交易对方不可用"));
    }

    private void ensureColumn(String table, String column, String definition) {
        try { jdbc.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition); }
        catch (Exception ignored) { }
    }
    private boolean mysqlProfile() { String profiles = System.getenv("SPRING_PROFILES_ACTIVE"); return profiles != null && profiles.contains("mysql"); }

    private int numericStatus(String status) {
        return switch (status) {
            case "待签署" -> 2;
            case "履行中" -> 3;
            case "已完成" -> 4;
            case "已终止" -> 5;
            case "已作废" -> 6;
            default -> 0;
        };
    }

    private void require(Long id) { detail(id); }
    private ContractType requireType(Long id) { return types.stream().filter(t -> t.id().equals(id)).findFirst().orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "合同类型不可用")); }
    private TradeParty requireParty(String id) { return parties.stream().filter(p -> p.id().equals(id)).findFirst().orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "交易对方不可用")); }
    private void validateName(String name) { if (name == null || name.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "合同名称不能为空"); }
    private String currency(String value) { return value == null || value.isBlank() ? "CNY" : value.trim().toUpperCase(); }
    private void validateFormal(java.math.BigDecimal amount, String currency, Integer direction, String expireDate) {
        if (amount != null && amount.signum() < 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "合同金额不能小于零");
        if (currency != null && !currency.isBlank() && !currency.trim().matches("[A-Za-z]{3,8}")) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "币种格式不正确");
        if (direction != null && direction != 1 && direction != 2) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "收付方向无效");
        if (expireDate != null && !expireDate.isBlank()) try { LocalDate.parse(expireDate); } catch (RuntimeException e) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "到期日期格式不正确"); }
    }
}
