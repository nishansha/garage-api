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

    @Query("SELECT s FROM Sale s WHERE s.inventory.id = :id AND s.status.code <> 'RETURNED'")
    Sale findByInventoryId(@Param("id") Long id);

    @Query(value = "SELECT nextval('so_ref_no_seq')", nativeQuery = true)
    Long getNextReferenceNumber();

    @Query(value = """
            SELECT
              COALESCE((SELECT SUM(s.sale_rate) FROM app_sale s
                        WHERE s.sale_date BETWEEN :startDate AND :endDate AND s.deleted = false), 0)
              -
              COALESCE((SELECT SUM(s.sale_rate) FROM app_sale_return sr
                        JOIN app_sale s ON s.id = sr.sale_id
                        WHERE sr.return_date BETWEEN :startDate AND :endDate AND sr.deleted = false), 0) as totalSales,
              COALESCE((SELECT SUM(s.landed_cost_at_sale) FROM app_sale s
                        WHERE s.sale_date BETWEEN :startDate AND :endDate AND s.deleted = false), 0)
              -
              COALESCE((SELECT SUM(s.landed_cost_at_sale) FROM app_sale_return sr
                        JOIN app_sale s ON s.id = sr.sale_id
                        WHERE sr.return_date BETWEEN :startDate AND :endDate AND sr.deleted = false), 0) as totalCost,
              COALESCE((SELECT SUM(s.sale_rate - COALESCE(s.landed_cost_at_sale, 0)) FROM app_sale s
                        WHERE s.sale_date BETWEEN :startDate AND :endDate AND s.deleted = false), 0)
              -
              COALESCE((SELECT SUM(s.sale_rate - COALESCE(s.landed_cost_at_sale, 0)) FROM app_sale_return sr
                        JOIN app_sale s ON s.id = sr.sale_id
                        WHERE sr.return_date BETWEEN :startDate AND :endDate AND sr.deleted = false), 0) as netProfit,
              COALESCE((SELECT COUNT(*) FROM app_sale s
                        WHERE s.sale_date BETWEEN :startDate AND :endDate AND s.deleted = false), 0)
              -
              COALESCE((SELECT COUNT(*) FROM app_sale_return sr
                        JOIN app_sale s ON s.id = sr.sale_id
                        WHERE sr.return_date BETWEEN :startDate AND :endDate AND sr.deleted = false), 0) as unitsSold
            FROM (SELECT 1) data
            """, nativeQuery = true)
    ProfitMetrics getProfitReport(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query(value = """
            SELECT
                s.id            as saleId,
                s.invoice_no    as invoiceNo,
                s.sale_date     as saleDate,
                inv.product_no  as vehicleNo,
                c.name          as customerName,
                pod.unit_cost   as purchaseRate,
                (COALESCE(s.landed_cost_at_sale, 0) - COALESCE(pod.unit_cost, 0)) as purchaseExpenses,
                s.sale_rate     as saleRate,
                CASE WHEN sr.id IS NOT NULL
                     THEN COALESCE(sr.sold_vehicle_deduction_amount, 0)
                     ELSE s.profit_amount END as profit,
                (sr.id IS NOT NULL) as returned,
                CASE WHEN sr.id IS NOT NULL THEN 0
                     ELSE GREATEST(0, COALESCE(s.net_sale_amount, 0)
                          - COALESCE((SELECT SUM(sp.amount) FROM app_sale_payment sp
                                      WHERE sp.sale_id = s.id AND sp.deleted = false
                                        AND sp.payment_date <= :endDate), 0))
                END as pendingAmount,
                CASE WHEN sr_all.id IS NOT NULL THEN 0
                     ELSE GREATEST(0, COALESCE(s.net_sale_amount, 0)
                          - COALESCE((SELECT SUM(sp.amount) FROM app_sale_payment sp
                                      WHERE sp.sale_id = s.id AND sp.deleted = false), 0))
                END as pendingTillDate
            FROM app_sale s
            JOIN app_inventory inv ON inv.id = s.inventory_id
            JOIN app_purchase_order_detail pod ON pod.id = inv.purchase_order_detail_id
            JOIN app_customer c ON c.id = s.customer_id
            LEFT JOIN app_sale_return sr ON sr.sale_id = s.id AND sr.deleted = false
                                        AND sr.return_date <= :endDate
            LEFT JOIN app_sale_return sr_all ON sr_all.sale_id = s.id AND sr_all.deleted = false
            WHERE s.deleted = false
              AND s.sale_date BETWEEN :startDate AND :endDate
            ORDER BY s.sale_date, s.id
            """, nativeQuery = true)
    List<SaleLineRow> getSaleLinesByPeriod(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query(value = """
            SELECT
              COALESCE((SELECT SUM(s.sale_rate) FROM app_sale s
                        WHERE s.sale_date >= :startOfMonth AND s.deleted = false), 0)
              -
              COALESCE((SELECT SUM(s.sale_rate) FROM app_sale_return sr
                        JOIN app_sale s ON s.id = sr.sale_id
                        WHERE sr.return_date >= :startOfMonth AND sr.deleted = false), 0) as totalSalesThisMonth,
              COALESCE((SELECT SUM(s.sale_rate) FROM app_sale s
                        WHERE s.sale_date BETWEEN :startOfLastMonth AND :endOfLastMonth AND s.deleted = false), 0)
              -
              COALESCE((SELECT SUM(s.sale_rate) FROM app_sale_return sr
                        JOIN app_sale s ON s.id = sr.sale_id
                        WHERE sr.return_date BETWEEN :startOfLastMonth AND :endOfLastMonth AND sr.deleted = false), 0) as totalSalesLastMonth,
              COALESCE((SELECT COUNT(*) FROM app_sale s
                        WHERE s.sale_date = :today AND s.deleted = false
                          AND s.payment_status <> 'REFUND'), 0) as todayCount,
              COALESCE((SELECT COUNT(*) FROM app_sale s
                        WHERE s.sale_date >= :startOfMonth AND s.deleted = false
                          AND s.payment_status <> 'REFUND'), 0) as monthCount
            FROM (SELECT 1) data
            """, nativeQuery = true)
    SaleMetrics getSalesSummaryMetrics(@Param("startOfLastMonth") LocalDate startOfLastMonth, @Param("endOfLastMonth") LocalDate endOfLastMonth, @Param("startOfMonth") LocalDate startOfMonth, @Param("today") LocalDate today);

    @Query(value = """
            SELECT
              (SELECT COALESCE(SUM(sale_rate), 0) FROM app_sale
               WHERE deleted = false AND sale_date >= :startOfMonth)
              -
              (SELECT COALESCE(SUM(s.sale_rate), 0) FROM app_sale_return sr
               JOIN app_sale s ON s.id = sr.sale_id
               WHERE sr.deleted = false AND sr.return_date >= :startOfMonth) as totalSales,

              (SELECT COALESCE(SUM(sale_rate), 0) FROM app_sale
               WHERE deleted = false AND sale_date >= :startOfLastMonth AND sale_date < :startOfMonth)
              -
              (SELECT COALESCE(SUM(s.sale_rate), 0) FROM app_sale_return sr
               JOIN app_sale s ON s.id = sr.sale_id
               WHERE sr.deleted = false
                 AND sr.return_date >= :startOfLastMonth AND sr.return_date < :startOfMonth) as salesBeforeMonth,

              (SELECT COALESCE(SUM(total_amount), 0) FROM app_purchase_order
               WHERE deleted = false AND order_date >= :startOfMonth)
              -
              (SELECT COALESCE(SUM(pr.inventory_landed_cost), 0) FROM app_purchase_return pr
               WHERE pr.deleted = false AND pr.return_date >= :startOfMonth) as totalPurchases,

              (SELECT COALESCE(SUM(total_amount), 0) FROM app_purchase_order
               WHERE deleted = false AND order_date >= :startOfLastMonth AND order_date < :startOfMonth)
              -
              (SELECT COALESCE(SUM(pr.inventory_landed_cost), 0) FROM app_purchase_return pr
               WHERE pr.deleted = false
                 AND pr.return_date >= :startOfLastMonth AND pr.return_date < :startOfMonth) as purchasesBeforeMonth,

              (SELECT COALESCE(SUM(amount), 0) FROM app_expense
               WHERE deleted = false AND purchase_order_id IS NULL AND date >= :startOfMonth)
              +
              (SELECT COALESCE(SUM(d.amount), 0) FROM app_direct_entry d
               JOIN fnd_chart_of_accounts coa ON coa.id = d.coa_id
               WHERE d.deleted = false AND d.direction = 'OUT'
                 AND coa.type = 'EXPENSE'
                 AND d.entry_date >= :startOfMonth)
              +
              (SELECT COALESCE(SUM(GREATEST(0, pr.inventory_landed_cost - pr.return_amount)), 0)
               FROM app_purchase_return pr
               WHERE pr.deleted = false AND pr.return_date >= :startOfMonth)
              +
              (SELECT COALESCE(SUM(GREATEST(0, inv.landed_cost - s.exchange_amount)), 0)
               FROM app_sale_return sr
               JOIN app_sale s ON s.id = sr.sale_id
               JOIN app_inventory inv ON inv.source_sale_id = s.id
               WHERE sr.deleted = false AND sr.return_date >= :startOfMonth
                 AND sr.exchange_handling = 'RETURN_TO_BUYER') as totalExpenses,

              (SELECT COALESCE(SUM(amount), 0) FROM app_expense
               WHERE deleted = false AND purchase_order_id IS NULL AND date >= :startOfLastMonth AND date < :startOfMonth)
              +
              (SELECT COALESCE(SUM(d.amount), 0) FROM app_direct_entry d
               JOIN fnd_chart_of_accounts coa ON coa.id = d.coa_id
               WHERE d.deleted = false AND d.direction = 'OUT'
                 AND coa.type = 'EXPENSE'
                 AND d.entry_date >= :startOfLastMonth AND d.entry_date < :startOfMonth)
              +
              (SELECT COALESCE(SUM(GREATEST(0, pr.inventory_landed_cost - pr.return_amount)), 0)
               FROM app_purchase_return pr
               WHERE pr.deleted = false
                 AND pr.return_date >= :startOfLastMonth AND pr.return_date < :startOfMonth)
              +
              (SELECT COALESCE(SUM(GREATEST(0, inv.landed_cost - s.exchange_amount)), 0)
               FROM app_sale_return sr
               JOIN app_sale s ON s.id = sr.sale_id
               JOIN app_inventory inv ON inv.source_sale_id = s.id
               WHERE sr.deleted = false
                 AND sr.return_date >= :startOfLastMonth AND sr.return_date < :startOfMonth
                 AND sr.exchange_handling = 'RETURN_TO_BUYER') as expensesBeforeMonth,

              (SELECT COALESCE(SUM(sale_rate - COALESCE(landed_cost_at_sale, 0)), 0) FROM app_sale
               WHERE deleted = false AND sale_date >= :startOfMonth)
              -
              (SELECT COALESCE(SUM(s.sale_rate - COALESCE(s.landed_cost_at_sale, 0)), 0) FROM app_sale_return sr
               JOIN app_sale s ON s.id = sr.sale_id
               WHERE sr.deleted = false AND sr.return_date >= :startOfMonth)
              +
              (SELECT COALESCE(SUM(sr.sold_vehicle_deduction_amount + sr.exchange_vehicle_deduction_amount), 0)
               FROM app_sale_return sr
               WHERE sr.deleted = false AND sr.return_date >= :startOfMonth) as totalGrossProfit,

              (SELECT COALESCE(SUM(sale_rate - COALESCE(landed_cost_at_sale, 0)), 0) FROM app_sale
               WHERE deleted = false AND sale_date >= :startOfLastMonth AND sale_date < :startOfMonth)
              -
              (SELECT COALESCE(SUM(s.sale_rate - COALESCE(s.landed_cost_at_sale, 0)), 0) FROM app_sale_return sr
               JOIN app_sale s ON s.id = sr.sale_id
               WHERE sr.deleted = false
                 AND sr.return_date >= :startOfLastMonth AND sr.return_date < :startOfMonth)
              +
              (SELECT COALESCE(SUM(sr.sold_vehicle_deduction_amount + sr.exchange_vehicle_deduction_amount), 0)
               FROM app_sale_return sr
               WHERE sr.deleted = false
                 AND sr.return_date >= :startOfLastMonth AND sr.return_date < :startOfMonth) as grossProfitBeforeMonth

            FROM (SELECT 1) data
            """, nativeQuery = true)
    SummaryMetrics getFinancialSummary(@Param("startOfMonth") LocalDate startOfMonth,
                                       @Param("startOfLastMonth") LocalDate startOfLastMonth);

    @Query(value = """
            (SELECT 'SALE' as activityType, 
                    CONCAT(v.description, ' sale to ', c.name) as description, 
                    CAST(s.sale_date AS TIMESTAMP) as dateTime, 'C' as txnType, s.sale_rate as txnAmount
             FROM app_sale s
             JOIN app_inventory i ON s.inventory_id = i.id
             JOIN app_product p ON i.product_id = p.id
             JOIN fnd_brand_model v ON p.model_id = v.id
             JOIN app_customer c ON s.customer_id = c.id
             WHERE s.deleted = false AND s.payment_status <> 'REFUND')
            UNION ALL
            (SELECT 'PURCHASE' as activityType,
                    CONCAT('Purchase from ', ven.name) as description,
                    CAST(p_order.order_date AS TIMESTAMP) as dateTime, 'D' as txnType, p_order.total_amount as txnAmount
             FROM app_purchase_order p_order
             JOIN app_vendor ven ON p_order.vendor_id = ven.id
             LEFT JOIN fnd_lookup_master ls ON ls.id = p_order.status_id
             WHERE p_order.deleted = false AND (ls.code IS NULL OR ls.code <> 'RETURNED'))
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
              AND s.payment_status <> 'REFUND'
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
              AND s.payment_status <> 'REFUND'
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
            COALESCE((SELECT SUM(sale_rate) FROM app_sale s
                      WHERE DATE_TRUNC('month', s.sale_date) = m.month_date AND s.deleted = false), 0)
            -
            COALESCE((SELECT SUM(s.sale_rate) FROM app_sale_return sr
                      JOIN app_sale s ON s.id = sr.sale_id
                      WHERE DATE_TRUNC('month', sr.return_date) = m.month_date AND sr.deleted = false), 0) as sales,
            COALESCE((SELECT SUM(
                          CASE WHEN tradein.unit_cost IS NOT NULL
                               THEN tradein.unit_cost - LEAST(tradein.sale_rate, tradein.unit_cost)
                               ELSE p.total_amount END)
                      FROM app_purchase_order p
                      LEFT JOIN (
                          SELECT pod.purchase_order_id, pod.unit_cost, s.sale_rate
                          FROM app_purchase_order_detail pod
                          JOIN app_inventory inv ON inv.purchase_order_detail_id = pod.id
                          JOIN app_sale s ON s.id = inv.source_sale_id
                          WHERE inv.source_sale_id IS NOT NULL
                      ) tradein ON tradein.purchase_order_id = p.id
                      WHERE DATE_TRUNC('month', p.order_date) = m.month_date AND p.deleted = false), 0)
            -
            COALESCE((SELECT SUM(pr.inventory_landed_cost) FROM app_purchase_return pr
                      WHERE DATE_TRUNC('month', pr.return_date) = m.month_date AND pr.deleted = false), 0) as purchases,
            COALESCE((SELECT SUM(amount) FROM app_expense e
                      WHERE DATE_TRUNC('month', e.date) = m.month_date AND e.deleted = false AND e.purchase_order_id IS NULL), 0)
            +
            COALESCE((SELECT SUM(GREATEST(0, pr.inventory_landed_cost - pr.return_amount))
                      FROM app_purchase_return pr
                      WHERE DATE_TRUNC('month', pr.return_date) = m.month_date AND pr.deleted = false), 0)
            +
            COALESCE((SELECT SUM(GREATEST(0, inv.landed_cost - s.exchange_amount))
                      FROM app_sale_return sr
                      JOIN app_sale s ON s.id = sr.sale_id
                      JOIN app_inventory inv ON inv.source_sale_id = s.id
                      WHERE DATE_TRUNC('month', sr.return_date) = m.month_date AND sr.deleted = false
                      AND sr.exchange_handling = 'RETURN_TO_BUYER'), 0) as expenses,
            COALESCE((SELECT SUM(s.sale_rate - COALESCE(s.landed_cost_at_sale, 0)) FROM app_sale s
                      WHERE DATE_TRUNC('month', s.sale_date) = m.month_date AND s.deleted = false), 0)
            -
            COALESCE((SELECT SUM(s.sale_rate - COALESCE(s.landed_cost_at_sale, 0)) FROM app_sale_return sr
                      JOIN app_sale s ON s.id = sr.sale_id
                      WHERE DATE_TRUNC('month', sr.return_date) = m.month_date AND sr.deleted = false), 0)
            +
            COALESCE((SELECT SUM(sr.sold_vehicle_deduction_amount + sr.exchange_vehicle_deduction_amount)
                      FROM app_sale_return sr
                      WHERE DATE_TRUNC('month', sr.return_date) = m.month_date AND sr.deleted = false), 0)
            +
            COALESCE((SELECT SUM(d.amount) FROM app_direct_entry d
                      JOIN fnd_chart_of_accounts coa ON coa.id = d.coa_id
                      WHERE DATE_TRUNC('month', d.entry_date) = m.month_date
                      AND d.deleted = false AND d.direction = 'IN'
                      AND coa.type = 'REVENUE'), 0)
            -
            COALESCE((SELECT SUM(e.amount) FROM app_expense e
                      WHERE DATE_TRUNC('month', e.date) = m.month_date AND e.deleted = false AND e.purchase_order_id IS NULL), 0)
            -
            COALESCE((SELECT SUM(d.amount) FROM app_direct_entry d
                      JOIN fnd_chart_of_accounts coa ON coa.id = d.coa_id
                      WHERE DATE_TRUNC('month', d.entry_date) = m.month_date
                      AND d.deleted = false AND d.direction = 'OUT'
                      AND coa.type = 'EXPENSE'), 0)
            -
            COALESCE((SELECT SUM(GREATEST(0, pr.inventory_landed_cost - pr.return_amount))
                      FROM app_purchase_return pr
                      WHERE DATE_TRUNC('month', pr.return_date) = m.month_date AND pr.deleted = false), 0)
            -
            COALESCE((SELECT SUM(GREATEST(0, inv.landed_cost - s.exchange_amount))
                      FROM app_sale_return sr
                      JOIN app_sale s ON s.id = sr.sale_id
                      JOIN app_inventory inv ON inv.source_sale_id = s.id
                      WHERE DATE_TRUNC('month', sr.return_date) = m.month_date AND sr.deleted = false
                      AND sr.exchange_handling = 'RETURN_TO_BUYER'), 0) as profit
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
                          AND s.deleted = false
                          AND s.payment_status <> 'REFUND'), 0) as salesCount,

                COALESCE((SELECT SUM(s.sale_rate) FROM app_sale s
                          WHERE DATE_TRUNC('month', s.sale_date) = m.month_date
                          AND s.deleted = false), 0)
                +
                COALESCE((SELECT SUM(d.amount) FROM app_direct_entry d
                          JOIN fnd_chart_of_accounts coa ON coa.id = d.coa_id
                          WHERE DATE_TRUNC('month', d.entry_date) = m.month_date
                          AND d.deleted = false AND d.direction = 'IN'
                          AND coa.type = 'REVENUE'), 0)
                -
                COALESCE((SELECT SUM(s.sale_rate) FROM app_sale_return sr
                          JOIN app_sale s ON s.id = sr.sale_id
                          WHERE DATE_TRUNC('month', sr.return_date) = m.month_date
                          AND sr.deleted = false), 0)
                +
                COALESCE((SELECT SUM(sr.sold_vehicle_deduction_amount + sr.exchange_vehicle_deduction_amount)
                          FROM app_sale_return sr
                          WHERE DATE_TRUNC('month', sr.return_date) = m.month_date
                          AND sr.deleted = false), 0) as totalRevenue,

                COALESCE((SELECT SUM(d.amount) FROM app_direct_entry d
                          JOIN fnd_chart_of_accounts coa ON coa.id = d.coa_id
                          WHERE DATE_TRUNC('month', d.entry_date) = m.month_date
                          AND d.deleted = false AND d.direction = 'IN'
                          AND coa.type = 'REVENUE'), 0)
                +
                COALESCE((SELECT SUM(sr.sold_vehicle_deduction_amount + sr.exchange_vehicle_deduction_amount)
                          FROM app_sale_return sr
                          WHERE DATE_TRUNC('month', sr.return_date) = m.month_date
                          AND sr.deleted = false), 0) as otherIncome,

                COALESCE((SELECT SUM(s.sale_rate - COALESCE(s.landed_cost_at_sale, 0)) FROM app_sale s
                          WHERE DATE_TRUNC('month', s.sale_date) = m.month_date
                          AND s.deleted = false), 0)
                -
                COALESCE((SELECT SUM(s.sale_rate - COALESCE(s.landed_cost_at_sale, 0))
                          FROM app_sale_return sr
                          JOIN app_sale s ON s.id = sr.sale_id
                          WHERE DATE_TRUNC('month', sr.return_date) = m.month_date
                          AND sr.deleted = false), 0)
                +
                COALESCE((SELECT SUM(sr.sold_vehicle_deduction_amount + sr.exchange_vehicle_deduction_amount)
                          FROM app_sale_return sr
                          WHERE DATE_TRUNC('month', sr.return_date) = m.month_date
                          AND sr.deleted = false), 0) as grossProfit,

                COALESCE((
                    SELECT SUM(s.net_sale_amount) - COALESCE(SUM(sp_sum.paid), 0)
                    FROM app_sale s
                    LEFT JOIN (
                        SELECT sale_id, SUM(amount) as paid
                        FROM app_sale_payment
                        WHERE deleted = false
                        GROUP BY sale_id
                    ) sp_sum ON sp_sum.sale_id = s.id
                    WHERE DATE_TRUNC('month', s.sale_date) = m.month_date
                    AND s.deleted = false
                    AND s.payment_status IN ('PENDING','PARTIAL','FINANCE_PENDING')
                ), 0) as totalReceivables,

                COALESCE((
                    SELECT SUM(
                        CASE WHEN tradein.unit_cost IS NOT NULL
                             THEN tradein.unit_cost - LEAST(tradein.sale_rate, tradein.unit_cost) - COALESCE(pp_sum.paid, 0)
                             ELSE GREATEST(0,
                                      po.total_amount
                                      - COALESCE(pp_sum.paid, 0)
                                      - COALESCE(exp_sum.expense, 0)
                                      - COALESCE(pr_sum.returned_unwind, 0))
                        END
                    )
                    FROM app_purchase_order po
                    LEFT JOIN (
                        SELECT purchase_order_id, SUM(amount) as paid
                        FROM app_purchase_payment
                        WHERE deleted = false
                        GROUP BY purchase_order_id
                    ) pp_sum ON pp_sum.purchase_order_id = po.id
                    LEFT JOIN (
                        SELECT purchase_order_id, SUM(amount) as expense
                        FROM app_expense
                        WHERE deleted = false AND purchase_order_id IS NOT NULL
                        GROUP BY purchase_order_id
                    ) exp_sum ON exp_sum.purchase_order_id = po.id
                    LEFT JOIN (
                        SELECT pod.purchase_order_id, pod.unit_cost, s.sale_rate
                        FROM app_purchase_order_detail pod
                        JOIN app_inventory inv ON inv.purchase_order_detail_id = pod.id
                        JOIN app_sale s ON s.id = inv.source_sale_id
                        WHERE inv.source_sale_id IS NOT NULL
                    ) tradein ON tradein.purchase_order_id = po.id
                    LEFT JOIN (
                        SELECT purchase_id, SUM(return_amount) as returned_unwind
                        FROM app_purchase_return
                        WHERE deleted = false
                        GROUP BY purchase_id
                    ) pr_sum ON pr_sum.purchase_id = po.id
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
                          AND coa.type = 'EXPENSE'), 0)
                +
                COALESCE((SELECT SUM(GREATEST(0, pr.inventory_landed_cost - pr.return_amount))
                          FROM app_purchase_return pr
                          WHERE DATE_TRUNC('month', pr.return_date) = m.month_date
                          AND pr.deleted = false), 0)
                +
                COALESCE((SELECT SUM(GREATEST(0, inv.landed_cost - s.exchange_amount))
                          FROM app_sale_return sr
                          JOIN app_sale s ON s.id = sr.sale_id
                          JOIN app_inventory inv ON inv.source_sale_id = s.id
                          WHERE DATE_TRUNC('month', sr.return_date) = m.month_date
                          AND sr.deleted = false
                          AND sr.exchange_handling = 'RETURN_TO_BUYER'), 0) as totalExpenses

            FROM months m
            ORDER BY m.month_date ASC
            """, nativeQuery = true)
    List<MonthlyTrendMetrics> getMonthlyTrend(@Param("monthCount") int monthCount);

    @Query(value = """
            SELECT
              COUNT(*) as pendingCount,
              COALESCE(SUM(s.net_sale_amount) - COALESCE(SUM(sp_sum.paid), 0), 0) as pendingAmount,
              COALESCE(SUM(CASE WHEN s.payment_status = 'FINANCE_PENDING' THEN s.finance_amount ELSE 0 END), 0) as financePendingAmount
            FROM app_sale s
            LEFT JOIN (
                SELECT sale_id, SUM(amount) as paid
                FROM app_sale_payment
                WHERE deleted = false
                GROUP BY sale_id
            ) sp_sum ON sp_sum.sale_id = s.id
            WHERE s.deleted = false
              AND s.sale_date BETWEEN :startDate AND :endDate
              AND s.payment_status IN ('PENDING', 'PARTIAL', 'FINANCE_PENDING')
            """, nativeQuery = true)
    PLPendingMetrics getPendingByPeriod(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query(value = """
            SELECT
                s.id            as saleId,
                s.invoice_no    as invoiceNo,
                s.payment_status as paymentStatus,
                inv.product_no  as vehicleNo,
                s.sale_date     as saleDate,
                s.net_sale_amount as amount,
                (s.net_sale_amount - COALESCE(sp_sum.paid, 0)) as pendingAmount,
                sp_sum.last_payment_date as lastPaymentDate,
                c.name          as customerName,
                c.mobile        as customerMobile
            FROM app_sale s
            JOIN app_inventory inv ON inv.id = s.inventory_id
            JOIN app_customer c    ON c.id = s.customer_id
            LEFT JOIN (
                SELECT sale_id,
                       SUM(amount)      as paid,
                       MAX(payment_date) as last_payment_date
                FROM app_sale_payment
                WHERE deleted = false
                GROUP BY sale_id
            ) sp_sum ON sp_sum.sale_id = s.id
            WHERE s.deleted = false
              AND s.payment_status IN ('PENDING','PARTIAL','FINANCE_PENDING')
            ORDER BY s.sale_date DESC
            """, nativeQuery = true)
    List<ReceivableRow> findReceivables();

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
              AND s.status.code <> 'RETURNED'
            """)
    List<PurchaseEditabilityProjection> findEditabilityInfoByPurchaseIds(@Param("purchaseIds") List<Long> purchaseIds);

}
