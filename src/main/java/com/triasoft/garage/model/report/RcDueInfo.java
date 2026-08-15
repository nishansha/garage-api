package com.triasoft.garage.model.report;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class RcDueInfo {
    private Long purchaseId;
    private String referenceNo;
    private LocalDate purchaseDate;
    // RC due is visible here from purchase time onward, before any sale exists - the actual
    // RECEIPT action still requires the unit to be sold (see PurchaseService.requireSold), but
    // showing the pending amount isn't gated on that. These sale fields are populated only once
    // a sale exists for this unit.
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
