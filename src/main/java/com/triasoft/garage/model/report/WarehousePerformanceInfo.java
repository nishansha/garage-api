package com.triasoft.garage.model.report;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class WarehousePerformanceInfo {
    private Long warehouseId;
    private String warehouseCode;
    private String warehouseName;
    private long stockCount;
    private BigDecimal stockValue;
    private long salesCount;
    private BigDecimal salesRevenue;
    private BigDecimal grossProfit;
    private double grossMarginPct;
    // Standalone service sales (car wash, etc.) - kept separate from vehicle sales above since
    // service sales carry no COGS/inventory leg (see JournalService.handleServiceSale), so
    // folding them into grossProfit/grossMarginPct would misrepresent vehicle margin. Same
    // "serviceRevenue" naming as ReportService's P&L (SystemCoaRole.SERVICE_REVENUE).
    private long serviceSalesCount;
    private BigDecimal serviceRevenue;
    // Purchasing activity for the period, by order_date - a DIFFERENT vehicle cohort than
    // salesCount/salesRevenue/grossProfit above (which are by sale_date). Informational, same
    // role as the Purchases section of /reports/pl: purchaseExpenses is already absorbed into
    // landedCost (and, once a vehicle sells, into grossProfit via landed_cost_at_sale) - it is
    // NOT a separate deduction on top of grossProfit, so do not subtract it again client-side.
    private long purchaseCount;
    private BigDecimal purchaseCost;
    private BigDecimal landedCost;
    private BigDecimal purchaseExpenses;
    // Live, as-of-now vendor payables for this warehouse (matches /reports/payables' own
    // semantics - not period-filtered).
    private long payablesCount;
    private BigDecimal totalPayables;
    // General (non-purchase-linked) expenses explicitly tagged to this warehouse via
    // Expense.warehouseId. Expenses left untagged fall into
    // WarehouseComparisonRs.unallocatedGeneralExpenses instead - same split CompanyPerformanceInfo
    // doesn't need, since a company-level expense is always attributable to that company via its
    // CoA account.
    private BigDecimal generalExpenses;
}
