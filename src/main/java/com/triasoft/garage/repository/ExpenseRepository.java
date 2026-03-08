package com.triasoft.garage.repository;

import com.triasoft.garage.entity.Expense;
import com.triasoft.garage.projection.ExpenseMetrics;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
            "WHERE e.deleted = false", nativeQuery = true)
    ExpenseMetrics getExpenseMetrics(@Param("startOfMonth") LocalDate startOfMonth);

    Page<Expense> findByPurchaseIsNull(Pageable pageable);

    Page<Expense> findByPurchaseIsNotNull(Pageable pageable);
}
