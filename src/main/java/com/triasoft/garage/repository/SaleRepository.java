package com.triasoft.garage.repository;

import com.triasoft.garage.entity.Sale;
import com.triasoft.garage.projection.*;
import com.triasoft.garage.projection.MonthlyTrendMetrics;
import com.triasoft.garage.projection.PLPendingMetrics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface SaleRepository extends JpaRepository<Sale, Long>, JpaSpecificationExecutor<Sale> {

    Sale findByInventoryId(Long id);

    @Query(value = "SELECT nextval('so_ref_no_seq')", nativeQuery = true)
    Long getNextReferenceNumber();

    @Query(value = "SELECT " +
            "COALESCE(SUM(s.sale_rate), 0) as totalSales, " +
            "COALESCE(SUM(s.landed_cost_at_sale), 0) as totalCost, " +
            "COALESCE(SUM(s.sale_rate - s.landed_cost_at_sale), 0) as netProfit, " +
            "COUNT(s.id) as unitsSold " +
            "FROM app_sale s " +
            "WHERE s.sale_date BETWEEN :startDate AND :endDate " +
            "AND s.deleted = false", nativeQuery = true)
    ProfitMetrics getProfitReport(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query(value = "SELECT " +
            "COALESCE(SUM(CASE WHEN s.sale_date >= :startOfMonth THEN s.sale_rate ELSE 0 END), 0) as totalSalesThisMonth, " +
            "COALESCE(SUM(CASE WHEN s.sale_date >= :startOfLastMonth AND s.sale_date <= :endOfLastMonth THEN s.sale_rate ELSE 0 END), 0) as totalSalesLastMonth, " +
            "COUNT(CASE WHEN s.sale_date = :today THEN 1 END) as todayCount, " +
            "COUNT(CASE WHEN s.sale_date >= :startOfMonth THEN 1 END) as monthCount " +
            "FROM app_sale s " +
            "WHERE s.sale_date >= :startOfLastMonth " +
            "AND s.deleted = false", nativeQuery = true)
    SaleMetrics getSalesSummaryMetrics(@Param("startOfLastMonth") LocalDate startOfLastMonth, @Param("endOfLastMonth") LocalDate endOfLastMonth, @Param("startOfMonth") LocalDate startOfMonth, @Param("today") LocalDate today);

    @Query(value = """
            SELECT
              (SELECT COALESCE(SUM(net_sale_amount), 0) FROM app_sale
               WHERE deleted = false AND sale_date >= :startOfMonth) as totalSales,
              (SELECT COALESCE(SUM(net_sale_amount), 0) FROM app_sale
               WHERE deleted = false AND sale_date >= :startOfLastMonth AND sale_date < :startOfMonth) as salesBeforeMonth,

              (SELECT COALESCE(SUM(total_amount), 0) FROM app_purchase_order
               WHERE deleted = false AND order_date >= :startOfMonth) as totalPurchases,
              (SELECT COALESCE(SUM(total_amount), 0) FROM app_purchase_order
               WHERE deleted = false AND order_date >= :startOfLastMonth AND order_date < :startOfMonth) as purchasesBeforeMonth,

              (SELECT COALESCE(SUM(amount), 0) FROM app_expense
               WHERE deleted = false AND purchase_order_id IS NULL AND date >= :startOfMonth)
              +
              (SELECT COALESCE(SUM(d.amount), 0) FROM app_direct_entry d
               JOIN fnd_chart_of_accounts coa ON coa.id = d.coa_id
               WHERE d.deleted = false AND d.direction = 'OUT'
                 AND coa.type <> 'EQUITY'
                 AND d.entry_date >= :startOfMonth) as totalExpenses,

              (SELECT COALESCE(SUM(amount), 0) FROM app_expense
               WHERE deleted = false AND purchase_order_id IS NULL AND date >= :startOfLastMonth AND date < :startOfMonth)
              +
              (SELECT COALESCE(SUM(d.amount), 0) FROM app_direct_entry d
               JOIN fnd_chart_of_accounts coa ON coa.id = d.coa_id
               WHERE d.deleted = false AND d.direction = 'OUT'
                 AND coa.type <> 'EQUITY'
                 AND d.entry_date >= :startOfLastMonth AND d.entry_date < :startOfMonth) as expensesBeforeMonth,

              (SELECT COALESCE(SUM(net_sale_amount - COALESCE(landed_cost_at_sale, 0)), 0) FROM app_sale
               WHERE deleted = false AND sale_date >= :startOfMonth) as totalGrossProfit,
              (SELECT COALESCE(SUM(net_sale_amount - COALESCE(landed_cost_at_sale, 0)), 0) FROM app_sale
               WHERE deleted = false AND sale_date >= :startOfLastMonth AND sale_date < :startOfMonth) as grossProfitBeforeMonth

            FROM (SELECT 1) data
            """, nativeQuery = true)
    SummaryMetrics getFinancialSummary(@Param("startOfMonth") LocalDate startOfMonth,
                                       @Param("startOfLastMonth") LocalDate startOfLastMonth);

    @Query(value = """
            (SELECT 'SALE' as activityType, 
                    CONCAT(v.description, ' sale to ', c.name) as description, 
                    CAST(s.sale_date AS TIMESTAMP) as dateTime, 'C' as txnType, s.net_sale_amount as txnAmount
             FROM app_sale s
             JOIN app_inventory i ON s.inventory_id = i.id
             JOIN app_product p ON i.product_id = p.id
             JOIN fnd_brand_model v ON p.model_id = v.id
             JOIN app_customer c ON s.customer_id = c.id
             WHERE s.deleted = false)
            UNION ALL
            (SELECT 'PURCHASE' as activityType, 
                    CONCAT('Purchase from ', ven.name) as description, 
                    CAST(p_order.order_date AS TIMESTAMP) as dateTime, 'D' as txnType, p_order.total_amount as txnAmount
             FROM app_purchase_order p_order
             JOIN app_vendor ven ON p_order.vendor_id = ven.id
             WHERE p_order.deleted = false)
            UNION ALL
            (SELECT 'EXPENSE' as activityType, 
                    e.description as description, 
                    CAST(e.date AS TIMESTAMP) as dateTime, 'D' as txnType, e.amount as txnAmount
             FROM app_expense e
             WHERE e.deleted = false AND e.purchase_order_id IS NULL)
            ORDER BY dateTime DESC LIMIT 5
            """, nativeQuery = true)
    List<ActivityProjection> findRecentActivities();

    @Query(value = """
            SELECT 
                m.description as name, 
                COUNT(s.id) as countValue
            FROM app_sale s
            JOIN app_inventory i ON s.inventory_id = i.id
            JOIN app_product p ON i.product_id = p.id
            JOIN fnd_brand_model m ON p.model_id = m.id
            WHERE s.deleted = false
            GROUP BY m.description
            ORDER BY countValue DESC
            LIMIT 5
            """, nativeQuery = true)
    List<ProductMetrics> findTopSoldProducts();

    @Query(value = """
            SELECT 
                m.description as name, 
                SUM(s.profit_amount) as revenueValue
            FROM app_sale s
            JOIN app_inventory i ON s.inventory_id = i.id
            JOIN app_product p ON i.product_id = p.id
            JOIN fnd_brand_model m ON p.model_id = m.id
            WHERE s.deleted = false
            GROUP BY m.description
            ORDER BY revenueValue DESC
            LIMIT 5
            """, nativeQuery = true)
    List<ProductMetrics> findTopProfitProducts();

    @Query(value = """
        WITH RECURSIVE months AS (
            SELECT DATE_TRUNC('month', CURRENT_DATE) as month_date
            UNION ALL
            SELECT DATE_TRUNC('month', month_date - INTERVAL '1 month')
            FROM months
            WHERE month_date > DATE_TRUNC('month', CURRENT_DATE - CAST(:monthCount - 1 || ' month' AS INTERVAL))
        )
        SELECT 
            TO_CHAR(m.month_date, 'YYYY-MM') as monthName,
            COALESCE((SELECT SUM(net_sale_amount) FROM app_sale s 
                      WHERE DATE_TRUNC('month', s.sale_date) = m.month_date AND s.deleted = false), 0) as sales,
            COALESCE((SELECT SUM(total_amount) FROM app_purchase_order p 
                      WHERE DATE_TRUNC('month', p.order_date) = m.month_date AND p.deleted = false), 0) as purchases,
            COALESCE((SELECT SUM(amount) FROM app_expense e 
                      WHERE DATE_TRUNC('month', e.date) = m.month_date AND e.deleted = false AND e.purchase_order_id IS NULL), 0) as expenses,
            COALESCE((SELECT SUM(s.net_sale_amount - COALESCE(s.landed_cost_at_sale, 0)) FROM app_sale s
                      WHERE DATE_TRUNC('month', s.sale_date) = m.month_date AND s.deleted = false), 0)
            -
            COALESCE((SELECT SUM(e.amount) FROM app_expense e
                      WHERE DATE_TRUNC('month', e.date) = m.month_date AND e.deleted = false AND e.purchase_order_id IS NULL), 0)
            -
            COALESCE((SELECT SUM(d.amount) FROM app_direct_entry d
                      JOIN fnd_chart_of_accounts coa ON coa.id = d.coa_id
                      WHERE DATE_TRUNC('month', d.entry_date) = m.month_date
                      AND d.deleted = false AND d.direction = 'OUT'
                      AND coa.type <> 'EQUITY'), 0) as profit
        FROM months m
        ORDER BY m.month_date DESC
        """, nativeQuery = true)
    List<BalanceMetrics> getMonthlyBalanceSheet(@Param("monthCount") int monthCount);

    @Query(value = """
            WITH RECURSIVE months AS (
                SELECT DATE_TRUNC('month', CURRENT_DATE) as month_date
                UNION ALL
                SELECT DATE_TRUNC('month', month_date - INTERVAL '1 month')
                FROM months
                WHERE month_date > DATE_TRUNC('month',
                      CURRENT_DATE - CAST((:monthCount - 1) || ' month' AS INTERVAL))
            )
            SELECT
                TO_CHAR(m.month_date, 'YYYY-MM')  as month,
                TO_CHAR(m.month_date, 'Mon YYYY') as monthLabel,

                COALESCE((SELECT COUNT(*) FROM app_sale s
                          WHERE DATE_TRUNC('month', s.sale_date) = m.month_date
                          AND s.deleted = false), 0) as salesCount,

                COALESCE((SELECT SUM(s.sale_rate) FROM app_sale s
                          WHERE DATE_TRUNC('month', s.sale_date) = m.month_date
                          AND s.deleted = false), 0) as totalRevenue,

                COALESCE((SELECT SUM(s.net_sale_amount - COALESCE(s.landed_cost_at_sale, 0)) FROM app_sale s
                          WHERE DATE_TRUNC('month', s.sale_date) = m.month_date
                          AND s.deleted = false), 0) as grossProfit,

                COALESCE((SELECT SUM(s.net_sale_amount) FROM app_sale s
                          WHERE DATE_TRUNC('month', s.sale_date) = m.month_date
                          AND s.deleted = false
                          AND s.payment_status IN ('PENDING','PARTIAL','FINANCE_PENDING')), 0) as totalReceivables,

                COALESCE((
                    SELECT SUM(po.total_amount) - COALESCE(SUM(pp_sum.paid), 0)
                    FROM app_purchase_order po
                    LEFT JOIN (
                        SELECT purchase_order_id, SUM(amount) as paid
                        FROM app_purchase_payment
                        WHERE deleted = false
                        GROUP BY purchase_order_id
                    ) pp_sum ON pp_sum.purchase_order_id = po.id
                    WHERE DATE_TRUNC('month', po.order_date) = m.month_date
                    AND po.deleted = false
                ), 0) as totalPayables,

                COALESCE((SELECT SUM(e.amount) FROM app_expense e
                          WHERE DATE_TRUNC('month', e.date) = m.month_date
                          AND e.deleted = false
                          AND e.purchase_order_id IS NULL), 0)
                +
                COALESCE((SELECT SUM(d.amount) FROM app_direct_entry d
                          JOIN fnd_chart_of_accounts coa ON coa.id = d.coa_id
                          WHERE DATE_TRUNC('month', d.entry_date) = m.month_date
                          AND d.deleted = false AND d.direction = 'OUT'
                          AND coa.type <> 'EQUITY'), 0) as totalExpenses

            FROM months m
            ORDER BY m.month_date ASC
            """, nativeQuery = true)
    List<MonthlyTrendMetrics> getMonthlyTrend(@Param("monthCount") int monthCount);

    @Query(value = """
            SELECT
              COUNT(*) as pendingCount,
              COALESCE(SUM(s.net_sale_amount), 0) as pendingAmount,
              COALESCE(SUM(CASE WHEN s.payment_status = 'FINANCE_PENDING' THEN s.finance_amount ELSE 0 END), 0) as financePendingAmount
            FROM app_sale s
            WHERE s.deleted = false
              AND s.sale_date BETWEEN :startDate AND :endDate
              AND s.payment_status IN ('PENDING', 'PARTIAL', 'FINANCE_PENDING')
            """, nativeQuery = true)
    PLPendingMetrics getPendingByPeriod(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("""
            SELECT pd.purchase.id as purchaseId,
                   s.saleDate as saleDate,
                   pc.expenseLockEnabled as expenseLockEnabled,
                   pc.expenseLockWindow as expenseLockWindow
            FROM Sale s
            JOIN s.inventory i
            JOIN i.purchaseOrderDetail pd
            JOIN pd.product p
            JOIN p.category pc
            WHERE pd.purchase.id IN :purchaseIds
            """)
    List<PurchaseEditabilityProjection> findEditabilityInfoByPurchaseIds(@Param("purchaseIds") List<Long> purchaseIds);

}
