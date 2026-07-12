package com.triasoft.garage.model.report;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class PurchaseTotals {
    private long count;                 // vehicles purchased (returned excluded)
    private long returnCount;           // returned rows in the list
    private BigDecimal purchaseRate;    // total base buy value
    private BigDecimal purchaseExpenses; // total capitalized expenses
    private BigDecimal landedCost;      // total landed cost
    private BigDecimal returnAmount;    // total vendor refund on returns
}
