package com.triasoft.garage.dto;

import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class ExpenseDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = -6624797252960867490L;
    private Long id;
    private LocalDate date;
    private String title;
    private String description;
    private BigDecimal amount;
}
