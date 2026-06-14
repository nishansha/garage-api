package com.triasoft.garage.projection;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface PayableRow {
    Long getPurchaseId();
    String getReferenceNo();
    String getVehicleNo();
    LocalDate getPurchaseDate();
    BigDecimal getAmount();
    BigDecimal getPendingAmount();
    LocalDate getLastPaymentDate();
    String getVendorName();
    String getVendorMobile();
}
