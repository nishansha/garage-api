package com.triasoft.garage.servicesale.model;

import com.triasoft.garage.locking.Versioned;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class ServiceOfferingRq implements Serializable, Versioned {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long version;

    @NotNull(message = "REQUIRED")
    private Long warehouseId;

    @NotBlank(message = "REQUIRED")
    @Size(max = 50, message = "MAX_LENGTH")
    private String code;

    @NotBlank(message = "REQUIRED")
    @Size(max = 255, message = "MAX_LENGTH")
    private String name;

    @NotNull(message = "REQUIRED")
    @DecimalMin(value = "0.0", inclusive = false, message = "MUST_BE_POSITIVE")
    private BigDecimal defaultRate;

    private boolean active = true;
}
