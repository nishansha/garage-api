package com.triasoft.garage.model.report;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class FinanceReceivableSaleInfo {
    private Long saleId;
    private String invoiceNo;
    private String paymentStatus;
    private String vehicleNo;
    private LocalDate saleDate;
    private BigDecimal financeAmount;
    private BigDecimal pendingAmount;
    private String customerName;
}
