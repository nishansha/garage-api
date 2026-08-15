package com.triasoft.garage.model.report;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class FinanceCompanyReceivableInfo {
    private Long financeCompanyId;
    private String financeCompanyName;
    private String contactNumber;
    private BigDecimal totalPending;
    private List<FinanceReceivableSaleInfo> sales;
}
