package com.triasoft.garage.projection;

import java.math.BigDecimal;

public interface SaleMetrics {
    BigDecimal getTotalSalesThisMonth();

    BigDecimal getTotalSalesLastMonth();

    Long getTodayCount();

    Long getMonthCount();
}
