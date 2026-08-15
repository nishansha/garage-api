package com.triasoft.garage.ledger.projection;

import java.math.BigDecimal;

// Same as SourceBalanceRow but grouped by (party_id, source_id) - needed wherever a source
// can carry lines for more than one party, so balances aren't blended across parties.
public interface PartySourceBalanceRow {
    Long getPartyId();
    Long getSourceId();
    BigDecimal getDebit();
    BigDecimal getCredit();
}
