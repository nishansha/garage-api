package com.triasoft.garage.model.report;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class DirectEntryTotals {
    // ── By cash direction ─────────────────────────
    private long inCount;
    private BigDecimal inAmount;   // total of direction = IN
    private long outCount;
    private BigDecimal outAmount;  // total of direction = OUT

    // ── By P&L classification (reconciles with netProfit) ─
    private long incomeCount;
    private BigDecimal incomeAmount;   // classification = INCOME (feeds other income)
    private long expenseCount;
    private BigDecimal expenseAmount;  // classification = EXPENSE (feeds adjustments)
    private long otherCount;
    private BigDecimal otherAmount;    // classification = OTHER (capital/drawings, not P&L)
}
