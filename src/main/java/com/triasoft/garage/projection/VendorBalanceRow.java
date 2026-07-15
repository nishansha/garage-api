package com.triasoft.garage.projection;

import java.math.BigDecimal;

public interface VendorBalanceRow {
    Long getId();
    String getName();
    String getMobile();
    String getAddress();
    BigDecimal getOutstandingBalance();
}
