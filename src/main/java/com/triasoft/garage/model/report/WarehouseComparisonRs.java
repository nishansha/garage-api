package com.triasoft.garage.model.report;

import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class WarehouseComparisonRs implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String month;
    private List<WarehousePerformanceInfo> warehouses;
    // General (non-purchase-tied) expenses for the period - rent, salaries, etc. have no link
    // to any vehicle/PO, so there is no warehouse to attribute them to. Shown once, tenant-wide,
    // rather than forced into any single warehouse or the "Unassigned" bucket (which is reserved
    // for vehicles/sales with no warehouse set).
    private BigDecimal unallocatedGeneralExpenses;

}
