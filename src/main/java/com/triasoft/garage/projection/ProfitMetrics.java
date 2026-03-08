package com.triasoft.garage.projection;

import java.math.BigDecimal;

public interface ProfitMetrics {
    BigDecimal getTotalSales();

    BigDecimal getTotalCost();

    BigDecimal getNetProfit();

    Long getUnitsSold();
}
