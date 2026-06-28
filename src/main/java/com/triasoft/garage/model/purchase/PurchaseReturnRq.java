package com.triasoft.garage.model.purchase;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class PurchaseReturnRq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private LocalDate returnDate;
    private String reason;
    private String notes;

    // Cash the vendor will refund us. Defaults to paidToVendor (full refund).
    // Must be between 0 and paidToVendor. The unwind value used in the journal
    // is derived as: outstandingAp + refundAmount.
    private BigDecimal refundAmount;
}
