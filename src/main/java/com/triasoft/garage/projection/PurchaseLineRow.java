package com.triasoft.garage.projection;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface PurchaseLineRow {
    Long getPurchaseId();
    String getReferenceNo();
    LocalDate getPurchaseDate();
    String getVehicleNo();
    String getVendorName();
    BigDecimal getPurchaseRate();
    BigDecimal getPurchaseExpenses();
    BigDecimal getLandedCost();
    Boolean getReturned();
    BigDecimal getReturnAmount();
    BigDecimal getPendingAmount();    // PO vendor payable as of month-end
    BigDecimal getPendingTillDate();  // PO vendor payable as of today (for totals only)
}
