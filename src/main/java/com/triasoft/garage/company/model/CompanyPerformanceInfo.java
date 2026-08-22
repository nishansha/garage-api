package com.triasoft.garage.company.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class CompanyPerformanceInfo {
    private Long companyId;
    private String companyCode;
    private String companyName;
    private long salesCount;
    private BigDecimal salesRevenue;
    private BigDecimal grossProfit;
    private long purchaseCount;
    private BigDecimal purchaseCost;
    private long serviceSaleCount;
    private BigDecimal serviceRevenue;
    private BigDecimal generalExpenses;
}
