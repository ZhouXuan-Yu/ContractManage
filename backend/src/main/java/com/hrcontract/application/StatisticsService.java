package com.hrcontract.application;

import com.hrcontract.application.ContractModels.StatisticsBucket;
import com.hrcontract.application.ContractModels.StatisticsOverview;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * Reads the authoritative t_contract_* tables (DOCX-aligned). String statuses from the
 * compatibility tables are replaced by the formal TINYINT codes materialized by the cutover
 * triggers: contract_status 0=draft 2=pending-sign 3=fulfilling 4=completed 5=terminated 6=voided;
 * pay_status/perform_status 0=pending 2=done 3=cancelled; ai task_status 1=pending 2=done 3=failed;
 * ai review overall_risk_level 3=high.
 */
@Service
public class StatisticsService {
    private final JdbcTemplate jdbc;
    private final PermissionAuditService permissions;

    public StatisticsService(JdbcTemplate jdbc, PermissionAuditService permissions) {
        this.jdbc = jdbc;
        this.permissions = permissions;
    }

    public StatisticsOverview overview(String role) {
        return overview(role, "10001", "100", "101");
    }

    public StatisticsOverview overview(String role, String userId, String orgId, String departmentId) {
        var permission = permissions.role(role);
        String typeFilter = "";
        java.util.List<Object> filterArgs = new java.util.ArrayList<>();
        if ("SELF_AND_PARTICIPANTS".equals(permission.dataScope())) { typeFilter += " AND c.applicant_id=?"; filterArgs.add(userId); }
        else if ("ORGANIZATION".equals(permission.dataScope())) { typeFilter += " AND c.applicant_org_id=?"; filterArgs.add(orgId); }
        else if ("DEPARTMENT".equals(permission.dataScope())) { typeFilter += " AND c.applicant_department_id=?"; filterArgs.add(departmentId); }
        else if ("NAMED_CONTRACTS".equals(permission.dataScope())) typeFilter += " AND 1=0";
        if (!permission.typeIds().isEmpty() && !"ADMIN".equals(role)) {
            typeFilter += " AND c.category_type_id IN (" + "?, ".repeat(permission.typeIds().size()).replaceFirst(", $", "") + ")";
            filterArgs.addAll(permission.typeIds());
        }
        Object[] typeArgs = filterArgs.toArray();
        String from = " FROM t_contract_main c LEFT JOIN contract_draft d ON d.id=c.id WHERE c.deleted=0" + typeFilter;
        int total = count(from, typeArgs);
        int archivedCount = count(from + " AND COALESCE(d.archived,0)=1", typeArgs);
        String activeFrom = from + " AND COALESCE(d.archived,0)=0";
        int voided = count(activeFrom + " AND c.contract_status=6", typeArgs);
        int valid = total - archivedCount - voided;
        int draft = count(activeFrom + " AND c.contract_status=0", typeArgs);
        int sign = count(activeFrom + " AND c.contract_status=2", typeArgs);
        int fulfilling = count(activeFrom + " AND c.contract_status=3", typeArgs);
        int completed = count(activeFrom + " AND c.contract_status=4", typeArgs);
        int terminated = count(activeFrom + " AND c.contract_status=5", typeArgs);
        String paymentFrom = " FROM t_contract_payment_plan p JOIN t_contract_main c ON c.id=p.contract_id AND c.deleted=0 LEFT JOIN contract_draft d ON d.id=c.id WHERE COALESCE(d.archived,0)=0" + typeFilter;
        String milestoneFrom = " FROM t_contract_perform_item m JOIN t_contract_main c ON c.id=m.contract_id AND c.deleted=0 LEFT JOIN contract_draft d ON d.id=c.id WHERE COALESCE(d.archived,0)=0" + typeFilter;
        String aiFrom = " JOIN t_contract_main c ON c.id=%s.contract_id AND c.deleted=0 LEFT JOIN contract_draft d ON d.id=c.id WHERE COALESCE(d.archived,0)=0" + typeFilter;
        int paymentPending = scalar("SELECT COUNT(*)" + paymentFrom + " AND p.pay_status=0", typeArgs);
        int paymentOverdue = scalar("SELECT COUNT(*)" + paymentFrom + " AND p.pay_status=0 AND p.plan_due_date < CURRENT_DATE", typeArgs);
        int milestonePending = scalar("SELECT COUNT(*)" + milestoneFrom + " AND m.perform_status=0", typeArgs);
        int milestoneOverdue = scalar("SELECT COUNT(*)" + milestoneFrom + " AND m.perform_status=0 AND m.plan_finish_date < CURRENT_DATE", typeArgs);
        int aiPending = scalar("SELECT COUNT(*) FROM t_contract_ai_extract_task a" + aiFrom.formatted("a") + " AND a.task_status=1", typeArgs);
        int aiFailed = scalar("SELECT COUNT(*) FROM t_contract_ai_extract_task a" + aiFrom.formatted("a") + " AND a.task_status=3", typeArgs);
        int highRisk = scalar("SELECT COUNT(*) FROM t_contract_ai_review r" + aiFrom.formatted("r") + " AND r.overall_risk_level=3", typeArgs);
        BigDecimal contractAmountTotal = decimal("SELECT COALESCE(SUM(c.total_amount),0)" + activeFrom, typeArgs);
        BigDecimal paymentTotal = decimal("SELECT COALESCE(SUM(p.plan_amount),0)" + paymentFrom, typeArgs);
        BigDecimal paidTotal = decimal("SELECT COALESCE(SUM(r.payment_amount),0) FROM t_contract_payment_record r JOIN t_contract_payment_plan p ON p.id=r.payment_plan_id JOIN t_contract_main c ON c.id=p.contract_id AND c.deleted=0 LEFT JOIN contract_draft d ON d.id=c.id WHERE COALESCE(d.archived,0)=0 AND r.deleted=0 AND r.payment_status=1" + typeFilter, typeArgs);
        int changeCount = scalar("SELECT COUNT(*) FROM t_contract_change x JOIN t_contract_main c ON c.id=x.contract_id AND c.deleted=0 LEFT JOIN contract_draft d ON d.id=c.id WHERE COALESCE(d.archived,0)=0 AND x.deleted=0" + typeFilter, typeArgs);
        return new StatisticsOverview(total, valid, draft, sign, fulfilling, completed, terminated, voided, archivedCount, true,
                contractAmountTotal, paymentTotal, paidTotal, paymentPending, paymentOverdue, milestonePending, milestoneOverdue,
                aiPending, aiFailed, highRisk, changeCount, lifecycle(activeFrom, typeArgs), types(typeFilter + " AND COALESCE(d.archived,0)=0", typeArgs), aiRisks(typeFilter + " AND COALESCE(d.archived,0)=0", typeArgs));
    }

