package com.triasoft.garage.projection;

import java.math.BigDecimal;

public interface BalanceMetrics {
    String getMonthName();

    BigDecimal getSales();

    BigDecimal getPurchases();

    BigDecimal getExpenses();

    BigDecimal getProfit();
}
