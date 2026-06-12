package com.triasoft.garage.projection;

import com.triasoft.garage.constants.ExpenseLockWindow;

import java.time.LocalDate;

public interface PurchaseEditabilityProjection {
    Long getPurchaseId();
    LocalDate getSaleDate();
    Boolean getExpenseLockEnabled();
    ExpenseLockWindow getExpenseLockWindow();
}
