package com.triasoft.garage.repository;

import com.triasoft.garage.ledger.entity.Journal;
import com.triasoft.garage.projection.BalanceMetrics;
import com.triasoft.garage.projection.MonthlyTrendMetrics;
import com.triasoft.garage.projection.SummaryMetrics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

// Dashboard-specific journal aggregations - unlike ledger.repository.JournalRepository
// (generic double-entry mechanics), these hardcode this business's own reference-type
// vocabulary (PURCHASE/EXPENSE/PURCHASE_RETURN) and system-role naming
// (SALES_REVENUE/INVENTORY/COGS), so they stay in garage-api rather than moving to the
// ledger package.
@Repository
public interface JournalDashboardRepository extends JpaRepository<Journal, Long> {

    /**
     * Journal-derived financial summary for the dashboard. Aggregates CoA balances
     * directly from app_journal_detail, so any event that posts a balanced journal
     * (sales, purchases, returns, direct entries, manual journals, post-sale expenses)
     * is reflected without per-entity subselects.
     *
     * Includes both POSTED and REVERSED journals — the reversal entry's swapped
     * lines do the cancellation, matching the convention used by getTrialBalance
     * and getPLAccountBalances.
     *
     * CoA mapping (must match codes seeded by JournalService):
     *   4000 SALES_REVENUE        — vehicle sales (net of returns; revenue is credit-balance)
     *   1200 INVENTORY            — purchase inflows (filtered to PURCHASE/EXPENSE/PURCHASE_RETURN refs)
     *   5000 COGS                 — excluded from totalExpenses (operating expenses only)
     *   EXPENSE-type excl 5000    — operating + return losses (5510, 5520) + general (6xxx)
     *   4000 + 4520 − 5000        — gross profit (sales + return deduction income − COGS)
     */
    @Query(value = """
            SELECT
              COALESCE(SUM(CASE WHEN coa.system_role = 'SALES_REVENUE'
                                 AND j.journal_date >= :startOfMonth
                            THEN jd.credit_amount - jd.debit_amount ELSE 0 END), 0) as totalSales,
              COALESCE(SUM(CASE WHEN coa.system_role = 'SALES_REVENUE'
                                 AND j.journal_date >= :startOfLastMonth
                                 AND j.journal_date <  :startOfMonth
                            THEN jd.credit_amount - jd.debit_amount ELSE 0 END), 0) as salesBeforeMonth,

              COALESCE(SUM(CASE WHEN coa.system_role = 'INVENTORY'
                                 AND j.reference_type IN ('PURCHASE','EXPENSE','PURCHASE_RETURN')
                                 AND j.journal_date >= :startOfMonth
                            THEN jd.debit_amount - jd.credit_amount ELSE 0 END), 0) as totalPurchases,
              COALESCE(SUM(CASE WHEN coa.system_role = 'INVENTORY'
                                 AND j.reference_type IN ('PURCHASE','EXPENSE','PURCHASE_RETURN')
                                 AND j.journal_date >= :startOfLastMonth
                                 AND j.journal_date <  :startOfMonth
                            THEN jd.debit_amount - jd.credit_amount ELSE 0 END), 0) as purchasesBeforeMonth,

              COALESCE(SUM(CASE WHEN coa.type = 'EXPENSE'
                                 AND coa.system_role IS DISTINCT FROM 'COGS'
                                 AND j.journal_date >= :startOfMonth
                            THEN jd.debit_amount - jd.credit_amount ELSE 0 END), 0) as totalExpenses,
              COALESCE(SUM(CASE WHEN coa.type = 'EXPENSE'
                                 AND coa.system_role IS DISTINCT FROM 'COGS'
                                 AND j.journal_date >= :startOfLastMonth
                                 AND j.journal_date <  :startOfMonth
                            THEN jd.debit_amount - jd.credit_amount ELSE 0 END), 0) as expensesBeforeMonth,

              COALESCE(SUM(CASE WHEN coa.system_role IN ('SALES_REVENUE','RETURN_DEDUCTION_INCOME','COGS')
                                 AND j.journal_date >= :startOfMonth
                            THEN jd.credit_amount - jd.debit_amount ELSE 0 END), 0) as totalGrossProfit,
              COALESCE(SUM(CASE WHEN coa.system_role IN ('SALES_REVENUE','RETURN_DEDUCTION_INCOME','COGS')
                                 AND j.journal_date >= :startOfLastMonth
                                 AND j.journal_date <  :startOfMonth
                            THEN jd.credit_amount - jd.debit_amount ELSE 0 END), 0) as grossProfitBeforeMonth
            FROM app_journal_detail jd
            JOIN app_journal j ON j.id = jd.journal_id
            JOIN fnd_chart_of_accounts coa ON coa.id = jd.account_id
            WHERE j.tenant_id = :tenantId
              AND j.journal_date >= :startOfLastMonth
            """, nativeQuery = true)
    SummaryMetrics getFinancialSummaryFromJournal(@Param("tenantId") Long tenantId,
                                                  @Param("startOfMonth") LocalDate startOfMonth,
                                                  @Param("startOfLastMonth") LocalDate startOfLastMonth);

