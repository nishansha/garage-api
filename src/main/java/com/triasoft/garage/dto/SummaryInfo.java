package com.triasoft.garage.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SummaryInfo implements Serializable {
    @Serial
    private static final long serialVersionUID = -3056695033130876024L;
    private String month;
    private BigDecimal totalSales;
    private BigDecimal totalPurchase;
    private BigDecimal totalExpenses;
    private BigDecimal totalProfit;
}
