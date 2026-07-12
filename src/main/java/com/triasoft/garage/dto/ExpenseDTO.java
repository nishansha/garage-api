package com.triasoft.garage.dto;

import com.triasoft.garage.validation.NullOrNotBlank;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
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
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = -6624797252960867490L;
    private Long id;
    private Long version;
    private LocalDate date;
    private Long typeId;

    @Size(max = 100, message = "MAX_LENGTH")
    @Pattern(regexp = "^[A-Za-z0-9 ]+$", message = "INVALID_CHARS")
    @NullOrNotBlank
    private String title;

    @Size(max = 255, message = "MAX_LENGTH")
    @NullOrNotBlank
    private String description;

    @NotNull(message = "REQUIRED")
    @DecimalMin(value = "0.0", inclusive = false, message = "MUST_BE_POSITIVE")
    private BigDecimal amount;

    private Long paymentAccountId;
    private Long purchaseId;
}
