package com.triasoft.garage.service.impl;

import com.triasoft.garage.constants.TransactionDirectionEnum;
import com.triasoft.garage.entity.PaymentAccount;
import com.triasoft.garage.model.report.AccountBalanceInfo;
import com.triasoft.garage.model.report.MonthlyTrendInfo;
import com.triasoft.garage.model.report.MonthlyTrendRs;
import com.triasoft.garage.model.report.PLReportRs;
import com.triasoft.garage.projection.MonthlyTrendMetrics;
import com.triasoft.garage.projection.PLDirectEntryMetrics;
import com.triasoft.garage.projection.PLExpenseMetrics;
import com.triasoft.garage.projection.PLPendingMetrics;
import com.triasoft.garage.projection.ProfitMetrics;
import com.triasoft.garage.repository.DirectEntryRepository;
import com.triasoft.garage.repository.ExpenseRepository;
import com.triasoft.garage.repository.PaymentAccountRepository;
import com.triasoft.garage.repository.SaleRepository;
import com.triasoft.garage.repository.TransactionRepository;
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
        BigDecimal netProfit = grossProfit.subtract(totalOpEx);

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
        List<MonthlyTrendMetrics> rows = saleRepository.getMonthlyTrend(months);
        List<MonthlyTrendInfo> trend = rows.stream().map(r -> {
            BigDecimal revenue = safe(r.getTotalRevenue());
            BigDecimal grossProfit = safe(r.getGrossProfit());
            return MonthlyTrendInfo.builder()
                    .month(r.getMonth())
                    .monthLabel(r.getMonthLabel())
                    .salesCount(r.getSalesCount() != null ? r.getSalesCount() : 0L)
                    .totalRevenue(revenue)
                    .grossProfit(grossProfit)
                    .grossMarginPct(pct(grossProfit, revenue))
                    .totalReceivables(safe(r.getTotalReceivables()))
                    .totalPayables(safe(r.getTotalPayables()))
                    .totalExpenses(safe(r.getTotalExpenses()))
                    .build();
        }).toList();
        return MonthlyTrendRs.builder().trend(trend).build();
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
