package com.triasoft.garage.projection;

import java.math.BigDecimal;

public interface ExpenseMetrics {
    BigDecimal getTotalGeneralExpense();
    BigDecimal getTotalPurchaseExpense();
    BigDecimal getGeneralExpenseThisMonth();
    BigDecimal getPurchaseExpenseThisMonth();
}
