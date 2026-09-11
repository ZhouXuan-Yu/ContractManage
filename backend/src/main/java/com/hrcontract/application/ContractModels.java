package com.hrcontract.application;

import java.time.Instant;
import java.util.List;

public final class ContractModels {
    private ContractModels() {
    }

    public record ContractType(Long id, String category, String name, String subtype, boolean enabled) {
    }

    public record TradeParty(String id, String name, String relationType, String sourceType, String nature) {
    }

    public record ConfigParty(String id, String name, String relationType, String sourceType,
                              String nature, String creditCode, String contactName, String contactPhone,
                              boolean enabled, String remark) {
    }

    public record ContractSummary(Long id, String contractNo, String name, String typeName,
                                  String partyName, String status, Instant updatedAt) {
    }

    public record ContractDetail(Long id, String contractNo, String name, Long typeId,
                                 String typeName, String partyId, String partyName,
                                 String relationType, String sourceType, String status,
                                 String remark, int version, Instant updatedAt,
                                 java.math.BigDecimal totalAmount, String currency, String paymentDirection,
                                 String signDate, String effectiveDate, String expireDate, String sourceTypeCode,
                                 List<AuditEntry> auditEntries, List<ContentVersion> contentVersions,
                                 List<Attachment> attachments, boolean archived) {
    }

    public record CreateContractRequest(Long typeId, String partyId, String name, String remark, Long templateId,
                                        java.math.BigDecimal totalAmount, String currency, Integer paymentDirection,
                                        String expireDate) {
    }

    public record UpdateDraftRequest(String name, Long typeId, String partyId, String remark, int version,
                                     java.math.BigDecimal totalAmount, String currency, Integer paymentDirection,
                                     String expireDate) {
    }

    public record AuditEntry(String action, String description, Instant operatedAt) {
    }

    public record ContentVersion(Long id, int versionNo, String source, String contentHash,
                                 boolean current, Instant createdAt) {
    }

    public record Attachment(Long id, String fileName, long fileSize, String fileRef,
                             Long contentVersionId, Integer bizAttachType, Instant uploadedAt) {
    }

    public record AiTask(Long id, String taskId, String taskType, String status, Long contentVersionId,
                         String summary, Instant createdAt, Instant finishedAt) {
    }

    public record RiskItem(String category, String level, String riskPoint, String basis, String advice,
                           boolean confirmed) {
    }

    public record AiReview(Long id, Long taskId, String reviewView, String overallLevel,
                           String summary, List<RiskItem> risks, boolean confirmed, Instant createdAt) {
    }

    public record ApprovalInfo(Long id, String approvalNo, String status, String result, Instant updatedAt) {
    }

    public record TemplateSummary(Long id, String name, Long typeId, String typeName,
                                  String status, int versionNo, Instant updatedAt) {
    }

    public record TemplateDetail(Long id, String name, Long typeId, String typeName,
                                 String status, int versionNo, String content,
                                 List<String> variables, Instant updatedAt) {
    }

    public record CreateTemplateRequest(String name, Long typeId, String content) {
    }

    public record CreateTemplateVersionRequest(String content) {
    }

    public record PaymentPlan(Long id, Long contractId, String itemName, String direction,
                              java.math.BigDecimal amount, String dueDate, String status,
                              String paidAt, String remark) {
    }

    public record PaymentRecord(Long id, Long paymentPlanId, java.math.BigDecimal amount, String paymentDate,
                                String status, Long voucherAttachmentId, String remark, Instant createdAt) {
    }

    public record CreatePaymentPlanRequest(String itemName, String direction,
                                           java.math.BigDecimal amount, String dueDate, String remark) {
    }

    public record CreatePaymentRecordRequest(java.math.BigDecimal amount, String paymentDate,
                                             Long voucherAttachmentId, String remark) {
    }

    public record FulfillmentMilestone(Long id, Long contractId, String name, String dueDate,
                                      String status, String completedAt, String remark) {
    }

    public record ContractChange(Long id, Long contractId, String changeNo, int changeType,
                                 String changeTitle, String changeReason, Long beforeContentVersionId,
                                 Long afterContentVersionId, Long effectiveContentVersionId,
                                 String signDate, String effectDate, int approvalStatus,
                                 String approvalNo, Instant updatedAt) {
    }

