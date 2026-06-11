package com.triasoft.garage.projection;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface PurchaseListProjection {
    Long getId();
    LocalDate getDate();
    String getCode();
    String getVehicleNo();
    String getBrandName();
    String getModelName();
    String getVariantName();
    BigDecimal getPurchaseRate();
}
