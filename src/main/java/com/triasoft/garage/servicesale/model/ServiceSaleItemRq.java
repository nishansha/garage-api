package com.triasoft.garage.servicesale.model;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class ServiceSaleItemRq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // Nullable — an ad-hoc line not tied to the catalog.
    private Long serviceOfferingId;

    @NotBlank(message = "REQUIRED")
    @Size(max = 255, message = "MAX_LENGTH")
    private String description;

    @NotNull(message = "REQUIRED")
    @DecimalMin(value = "0.0", inclusive = false, message = "MUST_BE_POSITIVE")
    private BigDecimal qty;

    @NotNull(message = "REQUIRED")
    @DecimalMin(value = "0.0", inclusive = false, message = "MUST_BE_POSITIVE")
    private BigDecimal rate;
}
