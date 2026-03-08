package com.triasoft.garage.model.expense;

import com.triasoft.garage.model.common.GenericRq;
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
public class ExpenseRq extends GenericRq {

    @Serial
    private static final long serialVersionUID = 2329201647396146253L;

    private Long id;
    private Long typeId;
    private String name;
    private String title;
    private LocalDate date;
    private BigDecimal amount;
    private String description;
}
