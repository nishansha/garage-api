package com.triasoft.garage.model.report;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class SalesTotals {
    private long count;                 // net units sold (returned excluded)
    private long returnCount;           // returned rows in the list
    private BigDecimal saleRate;        // total sale value
    private BigDecimal cost;            // total COGS (landed cost)
    private BigDecimal purchaseExpenses; // capitalized expenses within cost
    private BigDecimal profit;          // gross profit
}
