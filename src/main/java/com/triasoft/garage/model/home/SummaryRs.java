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
    private String totalPurchase;
    private String totalExpenses;
    private String totalProfit;
}
