package com.triasoft.garage.company.service;

import com.triasoft.garage.company.entity.Company;
import com.triasoft.garage.company.model.CompanyComparisonRs;
import com.triasoft.garage.company.model.CompanyPerformanceInfo;
import com.triasoft.garage.company.repository.CompanyRepository;
import com.triasoft.garage.company.repository.CompanyReportRepository;
import com.triasoft.garage.company.repository.CompanyReportRepository.CompanyExpenseRow;
import com.triasoft.garage.company.repository.CompanyReportRepository.CompanyPurchaseRow;
import com.triasoft.garage.company.repository.CompanyReportRepository.CompanySalesRow;
import com.triasoft.garage.company.repository.CompanyReportRepository.CompanyServiceSaleRow;
import com.triasoft.garage.constants.SystemCoaRole;
import com.triasoft.garage.dto.UserDTO;
import com.triasoft.garage.ledger.repository.JournalDetailRepository;
import com.triasoft.garage.projection.CompanyAmountRow;
import com.triasoft.garage.repository.SaleReturnRepository;
import com.triasoft.garage.security.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Company-level analog of WarehouseReportService — same "compare N peers for a period"
 * shape, one level up. Gated by the caller's company grants: never a blend of books the
 * caller isn't authorized to see, and SUPERADMIN (which bypasses all privilege checks
 * elsewhere) sees every company, consistent with that existing convention.
 */
@Service
@RequiredArgsConstructor
public class CompanyReportService {

    private static final DateTimeFormatter MONTH_DISPLAY = DateTimeFormatter.ofPattern("MMMM yyyy");

    private final CompanyRepository companyRepository;
    private final CompanyService companyService;
    private final CompanyReportRepository companyReportRepository;
    private final SaleReturnRepository saleReturnRepository;
    private final JournalDetailRepository journalDetailRepository;

    public CompanyComparisonRs compare(YearMonth yearMonth, UserDTO user) {
        Long tenantId = TenantContext.get();
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        List<Company> visibleCompanies = resolveVisibleCompanies(user);

        Map<Long, CompanySalesRow> salesByCompany = index(companyReportRepository.getSalesByCompany(tenantId, startDate, endDate), CompanySalesRow::getCompanyId);
        Map<Long, CompanyPurchaseRow> purchasesByCompany = index(companyReportRepository.getPurchasesByCompany(tenantId, startDate, endDate), CompanyPurchaseRow::getCompanyId);
        Map<Long, CompanyServiceSaleRow> serviceSalesByCompany = index(companyReportRepository.getServiceSalesByCompany(tenantId, startDate, endDate), CompanyServiceSaleRow::getCompanyId);
        Map<Long, CompanyExpenseRow> expensesByCompany = index(companyReportRepository.getGeneralExpensesByCompany(tenantId, startDate, endDate), CompanyExpenseRow::getCompanyId);

        // Retained deductions + exchange gains on sale returns are real profit earned on the
        // return itself, netted out of the plain sales figures above (same gap ReportService.
        // getProfitAndLoss already closes tenant-wide) - add them back here too. Symmetrically,
        // LOSS_RETURNED_EXCHANGE (sunk cost on a RETURN_TO_BUYER exchange) and LOSS_PURCHASE_
        // RETURN (unrecovered cost on a vendor return) are the loss-side counterparts of the
        // same events and must be subtracted, or profit is systematically overstated. All three
        // ledger roles can be grouped straight by company_id (it lives on app_journal itself
        // regardless of which reference type posted it) - no reference-type join needed here,
        // unlike the warehouse-level queries in WarehouseReportService.
        Map<Long, CompanyAmountRow> returnDeductionByCompany = index(saleReturnRepository.sumDeductionIncomeByCompany(tenantId, startDate, endDate), CompanyAmountRow::getCompanyId);
        Map<Long, CompanyAmountRow> exchangeGainByCompany = index(journalDetailRepository.sumBySystemRoleByCompany(tenantId, SystemCoaRole.GAIN_ON_EXCHANGE_ADJ.name(), startDate, endDate), CompanyAmountRow::getCompanyId);
        Map<Long, CompanyAmountRow> exchangeReturnLossByCompany = index(journalDetailRepository.sumBySystemRoleByCompany(tenantId, SystemCoaRole.LOSS_RETURNED_EXCHANGE.name(), startDate, endDate), CompanyAmountRow::getCompanyId);
        Map<Long, CompanyAmountRow> purchaseReturnLossByCompany = index(journalDetailRepository.sumBySystemRoleByCompany(tenantId, SystemCoaRole.LOSS_PURCHASE_RETURN.name(), startDate, endDate), CompanyAmountRow::getCompanyId);

        List<CompanyPerformanceInfo> companies = visibleCompanies.stream()
                .map(company -> toInfo(company,
                        salesByCompany.get(company.getId()),
                        purchasesByCompany.get(company.getId()),
                        serviceSalesByCompany.get(company.getId()),
                        expensesByCompany.get(company.getId()),
                        returnDeductionByCompany.get(company.getId()),
                        exchangeGainByCompany.get(company.getId()),
                        exchangeReturnLossByCompany.get(company.getId()),
                        purchaseReturnLossByCompany.get(company.getId())))
                .toList();

        return CompanyComparisonRs.builder()
                .month(yearMonth.format(MONTH_DISPLAY))
                .companies(companies)
                .build();
    }