    /**
     * Journal-derived monthly balance sheet. Same CoA-aggregation pattern as
     * {@link #getFinancialSummaryFromJournal}, bucketed per month.
     *   sales     = CoA 4000 (Sales Revenue)
     *   purchases = CoA 1200 (Inventory) on PURCHASE/EXPENSE/PURCHASE_RETURN refs
     *   expenses  = EXPENSE-type CoAs excluding COGS (5000)
     *   profit    = all REVENUE + EXPENSE CoAs net (revenue earned − all expenses incurred,
     *               including COGS as an expense — gives true net profit for the month)
     */
    @Query(value = """
            WITH RECURSIVE months AS (
                SELECT DATE_TRUNC('month', CURRENT_DATE) as month_date
                UNION ALL
                SELECT DATE_TRUNC('month', month_date - INTERVAL '1 month')
                FROM months
                WHERE month_date > DATE_TRUNC('month', CURRENT_DATE - CAST(:monthCount - 1 || ' month' AS INTERVAL))
            )
            SELECT
                TO_CHAR(m.month_date, 'YYYY-MM') as monthName,
                COALESCE((SELECT SUM(jd.credit_amount - jd.debit_amount)
                          FROM app_journal_detail jd
                          JOIN app_journal j ON j.id = jd.journal_id
                          JOIN fnd_chart_of_accounts coa ON coa.id = jd.account_id
                          WHERE j.tenant_id = :tenantId AND coa.system_role = 'SALES_REVENUE'
                            AND DATE_TRUNC('month', j.journal_date) = m.month_date), 0) as sales,
                COALESCE((SELECT SUM(jd.debit_amount - jd.credit_amount)
                          FROM app_journal_detail jd
                          JOIN app_journal j ON j.id = jd.journal_id
                          JOIN fnd_chart_of_accounts coa ON coa.id = jd.account_id
                          WHERE j.tenant_id = :tenantId AND coa.system_role = 'INVENTORY'
                            AND j.reference_type IN ('PURCHASE','EXPENSE','PURCHASE_RETURN')
                            AND DATE_TRUNC('month', j.journal_date) = m.month_date), 0) as purchases,
                COALESCE((SELECT SUM(jd.debit_amount - jd.credit_amount)
                          FROM app_journal_detail jd
                          JOIN app_journal j ON j.id = jd.journal_id
                          JOIN fnd_chart_of_accounts coa ON coa.id = jd.account_id
                          WHERE j.tenant_id = :tenantId AND coa.type = 'EXPENSE' AND coa.system_role IS DISTINCT FROM 'COGS'
                            AND DATE_TRUNC('month', j.journal_date) = m.month_date), 0) as expenses,
                COALESCE((SELECT SUM(jd.credit_amount - jd.debit_amount)
                          FROM app_journal_detail jd
                          JOIN app_journal j ON j.id = jd.journal_id
                          JOIN fnd_chart_of_accounts coa ON coa.id = jd.account_id
                          WHERE j.tenant_id = :tenantId AND coa.type IN ('REVENUE','EXPENSE')
                            AND DATE_TRUNC('month', j.journal_date) = m.month_date), 0) as profit
            FROM months m
            ORDER BY m.month_date DESC
            """, nativeQuery = true)
    List<BalanceMetrics> getMonthlyBalanceSheetFromJournal(@Param("tenantId") Long tenantId, @Param("monthCount") int monthCount);

