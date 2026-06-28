package com.triasoft.garage.dto;

import com.triasoft.garage.constants.ReturnStatusEnum;
import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class PurchaseReturnDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = -4252912998657330410L;
    private Long id;
    private Long purchaseId;
    private String purchaseReferenceNo;
    private Long inventoryId;
    private String uin;
    private String vehicleNo;
    private String vendorName;
    private LocalDate returnDate;
    private String reason;
    private String notes;
    private BigDecimal inventoryLandedCost;
    private BigDecimal vendorInvoiceAmount;     // what vendor originally billed (= purchase.totalAmount - expenses)
    private BigDecimal paidToVendor;            // cash already paid to vendor before return
    private BigDecimal outstandingAp;           // vendorInvoiceAmount - paidToVendor (auto-cancelled on return)
    private BigDecimal refundAmount;            // user-entered: what vendor will physically refund
    private BigDecimal unwindAmount;            // derived: outstandingAp + refundAmount (the DR A/P value on the journal)
    private BigDecimal lossOnReturn;            // = inventoryLandedCost - unwindAmount (restocking fee + sunk expenses)
    private ReturnStatusEnum status;
    private BigDecimal totalReceived;
    private BigDecimal remainingReceivable;     // refundAmount - totalReceived
    private java.util.List<PurchaseReturnReceiptDTO> receipts;
}
