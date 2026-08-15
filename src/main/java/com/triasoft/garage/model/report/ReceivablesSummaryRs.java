package com.triasoft.garage.model.report;

import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class ReceivablesSummaryRs implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private long totalCount;
    private BigDecimal totalPendingAmount;
    // Additive fields (existing consumers unaffected): totalPendingAmount stays customer-only
    // for list/drill-down consistency, these two exist purely for the "Total Outstanding" KPI,
    // which must include what finance companies haven't yet disbursed - see
    // ReportService.getFinanceReceivablesSummary() for the finance-side breakdown/drill-down.
    private BigDecimal financePendingAmount;
    private BigDecimal totalOutstandingAmount;
    private List<ReceivableInfo> items;

}