    /**
     * Journal-derived monthly trend. P&L metrics (revenue, gross profit, expenses)
     * come from CoA aggregations. salesCount, totalReceivables, totalPayables stay
     * snapshot-based because they're entity-state concepts (counts, pending balances)
     * that don't have a clean single-CoA mapping.
     *   totalRevenue    = all REVENUE CoAs (includes sales, return income, exchange gain, direct entries)
     *   otherIncome     = REVENUE CoAs excluding 4000 (sales)
     *   grossProfit     = 4000 + 4520 − 5000 (sales + return deduction income − COGS)
     *   totalExpenses   = EXPENSE CoAs excluding COGS (5000)
     *
     * totalReceivables/totalPayables are period-cohort (of sales/purchases whose sale_date/
     * order_date falls in that month) - NOT the same metric as ReportService.getReceivablesSummary()/
     * getPayablesSummary() (all-time outstanding, as of right now). Do not "fix" these to match
     * the ledger-derived current-balance queries - that would leak every prior month's still-open
     * balance into each month's trend point, which is a different (and wrong) thing for a
     * per-month trend to show. Also note: unlike the P&L report's totalReceivables (which cuts
     * payments off at period end) and totalPayables, both figures here sum ALL payments/receipts
     * ever recorded against that month's cohort, with no cutoff - i.e. "as of today," not "as of
     * that month's end." Verified by hand (2026-08-13): neither formula was ever exposed to the
     * control-account blending bugs found in the ledger-derived receivables/payables queries (the
     * sales formula never separates customer vs. finance-company debt, and the purchase formula's
     * total_amount already includes RC due and is never netted against it).
     */
    @Query(value = """
            WITH RECURSIVE months AS (
                SELECT DATE_TRUNC('month', CURRENT_DATE) as month_date
                UNION ALL
                SELECT DATE_TRUNC('month', month_date - INTERVAL '1 month')
                FROM months
                WHERE month_date > DATE_TRUNC('month',
                      CURRENT_DATE - CAST((:monthCount - 1) || ' month' AS INTERVAL))
            )
            SELECT
                TO_CHAR(m.month_date, 'YYYY-MM')  as month,
                TO_CHAR(m.month_date, 'Mon YYYY') as monthLabel,

                COALESCE((SELECT COUNT(*) FROM app_sale s
                          WHERE DATE_TRUNC('month', s.sale_date) = m.month_date
                          AND s.deleted = false AND s.tenant_id = :tenantId
                          AND s.payment_status <> 'REFUND'), 0) as salesCount,

                COALESCE((SELECT SUM(jd.credit_amount - jd.debit_amount)
                          FROM app_journal_detail jd
                          JOIN app_journal j ON j.id = jd.journal_id
                          JOIN fnd_chart_of_accounts coa ON coa.id = jd.account_id
                          WHERE j.tenant_id = :tenantId AND coa.type = 'REVENUE'
                            AND DATE_TRUNC('month', j.journal_date) = m.month_date), 0) as totalRevenue,

                COALESCE((SELECT SUM(jd.credit_amount - jd.debit_amount)
                          FROM app_journal_detail jd
                          JOIN app_journal j ON j.id = jd.journal_id
                          JOIN fnd_chart_of_accounts coa ON coa.id = jd.account_id
                          WHERE j.tenant_id = :tenantId AND coa.type = 'REVENUE' AND coa.system_role <> 'SALES_REVENUE'
                            AND DATE_TRUNC('month', j.journal_date) = m.month_date), 0) as otherIncome,

                COALESCE((SELECT SUM(jd.credit_amount - jd.debit_amount)
                          FROM app_journal_detail jd
                          JOIN app_journal j ON j.id = jd.journal_id
                          JOIN fnd_chart_of_accounts coa ON coa.id = jd.account_id
                          WHERE j.tenant_id = :tenantId AND coa.system_role IN ('SALES_REVENUE','RETURN_DEDUCTION_INCOME','COGS')
                            AND DATE_TRUNC('month', j.journal_date) = m.month_date), 0) as grossProfit,

                COALESCE((
                    SELECT SUM(s.net_sale_amount) - COALESCE(SUM(sp_sum.paid), 0)
                    FROM app_sale s
                    LEFT JOIN (
                        SELECT sale_id, SUM(amount) as paid
                        FROM app_sale_payment
                        WHERE deleted = false
                        GROUP BY sale_id
                    ) sp_sum ON sp_sum.sale_id = s.id
                    WHERE DATE_TRUNC('month', s.sale_date) = m.month_date
                    AND s.deleted = false AND s.tenant_id = :tenantId
                    AND s.payment_status IN ('PENDING','PARTIAL','FINANCE_PENDING')
                ), 0) as totalReceivables,

                COALESCE((
                    SELECT SUM(
                        CASE WHEN tradein.unit_cost IS NOT NULL
                             THEN tradein.unit_cost - LEAST(tradein.sale_rate, tradein.unit_cost) - COALESCE(pp_sum.paid, 0)
                             ELSE GREATEST(0,
                                      po.total_amount
                                      - COALESCE(pp_sum.paid, 0)
                                      - COALESCE(exp_sum.expense, 0)
                                      - COALESCE(pr_sum.returned_unwind, 0))
                        END
                    )
                    FROM app_purchase_order po
                    LEFT JOIN (
                        SELECT purchase_order_id, SUM(amount) as paid
                        FROM app_purchase_payment
                        WHERE deleted = false
                        GROUP BY purchase_order_id
                    ) pp_sum ON pp_sum.purchase_order_id = po.id
                    LEFT JOIN (
                        SELECT purchase_order_id, SUM(amount) as expense
                        FROM app_expense
                        WHERE deleted = false AND purchase_order_id IS NOT NULL
                        GROUP BY purchase_order_id
                    ) exp_sum ON exp_sum.purchase_order_id = po.id
                    LEFT JOIN (
                        SELECT pod.purchase_order_id, pod.unit_cost, s.sale_rate
                        FROM app_purchase_order_detail pod
                        JOIN app_inventory inv ON inv.purchase_order_detail_id = pod.id
                        JOIN app_sale s ON s.id = inv.source_sale_id
                        JOIN app_purchase_order po_x ON po_x.id = pod.purchase_order_id
                        WHERE inv.source_sale_id IS NOT NULL
                          AND po_x.buyback_recorded_at IS NULL
                    ) tradein ON tradein.purchase_order_id = po.id
                    LEFT JOIN (
                        SELECT purchase_id, SUM(return_amount) as returned_unwind
                        FROM app_purchase_return
                        WHERE deleted = false
                        GROUP BY purchase_id
                    ) pr_sum ON pr_sum.purchase_id = po.id
                    WHERE DATE_TRUNC('month', po.order_date) = m.month_date
                    AND po.deleted = false AND po.tenant_id = :tenantId
                    AND po.buyback_recorded_at IS NULL
                ), 0) as totalPayables,

                COALESCE((SELECT SUM(jd.debit_amount - jd.credit_amount)
                          FROM app_journal_detail jd
                          JOIN app_journal j ON j.id = jd.journal_id
                          JOIN fnd_chart_of_accounts coa ON coa.id = jd.account_id
                          WHERE j.tenant_id = :tenantId AND coa.type = 'EXPENSE' AND coa.system_role IS DISTINCT FROM 'COGS'
                            AND DATE_TRUNC('month', j.journal_date) = m.month_date), 0) as totalExpenses

            FROM months m
            ORDER BY m.month_date ASC
            """, nativeQuery = true)
    List<MonthlyTrendMetrics> getMonthlyTrendFromJournal(@Param("tenantId") Long tenantId, @Param("monthCount") int monthCount);

}
