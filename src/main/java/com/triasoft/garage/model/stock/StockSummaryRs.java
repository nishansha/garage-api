package com.triasoft.garage.model.stock;

import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
@Builder
public class StockSummaryRs implements Serializable {
    @Serial
    private static final long serialVersionUID = 8067260590248280154L;
    private String stockValue;
    private Double assetRate;

    private Long totalItems;
    private Long itemsThisMonth;

}
