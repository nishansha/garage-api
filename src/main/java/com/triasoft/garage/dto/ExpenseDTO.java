package com.triasoft.garage.dto;

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
    private LocalDate date;
    private Long typeId;
    private String title;
    private String description;
    private BigDecimal amount;
}
