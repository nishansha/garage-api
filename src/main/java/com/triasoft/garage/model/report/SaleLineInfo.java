package com.triasoft.garage.model.report;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class SaleLineInfo {
    private Long saleId;
    private String invoiceNo;
    private LocalDate saleDate;
    private String vehicleNo;
    private String customerName;
    private BigDecimal purchaseRate;
    private BigDecimal purchaseExpenses;
    private BigDecimal saleRate;
    private BigDecimal profit;
    private boolean returned;
    private BigDecimal pendingAmount; // receivable as of month-end
}
