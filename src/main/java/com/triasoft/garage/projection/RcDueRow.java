package com.triasoft.garage.projection;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface RcDueRow {
    Long getPurchaseId();
    Long getSaleId();
    String getInvoiceNo();
    String getVehicleNo();
    LocalDate getSaleDate();
    BigDecimal getAmount();
    BigDecimal getPendingAmount();
    LocalDate getLastReceiptDate();
    String getVendorName();
    String getVendorMobile();
}
