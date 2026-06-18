package com.triasoft.garage.model.report;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class PayableInfo {
    private Long purchaseId;
    private String referenceNo;
    private String vehicleNo;
    private LocalDate purchaseDate;
    private BigDecimal amount;
    private BigDecimal pendingAmount;
    private LocalDate lastPaymentDate;
    private String vendorName;
    private String vendorMobile;
}
