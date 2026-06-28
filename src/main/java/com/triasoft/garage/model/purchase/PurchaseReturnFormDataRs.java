package com.triasoft.garage.model.purchase;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PurchaseReturnFormDataRs implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long inventoryId;
    private String uin;
    private String vehicleNo;
    private Long purchaseId;
    private String purchaseReferenceNo;
    private String vendorName;
    private LocalDate purchaseDate;

    private BigDecimal inventoryLandedCost;     // full landed cost on books
    private BigDecimal vendorInvoiceAmount;     // what vendor billed us (totalAmount - expenses)
    private BigDecimal paidToVendor;            // sum of PurchasePayments so far
    private BigDecimal outstandingAp;           // vendorInvoice - paidToVendor (auto-cancelled on return)
    private BigDecimal suggestedRefundAmount;   // default = paidToVendor (full refund); user can lower if restocking fee applies
    private BigDecimal maxRefundAmount;         // = paidToVendor (vendor will never refund more than we paid)
}
