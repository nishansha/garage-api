package com.triasoft.garage.projection;

import java.math.BigDecimal;

public interface AccountBalanceRow {
    Long getAccountId();
    String getCode();
    String getName();
    String getLabel();
    String getType();
    BigDecimal getTotalDebit();
    BigDecimal getTotalCredit();
}
