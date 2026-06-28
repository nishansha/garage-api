package com.triasoft.garage.projection;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface PurchaseReturnReceivableRow {
    Long getPurchaseReturnId();
    Long getPurchaseId();
    String getPurchaseReferenceNo();
    String getVehicleNo();
    LocalDate getReturnDate();
    BigDecimal getCashRefundExpected();
    BigDecimal getPendingAmount();
    LocalDate getLastReceiptDate();
    String getVendorName();
    String getVendorMobile();
}
