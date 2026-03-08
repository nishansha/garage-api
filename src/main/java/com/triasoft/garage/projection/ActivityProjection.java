package com.triasoft.garage.projection;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface ActivityProjection {
    String getActivityType();
    String getDescription();
    LocalDateTime getDateTime();
    String getTxnType(); // 'C' for Credit (Sale), 'D' for Debit (Purchase/Expense)
    BigDecimal getTxnAmount();
}
