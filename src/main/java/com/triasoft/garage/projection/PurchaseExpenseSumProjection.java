package com.triasoft.garage.projection;

import java.math.BigDecimal;

public interface PurchaseExpenseSumProjection {
    Long getPurchaseId();
    BigDecimal getTotalExpenses();
}
