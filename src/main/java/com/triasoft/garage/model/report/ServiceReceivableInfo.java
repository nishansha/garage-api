package com.triasoft.garage.model.report;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class ServiceReceivableInfo {
    private Long serviceSaleId;
    private String invoiceNo;
    private String paymentStatus;
    private LocalDate saleDate;
    private BigDecimal amount;
    private BigDecimal pendingAmount;
    private LocalDate lastPaymentDate;
    private String customerName;
    private String customerMobile;
}
