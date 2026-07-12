package com.triasoft.garage.model.entry;

import com.triasoft.garage.concurrency.Versioned;
import com.triasoft.garage.constants.TransactionDirectionEnum;
import com.triasoft.garage.validation.NullOrNotBlank;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DirectEntryRq implements Serializable, Versioned {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long version;

    private LocalDate entryDate;

    @NotNull(message = "REQUIRED")
    private Long coaId;

    @NotNull(message = "REQUIRED")
    private TransactionDirectionEnum direction;

    @NotNull(message = "REQUIRED")
    @DecimalMin(value = "0.0", inclusive = false, message = "MUST_BE_POSITIVE")
    private BigDecimal amount;

    @NotNull(message = "REQUIRED")
    private Long paymentAccountId;

    @Size(max = 100, message = "MAX_LENGTH")
    @NullOrNotBlank
    private String partyName;

    @Size(max = 50, message = "MAX_LENGTH")
    @NullOrNotBlank
    private String referenceNo;

    @Size(max = 255, message = "MAX_LENGTH")
    @NullOrNotBlank
    private String description;

    @Size(max = 500, message = "MAX_LENGTH")
    @NullOrNotBlank
    private String notes;

}
