package com.triasoft.garage.service.impl;

import com.triasoft.garage.constants.TransactionDirectionEnum;
import com.triasoft.garage.entity.PaymentAccount;
import com.triasoft.garage.model.report.AccountBalanceInfo;
import com.triasoft.garage.model.report.MonthlyTrendInfo;
import com.triasoft.garage.model.report.MonthlyTrendRs;
import com.triasoft.garage.model.report.PLReportRs;
import com.triasoft.garage.model.report.PayableInfo;
import com.triasoft.garage.model.report.PayablesSummaryRs;
import com.triasoft.garage.model.report.ReceivableInfo;
import com.triasoft.garage.model.report.ReceivablesSummaryRs;
import com.triasoft.garage.projection.MonthlyTrendMetrics;
import com.triasoft.garage.projection.PLDirectEntryMetrics;
import com.triasoft.garage.projection.PLExpenseMetrics;
import com.triasoft.garage.projection.PLPendingMetrics;
import com.triasoft.garage.projection.PayableRow;
import com.triasoft.garage.projection.ProfitMetrics;
import com.triasoft.garage.projection.ReceivableRow;
import com.triasoft.garage.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportService {

    private static final DateTimeFormatter MONTH_DISPLAY = DateTimeFormatter.ofPattern("MMMM yyyy");
    private static final DateTimeFormatter PERIOD_DISPLAY = DateTimeFormatter.ofPattern("dd MMM yyyy");

    private final SaleRepository saleRepository;
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
        BigDecimal cogs              = safe(sales.getTotalCost());
        BigDecimal grossProfit       = safe(sales.getNetProfit());
        long unitsSold               = sales.getUnitsSold() != null ? sales.getUnitsSold() : 0L;

        // ── 2. Direct Entries ─────────────────────────────────────────────────
        PLDirectEntryMetrics de = directEntryRepository.getDirectEntryMetrics(startDate, endDate);
        BigDecimal otherIncome      = safe(de.getTotalIn());
        BigDecimal directAdjustments = safe(de.getTotalOut());

        // ── 3. Revenue totals ─────────────────────────────────────────────────
        BigDecimal totalRevenue = vehicleSalesRevenue.add(otherIncome);

        // ── 4. Expenses ───────────────────────────────────────────────────────
        PLExpenseMetrics exp = expenseRepository.getExpensesByPeriod(startDate, endDate);
        BigDecimal generalExpenses  = safe(exp.getGeneralExpenses());
        BigDecimal purchaseExpenses = safe(exp.getPurchaseExpenses());
        BigDecimal totalOpEx = generalExpenses.add(directAdjustments);

        // ── 5. Net profit ─────────────────────────────────────────────────────
        BigDecimal netProfit = grossProfit.add(otherIncome).subtract(totalOpEx);

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

        // ── 8. Pending collections (for the period's sales) ───────────────────
        PLPendingMetrics pending = saleRepository.getPendingByPeriod(startDate, endDate);

        return PLReportRs.builder()
                .month(yearMonth.format(MONTH_DISPLAY))
                .period(startDate.format(PERIOD_DISPLAY) + " – " + endDate.format(PERIOD_DISPLAY))
                .unitsSold(unitsSold)
                .vehicleSalesRevenue(vehicleSalesRevenue)
                .otherIncome(otherIncome)
                .totalRevenue(totalRevenue)
                .costOfGoodsSold(cogs)
                .grossProfit(grossProfit)
                .grossMarginPct(grossMarginPct)
                .purchaseExpenses(purchaseExpenses)
                .generalExpenses(generalExpenses)
                .directAdjustments(directAdjustments)
                .totalOperatingExpenses(totalOpEx)
                .netProfit(netProfit)
                .netMarginPct(netMarginPct)
                .cashPosition(cashPosition)
                .totalCashPosition(totalCash)
                .pendingCount(pending.getPendingCount() != null ? pending.getPendingCount() : 0L)
                .pendingAmount(safe(pending.getPendingAmount()))
                .financePendingAmount(safe(pending.getFinancePendingAmount()))
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

    private double pct(BigDecimal numerator, BigDecimal denominator) {
        if (denominator == null || denominator.compareTo(BigDecimal.ZERO) == 0) return 0.0;
        return numerator.multiply(BigDecimal.valueOf(100))
                .divide(denominator, 2, RoundingMode.HALF_UP)
                .doubleValue();
    }

}
