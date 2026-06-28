package com.triasoft.garage.model.report;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class SaleReturnPayableInfo {
    private Long saleReturnId;
    private Long saleId;
    private String invoiceNo;
    private String vehicleNo;
    private LocalDate returnDate;
    private BigDecimal refundAmount;
    private BigDecimal pendingAmount;
    private LocalDate lastRefundDate;
    private String customerName;
    private String customerMobile;
}
