package com.triasoft.garage.model.expense;

import com.triasoft.garage.locking.Versioned;
import com.triasoft.garage.model.common.GenericRq;
import com.triasoft.garage.validation.ExpenseTypeRequired;
import com.triasoft.garage.validation.NullOrNotBlank;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ExpenseTypeRequired
public class ExpenseRq extends GenericRq implements Versioned {

    @Serial
    private static final long serialVersionUID = 2329201647396146253L;

    private Long version;
    private Long id;

    private Long typeId;
    private String name;

    @Size(max = 100, message = "MAX_LENGTH")
    @Pattern(regexp = "^[A-Za-z0-9 ]+$", message = "INVALID_CHARS")
    @NullOrNotBlank
    private String title;

    @NotNull(message = "REQUIRED")
    private LocalDate date;

    @NotNull(message = "REQUIRED")
    @DecimalMin(value = "0.0", inclusive = false, message = "MUST_BE_POSITIVE")
    private BigDecimal amount;

    @Size(max = 255, message = "MAX_LENGTH")
    @NullOrNotBlank
    private String description;

    private Long purchaseId;

    @NotNull(message = "REQUIRED")
    private Long paymentAccountId;
}
