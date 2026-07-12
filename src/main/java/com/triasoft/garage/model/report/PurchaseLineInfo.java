package com.triasoft.garage.model.report;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class PurchaseLineInfo {
    private Long purchaseId;
    private String referenceNo;
    private LocalDate purchaseDate;
    private String vehicleNo;
    private String vendorName;
    private BigDecimal purchaseRate;
    private BigDecimal purchaseExpenses;
    private BigDecimal landedCost;
    private boolean returned;
    private BigDecimal returnAmount;
    private BigDecimal pendingAmount; // vendor payable as of month-end (PO level)
}
