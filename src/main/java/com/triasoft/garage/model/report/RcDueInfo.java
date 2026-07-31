package com.triasoft.garage.model.report;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class RcDueInfo {
    private Long purchaseId;
    private Long saleId;
    private String invoiceNo;
    private String vehicleNo;
    private LocalDate saleDate;
    private BigDecimal amount;
    private BigDecimal pendingAmount;
    private LocalDate lastReceiptDate;
    private String vendorName;
    private String vendorMobile;
}
