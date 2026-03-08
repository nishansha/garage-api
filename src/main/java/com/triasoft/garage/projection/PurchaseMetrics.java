package com.triasoft.garage.projection;

import java.math.BigDecimal;

public interface PurchaseMetrics {
    BigDecimal getTotalThisMonth();

    BigDecimal getTotalLastMonth();

    Long getTodayCount();

    Long getMonthCount();
}
