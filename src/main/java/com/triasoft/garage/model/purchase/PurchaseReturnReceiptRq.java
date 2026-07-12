package com.triasoft.garage.model.purchase;

import com.triasoft.garage.concurrency.Versioned;
import com.triasoft.garage.constants.PaymentMethodEnum;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class PurchaseReturnReceiptRq implements Serializable, Versioned {

    @Serial
    private static final long serialVersionUID = 1L;

    /** Optimistic-lock version the client last read (required on update). */
    private Long version;
    private BigDecimal amount;
    private LocalDate paymentDate;
    private PaymentMethodEnum paymentMethod;
    private Long paymentAccountId;
    private String referenceNo;
    private String notes;
}
