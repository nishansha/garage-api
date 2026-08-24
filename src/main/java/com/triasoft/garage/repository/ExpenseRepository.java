package com.triasoft.garage.repository;

import com.triasoft.garage.entity.Expense;
import com.triasoft.garage.projection.ExpenseLineRow;
import com.triasoft.garage.projection.ExpenseMetrics;
import com.triasoft.garage.projection.PLExpenseMetrics;
import com.triasoft.garage.projection.PurchaseExpenseSumProjection;
import com.triasoft.garage.projection.WarehouseAmountRow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long>, JpaSpecificationExecutor<Expense> {
    @Query(value = "SELECT " +
            "SUM(CASE WHEN e.purchase_order_id IS NULL THEN e.amount ELSE 0 END) as totalGeneralExpense, " +
            "SUM(CASE WHEN e.purchase_order_id IS NOT NULL THEN e.amount ELSE 0 END) as totalPurchaseExpense, " +
            "SUM(CASE WHEN e.purchase_order_id IS NULL AND e.date >= :startOfMonth THEN e.amount ELSE 0 END) as generalExpenseThisMonth, " +
            "SUM(CASE WHEN e.purchase_order_id IS NOT NULL AND e.date >= :startOfMonth THEN e.amount ELSE 0 END) as purchaseExpenseThisMonth " +
            "FROM app_expense e " +
            "WHERE e.deleted = false AND e.tenant_id = :tenantId", nativeQuery = true)
    ExpenseMetrics getExpenseMetrics(@Param("tenantId") Long tenantId, @Param("startOfMonth") LocalDate startOfMonth);

    Page<Expense> findByPurchaseIsNull(Pageable pageable);

    Page<Expense> findByPurchaseIsNotNull(Pageable pageable);

    List<Expense> findByPurchaseId(Long purchaseId);

    @Query("SELECT e.purchase.id as purchaseId, SUM(e.amount) as totalExpenses FROM Expense e WHERE e.purchase.id IN :purchaseIds GROUP BY e.purchase.id")
    List<PurchaseExpenseSumProjection> getTotalExpensesByPurchaseIds(@Param("purchaseIds") List<Long> purchaseIds);

    // Expense has no company_id column of its own - derives it via its (required)
    // expense_account_id, same as CompanyReportRepository.getGeneralExpensesByCompany.
    // companyId nullable - null means "overall", see SaleRepository.getProfitReport's comment.
    @Query(value = """
            SELECT
              COALESCE(SUM(CASE WHEN e.purchase_order_id IS NULL THEN e.amount ELSE 0 END), 0) as generalExpenses,
              COALESCE(SUM(CASE WHEN e.purchase_order_id IS NOT NULL THEN e.amount ELSE 0 END), 0) as purchaseExpenses
            FROM app_expense e
            JOIN fnd_chart_of_accounts coa ON coa.id = e.expense_account_id
            WHERE e.deleted = false
              AND e.tenant_id = :tenantId
              AND e.date BETWEEN :startDate AND :endDate
              AND (:companyId IS NULL OR coa.company_id = :companyId)
              AND (:warehouseId IS NULL OR e.warehouse_id = :warehouseId)
            """, nativeQuery = true)
    PLExpenseMetrics getExpensesByPeriod(@Param("tenantId") Long tenantId, @Param("companyId") Long companyId, @Param("warehouseId") Long warehouseId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query(value = """
            SELECT
                e.date                                            as date,
                COALESCE(NULLIF(TRIM(e.description), ''),
                         NULLIF(TRIM(e.other_expense), ''),
                         coa.name)                                as expenseName,
                e.amount                                          as amount,
                pa.name                                           as accountName
            FROM app_expense e
            JOIN fnd_chart_of_accounts coa ON coa.id = e.expense_account_id
            LEFT JOIN app_payment_account pa ON pa.id = e.payment_account_id
            WHERE e.deleted = false
              AND e.tenant_id = :tenantId
              AND e.purchase_order_id IS NULL
              AND e.date BETWEEN :startDate AND :endDate
              AND (:companyId IS NULL OR coa.company_id = :companyId)
              AND (:warehouseId IS NULL OR e.warehouse_id = :warehouseId)
            ORDER BY e.date, e.id
            """, nativeQuery = true)
    List<ExpenseLineRow> getExpenseLinesByPeriod(@Param("tenantId") Long tenantId, @Param("companyId") Long companyId, @Param("warehouseId") Long warehouseId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    // General (non-purchase-linked) expenses explicitly tagged to a warehouse, for
    // WarehouseReportService - only rows where warehouse_id was actually set on creation.
    @Query(value = """
            SELECT e.warehouse_id as warehouseId, COALESCE(SUM(e.amount), 0) as amount
            FROM app_expense e
            WHERE e.deleted = false
              AND e.tenant_id = :tenantId
              AND e.purchase_order_id IS NULL
              AND e.warehouse_id IS NOT NULL
              AND e.date BETWEEN :startDate AND :endDate
            GROUP BY e.warehouse_id
            """, nativeQuery = true)
    List<WarehouseAmountRow> getGeneralExpensesByWarehouse(@Param("tenantId") Long tenantId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    // The genuinely-unallocated remainder (no warehouse tagged) - what
    // WarehouseComparisonRs.unallocatedGeneralExpenses means now that warehouse tagging
    // exists. getExpensesByPeriod above is untouched (still ALL general expenses,
    // allocated or not) since ReportService's full P&L must keep summing everything.
    @Query(value = """
            SELECT COALESCE(SUM(e.amount), 0)
            FROM app_expense e
            WHERE e.deleted = false
              AND e.tenant_id = :tenantId
              AND e.purchase_order_id IS NULL
              AND e.warehouse_id IS NULL
              AND e.date BETWEEN :startDate AND :endDate
            """, nativeQuery = true)
    BigDecimal getUnallocatedGeneralExpensesByPeriod(@Param("tenantId") Long tenantId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}
