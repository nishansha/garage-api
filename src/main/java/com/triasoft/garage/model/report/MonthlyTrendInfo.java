package com.triasoft.garage.model.report;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class MonthlyTrendInfo {
    private String month;
    private String monthLabel;
    private long salesCount;
    private BigDecimal totalRevenue;
    private BigDecimal otherIncome;
    private BigDecimal grossProfit;
    private double grossMarginPct;
    private BigDecimal netProfit;
    private double netMarginPct;
    private BigDecimal totalReceivables;
    private BigDecimal totalPayables;
    private BigDecimal totalExpenses;
}
