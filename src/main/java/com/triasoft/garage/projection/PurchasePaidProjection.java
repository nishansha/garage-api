package com.triasoft.garage.projection;

import java.math.BigDecimal;

public interface PurchasePaidProjection {
    Long getPurchaseId();
    BigDecimal getTotalPaid();
}
