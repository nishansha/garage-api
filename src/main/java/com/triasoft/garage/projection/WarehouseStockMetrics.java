package com.triasoft.garage.projection;

import java.math.BigDecimal;

public interface WarehouseStockMetrics {
    Long getWarehouseId();
    String getWarehouseCode();
    String getWarehouseName();
    BigDecimal getTotalStockValue();
    BigDecimal getTotalStockValueLastMonth();
    Long getTotalItems();
    Long getItemsAddedThisMonth();
}
