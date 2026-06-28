package com.triasoft.garage.projection;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface SaleReturnPayableRow {
    Long getSaleReturnId();
    Long getSaleId();
    String getInvoiceNo();
    String getVehicleNo();
    LocalDate getReturnDate();
    BigDecimal getRefundAmount();
    BigDecimal getPendingAmount();
    LocalDate getLastRefundDate();
    String getCustomerName();
    String getCustomerMobile();
}
