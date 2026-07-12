package com.triasoft.garage.projection;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface SaleLineRow {
    Long getSaleId();
    String getInvoiceNo();
    LocalDate getSaleDate();
    String getVehicleNo();
    String getCustomerName();
    BigDecimal getPurchaseRate();
    BigDecimal getPurchaseExpenses();
    BigDecimal getSaleRate();
    BigDecimal getProfit();
    Boolean getReturned();
    BigDecimal getPendingAmount();    // as of month-end
    BigDecimal getPendingTillDate();  // as of today (for totals only)
}