    private List<Company> resolveVisibleCompanies(UserDTO user) {
        List<Company> all = companyRepository.findAllByOrderByIdAsc();
        if (user.getRoles() != null && user.getRoles().contains("SUPERADMIN")) {
            return all;
        }
        Set<Long> accessible = Set.copyOf(companyService.getAccessibleCompanyIds(user.getId()));
        return all.stream().filter(c -> accessible.contains(c.getId())).toList();
    }

    private CompanyPerformanceInfo toInfo(Company company, CompanySalesRow sales, CompanyPurchaseRow purchase,
                                           CompanyServiceSaleRow serviceSale, CompanyExpenseRow expense,
                                           CompanyAmountRow returnDeductionIncome, CompanyAmountRow exchangeGain,
                                           CompanyAmountRow exchangeReturnLoss, CompanyAmountRow purchaseReturnLoss) {
        // exchangeReturnLoss/purchaseReturnLoss come from sumBySystemRoleByCompany, which computes
        // credit - debit - already negative for these debit-normal loss accounts, so a plain
        // .add() both credits the gains and correctly debits (subtracts) the losses.
        BigDecimal grossProfit = (sales != null ? safe(sales.getGrossProfit()) : BigDecimal.ZERO)
                .add(returnDeductionIncome != null ? safe(returnDeductionIncome.getAmount()) : BigDecimal.ZERO)
                .add(exchangeGain != null ? safe(exchangeGain.getAmount()) : BigDecimal.ZERO)
                .add(exchangeReturnLoss != null ? safe(exchangeReturnLoss.getAmount()) : BigDecimal.ZERO)
                .add(purchaseReturnLoss != null ? safe(purchaseReturnLoss.getAmount()) : BigDecimal.ZERO);
        return CompanyPerformanceInfo.builder()
                .companyId(company.getId())
                .companyCode(company.getCode())
                .companyName(company.getName())
                .salesCount(sales != null && sales.getSalesCount() != null ? sales.getSalesCount() : 0L)
                .salesRevenue(sales != null ? safe(sales.getSalesRevenue()) : BigDecimal.ZERO)
                .grossProfit(grossProfit)
                .purchaseCount(purchase != null && purchase.getPurchaseCount() != null ? purchase.getPurchaseCount() : 0L)
                .purchaseCost(purchase != null ? safe(purchase.getPurchaseCost()) : BigDecimal.ZERO)
                .serviceSaleCount(serviceSale != null && serviceSale.getServiceSaleCount() != null ? serviceSale.getServiceSaleCount() : 0L)
                .serviceRevenue(serviceSale != null ? safe(serviceSale.getServiceRevenue()) : BigDecimal.ZERO)
                .generalExpenses(expense != null ? safe(expense.getTotalExpenses()) : BigDecimal.ZERO)
                .build();
    }

    private <T> Map<Long, T> index(List<T> rows, java.util.function.Function<T, Long> keyFn) {
        Map<Long, T> map = new LinkedHashMap<>();
        rows.forEach(r -> map.put(keyFn.apply(r), r));
        return map;
    }

    private BigDecimal safe(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
