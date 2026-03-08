package com.triasoft.garage.projection;

import java.math.BigDecimal;

public interface StockMetrics {
    BigDecimal getTotalStockValue();
    BigDecimal getTotalStockValueLastMonth();
    Long getTotalItems();
    Long getItemsAddedThisMonth();
}
