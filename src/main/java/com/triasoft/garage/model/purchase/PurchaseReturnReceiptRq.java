package com.triasoft.garage.model.purchase;

import com.triasoft.garage.locking.Versioned;
import com.triasoft.garage.constants.PaymentMethodEnum;
import com.triasoft.garage.validation.NullOrNotBlank;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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

    @NotNull(message = "REQUIRED")
    @DecimalMin(value = "0.0", inclusive = false, message = "MUST_BE_POSITIVE")
    private BigDecimal amount;

    private LocalDate paymentDate;
    private PaymentMethodEnum paymentMethod;

    @NotNull(message = "REQUIRED")
    private Long paymentAccountId;

    @Size(max = 50, message = "MAX_LENGTH")
    @NullOrNotBlank
    private String referenceNo;

    @Size(max = 500, message = "MAX_LENGTH")
    @NullOrNotBlank
    private String notes;
}
