package com.hrcontract.application;

import com.hrcontract.application.ContractModels.AttentionItem;
import com.hrcontract.application.ContractModels.ContractSummary;
import com.hrcontract.application.ContractModels.FulfillmentMilestone;
import com.hrcontract.application.ContractModels.PaymentPlan;
import com.hrcontract.application.ContractModels.StatisticsOverview;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

/** Builds scoped CSV reports without persisting an additional file copy. */
@Service
public class ExportService {
    private final ContractService contracts;
    private final FulfillmentService fulfillment;
    private final StatisticsService statistics;
    private final AttentionService attention;
    private final PermissionAuditService permissions;

    public ExportService(ContractService contracts, FulfillmentService fulfillment, StatisticsService statistics, AttentionService attention, PermissionAuditService permissions) {
        this.contracts = contracts; this.fulfillment = fulfillment; this.statistics = statistics; this.attention = attention; this.permissions = permissions;
    }

    public byte[] report(String role, String userId, String orgId, String departmentId, String operator) {
        var permission = permissions.role(role);
        StatisticsOverview overview = statistics.overview(role, userId, orgId, departmentId);
        List<ContractSummary> visibleContracts = contracts.listContracts(permission.dataScope(), userId, orgId, departmentId, permission.typeIds());
        List<AttentionItem> items = attention.list(role, userId, orgId, departmentId);
        StringBuilder csv = new StringBuilder("\uFEFF");
        row(csv, "合同管理分析导出", "生成时间", Instant.now().toString(), "导出人", operator);
        row(csv, "汇总", "可见合同", overview.valid(), "履行中", overview.fulfilling(), "合同金额", overview.contractAmountTotal(), "计划收付款", overview.paymentTotal(), "实际收付款", overview.paidTotal(), "高风险", overview.highRisk());
        csv.append('\n');
        row(csv, "合同台账", "合同编号", "合同名称", "合同类型", "交易对方", "状态", "更新时间");
        for (ContractSummary contract : visibleContracts) row(csv, "合同", contract.contractNo(), contract.name(), contract.typeName(), contract.partyName(), contract.status(), contract.updatedAt());
        csv.append('\n');
        row(csv, "收付款计划", "合同编号", "款项名称", "方向", "计划金额", "到期日", "状态");
        for (ContractSummary contract : visibleContracts) for (PaymentPlan payment : fulfillment.payments(contract.id())) row(csv, "收付款", contract.contractNo(), payment.itemName(), payment.direction(), payment.amount(), payment.dueDate(), payment.status());
        csv.append('\n');
        row(csv, "履约节点", "合同编号", "节点名称", "计划完成日", "状态", "说明");
        for (ContractSummary contract : visibleContracts) for (FulfillmentMilestone milestone : fulfillment.milestones(contract.id())) row(csv, "履约", contract.contractNo(), milestone.name(), milestone.dueDate(), milestone.status(), milestone.remark());
        csv.append('\n');
        row(csv, "风险与待办", "合同编号", "事项", "优先级", "处理状态", "处理意见", "处理人", "处理时间", "已升级");
        for (AttentionItem item : items) row(csv, "待办", item.contractNo(), item.title(), item.severity(), item.handlingStatus(), item.handlingOpinion(), item.handledBy(), item.handledAt(), item.escalated() ? "是" : "否");
        permissions.audit("导出", "导出分析报表", "SUCCESS", operator, "导出 " + visibleContracts.size() + " 份可见合同、" + items.size() + " 条待办");
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private void row(StringBuilder csv, Object... values) { for (int index = 0; index < values.length; index++) { if (index > 0) csv.append(','); String value = values[index] == null ? "" : String.valueOf(values[index]); csv.append('"').append(value.replace("\"", "\"\"").replace("\r", " ").replace("\n", " ")).append('"'); } csv.append('\n'); }
}
