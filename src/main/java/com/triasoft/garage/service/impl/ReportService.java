package com.triasoft.garage.service.impl;

import com.triasoft.garage.constants.SystemCoaRole;
import com.triasoft.garage.constants.TransactionDirectionEnum;
import com.triasoft.garage.entity.FinanceCompany;
import com.triasoft.garage.entity.PaymentAccount;
import com.triasoft.garage.entity.Purchase;
import com.triasoft.garage.entity.Sale;
import com.triasoft.garage.ledger.projection.PartySourceBalanceRow;
import com.triasoft.garage.ledger.projection.SourceBalanceRow;
import com.triasoft.garage.ledger.repository.JournalDetailRepository;
import com.triasoft.garage.projection.CompanyAmountRow;
import com.triasoft.garage.projection.LastPaymentDateProjection;
import com.triasoft.garage.repository.JournalDashboardRepository;
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
import com.triasoft.garage.model.report.FinanceCompanyReceivableInfo;
import com.triasoft.garage.model.report.FinanceReceivableSaleInfo;
import com.triasoft.garage.model.report.FinanceReceivablesSummaryRs;
import com.triasoft.garage.model.report.ReceivableInfo;
import com.triasoft.garage.model.report.ServiceReceivableInfo;
import com.triasoft.garage.model.report.ServiceReceivablesSummaryRs;
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
import com.triasoft.garage.security.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {

    private static final DateTimeFormatter MONTH_DISPLAY = DateTimeFormatter.ofPattern("MMMM yyyy");
    private static final DateTimeFormatter PERIOD_DISPLAY = DateTimeFormatter.ofPattern("dd MMM yyyy");

    private final SaleRepository saleRepository;
    private final SaleReturnRepository saleReturnRepository;
    private final JournalDetailRepository journalDetailRepository;
    private final JournalDashboardRepository journalRepository;
    private final PurchaseRepository purchaseRepository;
    private final ExpenseRepository expenseRepository;
    private final DirectEntryRepository directEntryRepository;
    private final PaymentAccountRepository paymentAccountRepository;
    private final TransactionRepository transactionRepository;
    private final SalePaymentRepository salePaymentRepository;
    private final PurchasePaymentRepository purchasePaymentRepository;
    private final FinanceCompanyRepository financeCompanyRepository;
    private final com.triasoft.garage.servicesale.repository.ServiceSaleRepository serviceSaleRepository;
    private final com.triasoft.garage.servicesale.repository.ServiceSalePaymentRepository serviceSalePaymentRepository;

    public PLReportRs getProfitAndLoss(YearMonth yearMonth, Long companyId) {
        var startDate = yearMonth.atDay(1);
        var endDate = yearMonth.atEndOfMonth();
        Long tenantId = TenantContext.get();

        // companyId is nullable throughout this method - null means "overall" (every company
        // combined). Every term below is now company-scoped (or genuinely tenant-wide by
        // nature, like nothing left here) - this used to be a mix, which meant ?companyId=1 and
        // ?companyId=4 showed identical sales/direct-entry/expense figures and disagreed with
        // /companies/comparison. Fixed by threading companyId into every query below.
        // ── 1. Sales ─────────────────────────────────────────────────────────
        ProfitMetrics sales = saleRepository.getProfitReport(tenantId, companyId, startDate, endDate);
        BigDecimal vehicleSalesRevenue = safe(sales.getTotalSales());
        BigDecimal grossProfit       = safe(sales.getNetProfit());

        // ── 2. Direct Entries ─────────────────────────────────────────────────
        PLDirectEntryMetrics de = directEntryRepository.getDirectEntryMetrics(tenantId, companyId, startDate, endDate);
        BigDecimal otherIncome      = safe(de.getTotalIn());
        BigDecimal directAdjustments = safe(de.getTotalOut());

        // Retained deductions on sale returns are real income (RETURN_DEDUCTION_INCOME
        // in the ledger) but are netted out of the sales figures above, so add them back.
        BigDecimal returnDeductionIncome = safe(saleReturnRepository.sumDeductionIncomeByPeriod(tenantId, companyId, startDate, endDate));

        // Exchange/return gains & losses live only in the ledger (no operational entity),
        // so pull them from their system-role journal accounts for the period. This closes
        // the remaining entity-vs-journal P&L gap (only manual journals remain uncaptured).
        BigDecimal exchangeGain       = ledgerRevenue(tenantId, companyId, SystemCoaRole.GAIN_ON_EXCHANGE_ADJ, startDate, endDate);
        BigDecimal exchangeReturnLoss = ledgerExpense(tenantId, companyId, SystemCoaRole.LOSS_RETURNED_EXCHANGE, startDate, endDate);
        BigDecimal purchaseReturnLoss = ledgerExpense(tenantId, companyId, SystemCoaRole.LOSS_PURCHASE_RETURN, startDate, endDate);
        // Service-sale revenue and payroll expense — summary-level only for now (no
        // per-invoice/per-employee detail list yet, unlike sales/purchases/expenses below).
        BigDecimal serviceRevenue = ledgerRevenue(tenantId, companyId, SystemCoaRole.SERVICE_REVENUE, startDate, endDate);
        BigDecimal payrollExpense = ledgerExpense(tenantId, companyId, SystemCoaRole.SALARY_EXPENSE, startDate, endDate);

        // ── 3. Revenue totals ─────────────────────────────────────────────────
        BigDecimal totalRevenue = vehicleSalesRevenue.add(otherIncome)
                .add(returnDeductionIncome).add(exchangeGain).add(serviceRevenue);

        // ── 4. Expenses ───────────────────────────────────────────────────────
        PLExpenseMetrics exp = expenseRepository.getExpensesByPeriod(tenantId, companyId, startDate, endDate);
        BigDecimal generalExpenses  = safe(exp.getGeneralExpenses());
        BigDecimal totalOpEx = generalExpenses.add(directAdjustments)
                .add(exchangeReturnLoss).add(purchaseReturnLoss).add(payrollExpense);

        // ── 5. Net profit ─────────────────────────────────────────────────────
        BigDecimal netProfit = grossProfit.add(otherIncome).add(returnDeductionIncome)
                .add(exchangeGain).add(serviceRevenue).subtract(totalOpEx);

        // ── 6. Margin percentages ─────────────────────────────────────────────
        double grossMarginPct = pct(grossProfit, vehicleSalesRevenue);
        double netMarginPct   = pct(netProfit, totalRevenue);

        // ── 7. Cash & bank position (as of month-end) ─────────────────────────
        List<PaymentAccount> paymentAccounts = companyId == null
                ? paymentAccountRepository.findAllByIsActiveTrue()
                : paymentAccountRepository.findAllByIsActiveTrueAndCompanyId(companyId);
        List<AccountBalanceInfo> cashPosition = paymentAccounts
                .stream()
                .map(account -> toAccountBalance(account, endDate))
                .toList();
        BigDecimal totalCash = cashPosition.stream()
                .map(AccountBalanceInfo::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // ── 9. Per-vehicle sales breakdown for the period ─────────────────────
        List<SaleLineRow> saleRows = saleRepository.getSaleLinesByPeriod(tenantId, companyId, startDate, endDate);
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
        List<PurchaseLineRow> purchaseRows = purchaseRepository.getPurchaseLinesByPeriod(tenantId, companyId, startDate, endDate);
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
        List<ExpenseLineInfo> expenseLines = expenseRepository.getExpenseLinesByPeriod(tenantId, companyId, startDate, endDate)
                .stream()
                .map(r -> ExpenseLineInfo.builder()
                        .date(r.getDate())
                        .expenseName(r.getExpenseName())
                        .amount(safe(r.getAmount()))
                        .accountName(r.getAccountName())
                        .build())
                .toList();

        // ── 12. Direct entries breakdown (income / expense / other) ────────────
        List<DirectEntryLineInfo> directEntryLines = directEntryRepository.getDirectEntryLinesByPeriod(tenantId, companyId, startDate, endDate)
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
        // Deliberately period-cohort, NOT the same metric as getReceivablesSummary()/
        // getPayablesSummary() (which answer "of everything ever sold/bought, what's
        // outstanding right now"). This is "of what was sold/bought in THIS period, what's
        // still outstanding" - saleRows/purchaseRows are already filtered to sale_date/
        // order_date within [startDate, endDate]. A sale from a prior period that's still
        // partially unpaid correctly does NOT appear here, even though it would appear in
        // /reports/receivables. Do not "fix" this to match the ledger-derived current-balance
        // queries - that would inflate this period's figure with every prior period's
        // still-open balance, which is a different (and wrong) thing for a per-period report
        // to show. Verified by hand (2026-08-13) that both formulas below are internally
        // correct on their own terms and were never exposed to the control-account blending
        // bugs found in getReceivablesSummary()/getPayablesSummary(): the sales formula never
        // separates customer vs. finance-company debt in the first place (so nothing to
        // blend), and the purchase formula uses total_amount, which already includes RC due
        // as owed and is never netted against it.
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
                .exchangeGain(exchangeGain)
                .exchangeReturnLoss(exchangeReturnLoss)
                .purchaseReturnLoss(purchaseReturnLoss)
                .serviceRevenue(serviceRevenue)
                .payrollExpense(payrollExpense)
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

    private AccountBalanceInfo toAccountBalance(PaymentAccount account, java.time.LocalDate asOfDate) {
        BigDecimal in  = transactionRepository.sumAmountByAccountAndDirectionUpTo(account.getId(), TransactionDirectionEnum.IN, asOfDate);
        BigDecimal out = transactionRepository.sumAmountByAccountAndDirectionUpTo(account.getId(), TransactionDirectionEnum.OUT, asOfDate);
        BigDecimal balance = account.getOpeningBalance().add(in).subtract(out);
        return AccountBalanceInfo.builder()
                .id(account.getId())
                .name(account.getName())
                .accountType(account.getAccountType())
                .balance(balance)
                .build();
    }

    public MonthlyTrendRs getMonthlyTrend(int months) {
        List<MonthlyTrendMetrics> rows = journalRepository.getMonthlyTrendFromJournal(TenantContext.get(), months);
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

    // ─────────────────────────────────────────────────────────────────────────
    //  Receivables / Payables — ledger-derived (primary, live endpoints)
    //
    // Pending amount comes from JournalDetail.sourceType/sourceId (one formula, the same
    // one that actually posted AR/AP), not a hand-rolled SQL re-derivation. Entity lookups
    // below are for DISPLAY fields only (invoice/PO number, vehicle, contact, dates) - the
    // *FromEntities methods further down stay in the codebase, unchanged, as the audit
    // reference these are reconciled against.
    // ─────────────────────────────────────────────────────────────────────────

    public ReceivablesSummaryRs getReceivablesSummary(Long companyId) {
        Long tenantId = TenantContext.get();
        // Scoped to the AR account itself, not just party=CUSTOMER: a SALE source can also carry
        // FINANCE_RECEIVABLE, CUSTOMER_SETTLEMENT_PAYABLE, and CUSTOMER_REFUND_PAYABLE lines
        // tagged to the same customer+sale - summing across accounts (even filtered to one
        // party) would still net a payable-type line against the true AR balance. See
        // getFinanceReceivablesSummary() for the finance-company portion.
        List<SourceBalanceRow> balances = journalDetailRepository.getOpenSourceBalancesByRole(
                tenantId, companyId, JournalService.SOURCE_SALE, SystemCoaRole.AR.name());

        // SALE sources are receivable-natured (AR is DR-normal): pending = debit - credit.
        Map<Long, BigDecimal> pendingBySaleId = new LinkedHashMap<>();
        for (SourceBalanceRow row : balances) {
            BigDecimal pending = safe(row.getDebit()).subtract(safe(row.getCredit()));
            if (pending.signum() > 0) {
                pendingBySaleId.put(row.getSourceId(), pending);
            }
        }

        List<Long> saleIds = new ArrayList<>(pendingBySaleId.keySet());
        Map<Long, Sale> salesById = saleRepository.findAllById(saleIds).stream()
                .collect(Collectors.toMap(Sale::getId, s -> s));
        Map<Long, LocalDate> lastPaymentBySaleId = salePaymentRepository.findLastPaymentDatesBySaleIds(saleIds).stream()
                .collect(Collectors.toMap(LastPaymentDateProjection::getSourceId, LastPaymentDateProjection::getLastPaymentDate));

        List<ReceivableInfo> saleItems = pendingBySaleId.entrySet().stream()
                .map(e -> {
                    Sale sale = salesById.get(e.getKey());
                    if (sale == null) return null; // source predates this tenant's data / was hard-deleted
                    return ReceivableInfo.builder()
                            .saleId(sale.getId())
                            .invoiceNo(sale.getInvoiceNo())
                            .paymentStatus(sale.getPaymentStatus() != null ? sale.getPaymentStatus().name() : null)
                            .vehicleNo(sale.getInventory() != null ? sale.getInventory().getProductNo() : null)
                            .saleDate(sale.getSaleDate())
                            .amount(safe(sale.getNetSaleAmount()))
                            .pendingAmount(e.getValue())
                            .lastPaymentDate(lastPaymentBySaleId.get(sale.getId()))
                            .customerName(sale.getCustomer() != null ? sale.getCustomer().getName() : null)
                            .customerMobile(sale.getCustomer() != null ? sale.getCustomer().getMobile() : null)
                            .build();
                })
                .filter(java.util.Objects::nonNull)
                .toList();

        List<ReceivableInfo> items = saleItems.stream()
                .sorted((a, b) -> b.getSaleDate().compareTo(a.getSaleDate()))
                .toList();
        BigDecimal totalPending = items.stream()
                .map(ReceivableInfo::getPendingAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        // Combined KPI figure: what customers owe (above) plus what finance companies haven't
        // yet disbursed - deliberately kept as separate fields rather than folded into
        // totalPendingAmount/items, which stay customer-only (see field comments on the DTO).
        BigDecimal financePending = getFinanceReceivablesSummary(companyId).getTotalPendingAmount();
        return ReceivablesSummaryRs.builder()
                .totalCount(items.size())
                .totalPendingAmount(totalPending)
                .financePendingAmount(financePending)
                .totalOutstandingAmount(totalPending.add(financePending))
                .items(items)
                .build();
    }

    // Grouped by (party_id=financeCompanyId, source_id=saleId) and scoped to the
    // FINANCE_RECEIVABLE account itself (see getOpenSourceBalancesByRole) - a financed sale can
    // carry both a CUSTOMER-tagged AR line and a FINANCE-tagged FINANCE_RECEIVABLE line under
    // the same source_id, so both party AND account scoping matter here.
    public FinanceReceivablesSummaryRs getFinanceReceivablesSummary(Long companyId) {
        Long tenantId = TenantContext.get();
        List<PartySourceBalanceRow> balances = journalDetailRepository.getOpenPartySourceBalances(
                tenantId, companyId, JournalService.PARTY_FINANCE, JournalService.SOURCE_SALE, SystemCoaRole.FINANCE_RECEIVABLE.name());

        // FINANCE_RECEIVABLE is receivable-natured (asset, DR-normal): pending = debit - credit.
        Map<Long, Map<Long, BigDecimal>> pendingByCompanyThenSale = new LinkedHashMap<>();
        for (PartySourceBalanceRow row : balances) {
            BigDecimal pending = safe(row.getDebit()).subtract(safe(row.getCredit()));
            if (pending.signum() > 0) {
                pendingByCompanyThenSale
                        .computeIfAbsent(row.getPartyId(), k -> new LinkedHashMap<>())
                        .put(row.getSourceId(), pending);
            }
        }

        List<Long> saleIds = pendingByCompanyThenSale.values().stream()
                .flatMap(m -> m.keySet().stream())
                .distinct()
                .toList();
        Map<Long, Sale> salesById = saleRepository.findAllById(saleIds).stream()
                .collect(Collectors.toMap(Sale::getId, s -> s));
        Map<Long, FinanceCompany> companiesById = financeCompanyRepository.findAllById(pendingByCompanyThenSale.keySet()).stream()
                .collect(Collectors.toMap(FinanceCompany::getId, c -> c));

        List<FinanceCompanyReceivableInfo> items = pendingByCompanyThenSale.entrySet().stream()
                .map(companyEntry -> {
                    FinanceCompany company = companiesById.get(companyEntry.getKey());
                    if (company == null) return null; // party predates this tenant's data / was hard-deleted

                    List<FinanceReceivableSaleInfo> sales = companyEntry.getValue().entrySet().stream()
                            .map(saleEntry -> {
                                Sale sale = salesById.get(saleEntry.getKey());
                                if (sale == null) return null;
                                return FinanceReceivableSaleInfo.builder()
                                        .saleId(sale.getId())
                                        .invoiceNo(sale.getInvoiceNo())
                                        .paymentStatus(sale.getPaymentStatus() != null ? sale.getPaymentStatus().name() : null)
                                        .vehicleNo(sale.getInventory() != null ? sale.getInventory().getProductNo() : null)
                                        .saleDate(sale.getSaleDate())
                                        .financeAmount(safe(sale.getFinanceAmount()))
                                        .pendingAmount(saleEntry.getValue())
                                        .customerName(sale.getCustomer() != null ? sale.getCustomer().getName() : null)
                                        .build();
                            })
                            .filter(java.util.Objects::nonNull)
                            .sorted((a, b) -> b.getSaleDate().compareTo(a.getSaleDate()))
                            .toList();
                    BigDecimal totalPendingForCompany = sales.stream()
                            .map(FinanceReceivableSaleInfo::getPendingAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    return FinanceCompanyReceivableInfo.builder()
                            .financeCompanyId(company.getId())
                            .financeCompanyName(company.getName())
                            .contactNumber(company.getContactNumber())
                            .totalPending(totalPendingForCompany)
                            .sales(sales)
                            .build();
                })
                .filter(java.util.Objects::nonNull)
                .sorted((a, b) -> b.getTotalPending().compareTo(a.getTotalPending()))
                .toList();
        BigDecimal totalPending = items.stream()
                .map(FinanceCompanyReceivableInfo::getTotalPending)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return FinanceReceivablesSummaryRs.builder()
                .totalCount(items.size())
                .totalPendingAmount(totalPending)
                .items(items)
                .build();
    }

    public PayablesSummaryRs getPayablesSummary(Long companyId) {
        Long tenantId = TenantContext.get();
        // Scoped to the AP account itself, not just source=PURCHASE: a PURCHASE source can also
        // carry RC_DUE_RECEIVABLE and VENDOR_REFUND_RECEIVABLE lines tagged to the same
        // vendor+purchase - summing across accounts would net those receivables against the AP
        // balance and understate what we actually still owe the vendor in cash.
        List<SourceBalanceRow> balances = journalDetailRepository.getOpenSourceBalancesByRole(
                tenantId, companyId, JournalService.SOURCE_PURCHASE, SystemCoaRole.AP.name());

        // PURCHASE sources are payable-natured (AP is CR-normal): pending = credit - debit.
        Map<Long, BigDecimal> pendingByPurchaseId = new LinkedHashMap<>();
        for (SourceBalanceRow row : balances) {
            BigDecimal pending = safe(row.getCredit()).subtract(safe(row.getDebit()));
            if (pending.signum() > 0) {
                pendingByPurchaseId.put(row.getSourceId(), pending);
            }
        }

        List<Long> purchaseIds = new ArrayList<>(pendingByPurchaseId.keySet());
        Map<Long, Purchase> purchasesById = purchaseRepository.findAllById(purchaseIds).stream()
                .collect(Collectors.toMap(Purchase::getId, p -> p));
        Map<Long, LocalDate> lastPaymentByPurchaseId = purchasePaymentRepository.findLastPaymentDatesByPurchaseIds(purchaseIds).stream()
                .collect(Collectors.toMap(LastPaymentDateProjection::getSourceId, LastPaymentDateProjection::getLastPaymentDate));

        List<PayableInfo> items = new ArrayList<>(pendingByPurchaseId.entrySet().stream()
                .map(e -> {
                    Purchase purchase = purchasesById.get(e.getKey());
                    if (purchase == null) return null;
                    String vehicleNo = purchase.getPurchaseDetails().isEmpty() ? null : purchase.getPurchaseDetails().get(0).getProductNo();
                    return PayableInfo.builder()
                            .purchaseId(purchase.getId())
                            .referenceNo(purchase.getReferenceNo())
                            .vehicleNo(vehicleNo)
                            .purchaseDate(purchase.getOrderDate())
                            .amount(safe(purchase.getTotalAmount()))
                            .pendingAmount(e.getValue())
                            .lastPaymentDate(lastPaymentByPurchaseId.get(purchase.getId()))
                            .vendorName(purchase.getVendor() != null ? purchase.getVendor().getName() : null)
                            .vendorMobile(purchase.getVendor() != null ? purchase.getVendor().getMobile() : null)
                            .build();
                })
                .filter(java.util.Objects::nonNull)
                .toList());

        // Fallback: pending exchange/trade-in purchases have no PURCHASE journal at all
        // (PurchaseService deliberately skips posting until the buyback/return decision is
        // made), so they can never come from getOpenSourceBalances - merge them in from the
        // entity formula directly, the one case where "ledger as primary" has no ledger data
        // to be primary over. purchaseIds is already the full set of ledger-covered ids, so
        // any overlap here would mean a purchase somehow has both a journal AND is still
        // flagged trade-in-pending - shouldn't happen, but skip it defensively rather than
        // double-count if it ever does.
        for (PayableRow r : purchaseRepository.findPendingTradeInPurchases(tenantId)) {
            if (pendingByPurchaseId.containsKey(r.getPurchaseId())) continue;
            items.add(PayableInfo.builder()
                    .purchaseId(r.getPurchaseId())
                    .referenceNo(r.getReferenceNo())
                    .vehicleNo(r.getVehicleNo())
                    .purchaseDate(r.getPurchaseDate())
                    .amount(safe(r.getAmount()))
                    .pendingAmount(safe(r.getPendingAmount()))
                    .lastPaymentDate(r.getLastPaymentDate())
                    .vendorName(r.getVendorName())
                    .vendorMobile(r.getVendorMobile())
                    .build());
        }

        List<PayableInfo> sortedItems = items.stream()
                .sorted((a, b) -> b.getPurchaseDate().compareTo(a.getPurchaseDate()))
                .toList();
        BigDecimal totalPending = sortedItems.stream()
                .map(PayableInfo::getPendingAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return PayablesSummaryRs.builder()
                .totalCount(sortedItems.size())
                .totalPendingAmount(totalPending)
                .items(sortedItems)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Receivables / Payables — entity-derived (audit reference, kept per user's decision
    //  to keep both with ledger as primary; not called from ReportController)
    // ─────────────────────────────────────────────────────────────────────────

    public ReceivablesSummaryRs getReceivablesSummaryFromEntities() {
        List<ReceivableRow> rows = saleRepository.findReceivables(TenantContext.get());
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

    public PayablesSummaryRs getPayablesSummaryFromEntities() {
        List<PayableRow> rows = purchaseRepository.findPayables(TenantContext.get());
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

    // Own endpoint/DTO, deliberately not merged into getReceivablesSummary() — vehicle and
    // service receivables are different operational workflows (finance/RC-due-linked vs
    // walk-in/quick-turnover); see [[service_sale_cost_gap]]-adjacent memory for the split
    // rationale. A service sale posts to the same AR account as a vehicle sale (see
    // JournalService.handleServiceSale), so this must independently derive its own balances,
    // not read anything off getReceivablesSummary().
    public ServiceReceivablesSummaryRs getServiceReceivablesSummary(Long companyId) {
        Long tenantId = TenantContext.get();
        List<SourceBalanceRow> balances = journalDetailRepository.getOpenSourceBalancesByRole(
                tenantId, companyId, JournalService.SOURCE_SERVICE_SALE, SystemCoaRole.AR.name());

        Map<Long, BigDecimal> pendingByServiceSaleId = new LinkedHashMap<>();
        for (SourceBalanceRow row : balances) {
            BigDecimal pending = safe(row.getDebit()).subtract(safe(row.getCredit()));
            if (pending.signum() > 0) {
                pendingByServiceSaleId.put(row.getSourceId(), pending);
            }
        }

        List<Long> serviceSaleIds = new ArrayList<>(pendingByServiceSaleId.keySet());
        Map<Long, com.triasoft.garage.servicesale.entity.ServiceSale> salesById = serviceSaleRepository.findAllById(serviceSaleIds)
                .stream().collect(Collectors.toMap(com.triasoft.garage.servicesale.entity.ServiceSale::getId, s -> s));
        Map<Long, LocalDate> lastPaymentByServiceSaleId = serviceSalePaymentRepository.findLastPaymentDatesByServiceSaleIds(serviceSaleIds).stream()
                .collect(Collectors.toMap(LastPaymentDateProjection::getSourceId, LastPaymentDateProjection::getLastPaymentDate));

        List<ServiceReceivableInfo> items = pendingByServiceSaleId.entrySet().stream()
                .map(e -> {
                    var sale = salesById.get(e.getKey());
                    if (sale == null) return null;
                    return ServiceReceivableInfo.builder()
                            .serviceSaleId(sale.getId())
                            .invoiceNo(sale.getInvoiceNo())
                            .paymentStatus(sale.getPaymentStatus() != null ? sale.getPaymentStatus().name() : null)
                            .saleDate(sale.getSaleDate())
                            .amount(safe(sale.getTotalAmount()))
                            .pendingAmount(e.getValue())
                            .lastPaymentDate(lastPaymentByServiceSaleId.get(sale.getId()))
                            .customerName(sale.customerDisplayName())
                            .customerMobile(sale.getCustomer() != null ? sale.getCustomer().getMobile() : null)
                            .build();
                })
                .filter(java.util.Objects::nonNull)
                .sorted((a, b) -> b.getSaleDate().compareTo(a.getSaleDate()))
                .toList();

        BigDecimal totalPending = items.stream()
                .map(ServiceReceivableInfo::getPendingAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return ServiceReceivablesSummaryRs.builder()
                .totalCount(items.size())
                .totalPendingAmount(totalPending)
                .items(items)
                .build();
    }

    // Revenue accounts carry a normal credit balance → gain = credit − debit. companyId == null
    // means "overall" (all companies combined) - sumBySystemRoleByCompany already computes
    // credit - debit per company with no reference-type join needed (see CompanyReportService),
    // so summing its rows across every company gives the tenant-wide total directly.
    private BigDecimal ledgerRevenue(Long tenantId, Long companyId, SystemCoaRole role, java.time.LocalDate from, java.time.LocalDate to) {
        if (companyId == null) {
            return sumBd(journalDetailRepository.sumBySystemRoleByCompany(tenantId, role.name(), from, to), CompanyAmountRow::getAmount);
        }
        var r = journalDetailRepository.sumBySystemRoleInPeriod(tenantId, companyId, role.name(), from, to);
        return r == null ? BigDecimal.ZERO : safe(r.getCredit()).subtract(safe(r.getDebit()));
    }

    // Expense/loss accounts carry a normal debit balance → loss = debit − credit. sumBySystem
    // RoleByCompany's amount is credit - debit, so the all-companies total needs negating to
    // match this method's debit - credit convention.
    private BigDecimal ledgerExpense(Long tenantId, Long companyId, SystemCoaRole role, java.time.LocalDate from, java.time.LocalDate to) {
        if (companyId == null) {
            return sumBd(journalDetailRepository.sumBySystemRoleByCompany(tenantId, role.name(), from, to), CompanyAmountRow::getAmount).negate();
        }
        var r = journalDetailRepository.sumBySystemRoleInPeriod(tenantId, companyId, role.name(), from, to);
        return r == null ? BigDecimal.ZERO : safe(r.getDebit()).subtract(safe(r.getCredit()));
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