    private List<StatisticsBucket> lifecycle(String from, Object[] args) {
        return List.of(bucket("DRAFT", "草稿", count(from + " AND c.contract_status=0", args), null),
                bucket("SIGN", "待签署", count(from + " AND c.contract_status=2", args), null),
                bucket("FULFILLING", "履行中", count(from + " AND c.contract_status=3", args), null),
                bucket("COMPLETED", "已完成", count(from + " AND c.contract_status=4", args), null),
                bucket("OTHER", "其他有效状态", count(from + " AND c.contract_status NOT IN (0,2,3,6)", args), null));
    }
    private List<StatisticsBucket> types(String filter, Object[] args) {
        return jdbc.query("SELECT c.category_type_id, COUNT(*) FROM t_contract_main c LEFT JOIN contract_draft d ON d.id=c.id WHERE c.deleted=0" + filter + " GROUP BY c.category_type_id ORDER BY COUNT(*) DESC",
                (rs, row) -> bucket(String.valueOf(rs.getLong(1)), "类型 " + rs.getLong(1), rs.getInt(2), null), args);
    }
    private List<StatisticsBucket> aiRisks(String filter, Object[] args) {
        return jdbc.query("SELECT r.overall_risk_level, COUNT(*) FROM t_contract_ai_review r JOIN t_contract_main c ON c.id=r.contract_id AND c.deleted=0 LEFT JOIN contract_draft d ON d.id=c.id WHERE 1=1" + filter + " GROUP BY r.overall_risk_level ORDER BY COUNT(*) DESC",
                (rs, row) -> bucket(String.valueOf(rs.getInt(1)), riskLevelName(rs.getInt(1)), rs.getInt(2), null), args);
    }
    private String riskLevelName(int level) {
        return switch (level) { case 3 -> "高风险"; case 2 -> "中风险"; case 1 -> "低风险"; default -> "未知"; };
    }
    private StatisticsBucket bucket(String key, String label, int count, BigDecimal amount) { return new StatisticsBucket(key, label, count, amount); }
    private int count(String sql, Object[] args) { return scalar("SELECT COUNT(*)" + sql.substring(sql.indexOf(" FROM")), args); }
    private int scalar(String sql, Object[] args) { Integer value = jdbc.queryForObject(sql, Integer.class, args); return value == null ? 0 : value; }
    private BigDecimal decimal(String sql, Object[] args) {
        Number value = jdbc.queryForObject(sql, Number.class, args);
        return value == null ? BigDecimal.ZERO : BigDecimal.valueOf(value.doubleValue());
    }
}
