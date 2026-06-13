package com.triasoft.garage.projection;

import java.math.BigDecimal;

public interface MonthlyTrendMetrics {
    String getMonth();
    String getMonthLabel();
    Long getSalesCount();
    BigDecimal getTotalRevenue();
    BigDecimal getGrossProfit();
    BigDecimal getTotalReceivables();
    BigDecimal getTotalPayables();
    BigDecimal getTotalExpenses();
}
