package com.triasoft.garage.model.sale;

import com.triasoft.garage.validation.NullOrNotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReturnDeductionRq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long expenseId;

    @Size(max = 255, message = "MAX_LENGTH")
    @NullOrNotBlank
    private String description;

    private BigDecimal amount;
}
