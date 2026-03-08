package com.triasoft.garage.model.purchase;

import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
@Builder
public class PurchaseSummaryRs implements Serializable {
    @Serial
    private static final long serialVersionUID = 2517356722741425934L;

    private String totalThisMonth;
    private Double monthRate;

    private Long totalCount;
    private Long todayCount;
}
