package com.triasoft.garage.projection;

import java.math.BigDecimal;

public interface PLPendingMetrics {
    Long getPendingCount();
    BigDecimal getPendingAmount();
    BigDecimal getFinancePendingAmount();
}
