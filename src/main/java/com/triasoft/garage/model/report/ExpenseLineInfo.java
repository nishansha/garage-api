package com.triasoft.garage.model.report;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class ExpenseLineInfo {
    private LocalDate date;
    private String expenseName;
    private BigDecimal amount;
    private String accountName;
}