    public record CreateContractChangeRequest(int changeType, String changeTitle, String changeReason,
                                              String content) {
    }

    public record CategoryNode(Long id, Long parentId, String code, String name, int level,
                               boolean enabled, int versionNo, String versionStatus,
                               String description, Instant updatedAt) {
    }

    public record CreateCategoryRequest(Long parentId, String code, String name, int level,
                                        String description) {
    }

    public record UpdateCategoryRequest(String name, String description) {
    }

    public record CreateFulfillmentMilestoneRequest(String name, String dueDate, String remark) {
    }

    public record UpdateFulfillmentStatusRequest(String status, String remark) {
    }

    public record ExternalConnection(Long id, String connectionType, String name, String provider,
                                     String baseUrl, String status, boolean secretConfigured,
                                     String lastTestAt, String lastError, Instant updatedAt) {
    }

    public record CreateConnectionRequest(String connectionType, String name, String provider,
                                          String baseUrl, String secret) {
    }

    public record UpdateConnectionRequest(String name, String provider, String baseUrl, String secret) {
    }

    public record AiConfig(String name, String provider, String baseUrl, String model,
                           boolean secretConfigured, String draftWorkflowId, String extractWorkflowId,
                           String reviewWorkflowId, String fulfillmentWorkflowId, boolean enabled,
                           boolean draftEnabled, boolean extractEnabled, boolean reviewEnabled, boolean fulfillmentEnabled,
                           String runtimeProvider, String runtimeMode, String lastTestAt, String lastError) {
    }

    public record UpdateAiConfigRequest(String name, String provider, String baseUrl, String model,
                                        String secret, String draftWorkflowId, String extractWorkflowId,
                                        String reviewWorkflowId, String fulfillmentWorkflowId, boolean enabled,
                                        boolean draftEnabled, boolean extractEnabled, boolean reviewEnabled, boolean fulfillmentEnabled) {
    }

    public record AiDraftRequest(String contractName, String contractType, String partyName,
                                 String ourPartyName, String amount, String currency,
                                 String startDate, String endDate, String requirements) {
    }

    public record AiCapabilityTestResult(String capability, String provider, boolean success,
                                         String message, String testedAt) {
    }

    public record ConnectionLog(Long id, String action, String result, String message, Instant operatedAt) {
    }

    public record PermissionRole(String code, String name, String description, List<String> permissions,
                                 String dataScope, List<Long> typeIds, boolean sensitiveFieldAccess) {
    }

    public record UpdatePermissionRoleRequest(List<String> permissions, String dataScope,
                                              List<Long> typeIds, boolean sensitiveFieldAccess) {
    }

    public record ContractAuthorization(Long id, Long contractId, String targetUserId, boolean view, boolean edit,
                                        boolean export, boolean legalConfirm, int status, String expireTime, String remark) {}
    public record CreateContractAuthorizationRequest(Long contractId, String targetUserId, boolean view, boolean edit,
                                                     boolean export, boolean legalConfirm, String expireTime, String remark) {}

    public record GlobalAuditEntry(Long id, String module, String action, String result, String operator,
                                   String description, Instant operatedAt) {
    }

    public record StatisticsOverview(int total, int valid, int draft, int pendingSign, int fulfilling,
                                     int completed, int terminated, int voided, int archivedCount, boolean amountAvailable,
                                     java.math.BigDecimal contractAmountTotal,
                                     java.math.BigDecimal paymentTotal, java.math.BigDecimal paidTotal,
                                     int paymentPending, int paymentOverdue, int milestonePending,
                                     int milestoneOverdue, int aiPending, int aiFailed, int highRisk, int changeCount,
                                     List<StatisticsBucket> lifecycle, List<StatisticsBucket> types,
                                     List<StatisticsBucket> aiRisks) {
    }

    public record StatisticsBucket(String key, String label, int count, java.math.BigDecimal amount) {
    }

    public record AttentionItem(String key, String category, String severity, Long contractId, String contractNo,
                                String contractName, String title, String detail, String dueDate,
                                String target, String handlingStatus, String handlingOpinion,
                                String handledBy, Instant handledAt, boolean escalated) {
    }

    public record UpdateAttentionItemRequest(String status, String opinion) {
    }
}
