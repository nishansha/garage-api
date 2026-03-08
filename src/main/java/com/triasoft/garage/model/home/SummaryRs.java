package com.triasoft.garage.model.home;

import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
@Builder
public class SummaryRs implements Serializable {
    @Serial
    private static final long serialVersionUID = 6091616709728432616L;

    private String totalSales;
    private double salesDelta;
    private String totalPurchase;
    private double purchasesDelta;
    private String totalExpenses;
    private double expensesDelta;
    private String totalProfit;
    private double profitDelta;
}
