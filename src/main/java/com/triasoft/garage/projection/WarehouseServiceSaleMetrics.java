package com.triasoft.garage.projection;

import java.math.BigDecimal;

public interface WarehouseServiceSaleMetrics {
    Long getWarehouseId();
    Long getServiceSaleCount();
    BigDecimal getServiceRevenue();
}
