package com.triasoft.garage.model.sale;

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
    private String description;
    private BigDecimal amount;
}
