package com.hrcontract.api;

import com.hrcontract.application.ContractModels.ContractDetail;
import com.hrcontract.application.ContractModels.ContractSummary;
import com.hrcontract.application.ContractModels.ContractType;
import com.hrcontract.application.ContractModels.CreateContractRequest;
import com.hrcontract.application.ContractModels.TradeParty;
import com.hrcontract.application.ContractModels.ConfigParty;
import com.hrcontract.application.ContractModels.AiConfig;
import com.hrcontract.application.ContractModels.UpdateAiConfigRequest;
import com.hrcontract.application.ContractModels.AiDraftRequest;
import com.hrcontract.application.ContractModels.AiCapabilityTestResult;
import com.hrcontract.application.AiConfigService;
import com.hrcontract.application.ContractModels.UpdateDraftRequest;
import com.hrcontract.application.ContractModels.ContentVersion;
import com.hrcontract.application.ContractModels.Attachment;
import com.hrcontract.application.ContractService;
import com.hrcontract.application.AiWorkflowService;
import com.hrcontract.application.ContractModels.AiTask;
import com.hrcontract.application.ContractModels.AiReview;
import com.hrcontract.application.ContractModels.ApprovalInfo;
import com.hrcontract.application.ContractModels.TemplateDetail;
import com.hrcontract.application.ContractModels.TemplateSummary;
import com.hrcontract.application.ContractModels.CreateTemplateRequest;
import com.hrcontract.application.ContractModels.CreateTemplateVersionRequest;
import com.hrcontract.application.TemplateService;
import com.hrcontract.application.FulfillmentService;
import com.hrcontract.application.ContractModels.PaymentPlan;
import com.hrcontract.application.ContractModels.FulfillmentMilestone;
import com.hrcontract.application.ContractModels.CreatePaymentPlanRequest;
import com.hrcontract.application.ContractModels.CreateFulfillmentMilestoneRequest;
import com.hrcontract.application.ContractModels.AuditEntry;
import com.hrcontract.application.ContractModels.ContractChange;
import com.hrcontract.application.ContractModels.CreateContractChangeRequest;
import com.hrcontract.application.ChangeService;
import com.hrcontract.application.CategoryService;
import com.hrcontract.application.ContractModels.CategoryNode;
import com.hrcontract.application.ContractModels.CreateCategoryRequest;
import com.hrcontract.application.ContractModels.UpdateCategoryRequest;
import com.hrcontract.application.ConnectionService;
import com.hrcontract.application.ContractModels.ExternalConnection;
import com.hrcontract.application.ContractModels.ConnectionLog;
import com.hrcontract.application.ContractModels.CreateConnectionRequest;
import com.hrcontract.application.ContractModels.UpdateConnectionRequest;
import com.hrcontract.application.PermissionAuditService;
import com.hrcontract.application.ContractModels.PermissionRole;
import com.hrcontract.application.ContractModels.UpdatePermissionRoleRequest;
import com.hrcontract.application.ContractModels.GlobalAuditEntry;
import com.hrcontract.application.ContractModels.StatisticsOverview;
import com.hrcontract.application.ContractModels.AttentionItem;
import com.hrcontract.application.ContractModels.UpdateAttentionItemRequest;
import com.hrcontract.application.StatisticsService;
import com.hrcontract.application.ExportService;
import com.hrcontract.integration.ExternalIntegrationStatus;
import com.hrcontract.integration.IntegrationStatusService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

@RestController
@RequestMapping("/api")
public class ContractController {
    private final ContractService service;
    private final AiWorkflowService ai;
    private final TemplateService templates;
    private final FulfillmentService fulfillment;
    private final ChangeService changes;
    private final CategoryService categories;
    private final AccessControl access;
    private final ConnectionService connectionsService;
    private final AiConfigService aiConfig;
    private final PermissionAuditService permissionAudit;
    private final StatisticsService statistics;
    private final ExportService exports;
    private final com.hrcontract.application.AttentionService attention;
    private final IntegrationStatusService integrations;
    private final com.hrcontract.application.ContractAuthorizationService contractAuthorization;
    private final com.hrcontract.application.LocalAuthService auth;
    private final com.hrcontract.application.IdentityDirectoryService identityDirectory;
    private final com.hrcontract.application.AuthenticationService authentication;

