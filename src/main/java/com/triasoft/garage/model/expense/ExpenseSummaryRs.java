package com.triasoft.garage.model.expense;

import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
@Builder
public class ExpenseSummaryRs implements Serializable {
    @Serial
    private static final long serialVersionUID = 5295908043435203037L;
    private String companyExpenses;
    private String purchaseExpenses;
    private String companyExpThisMonth;
    private String purchaseExpThisMonth;
}
