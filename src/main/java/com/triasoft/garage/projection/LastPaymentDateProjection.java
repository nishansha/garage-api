package com.triasoft.garage.projection;

import java.time.LocalDate;

public interface LastPaymentDateProjection {
    Long getSourceId();
    LocalDate getLastPaymentDate();
}