    public ContractController(ContractService service, AiWorkflowService ai, TemplateService templates, FulfillmentService fulfillment, ChangeService changes, CategoryService categories, AccessControl access, ConnectionService connectionsService, PermissionAuditService permissionAudit, StatisticsService statistics, ExportService exports, com.hrcontract.application.AttentionService attention, com.hrcontract.application.ContractAuthorizationService contractAuthorization, IntegrationStatusService integrations, com.hrcontract.application.LocalAuthService auth, com.hrcontract.application.IdentityDirectoryService identityDirectory, com.hrcontract.application.AuthenticationService authentication, AiConfigService aiConfig) {
        this.service = service;
        this.ai = ai;
        this.templates = templates;
        this.fulfillment = fulfillment;
        this.changes = changes;
        this.categories = categories;
        this.access = access;
        this.connectionsService = connectionsService;
        this.aiConfig = aiConfig;
        this.permissionAudit = permissionAudit;
        this.statistics = statistics;
        this.exports = exports;
        this.attention = attention;
        this.contractAuthorization = contractAuthorization;
        this.integrations = integrations;
        this.auth = auth;
        this.identityDirectory = identityDirectory;
        this.authentication = authentication;
    }

    @PostMapping("/auth/login")
    public java.util.Map<String, Object> login(@RequestBody java.util.Map<String, String> body, HttpSession session) { return authentication.login(body.getOrDefault("account", ""), body.getOrDefault("password", ""), session); }

    @GetMapping("/auth/oidc/login")
    public void oidcLogin(@RequestParam(required = false) String returnTo, HttpSession session, jakarta.servlet.http.HttpServletResponse response) throws java.io.IOException { response.sendRedirect(authentication.beginOidc(session, returnTo)); }

    @GetMapping("/auth/oidc/callback")
    public void oidcCallback(@RequestParam String code, @RequestParam String state, HttpSession session, jakarta.servlet.http.HttpServletResponse response) throws java.io.IOException { response.sendRedirect("/?portalTarget=" + java.net.URLEncoder.encode(authentication.callback(code, state, session), java.nio.charset.StandardCharsets.UTF_8)); }

    @PostMapping("/bff/proxy")
    public com.fasterxml.jackson.databind.JsonNode bffProxy(@RequestBody java.util.Map<String, Object> body, HttpSession session, HttpServletRequest http) { access.require(http, "VIEW"); return authentication.gateway(session, String.valueOf(body.get("path")), String.valueOf(body.getOrDefault("method", "GET")), body.get("body") == null ? null : String.valueOf(body.get("body"))); }

    @GetMapping("/auth/session")
    public java.util.Map<String, Object> session(HttpSession session) { return auth.session(session); }

    @PostMapping("/auth/logout")
    public java.util.Map<String, String> logout(HttpSession session) { auth.logout(session); return java.util.Map.of("status", "OK"); }

    @GetMapping("/admin/users")
    public java.util.List<java.util.Map<String, Object>> adminUsers(HttpServletRequest http) { access.require(http, "TEMPLATE"); return auth.users(); }

    @PutMapping("/admin/users/{id}")
    public java.util.Map<String, Object> updateAdminUser(@PathVariable String id, @RequestBody java.util.Map<String, Object> body, HttpServletRequest http) { access.require(http, "TEMPLATE"); return auth.update(id, body, String.valueOf(access.currentUser(http).get("displayName"))); }

    @PostMapping("/admin/users/{id}/reset-password")
    public java.util.Map<String, String> resetAdminPassword(@PathVariable String id, @RequestBody java.util.Map<String, String> body, HttpServletRequest http) { access.require(http, "TEMPLATE"); auth.resetPassword(id, body.get("password"), String.valueOf(access.currentUser(http).get("displayName"))); return java.util.Map.of("status", "OK"); }

    @GetMapping("/contract-types")
    public List<ContractType> types() { return service.listTypes(); }

    @GetMapping("/categories")
    public List<CategoryNode> categories() { return categories.list(); }

    @PostMapping("/categories")
    public CategoryNode createCategory(@RequestBody CreateCategoryRequest request, HttpServletRequest http) { access.require(http, "TEMPLATE"); return categories.create(request); }

