package com.triasoft.garage.projection;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface ReceivableRow {
    Long getSaleId();
    String getInvoiceNo();
    String getPaymentStatus();
    String getVehicleNo();
    LocalDate getSaleDate();
    BigDecimal getAmount();
    BigDecimal getPendingAmount();
    LocalDate getLastPaymentDate();
    String getCustomerName();
    String getCustomerMobile();
}
