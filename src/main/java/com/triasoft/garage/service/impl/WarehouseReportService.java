package com.triasoft.garage.service.impl;

import com.triasoft.garage.constants.SystemCoaRole;
import com.triasoft.garage.ledger.repository.JournalDetailRepository;
import com.triasoft.garage.model.report.WarehouseComparisonRs;
import com.triasoft.garage.model.report.WarehousePerformanceInfo;
import com.triasoft.garage.projection.PayableRow;
import com.triasoft.garage.projection.PurchaseLineRow;
import com.triasoft.garage.projection.WarehouseAmountRow;
import com.triasoft.garage.projection.WarehouseSalesMetrics;
import com.triasoft.garage.projection.WarehouseServiceSaleMetrics;
import com.triasoft.garage.projection.WarehouseStockMetrics;
import com.triasoft.garage.repository.ExpenseRepository;
import com.triasoft.garage.repository.InventoryRepository;
import com.triasoft.garage.repository.PurchaseRepository;
import com.triasoft.garage.repository.SaleRepository;
import com.triasoft.garage.repository.SaleReturnRepository;
import com.triasoft.garage.security.tenant.TenantContext;
import com.triasoft.garage.servicesale.repository.ServiceSaleRepository;
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
    private final ServiceSaleRepository serviceSaleRepository;
    private final SaleReturnRepository saleReturnRepository;
    private final JournalDetailRepository journalDetailRepository;

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
        List<WarehouseServiceSaleMetrics> serviceSalesRows = serviceSaleRepository.getServiceSalePerformanceByWarehouse(tenantId, startDate, endDate);

        // Same "exclude returned rows from cost totals" rule ReportService.PurchaseTotals
        // already applies to /reports/pl, mirrored here for reconciliation.
        // null companyId = every company's warehouses, since this report spans all of them.
        Map<Long, PurchaseAgg> purchaseByWarehouse = purchaseRepository.getPurchaseLinesByPeriod(tenantId, null, null, startDate, endDate).stream()
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

        Map<Long, WarehouseServiceSaleMetrics> serviceSalesByWarehouse = new LinkedHashMap<>();
        for (WarehouseServiceSaleMetrics row : serviceSalesRows) {
            serviceSalesByWarehouse.put(key(row.getWarehouseId()), row);
        }

        // Retained deductions + exchange gains on sale returns are real profit earned on the
        // return itself, netted out of the plain sales figures above (same gap ReportService.
        // getProfitAndLoss already closes tenant-wide) - add them back here too. Symmetrically,
        // LOSS_RETURNED_EXCHANGE and LOSS_PURCHASE_RETURN are the loss-side counterparts and
        // must be subtracted, or profit is systematically overstated. GAIN_ON_EXCHANGE_ADJ is
        // posted on BOTH a SALE_RETURN journal AND a PURCHASE_RETURN journal (see
        // JournalDetailRepository's comment), so it needs both warehouse-trace queries, summed.
        Map<Long, BigDecimal> returnDeductionByWarehouse = toAmountMap(saleReturnRepository.sumDeductionIncomeByWarehouse(tenantId, startDate, endDate));
        Map<Long, BigDecimal> exchangeGainByWarehouse = mergeAmountMaps(
                toAmountMap(journalDetailRepository.sumSaleReturnRoleByWarehouse(tenantId, SystemCoaRole.GAIN_ON_EXCHANGE_ADJ.name(), startDate, endDate)),
                toAmountMap(journalDetailRepository.sumPurchaseReturnRoleByWarehouse(tenantId, SystemCoaRole.GAIN_ON_EXCHANGE_ADJ.name(), startDate, endDate)));
        Map<Long, BigDecimal> exchangeReturnLossByWarehouse = toAmountMap(journalDetailRepository.sumSaleReturnRoleByWarehouse(tenantId, SystemCoaRole.LOSS_RETURNED_EXCHANGE.name(), startDate, endDate));
        Map<Long, BigDecimal> purchaseReturnLossByWarehouse = toAmountMap(journalDetailRepository.sumPurchaseReturnRoleByWarehouse(tenantId, SystemCoaRole.LOSS_PURCHASE_RETURN.name(), startDate, endDate));

        // General expenses explicitly tagged to a warehouse (Expense.warehouseId) are broken out
        // per warehouse below; whatever's left untagged remains the tenant-wide
        // unallocatedGeneralExpenses figure, now genuinely "unallocated" rather than "all".
        Map<Long, BigDecimal> generalExpensesByWarehouse = toAmountMap(expenseRepository.getGeneralExpensesByWarehouse(tenantId, startDate, endDate));

        List<WarehousePerformanceInfo> warehouses = stockRows.stream()
                .map(stock -> toInfo(stock,
                        salesByWarehouse.get(key(stock.getWarehouseId())),
                        purchaseByWarehouse.get(key(stock.getWarehouseId())),
                        payablesByWarehouse.get(key(stock.getWarehouseId())),
                        serviceSalesByWarehouse.get(key(stock.getWarehouseId())),
                        returnDeductionByWarehouse.getOrDefault(key(stock.getWarehouseId()), BigDecimal.ZERO),
                        exchangeGainByWarehouse.getOrDefault(key(stock.getWarehouseId()), BigDecimal.ZERO),
                        exchangeReturnLossByWarehouse.getOrDefault(key(stock.getWarehouseId()), BigDecimal.ZERO),
                        purchaseReturnLossByWarehouse.getOrDefault(key(stock.getWarehouseId()), BigDecimal.ZERO),
                        generalExpensesByWarehouse.getOrDefault(key(stock.getWarehouseId()), BigDecimal.ZERO)))
                .filter(info -> warehouseId == null || warehouseId.equals(info.getWarehouseId()))
                .toList();

        BigDecimal unallocatedGeneralExpenses = expenseRepository.getUnallocatedGeneralExpensesByPeriod(tenantId, startDate, endDate);

        return WarehouseComparisonRs.builder()
                .month(yearMonth.format(MONTH_DISPLAY))
                .warehouses(warehouses)
                .unallocatedGeneralExpenses(safe(unallocatedGeneralExpenses))
                .build();
    }

    private WarehousePerformanceInfo toInfo(WarehouseStockMetrics stock, WarehouseSalesMetrics sales, PurchaseAgg purchase, PayableAgg payable, WarehouseServiceSaleMetrics serviceSales,
                                             BigDecimal returnDeductionIncome, BigDecimal exchangeGain, BigDecimal exchangeReturnLoss, BigDecimal purchaseReturnLoss,
                                             BigDecimal generalExpenses) {
        BigDecimal stockValue = safe(stock.getTotalStockValue());
        BigDecimal salesRevenue = sales != null ? safe(sales.getTotalRevenue()) : BigDecimal.ZERO;
        // exchangeReturnLoss/purchaseReturnLoss come from sumSaleReturnRoleByWarehouse/
        // sumPurchaseReturnRoleByWarehouse, which compute credit - debit - already negative for
        // these debit-normal loss accounts, so a plain .add() both credits the gains and
        // correctly debits (subtracts) the losses.
        BigDecimal grossProfit = (sales != null ? safe(sales.getGrossProfit()) : BigDecimal.ZERO)
                .add(safe(returnDeductionIncome))
                .add(safe(exchangeGain))
                .add(safe(exchangeReturnLoss))
                .add(safe(purchaseReturnLoss));
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
                .serviceSalesCount(serviceSales != null && serviceSales.getServiceSaleCount() != null ? serviceSales.getServiceSaleCount() : 0L)
                .serviceRevenue(serviceSales != null ? safe(serviceSales.getServiceRevenue()) : BigDecimal.ZERO)
                .purchaseCount(purchase != null ? purchase.count() : 0L)
                .purchaseCost(purchase != null ? purchase.purchaseCost() : BigDecimal.ZERO)
                .landedCost(purchase != null ? purchase.landedCost() : BigDecimal.ZERO)
                .purchaseExpenses(purchase != null ? purchase.purchaseExpenses() : BigDecimal.ZERO)
                .payablesCount(payable != null ? payable.count() : 0L)
                .totalPayables(payable != null ? payable.totalPending() : BigDecimal.ZERO)
                .generalExpenses(safe(generalExpenses))
                .build();
    }

    private Long key(Long warehouseId) {
        return warehouseId != null ? warehouseId : UNASSIGNED_KEY;
    }

    private Map<Long, BigDecimal> toAmountMap(List<WarehouseAmountRow> rows) {
        Map<Long, BigDecimal> map = new LinkedHashMap<>();
        for (WarehouseAmountRow row : rows) {
            map.merge(key(row.getWarehouseId()), safe(row.getAmount()), BigDecimal::add);
        }
        return map;
    }

    private Map<Long, BigDecimal> mergeAmountMaps(Map<Long, BigDecimal> a, Map<Long, BigDecimal> b) {
        Map<Long, BigDecimal> merged = new LinkedHashMap<>(a);
        b.forEach((k, v) -> merged.merge(k, v, BigDecimal::add));
        return merged;
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
