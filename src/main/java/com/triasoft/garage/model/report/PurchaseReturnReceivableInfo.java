package com.triasoft.garage.model.report;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class PurchaseReturnReceivableInfo {
    private Long purchaseReturnId;
    private Long purchaseId;
    private String purchaseReferenceNo;
    private String vehicleNo;
    private LocalDate returnDate;
    private BigDecimal cashRefundExpected;
    private BigDecimal pendingAmount;
    private LocalDate lastReceiptDate;
    private String vendorName;
    private String vendorMobile;
}
