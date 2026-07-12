package com.triasoft.garage.model.report;

import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class PLReportRs implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String month;
    private String period;

    // ── Revenue & Bottom Line ─────────────────────
    private BigDecimal totalRevenue;
    private BigDecimal grossProfit;
    private double grossMarginPct;
    private BigDecimal returnDeductionIncome; // retained deductions on sale returns (income)
    private BigDecimal totalOperatingExpenses;
    private BigDecimal netProfit;
    private double netMarginPct;

    // ── Section totals (align with the detail lists below) ─
    private SalesTotals salesTotals;
    private PurchaseTotals purchaseTotals;
    private ExpenseTotals expenseTotals;
    private DirectEntryTotals directEntryTotals;

    // ── Receivables / Payables (from this period's deals) ─
    private BigDecimal totalReceivables;         // outstanding at month-end
    private BigDecimal totalReceivablesTillDate; // still outstanding today
    private BigDecimal totalPayables;            // outstanding at month-end
    private BigDecimal totalPayablesTillDate;    // still outstanding today

    // ── Cash & Bank Position (current) ───────────
    private List<AccountBalanceInfo> cashPosition;
    private BigDecimal totalCashPosition;

    // ── Sales in this period (per-vehicle breakdown) ─
    private List<SaleLineInfo> sales;

    // ── Purchases in this period (per-vehicle breakdown) ─
    private List<PurchaseLineInfo> purchases;

    // ── General expenses in this period (excludes purchase expenses) ─
    private List<ExpenseLineInfo> expenses;

    // ── Direct entries in this period (income / expense / other) ─
    private List<DirectEntryLineInfo> directEntries;

}
