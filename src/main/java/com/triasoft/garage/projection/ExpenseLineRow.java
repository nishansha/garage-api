package com.triasoft.garage.projection;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface ExpenseLineRow {
    LocalDate getDate();
    String getExpenseName();
    BigDecimal getAmount();
    String getAccountName();
}
