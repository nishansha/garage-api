package com.triasoft.garage.projection;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface LedgerRow {
    Long getJournalId();
    LocalDate getJournalDate();
    String getReferenceType();
    Long getReferenceId();
    String getJournalDescription();
    String getLineDescription();
    BigDecimal getDebit();
    BigDecimal getCredit();
}
