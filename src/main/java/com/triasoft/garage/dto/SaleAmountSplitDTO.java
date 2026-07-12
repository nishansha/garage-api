package com.triasoft.garage.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Builder
public class SaleAmountSplitDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private Long id;

    @NotNull(message = "REQUIRED")
    private Long typeId;

    private String typeDesc;

    @NotNull(message = "REQUIRED")
    @DecimalMin(value = "0.0", inclusive = false, message = "MUST_BE_POSITIVE")
    private BigDecimal amount;
}
