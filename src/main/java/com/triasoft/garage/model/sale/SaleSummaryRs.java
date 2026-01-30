package com.triasoft.garage.model.sale;

import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
@Builder
public class SaleSummaryRs implements Serializable {
    @Serial
    private static final long serialVersionUID = -286836815777006586L;
    private String totalThisMonth;
    private Double monthRate;

    private Long totalCount;
    private Long todayCount;
}
