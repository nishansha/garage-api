package com.triasoft.garage.ledger.projection;

import java.math.BigDecimal;

// Raw debit/credit totals for one open item (source_id), agnostic to what the source
// actually is - whether a nonzero balance means "owed to us" or "owed by us" depends on
// which GL accounts normally back that source type, which is domain knowledge the ledger
// package doesn't have (see JournalService/ReportService for the sign interpretation).
public interface SourceBalanceRow {
    Long getSourceId();
    BigDecimal getDebit();
    BigDecimal getCredit();
}
