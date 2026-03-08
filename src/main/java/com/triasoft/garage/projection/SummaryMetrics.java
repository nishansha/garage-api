package com.triasoft.garage.projection;

import java.math.BigDecimal;

public interface SummaryMetrics {
    BigDecimal getTotalSales();
    BigDecimal getSalesBeforeMonth();

    BigDecimal getTotalPurchases();
    BigDecimal getPurchasesBeforeMonth();

    BigDecimal getTotalExpenses();
    BigDecimal getExpensesBeforeMonth();

    BigDecimal getTotalGrossProfit();
    BigDecimal getGrossProfitBeforeMonth();
}
