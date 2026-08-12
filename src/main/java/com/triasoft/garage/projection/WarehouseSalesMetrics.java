package com.triasoft.garage.projection;

import java.math.BigDecimal;

public interface WarehouseSalesMetrics {
    Long getWarehouseId();
    String getWarehouseCode();
    String getWarehouseName();
    Long getSalesCount();
    BigDecimal getTotalRevenue();
    BigDecimal getGrossProfit();
}
