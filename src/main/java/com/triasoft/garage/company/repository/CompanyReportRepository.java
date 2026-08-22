package com.triasoft.garage.company.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.Repository;
import com.triasoft.garage.company.entity.Company;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Per-company aggregates for the cross-company comparison report — deliberately reads
 * directly off the company_id columns already present on app_sale/app_purchase_order/
 * app_service_sale, rather than joining through Warehouse (those columns are the ledger's
 * own source of truth for "which company", set once at creation and never re-derived).
 */
public interface CompanyReportRepository extends Repository<Company, Long> {

    interface CompanySalesRow {
        Long getCompanyId();
        Long getSalesCount();
        BigDecimal getSalesRevenue();
        BigDecimal getGrossProfit();
    }

    interface CompanyPurchaseRow {
        Long getCompanyId();
        Long getPurchaseCount();
        BigDecimal getPurchaseCost();
    }

    interface CompanyServiceSaleRow {
        Long getCompanyId();
        Long getServiceSaleCount();
        BigDecimal getServiceRevenue();
    }

    interface CompanyExpenseRow {
        Long getCompanyId();
        BigDecimal getTotalExpenses();
    }

    // payment_status <> 'REFUND' excludes fully-returned sales - same "lightweight, not
    // full-P&L-return-adjusted" convention as SaleRepository.getSalesPerformanceByWarehouse.
    // SaleReturnService.create() flips the original sale's payment_status to REFUND in the
    // same transaction as the return, so this is a simple flag check, not a join.
    @Query(value = """
            SELECT company_id as companyId, COUNT(*) as salesCount,
                   COALESCE(SUM(sale_rate), 0) as salesRevenue,
                   COALESCE(SUM(profit_amount), 0) as grossProfit
            FROM app_sale
            WHERE tenant_id = :tenantId AND deleted = false
              AND sale_date BETWEEN :fromDate AND :toDate
              AND payment_status <> 'REFUND'
            GROUP BY company_id
            """, nativeQuery = true)
    List<CompanySalesRow> getSalesByCompany(@Param("tenantId") Long tenantId, @Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate);

    @Query(value = """
            SELECT company_id as companyId, COUNT(*) as purchaseCount,
                   COALESCE(SUM(total_amount), 0) as purchaseCost
            FROM app_purchase_order
            WHERE tenant_id = :tenantId AND deleted = false
              AND order_date BETWEEN :fromDate AND :toDate
            GROUP BY company_id
            """, nativeQuery = true)
    List<CompanyPurchaseRow> getPurchasesByCompany(@Param("tenantId") Long tenantId, @Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate);

    @Query(value = """
            SELECT company_id as companyId, COUNT(*) as serviceSaleCount,
                   COALESCE(SUM(total_amount), 0) as serviceRevenue
            FROM app_service_sale
            WHERE tenant_id = :tenantId AND deleted = false
              AND sale_date BETWEEN :fromDate AND :toDate
            GROUP BY company_id
            """, nativeQuery = true)
    List<CompanyServiceSaleRow> getServiceSalesByCompany(@Param("tenantId") Long tenantId, @Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate);

    // Expense has no direct company_id column — derives it via its (required) expense
    // account, same trick JournalService.handleExpense uses for posting.
    @Query(value = """
            SELECT coa.company_id as companyId, COALESCE(SUM(e.amount), 0) as totalExpenses
            FROM app_expense e
            JOIN fnd_chart_of_accounts coa ON coa.id = e.expense_account_id
            WHERE e.tenant_id = :tenantId
              AND e.date BETWEEN :fromDate AND :toDate
              AND e.purchase_order_id IS NULL
            GROUP BY coa.company_id
            """, nativeQuery = true)
    List<CompanyExpenseRow> getGeneralExpensesByCompany(@Param("tenantId") Long tenantId, @Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate);
}
