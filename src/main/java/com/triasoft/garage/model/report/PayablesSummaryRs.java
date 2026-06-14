package com.triasoft.garage.model.report;

import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class PayablesSummaryRs implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private long totalCount;
    private BigDecimal totalPendingAmount;
    private List<PayableInfo> items;

}
