package com.triasoft.garage.service.impl;

import com.triasoft.garage.model.report.WarehouseComparisonRs;
import com.triasoft.garage.model.report.WarehousePerformanceInfo;
import com.triasoft.garage.projection.PLExpenseMetrics;
import com.triasoft.garage.projection.PayableRow;
import com.triasoft.garage.projection.PurchaseLineRow;
import com.triasoft.garage.projection.WarehouseSalesMetrics;
import com.triasoft.garage.projection.WarehouseStockMetrics;
import com.triasoft.garage.repository.ExpenseRepository;
import com.triasoft.garage.repository.InventoryRepository;
import com.triasoft.garage.repository.PurchaseRepository;
import com.triasoft.garage.repository.SaleRepository;
import com.triasoft.garage.security.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WarehouseReportService {

    private static final DateTimeFormatter MONTH_DISPLAY = DateTimeFormatter.ofPattern("MMMM yyyy");
    // Collectors.groupingBy throws NPE on a null classifier result (warehouseId is legitimately
    // null for vehicles with no warehouse assigned) - group under this sentinel instead, no real
    // warehouse id can ever collide with it.
    private static final Long UNASSIGNED_KEY = -1L;

    private final InventoryRepository inventoryRepository;
    private final SaleRepository saleRepository;
    private final PurchaseRepository purchaseRepository;
    private final ExpenseRepository expenseRepository;

    private record PurchaseAgg(long count, BigDecimal purchaseCost, BigDecimal landedCost, BigDecimal purchaseExpenses) {
    }

    private record PayableAgg(long count, BigDecimal totalPending) {
    }

    public WarehouseComparisonRs compare(YearMonth yearMonth, Long warehouseId) {
        Long tenantId = TenantContext.get();
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();
        // Stock is a live/current-state concept, same as StockService.summary() - not tied
        // to the requested period. Only the sales and purchase figures below are period-scoped
        // (by sale_date and order_date respectively - two different vehicle cohorts). Payables
        // are live/as-of-now, matching /reports/payables' own semantics.
        LocalDateTime currentMonthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();

        List<WarehouseStockMetrics> stockRows = inventoryRepository.getStockSummaryByWarehouse(tenantId, currentMonthStart);
        List<WarehouseSalesMetrics> salesRows = saleRepository.getSalesPerformanceByWarehouse(tenantId, startDate, endDate);

        // Same "exclude returned rows from cost totals" rule ReportService.PurchaseTotals
        // already applies to /reports/pl, mirrored here for reconciliation.
        Map<Long, PurchaseAgg> purchaseByWarehouse = purchaseRepository.getPurchaseLinesByPeriod(tenantId, startDate, endDate).stream()
                .filter(r -> !Boolean.TRUE.equals(r.getReturned()))
                .collect(Collectors.groupingBy(r -> key(r.getWarehouseId()), LinkedHashMap::new, Collectors.collectingAndThen(Collectors.toList(), rows -> new PurchaseAgg(
                        rows.size(),
                        sumBd(rows, PurchaseLineRow::getPurchaseRate),
                        sumBd(rows, PurchaseLineRow::getLandedCost),
                        sumBd(rows, PurchaseLineRow::getPurchaseExpenses)
                ))));

        Map<Long, PayableAgg> payablesByWarehouse = purchaseRepository.findPayables(tenantId).stream()
                .collect(Collectors.groupingBy(r -> key(r.getWarehouseId()), LinkedHashMap::new, Collectors.collectingAndThen(Collectors.toList(), rows -> new PayableAgg(
                        rows.size(),
                        sumBd(rows, PayableRow::getPendingAmount)
                ))));

        Map<Long, WarehouseSalesMetrics> salesByWarehouse = new LinkedHashMap<>();
        for (WarehouseSalesMetrics row : salesRows) {
            salesByWarehouse.put(key(row.getWarehouseId()), row);
        }

        List<WarehousePerformanceInfo> warehouses = stockRows.stream()
                .map(stock -> toInfo(stock,
                        salesByWarehouse.get(key(stock.getWarehouseId())),
                        purchaseByWarehouse.get(key(stock.getWarehouseId())),
                        payablesByWarehouse.get(key(stock.getWarehouseId()))))
                .filter(info -> warehouseId == null || warehouseId.equals(info.getWarehouseId()))
                .toList();

        PLExpenseMetrics expenseMetrics = expenseRepository.getExpensesByPeriod(tenantId, startDate, endDate);

        return WarehouseComparisonRs.builder()
                .month(yearMonth.format(MONTH_DISPLAY))
                .warehouses(warehouses)
                .unallocatedGeneralExpenses(safe(expenseMetrics.getGeneralExpenses()))
                .build();
    }

    private WarehousePerformanceInfo toInfo(WarehouseStockMetrics stock, WarehouseSalesMetrics sales, PurchaseAgg purchase, PayableAgg payable) {
        BigDecimal stockValue = safe(stock.getTotalStockValue());
        BigDecimal salesRevenue = sales != null ? safe(sales.getTotalRevenue()) : BigDecimal.ZERO;
        BigDecimal grossProfit = sales != null ? safe(sales.getGrossProfit()) : BigDecimal.ZERO;
        return WarehousePerformanceInfo.builder()
                .warehouseId(stock.getWarehouseId())
                .warehouseCode(stock.getWarehouseCode())
                .warehouseName(stock.getWarehouseName())
                .stockCount(stock.getTotalItems() != null ? stock.getTotalItems() : 0L)
                .stockValue(stockValue)
                .salesCount(sales != null && sales.getSalesCount() != null ? sales.getSalesCount() : 0L)
                .salesRevenue(salesRevenue)
                .grossProfit(grossProfit)
                .grossMarginPct(pct(grossProfit, salesRevenue))
                .purchaseCount(purchase != null ? purchase.count() : 0L)
                .purchaseCost(purchase != null ? purchase.purchaseCost() : BigDecimal.ZERO)
                .landedCost(purchase != null ? purchase.landedCost() : BigDecimal.ZERO)
                .purchaseExpenses(purchase != null ? purchase.purchaseExpenses() : BigDecimal.ZERO)
                .payablesCount(payable != null ? payable.count() : 0L)
                .totalPayables(payable != null ? payable.totalPending() : BigDecimal.ZERO)
                .build();
    }

    private Long key(Long warehouseId) {
        return warehouseId != null ? warehouseId : UNASSIGNED_KEY;
    }

    private <T> BigDecimal sumBd(List<T> rows, Function<T, BigDecimal> extractor) {
        return rows.stream()
                .map(t -> safe(extractor.apply(t)))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal safe(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private double pct(BigDecimal numerator, BigDecimal denominator) {
        if (denominator == null || denominator.compareTo(BigDecimal.ZERO) == 0) return 0.0;
        return numerator.multiply(BigDecimal.valueOf(100))
                .divide(denominator, 2, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
