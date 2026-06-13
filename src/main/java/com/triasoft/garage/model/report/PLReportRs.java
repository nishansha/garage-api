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
    private long unitsSold;

    // ── Revenue ──────────────────────────────────
    private BigDecimal vehicleSalesRevenue;
    private BigDecimal otherIncome;
    private BigDecimal totalRevenue;

    // ── Cost of Goods Sold ────────────────────────
    private BigDecimal costOfGoodsSold;
    private BigDecimal grossProfit;
    private double grossMarginPct;

    // ── Operating Expenses ────────────────────────
    private BigDecimal purchaseExpenses;
    private BigDecimal generalExpenses;
    private BigDecimal directAdjustments;
    private BigDecimal totalOperatingExpenses;

    // ── Bottom Line ───────────────────────────────
    private BigDecimal netProfit;
    private double netMarginPct;

    // ── Cash & Bank Position (current) ───────────
    private List<AccountBalanceInfo> cashPosition;
    private BigDecimal totalCashPosition;

    // ── Pending Collections (for period) ─────────
    private long pendingCount;
    private BigDecimal pendingAmount;
    private BigDecimal financePendingAmount;

}