    @PutMapping("/categories/{id}")
    public CategoryNode updateCategory(@PathVariable Long id, @RequestBody UpdateCategoryRequest request, HttpServletRequest http) { access.require(http, "TEMPLATE"); return categories.update(id, request); }

    @PostMapping("/categories/{id}/toggle")
    public CategoryNode toggleCategory(@PathVariable Long id, HttpServletRequest http) { access.require(http, "TEMPLATE"); return categories.toggle(id); }

    @PostMapping("/categories/{id}/publish")
    public CategoryNode publishCategory(@PathVariable Long id, HttpServletRequest http) { access.require(http, "TEMPLATE"); return categories.publish(id); }

    @GetMapping("/connections")
    public List<ExternalConnection> connections(HttpServletRequest http) { access.require(http, "TEMPLATE"); return connectionsService.list(); }

    @GetMapping("/integrations/status")
    public List<ExternalIntegrationStatus> integrationStatus(HttpServletRequest http) { access.require(http, "TEMPLATE"); return integrations.statuses(); }

    @GetMapping("/ai/config")
    public AiConfig aiConfig(HttpServletRequest http) { access.require(http, "TEMPLATE"); return aiConfig.get(); }

    @PutMapping("/ai/config")
    public AiConfig updateAiConfig(@RequestBody UpdateAiConfigRequest request, HttpServletRequest http) { access.require(http, "TEMPLATE"); return aiConfig.update(request); }

    @PostMapping("/ai/config/test")
    public AiConfig testAiConfig(HttpServletRequest http) { access.require(http, "TEMPLATE"); return aiConfig.test(); }

    @PostMapping("/ai/config/test/{capability}")
    public AiCapabilityTestResult testAiCapability(@PathVariable String capability, HttpServletRequest http) {
        access.require(http, "TEMPLATE"); return aiConfig.testCapability(capability);
    }

    @PostMapping("/ai/draft")
    public com.fasterxml.jackson.databind.JsonNode aiDraft(@RequestBody AiDraftRequest request, HttpServletRequest http) {
        access.require(http, "EDIT"); return ai.draft(request);
    }

    @PostMapping("/connections")
    public ExternalConnection createConnection(@RequestBody CreateConnectionRequest request, HttpServletRequest http) { access.require(http, "TEMPLATE"); return connectionsService.create(request); }

    @PutMapping("/connections/{id}")
    public ExternalConnection updateConnection(@PathVariable Long id, @RequestBody UpdateConnectionRequest request, HttpServletRequest http) { access.require(http, "TEMPLATE"); return connectionsService.update(id, request); }

    @PostMapping("/connections/{id}/test")
    public ExternalConnection testConnection(@PathVariable Long id, HttpServletRequest http) { access.require(http, "TEMPLATE"); return connectionsService.test(id); }

    @PostMapping("/connections/{id}/toggle")
    public ExternalConnection toggleConnection(@PathVariable Long id, HttpServletRequest http) { access.require(http, "TEMPLATE"); return connectionsService.toggle(id); }

    @GetMapping("/connections/{id}/logs")
    public List<ConnectionLog> connectionLogs(@PathVariable Long id, HttpServletRequest http) { access.require(http, "TEMPLATE"); return connectionsService.logs(id); }

    @GetMapping("/permission-roles")
    public List<PermissionRole> permissionRoles(HttpServletRequest http) { access.require(http, "TEMPLATE"); return permissionAudit.roles(); }

    @PutMapping("/permission-roles/{code}")
    public PermissionRole updatePermissionRole(@PathVariable String code, @RequestBody UpdatePermissionRoleRequest request, HttpServletRequest http) { access.require(http, "TEMPLATE"); return permissionAudit.update(code, request, String.valueOf(access.currentUser(http).get("displayName"))); }

    @GetMapping("/audit-logs")
    public List<GlobalAuditEntry> auditLogs(@RequestParam(required = false) String module, @RequestParam(required = false) String keyword, HttpServletRequest http) { access.require(http, "TEMPLATE"); return permissionAudit.audit(module, keyword); }

