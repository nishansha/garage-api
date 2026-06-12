package com.triasoft.garage.dto;

import com.triasoft.garage.constants.StatusEnum;
import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class SaleDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 3200483156091203742L;
    private Long id;
    private String invoiceNo;
    private LocalDate date;
    private Long stockId;
    private String customerName;
    private String customerMobileNo;
    private String vehicleNo;
    private String brandName;
    private String modelName;
    private String variantName;
    private BigDecimal saleRate;
    private BigDecimal netSaleAmount;
    private BigDecimal profit;
    private StatusEnum paymentStatus;
    private boolean isExchange;
    private BigDecimal exchangeAmount;
    private PurchaseDTO exchangeVehicleDetails;
    private boolean isFinanced;
    private String financeCompany;
    private BigDecimal financeAmount;
    private BigDecimal emiAmount;
    private Long statusId;
    private String statusName;
    private BigDecimal paidAmount;
    private BigDecimal pendingAmount;
    private List<SalePaymentDTO> payments;

}
