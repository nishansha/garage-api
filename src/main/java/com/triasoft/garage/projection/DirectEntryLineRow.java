package com.triasoft.garage.projection;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface DirectEntryLineRow {
    LocalDate getDate();
    String getName();
    BigDecimal getAmount();
    String getCategory();
    String getAccountName();
    String getDirection();
    String getClassification();
}