    @GetMapping("/statistics/overview")
    public StatisticsOverview statistics(HttpServletRequest http) {
        access.require(http, "VIEW");
        var user = access.currentUser(http);
        return statistics.overview(access.currentRole(http), String.valueOf(user.get("userId")), String.valueOf(user.get("orgId")), String.valueOf(user.get("departmentId")));
    }

    @GetMapping(value = "/exports/analysis", produces = "text/csv")
    public ResponseEntity<byte[]> exportAnalysis(HttpServletRequest http) {
        access.require(http, "EXPORT");
        var user = access.currentUser(http);
        byte[] content = exports.report(access.currentRole(http), String.valueOf(user.get("userId")), String.valueOf(user.get("orgId")), String.valueOf(user.get("departmentId")), String.valueOf(user.get("displayName")));
        String filename = "contract-analysis-" + java.time.LocalDate.now() + ".csv";
        return ResponseEntity.ok().contentType(MediaType.parseMediaType("text/csv;charset=UTF-8")).header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"").body(content);
    }

    @GetMapping("/attention-items")
    public List<AttentionItem> attentionItems(HttpServletRequest http) {
        access.require(http, "VIEW");
        var user = access.currentUser(http);
        return attention.list(access.currentRole(http), String.valueOf(user.get("userId")), String.valueOf(user.get("orgId")), String.valueOf(user.get("departmentId")));
    }

    @PostMapping("/attention-items/{key}/handling")
    public AttentionItem updateAttentionItem(@PathVariable String key, @RequestBody UpdateAttentionItemRequest request, HttpServletRequest http) {
        access.require(http, "EDIT");
        var user = access.currentUser(http);
        return attention.update(key, request.status(), request.opinion(), String.valueOf(user.get("displayName")), access.currentRole(http), String.valueOf(user.get("userId")), String.valueOf(user.get("orgId")), String.valueOf(user.get("departmentId")));
    }

    @GetMapping("/templates")
    public List<TemplateSummary> templateList() { return templates.list(); }

    @GetMapping("/templates/{id}")
    public TemplateDetail template(@PathVariable Long id) { return templates.get(id); }

    @PostMapping("/templates")
    public TemplateDetail createTemplate(@Valid @RequestBody CreateTemplateRequest request, HttpServletRequest http) { access.require(http, "TEMPLATE"); return templates.create(request); }

    @PostMapping("/templates/{id}/versions")
    public TemplateDetail addTemplateVersion(@PathVariable Long id, @RequestBody CreateTemplateVersionRequest request, HttpServletRequest http) { access.require(http, "TEMPLATE"); return templates.addVersion(id, request); }

    @PostMapping("/templates/{id}/publish")
    public TemplateDetail publishTemplate(@PathVariable Long id, HttpServletRequest http) { access.require(http, "TEMPLATE"); return templates.publish(id); }

    @PostMapping("/templates/{id}/toggle")
    public TemplateDetail toggleTemplate(@PathVariable Long id, HttpServletRequest http) { access.require(http, "TEMPLATE"); return templates.toggle(id); }

    @GetMapping("/templates/{id}/versions")
    public List<TemplateDetail> templateVersions(@PathVariable Long id) { return templates.versions(id); }

    @GetMapping("/trade-parties")
    public List<TradeParty> parties(@RequestParam(required = false) String keyword) {
        return service.searchParties(keyword);
    }

    @GetMapping("/config/parties")
    public List<ConfigParty> partyConfig(@RequestParam(required = false) String keyword, HttpServletRequest http) { access.require(http, "TEMPLATE"); return service.listPartyConfig(keyword); }

    @PostMapping("/config/parties")
    public ConfigParty createPartyConfig(@RequestBody ConfigParty request, HttpServletRequest http) { access.require(http, "TEMPLATE"); return service.createPartyConfig(request); }

    @PostMapping("/config/parties/{id}/toggle")
    public ConfigParty togglePartyConfig(@PathVariable String id, HttpServletRequest http) { access.require(http, "TEMPLATE"); return service.togglePartyConfig(id); }

    @GetMapping("/contracts")
    public List<ContractSummary> contracts(HttpServletRequest http) {
        access.require(http, "VIEW");
        PermissionRole role = permissionAudit.role(access.currentRole(http));
        java.util.Map<String, Object> user = access.currentUser(http);
        return service.listContracts(role.dataScope(), String.valueOf(user.get("userId")), String.valueOf(user.get("orgId")), String.valueOf(user.get("departmentId")), role.typeIds());
    }

    @GetMapping("/directory/mock")
    public java.util.Map<String, Object> mockDirectory(HttpServletRequest http) {
        return java.util.Map.of("currentUser", access.currentUser(http), "users", identityDirectory.users().stream().map(user -> java.util.Map.of("id", user.id(), "name", user.name(), "role", user.role(), "orgId", user.orgId(), "departmentId", user.departmentId())).toList(), "organizations", java.util.List.of(java.util.Map.of("id", "100", "name", "合同管理示例组织")), "departments", java.util.List.of(java.util.Map.of("id", "101", "orgId", "100", "name", "业务部"), java.util.Map.of("id", "102", "orgId", "100", "name", "法务部")));
    }

    @GetMapping("/identity-directory/contract")
    public java.util.Map<String, Object> identityDirectoryContract(HttpServletRequest http) { access.require(http, "TEMPLATE"); return identityDirectory.contract(); }

    @GetMapping("/contracts/{id}")
    public ContractDetail contract(@PathVariable Long id, HttpServletRequest http) { ensureAction(id, http, "VIEW"); return service.get(id); }

    @PostMapping("/contracts")
    public ContractDetail create(@Valid @RequestBody CreateContractRequest request, HttpServletRequest http) {
        access.require(http, "EDIT");
        if (request.templateId() != null) {
            TemplateDetail template = templates.get(request.templateId());
            if (!"PUBLISHED".equals(template.status()) || !template.typeId().equals(request.typeId())) {
                throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Template is not published or does not match contract type");
            }
        }
        ContractDetail created = service.create(request);
        if (request.templateId() != null) {
            String rendered = templates.render(request.templateId(), created.name(), created.contractNo(), created.partyName());
            Long templateVersionId = templates.currentVersionId(request.templateId());
            service.bindTemplateVersion(created.id(), templateVersionId);
            service.saveContent(created.id(), rendered, "模板生成");
            return service.get(created.id());
        }
        return created;
    }

    @PostMapping("/contracts/intake")
    public ContractDetail createIntake(@RequestParam String path, HttpServletRequest http) {
        access.require(http, "EDIT");
        return service.createIntake(path);
    }

    @PutMapping("/contracts/{id}/draft")
    public ContractDetail update(@PathVariable Long id, @Valid @RequestBody UpdateDraftRequest request, HttpServletRequest http) {
        access.require(http, "EDIT"); ensureAction(id, http, "EDIT");
        return service.update(id, request);
    }

    @PostMapping("/contracts/{id}/status")
    public ContractDetail changeStatus(@PathVariable Long id, @RequestParam String target, HttpServletRequest http) {
        access.require(http, "EDIT"); ensureAction(id, http, "EDIT");
        return service.changeStatus(id, target);
    }

    @PostMapping("/contracts/{id}/archive")
    public ContractDetail archive(@PathVariable Long id, HttpServletRequest http) {
        access.require(http, "EDIT"); ensureAction(id, http, "EDIT");
        return service.archive(id);
    }

    @GetMapping("/contracts/{id}/payments")
    public List<PaymentPlan> payments(@PathVariable Long id, HttpServletRequest http) { ensureAction(id, http, "VIEW"); return fulfillment.payments(id); }

    @PostMapping("/contracts/{id}/payments")
    public PaymentPlan addPayment(@PathVariable Long id, @RequestBody CreatePaymentPlanRequest request, HttpServletRequest http) { access.require(http, "FULFILL"); ensureVisible(id, http); return fulfillment.addPayment(id, request); }

    @PostMapping("/contracts/{id}/payments/{paymentId}/status")
    public PaymentPlan markPayment(@PathVariable Long id, @PathVariable Long paymentId, @RequestParam String status, HttpServletRequest http) { access.require(http, "FULFILL"); ensureAction(id, http, "EDIT"); return fulfillment.markPayment(id, paymentId, status); }

    @GetMapping("/contracts/{id}/payments/{paymentId}/records")
    public List<com.hrcontract.application.ContractModels.PaymentRecord> paymentRecords(@PathVariable Long id, @PathVariable Long paymentId, HttpServletRequest http) { ensureAction(id, http, "VIEW"); return fulfillment.paymentRecords(id, paymentId); }

    @PostMapping("/contracts/{id}/payments/{paymentId}/records")
    public com.hrcontract.application.ContractModels.PaymentRecord addPaymentRecord(@PathVariable Long id, @PathVariable Long paymentId, @RequestBody com.hrcontract.application.ContractModels.CreatePaymentRecordRequest request, HttpServletRequest http) { access.require(http, "FULFILL"); ensureAction(id, http, "EDIT"); return fulfillment.addPaymentRecord(id, paymentId, request); }

    @GetMapping("/contracts/{id}/milestones")
    public List<FulfillmentMilestone> milestones(@PathVariable Long id, HttpServletRequest http) { ensureAction(id, http, "VIEW"); return fulfillment.milestones(id); }

    @PostMapping("/contracts/{id}/milestones")
    public FulfillmentMilestone addMilestone(@PathVariable Long id, @RequestBody CreateFulfillmentMilestoneRequest request, HttpServletRequest http) { access.require(http, "FULFILL"); ensureVisible(id, http); return fulfillment.addMilestone(id, request); }

    @PostMapping("/contracts/{id}/milestones/{milestoneId}/complete")
    public FulfillmentMilestone completeMilestone(@PathVariable Long id, @PathVariable Long milestoneId, HttpServletRequest http) { access.require(http, "FULFILL"); ensureVisible(id, http); return fulfillment.completeMilestone(id, milestoneId); }

    @PostMapping("/contracts/{id}/milestones/{milestoneId}/status")
    public FulfillmentMilestone updateMilestoneStatus(@PathVariable Long id, @PathVariable Long milestoneId, @RequestBody com.hrcontract.application.ContractModels.UpdateFulfillmentStatusRequest request, HttpServletRequest http) { access.require(http, "FULFILL"); ensureAction(id, http, "EDIT"); return fulfillment.updateMilestoneStatus(id, milestoneId, request); }

    @GetMapping("/contracts/{id}/content-versions")
    public List<ContentVersion> contentVersions(@PathVariable Long id, HttpServletRequest http) { ensureAction(id, http, "VIEW"); return service.contentVersions(id); }

    @PostMapping("/contracts/{id}/content-versions")
    public ContentVersion saveContent(@PathVariable Long id, @RequestBody String content, HttpServletRequest http) {
        access.require(http, "EDIT"); ensureAction(id, http, "EDIT");
        return service.saveContent(id, content);
    }

    @GetMapping("/contracts/{id}/audit")
    public List<AuditEntry> audit(@PathVariable Long id, HttpServletRequest http) { ensureAction(id, http, "VIEW"); return service.auditEntries(id); }

    @GetMapping("/contracts/{id}/changes")
    public List<ContractChange> changes(@PathVariable Long id, HttpServletRequest http) { ensureAction(id, http, "VIEW"); return changes.list(id); }

    @PostMapping("/contracts/{id}/changes")
    public ContractChange createChange(@PathVariable Long id, @RequestBody CreateContractChangeRequest request, HttpServletRequest http) {
        access.require(http, "EDIT"); ensureAction(id, http, "EDIT");
        return changes.create(id, request);
    }

    @PostMapping("/contracts/{id}/changes/{changeId}/submit")
    public ContractChange submitChange(@PathVariable Long id, @PathVariable Long changeId, HttpServletRequest http) {
        access.require(http, "APPROVE"); ensureAction(id, http, "APPROVE");
        return changes.submit(id, changeId);
    }

    @PostMapping("/contracts/{id}/changes/{changeId}/mock-approve")
    public ContractChange approveChange(@PathVariable Long id, @PathVariable Long changeId, HttpServletRequest http) {
        access.require(http, "APPROVE"); ensureAction(id, http, "APPROVE");
        return changes.approve(id, changeId);
    }

    @PostMapping("/contracts/{id}/changes/{changeId}/reject")
    public ContractChange rejectChange(@PathVariable Long id, @PathVariable Long changeId, HttpServletRequest http) {
        access.require(http, "APPROVE"); ensureAction(id, http, "APPROVE"); return changes.reject(id, changeId);
    }

    @PostMapping("/contracts/{id}/changes/{changeId}/withdraw")
    public ContractChange withdrawChange(@PathVariable Long id, @PathVariable Long changeId, HttpServletRequest http) {
        access.require(http, "APPROVE"); ensureAction(id, http, "APPROVE"); return changes.withdraw(id, changeId);
    }

    @PostMapping("/contracts/{id}/changes/{changeId}/effective")
    public ContractChange effectiveChange(@PathVariable Long id, @PathVariable Long changeId, @RequestParam String signDate, @RequestParam String effectDate, HttpServletRequest http) {
        access.require(http, "EDIT"); ensureAction(id, http, "EDIT");
        return changes.effective(id, changeId, signDate, effectDate);
    }

    @PostMapping(value = "/contracts/{id}/attachments", consumes = "multipart/form-data")
    public Attachment upload(@PathVariable Long id, @RequestPart("file") MultipartFile file,
                             @RequestParam(defaultValue = "3") int bizAttachType, HttpServletRequest http) {
        access.require(http, "EDIT"); ensureAction(id, http, "EDIT");
        return service.saveAttachment(id, file, bizAttachType);
    }

    @PostMapping("/contracts/{id}/ai/extract-file")
    public AiTask extractFile(@PathVariable Long id, @RequestParam Long attachmentId, HttpServletRequest http) {
        access.require(http, "EDIT"); ensureAction(id, http, "EDIT");
        return ai.extractAttachment(id, attachmentId);
    }

    @PostMapping("/contracts/{id}/effective")
    public ContractDetail confirmEffective(@PathVariable Long id, @RequestParam Long signedFileId,
                                           @RequestParam String signDate, @RequestParam String effectiveDate,
                                           HttpServletRequest http) {
        access.require(http, "EDIT"); ensureAction(id, http, "EDIT");
        return service.confirmEffective(id, signedFileId, signDate, effectiveDate);
    }

    @PostMapping("/contracts/{id}/register-signed")
    public ContractDetail registerSigned(@PathVariable Long id, @RequestParam Long signedFileId,
                                         @RequestParam String signDate, @RequestParam String effectiveDate,
                                         HttpServletRequest http) {
        access.require(http, "EDIT"); ensureAction(id, http, "EDIT");
        return service.registerSigned(id, signedFileId, signDate, effectiveDate);
    }

    @PostMapping("/contracts/{id}/ai/extract")
    public AiTask extract(@PathVariable Long id, HttpServletRequest http) { access.require(http, "EDIT"); ensureAction(id, http, "EDIT"); return ai.extract(id); }

    @GetMapping("/contracts/{id}/ai/extract/latest")
    public com.fasterxml.jackson.databind.JsonNode latestExtraction(@PathVariable Long id, HttpServletRequest http) { ensureAction(id, http, "VIEW"); return ai.latestExtraction(id); }

    @GetMapping("/contracts/{id}/ai/extract/fulfillment-recommendations")
    public java.util.Map<String, Object> fulfillmentRecommendations(@PathVariable Long id, HttpServletRequest http) { ensureAction(id, http, "VIEW"); return ai.fulfillmentRecommendations(id); }

    @PostMapping("/contracts/{id}/ai/extract/fulfillment-revoke")
    public java.util.Map<String, Object> revokeFulfillment(@PathVariable Long id, HttpServletRequest http) { access.require(http, "FULFILL"); ensureAction(id, http, "EDIT"); return ai.revokeFulfillment(id); }

    @PostMapping("/contracts/{id}/ai/extract/confirm")
    public ContractDetail confirmExtraction(@PathVariable Long id, @RequestBody java.util.Map<String, Object> confirmation, HttpServletRequest http) { access.require(http, "EDIT"); ensureAction(id, http, "EDIT"); return ai.confirmExtraction(id, confirmation); }

    @PostMapping("/contracts/{id}/ai/review")
    public AiReview review(@PathVariable Long id, @RequestParam(required = false) String reviewView, HttpServletRequest http) { access.require(http, "EDIT"); ensureAction(id, http, "EDIT"); return ai.review(id, reviewView); }

    @GetMapping("/contracts/{id}/ai/tasks")
    public List<AiTask> aiTasks(@PathVariable Long id, HttpServletRequest http) { ensureAction(id, http, "VIEW"); return ai.tasks(id); }

    @GetMapping("/contracts/{id}/ai/review")
    public AiReview aiReview(@PathVariable Long id, HttpServletRequest http) { ensureAction(id, http, "VIEW"); return ai.review(id); }

    @PostMapping("/contracts/{id}/ai/review/confirm")
    public AiReview confirmReview(@PathVariable Long id, HttpServletRequest http) { access.require(http, "APPROVE"); ensureAction(id, http, "APPROVE"); return ai.confirmReview(id); }

    @PostMapping("/contracts/{id}/approval")
    public ApprovalInfo submitApproval(@PathVariable Long id, HttpServletRequest http) { access.require(http, "APPROVE"); ensureAction(id, http, "APPROVE"); return ai.submitApproval(id); }

    @GetMapping("/contracts/{id}/approval")
    public ApprovalInfo approval(@PathVariable Long id, HttpServletRequest http) { ensureAction(id, http, "VIEW"); return ai.approval(id); }

    @PostMapping("/contracts/{id}/approval/mock-approve")
    public ApprovalInfo approve(@PathVariable Long id, HttpServletRequest http) { access.require(http, "APPROVE"); ensureAction(id, http, "APPROVE"); return ai.approve(id); }

    @PostMapping("/contracts/{id}/approval/approve")
    public ApprovalInfo approveCurrent(@PathVariable Long id, HttpServletRequest http) { access.require(http, "APPROVE"); ensureAction(id, http, "APPROVE"); return ai.approve(id); }

    @PostMapping("/contracts/{id}/approval/reject")
    public ApprovalInfo reject(@PathVariable Long id, @RequestBody(required = false) java.util.Map<String, String> body, HttpServletRequest http) {
        access.require(http, "APPROVE"); ensureAction(id, http, "APPROVE");
        return ai.reject(id, body == null ? null : body.get("opinion"));
    }

    @PostMapping("/contracts/{id}/approval/withdraw")
    public ApprovalInfo withdraw(@PathVariable Long id, @RequestBody(required = false) java.util.Map<String, String> body, HttpServletRequest http) {
        access.require(http, "APPROVE"); ensureAction(id, http, "APPROVE");
        return ai.withdraw(id, body == null ? null : body.get("opinion"));
    }

    @GetMapping("/contract-authorizations")
    public List<com.hrcontract.application.ContractModels.ContractAuthorization> authorizations(@RequestParam Long contractId, HttpServletRequest http) { access.require(http, "TEMPLATE"); ensureVisible(contractId, http); return contractAuthorization.list(contractId); }
    @PostMapping("/contract-authorizations")
    public com.hrcontract.application.ContractModels.ContractAuthorization createAuthorization(@RequestBody com.hrcontract.application.ContractModels.CreateContractAuthorizationRequest request, HttpServletRequest http) { access.require(http, "TEMPLATE"); ensureVisible(request.contractId(), http); return contractAuthorization.create(request); }
    @PostMapping("/contract-authorizations/{id}/revoke")
    public void revokeAuthorization(@PathVariable Long id, HttpServletRequest http) { access.require(http, "TEMPLATE"); contractAuthorization.revoke(id); }

    private void ensureVisible(Long id, HttpServletRequest http) {
        var user = access.currentUser(http); var role = permissionAudit.role(access.currentRole(http));
        if (!service.visible(id, role.dataScope(), String.valueOf(user.get("userId")), String.valueOf(user.get("orgId")), String.valueOf(user.get("departmentId")), role.typeIds()) && !contractAuthorization.allowed(id, String.valueOf(user.get("userId")), "VIEW")) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "合同不存在或无权查看");
        }
    }
    private void ensureAction(Long id, HttpServletRequest http, String permission) {
        var user = access.currentUser(http); var role = permissionAudit.role(access.currentRole(http));
        boolean visible = service.visible(id, role.dataScope(), String.valueOf(user.get("userId")), String.valueOf(user.get("orgId")), String.valueOf(user.get("departmentId")), role.typeIds());
        if (!visible && !contractAuthorization.allowed(id, String.valueOf(user.get("userId")), permission)) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "合同不存在或无权操作");
    }
}
