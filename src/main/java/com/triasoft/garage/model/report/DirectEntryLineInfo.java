package com.triasoft.garage.model.report;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class DirectEntryLineInfo {
    private LocalDate date;
    private String name;
    private BigDecimal amount;
    private String category;      // Chart-of-account head (income/expense category)
    private String accountName;   // Payment account the cash moved through
    private String direction;     // IN | OUT
    private String classification; // INCOME | EXPENSE | OTHER
}
