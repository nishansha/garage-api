package com.triasoft.garage.servicesale.model;

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
public class ServiceSalePaymentRq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull(message = "REQUIRED")
    @DecimalMin(value = "0.0", inclusive = false, message = "MUST_BE_POSITIVE")
    private BigDecimal amount;

    @NotNull(message = "REQUIRED")
    private LocalDate paymentDate;

    @NotNull(message = "REQUIRED")
    private PaymentMethodEnum paymentMethod;

    @Size(max = 100, message = "MAX_LENGTH")
    @NullOrNotBlank
    private String referenceNo;

    @Size(max = 500, message = "MAX_LENGTH")
    @NullOrNotBlank
    private String notes;

    @NotNull(message = "REQUIRED")
    private Long paymentAccountId;
}
