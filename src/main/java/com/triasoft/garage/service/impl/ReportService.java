package com.triasoft.garage.service.impl;

import com.triasoft.garage.constants.TransactionDirectionEnum;
import com.triasoft.garage.entity.PaymentAccount;
import com.triasoft.garage.model.report.AccountBalanceInfo;
import com.triasoft.garage.model.report.DirectEntryLineInfo;
import com.triasoft.garage.model.report.ExpenseLineInfo;
import com.triasoft.garage.model.report.MonthlyTrendInfo;
import com.triasoft.garage.model.report.MonthlyTrendRs;
import com.triasoft.garage.model.report.PLReportRs;
import com.triasoft.garage.model.report.DirectEntryTotals;
import com.triasoft.garage.model.report.ExpenseTotals;
import com.triasoft.garage.model.report.PayableInfo;
import com.triasoft.garage.model.report.PayablesSummaryRs;
import com.triasoft.garage.model.report.PurchaseLineInfo;
import com.triasoft.garage.model.report.PurchaseTotals;
import com.triasoft.garage.model.report.SalesTotals;
import com.triasoft.garage.model.report.ReceivableInfo;
import com.triasoft.garage.model.report.ReceivablesSummaryRs;
import com.triasoft.garage.model.report.SaleLineInfo;
import com.triasoft.garage.projection.MonthlyTrendMetrics;
import com.triasoft.garage.projection.PLDirectEntryMetrics;
import com.triasoft.garage.projection.PLExpenseMetrics;
import com.triasoft.garage.projection.PayableRow;
import com.triasoft.garage.projection.ProfitMetrics;
import com.triasoft.garage.projection.PurchaseLineRow;
import com.triasoft.garage.projection.ReceivableRow;
import com.triasoft.garage.projection.SaleLineRow;
import com.triasoft.garage.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReportService {

    private static final DateTimeFormatter MONTH_DISPLAY = DateTimeFormatter.ofPattern("MMMM yyyy");
    private static final DateTimeFormatter PERIOD_DISPLAY = DateTimeFormatter.ofPattern("dd MMM yyyy");

    private final SaleRepository saleRepository;
    private final SaleReturnRepository saleReturnRepository;
    private final JournalRepository journalRepository;
    private final PurchaseRepository purchaseRepository;
    private final ExpenseRepository expenseRepository;
    private final DirectEntryRepository directEntryRepository;
    private final PaymentAccountRepository paymentAccountRepository;
    private final TransactionRepository transactionRepository;

    public PLReportRs getProfitAndLoss(YearMonth yearMonth) {
        var startDate = yearMonth.atDay(1);
        var endDate = yearMonth.atEndOfMonth();

        // ── 1. Sales ─────────────────────────────────────────────────────────
        ProfitMetrics sales = saleRepository.getProfitReport(startDate, endDate);
        BigDecimal vehicleSalesRevenue = safe(sales.getTotalSales());
        BigDecimal grossProfit       = safe(sales.getNetProfit());

        // ── 2. Direct Entries ─────────────────────────────────────────────────
        PLDirectEntryMetrics de = directEntryRepository.getDirectEntryMetrics(startDate, endDate);
        BigDecimal otherIncome      = safe(de.getTotalIn());
        BigDecimal directAdjustments = safe(de.getTotalOut());

        // Retained deductions on sale returns are real income (RETURN_DEDUCTION_INCOME
        // in the ledger) but are netted out of the sales figures above, so add them back.
        BigDecimal returnDeductionIncome = safe(saleReturnRepository.sumDeductionIncomeByPeriod(startDate, endDate));

        // ── 3. Revenue totals ─────────────────────────────────────────────────
        BigDecimal totalRevenue = vehicleSalesRevenue.add(otherIncome).add(returnDeductionIncome);

        // ── 4. Expenses ───────────────────────────────────────────────────────
        PLExpenseMetrics exp = expenseRepository.getExpensesByPeriod(startDate, endDate);
        BigDecimal generalExpenses  = safe(exp.getGeneralExpenses());
        BigDecimal totalOpEx = generalExpenses.add(directAdjustments);

        // ── 5. Net profit ─────────────────────────────────────────────────────
        BigDecimal netProfit = grossProfit.add(otherIncome).add(returnDeductionIncome).subtract(totalOpEx);

        // ── 6. Margin percentages ─────────────────────────────────────────────
        double grossMarginPct = pct(grossProfit, vehicleSalesRevenue);
        double netMarginPct   = pct(netProfit, totalRevenue);

        // ── 7. Cash & bank position (current, not period-bound) ───────────────
        List<AccountBalanceInfo> cashPosition = paymentAccountRepository.findAllByIsActiveTrue()
                .stream()
                .map(this::toAccountBalance)
                .toList();
        BigDecimal totalCash = cashPosition.stream()
                .map(AccountBalanceInfo::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // ── 9. Per-vehicle sales breakdown for the period ─────────────────────
        List<SaleLineRow> saleRows = saleRepository.getSaleLinesByPeriod(startDate, endDate);
        List<SaleLineInfo> saleLines = saleRows.stream()
                .map(r -> SaleLineInfo.builder()
                        .saleId(r.getSaleId())
                        .invoiceNo(r.getInvoiceNo())
                        .saleDate(r.getSaleDate())
                        .vehicleNo(r.getVehicleNo())
                        .customerName(r.getCustomerName())
                        .purchaseRate(safe(r.getPurchaseRate()))
                        .purchaseExpenses(safe(r.getPurchaseExpenses()))
                        .saleRate(safe(r.getSaleRate()))
                        .profit(safe(r.getProfit()))
                        .returned(Boolean.TRUE.equals(r.getReturned()))
                        .pendingAmount(safe(r.getPendingAmount()))
                        .build())
                .toList();

        // ── 10. Per-vehicle purchases breakdown for the period ────────────────
        List<PurchaseLineRow> purchaseRows = purchaseRepository.getPurchaseLinesByPeriod(startDate, endDate);
        List<PurchaseLineInfo> purchaseLines = purchaseRows.stream()
                .map(r -> PurchaseLineInfo.builder()
                        .purchaseId(r.getPurchaseId())
                        .referenceNo(r.getReferenceNo())
                        .purchaseDate(r.getPurchaseDate())
                        .vehicleNo(r.getVehicleNo())
                        .vendorName(r.getVendorName())
                        .purchaseRate(safe(r.getPurchaseRate()))
                        .purchaseExpenses(safe(r.getPurchaseExpenses()))
                        .landedCost(safe(r.getLandedCost()))
                        .returned(Boolean.TRUE.equals(r.getReturned()))
                        .returnAmount(r.getReturnAmount())
                        .pendingAmount(safe(r.getPendingAmount()))
                        .build())
                .toList();

        // ── 11. General expenses breakdown (excludes purchase expenses) ────────
        List<ExpenseLineInfo> expenseLines = expenseRepository.getExpenseLinesByPeriod(startDate, endDate)
                .stream()
                .map(r -> ExpenseLineInfo.builder()
                        .date(r.getDate())
                        .expenseName(r.getExpenseName())
                        .amount(safe(r.getAmount()))
                        .accountName(r.getAccountName())
                        .build())
                .toList();

        // ── 12. Direct entries breakdown (income / expense / other) ────────────
        List<DirectEntryLineInfo> directEntryLines = directEntryRepository.getDirectEntryLinesByPeriod(startDate, endDate)
                .stream()
                .map(r -> DirectEntryLineInfo.builder()
                        .date(r.getDate())
                        .name(r.getName())
                        .amount(safe(r.getAmount()))
                        .category(r.getCategory())
                        .accountName(r.getAccountName())
                        .direction(r.getDirection())
                        .classification(r.getClassification())
                        .build())
                .toList();

        // ── 13. Section totals (aligned with the detail lists) ────────────────
        // Totals reflect net realized activity — returned rows are excluded; returnCount
        // reports the returned rows so that count + returnCount = list length.
        List<SaleLineInfo> soldRows = saleLines.stream().filter(s -> !s.isReturned()).toList();
        SalesTotals salesTotals = SalesTotals.builder()
                .count(soldRows.size())
                .returnCount(saleLines.size() - soldRows.size())
                .saleRate(sumBd(soldRows, SaleLineInfo::getSaleRate))
                .cost(soldRows.stream()
                        .map(s -> safe(s.getPurchaseRate()).add(safe(s.getPurchaseExpenses())))
                        .reduce(BigDecimal.ZERO, BigDecimal::add))
                .purchaseExpenses(sumBd(soldRows, SaleLineInfo::getPurchaseExpenses))
                .profit(sumBd(soldRows, SaleLineInfo::getProfit))
                .build();

        List<PurchaseLineInfo> activePurchases = purchaseLines.stream().filter(p -> !p.isReturned()).toList();
        PurchaseTotals purchaseTotals = PurchaseTotals.builder()
                .count(activePurchases.size())
                .returnCount(purchaseLines.size() - activePurchases.size())
                .purchaseRate(sumBd(activePurchases, PurchaseLineInfo::getPurchaseRate))
                .purchaseExpenses(sumBd(activePurchases, PurchaseLineInfo::getPurchaseExpenses))
                .landedCost(sumBd(activePurchases, PurchaseLineInfo::getLandedCost))
                .returnAmount(sumBd(purchaseLines, PurchaseLineInfo::getReturnAmount))
                .build();

        ExpenseTotals expenseTotals = ExpenseTotals.builder()
                .count(expenseLines.size())
                .amount(sumBd(expenseLines, ExpenseLineInfo::getAmount))
                .build();

        List<DirectEntryLineInfo> inRows = directEntryLines.stream()
                .filter(d -> "IN".equalsIgnoreCase(d.getDirection())).toList();
        List<DirectEntryLineInfo> outRows = directEntryLines.stream()
                .filter(d -> "OUT".equalsIgnoreCase(d.getDirection())).toList();
        List<DirectEntryLineInfo> incomeRows = directEntryLines.stream()
                .filter(d -> "INCOME".equalsIgnoreCase(d.getClassification())).toList();
        List<DirectEntryLineInfo> deExpenseRows = directEntryLines.stream()
                .filter(d -> "EXPENSE".equalsIgnoreCase(d.getClassification())).toList();
        List<DirectEntryLineInfo> otherRows = directEntryLines.stream()
                .filter(d -> "OTHER".equalsIgnoreCase(d.getClassification())).toList();
        DirectEntryTotals directEntryTotals = DirectEntryTotals.builder()
                .inCount(inRows.size())
                .inAmount(sumBd(inRows, DirectEntryLineInfo::getAmount))
                .outCount(outRows.size())
                .outAmount(sumBd(outRows, DirectEntryLineInfo::getAmount))
                .incomeCount(incomeRows.size())
                .incomeAmount(sumBd(incomeRows, DirectEntryLineInfo::getAmount))
                .expenseCount(deExpenseRows.size())
                .expenseAmount(sumBd(deExpenseRows, DirectEntryLineInfo::getAmount))
                .otherCount(otherRows.size())
                .otherAmount(sumBd(otherRows, DirectEntryLineInfo::getAmount))
                .build();

        // ── 14. Receivables / Payables arising from this period's deals ───────
        BigDecimal totalReceivables = sumBd(saleRows, SaleLineRow::getPendingAmount);
        BigDecimal totalReceivablesTillDate = sumBd(saleRows, SaleLineRow::getPendingTillDate);

        // Purchase pending is a PO-level figure repeated across a PO's vehicle rows;
        // dedupe by purchaseId so multi-vehicle POs are not counted more than once.
        Map<Long, BigDecimal> payableByPo = new LinkedHashMap<>();
        Map<Long, BigDecimal> payableTillDateByPo = new LinkedHashMap<>();
        for (PurchaseLineRow r : purchaseRows) {
            payableByPo.putIfAbsent(r.getPurchaseId(), safe(r.getPendingAmount()));
            payableTillDateByPo.putIfAbsent(r.getPurchaseId(), safe(r.getPendingTillDate()));
        }
        BigDecimal totalPayables = payableByPo.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalPayablesTillDate = payableTillDateByPo.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);

        return PLReportRs.builder()
                .month(yearMonth.format(MONTH_DISPLAY))
                .period(startDate.format(PERIOD_DISPLAY) + " – " + endDate.format(PERIOD_DISPLAY))
                .totalRevenue(totalRevenue)
                .grossProfit(grossProfit)
                .grossMarginPct(grossMarginPct)
                .returnDeductionIncome(returnDeductionIncome)
                .totalOperatingExpenses(totalOpEx)
                .netProfit(netProfit)
                .netMarginPct(netMarginPct)
                .salesTotals(salesTotals)
                .purchaseTotals(purchaseTotals)
                .expenseTotals(expenseTotals)
                .directEntryTotals(directEntryTotals)
                .totalReceivables(totalReceivables)
                .totalReceivablesTillDate(totalReceivablesTillDate)
                .totalPayables(totalPayables)
                .totalPayablesTillDate(totalPayablesTillDate)
                .cashPosition(cashPosition)
                .totalCashPosition(totalCash)
                .sales(saleLines)
                .purchases(purchaseLines)
                .expenses(expenseLines)
                .directEntries(directEntryLines)
                .build();
    }

    private AccountBalanceInfo toAccountBalance(PaymentAccount account) {
        BigDecimal in  = transactionRepository.sumAmountByAccountAndDirection(account.getId(), TransactionDirectionEnum.IN);
        BigDecimal out = transactionRepository.sumAmountByAccountAndDirection(account.getId(), TransactionDirectionEnum.OUT);
        BigDecimal balance = account.getOpeningBalance().add(in).subtract(out);
        return AccountBalanceInfo.builder()
                .id(account.getId())
                .name(account.getName())
                .accountType(account.getAccountType())
                .balance(balance)
                .build();
    }

    public MonthlyTrendRs getMonthlyTrend(int months) {
        List<MonthlyTrendMetrics> rows = journalRepository.getMonthlyTrendFromJournal(months);
        List<MonthlyTrendInfo> trend = rows.stream().map(r -> {
            BigDecimal revenue = safe(r.getTotalRevenue());
            BigDecimal otherIncome = safe(r.getOtherIncome());
            BigDecimal grossProfit = safe(r.getGrossProfit());
            BigDecimal expenses = safe(r.getTotalExpenses());
            BigDecimal netProfit = grossProfit.add(otherIncome).subtract(expenses);
            return MonthlyTrendInfo.builder()
                    .month(r.getMonth())
                        .monthLabel(r.getMonthLabel())
                    .salesCount(r.getSalesCount() != null ? r.getSalesCount() : 0L)
                    .totalRevenue(revenue)
                    .otherIncome(otherIncome)
                    .grossProfit(grossProfit)
                    .grossMarginPct(pct(grossProfit, revenue))
                    .netProfit(netProfit)
                    .netMarginPct(pct(netProfit, revenue))
                    .totalReceivables(safe(r.getTotalReceivables()))
                    .totalPayables(safe(r.getTotalPayables()))
                    .totalExpenses(expenses)
                    .build();
        }).toList();
        return MonthlyTrendRs.builder().trend(trend).build();
    }

    public ReceivablesSummaryRs getReceivablesSummary() {
        List<ReceivableRow> rows = saleRepository.findReceivables();
        List<ReceivableInfo> items = rows.stream().map(r -> ReceivableInfo.builder()
                .saleId(r.getSaleId())
                .invoiceNo(r.getInvoiceNo())
                .paymentStatus(r.getPaymentStatus())
                .vehicleNo(r.getVehicleNo())
                .saleDate(r.getSaleDate())
                .amount(safe(r.getAmount()))
                .pendingAmount(safe(r.getPendingAmount()))
                .lastPaymentDate(r.getLastPaymentDate())
                .customerName(r.getCustomerName())
                .customerMobile(r.getCustomerMobile())
                .build()).toList();
        BigDecimal totalPending = items.stream()
                .map(ReceivableInfo::getPendingAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return ReceivablesSummaryRs.builder()
                .totalCount(items.size())
                .totalPendingAmount(totalPending)
                .items(items)
                .build();
    }

    public PayablesSummaryRs getPayablesSummary() {
        List<PayableRow> rows = purchaseRepository.findPayables();
        List<PayableInfo> items = rows.stream().map(r -> PayableInfo.builder()
                .purchaseId(r.getPurchaseId())
                .referenceNo(r.getReferenceNo())
                .vehicleNo(r.getVehicleNo())
                .purchaseDate(r.getPurchaseDate())
                .amount(safe(r.getAmount()))
                .pendingAmount(safe(r.getPendingAmount()))
                .lastPaymentDate(r.getLastPaymentDate())
                .vendorName(r.getVendorName())
                .vendorMobile(r.getVendorMobile())
                .build()).toList();
        BigDecimal totalPending = items.stream()
                .map(PayableInfo::getPendingAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return PayablesSummaryRs.builder()
                .totalCount(items.size())
                .totalPendingAmount(totalPending)
                .items(items)
                .build();
    }

    private BigDecimal safe(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private <T> BigDecimal sumBd(List<T> rows, java.util.function.Function<T, BigDecimal> extractor) {
        return rows.stream()
                .map(t -> safe(extractor.apply(t)))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private double pct(BigDecimal numerator, BigDecimal denominator) {
        if (denominator == null || denominator.compareTo(BigDecimal.ZERO) == 0) return 0.0;
        return numerator.multiply(BigDecimal.valueOf(100))
                .divide(denominator, 2, RoundingMode.HALF_UP)
                .doubleValue();
    }

}
