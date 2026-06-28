package com.triasoft.garage.repository;

import com.triasoft.garage.entity.Purchase;
import com.triasoft.garage.projection.PayableRow;
import com.triasoft.garage.projection.PurchaseListProjection;
import com.triasoft.garage.projection.PurchaseMetrics;
import com.triasoft.garage.projection.SummaryMetrics;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PurchaseRepository extends JpaRepository<Purchase, Long>, JpaSpecificationExecutor<Purchase> {

    @Query(value = "SELECT nextval('po_ref_no_seq')", nativeQuery = true)
    Long getNextReferenceNumber();

    @Query(value = """
            SELECT p.id as id, p.orderDate as date, pd.uuid as code, pd.productNo as vehicleNo,
                   brand.description as brandName, model.description as modelName, variant.description as variantName,
                   pd.unitCost as purchaseRate
            FROM Purchase p
            JOIN p.purchaseDetails pd
            JOIN pd.product prod
            LEFT JOIN prod.brand brand
            LEFT JOIN prod.model model
            LEFT JOIN prod.varient variant
            """,
            countQuery = """
            SELECT COUNT(p) FROM Purchase p
            JOIN p.purchaseDetails pd
            JOIN pd.product prod
            """)
    Page<PurchaseListProjection> findAllForList(Pageable pageable);

    @Query(value = """
            SELECT p.id as id, p.orderDate as date, pd.uuid as code, pd.productNo as vehicleNo,
                   brand.description as brandName, model.description as modelName, variant.description as variantName,
                   pd.unitCost as purchaseRate
            FROM Purchase p
            JOIN p.purchaseDetails pd
            JOIN pd.product prod
            LEFT JOIN prod.brand brand
            LEFT JOIN prod.model model
            LEFT JOIN prod.varient variant
            JOIN p.vendor vendor
            WHERE (:fromDate IS NULL OR p.orderDate >= :fromDate)
              AND (:toDate IS NULL OR p.orderDate <= :toDate)
              AND (:brandId IS NULL OR brand.id = :brandId)
              AND (:modelId IS NULL OR model.id = :modelId)
              AND (:variantId IS NULL OR variant.id = :variantId)
              AND (:vehicleNo IS NULL OR LOWER(pd.productNo) LIKE LOWER(CONCAT('%', :vehicleNo, '%')))
              AND (:searchText IS NULL OR (
                   LOWER(p.referenceNo) LIKE LOWER(CONCAT('%', :searchText, '%'))
                   OR LOWER(p.notes) LIKE LOWER(CONCAT('%', :searchText, '%'))
                   OR LOWER(pd.productNo) LIKE LOWER(CONCAT('%', :searchText, '%'))
                   OR LOWER(vendor.name) LIKE LOWER(CONCAT('%', :searchText, '%'))))
            """,
            countQuery = """
            SELECT COUNT(p) FROM Purchase p
            JOIN p.purchaseDetails pd
            JOIN pd.product prod
            LEFT JOIN prod.brand brand
            LEFT JOIN prod.model model
            LEFT JOIN prod.varient variant
            JOIN p.vendor vendor
            WHERE (:fromDate IS NULL OR p.orderDate >= :fromDate)
              AND (:toDate IS NULL OR p.orderDate <= :toDate)
              AND (:brandId IS NULL OR brand.id = :brandId)
              AND (:modelId IS NULL OR model.id = :modelId)
              AND (:variantId IS NULL OR variant.id = :variantId)
              AND (:vehicleNo IS NULL OR LOWER(pd.productNo) LIKE LOWER(CONCAT('%', :vehicleNo, '%')))
              AND (:searchText IS NULL OR (
                   LOWER(p.referenceNo) LIKE LOWER(CONCAT('%', :searchText, '%'))
                   OR LOWER(p.notes) LIKE LOWER(CONCAT('%', :searchText, '%'))
                   OR LOWER(pd.productNo) LIKE LOWER(CONCAT('%', :searchText, '%'))
                   OR LOWER(vendor.name) LIKE LOWER(CONCAT('%', :searchText, '%'))))
            """)
    Page<PurchaseListProjection> searchForList(
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("brandId") Long brandId,
            @Param("modelId") Long modelId,
            @Param("variantId") Long variantId,
            @Param("vehicleNo") String vehicleNo,
            @Param("searchText") String searchText,
            Pageable pageable);

    @Query(value = """
            SELECT p.id as id, p.orderDate as date, pd.uuid as code, pd.productNo as vehicleNo,
                   brand.description as brandName, model.description as modelName, variant.description as variantName,
                   pd.unitCost as purchaseRate
            FROM Purchase p
            JOIN p.purchaseDetails pd
            JOIN pd.product prod
            LEFT JOIN prod.brand brand
            LEFT JOIN prod.model model
            LEFT JOIN prod.varient variant
            WHERE EXISTS (SELECT 1 FROM Expense e WHERE e.purchase = p)
            """,
            countQuery = """
            SELECT COUNT(p) FROM Purchase p
            JOIN p.purchaseDetails pd
            JOIN pd.product prod
            WHERE EXISTS (SELECT 1 FROM Expense e WHERE e.purchase = p)
            """)
    Page<PurchaseListProjection> findAllWithExpenses(Pageable pageable);

    @Query(value = "SELECT " +
            "COALESCE(SUM(CASE WHEN p.order_date >= :startOfMonth THEN p.total_amount ELSE 0 END), 0) as totalThisMonth, " +
            "COALESCE(SUM(CASE WHEN p.order_date >= :startOfLastMonth AND p.order_date <= :endOfLastMonth THEN p.total_amount ELSE 0 END), 0) as totalLastMonth, " +
            "COUNT(CASE WHEN p.order_date = :today THEN 1 END) as todayCount, " +
            "COUNT(CASE WHEN p.order_date >= :startOfMonth THEN 1 END) as monthCount " +
            "FROM app_purchase_order p " +
            "WHERE p.order_date >= :startOfLastMonth " +
            "AND p.deleted = false", nativeQuery = true)
    PurchaseMetrics getPurchaseSummaryMetrics(@Param("startOfLastMonth") LocalDate startOfLastMonth, @Param("endOfLastMonth") LocalDate endOfLastMonth,
                                              @Param("startOfMonth") LocalDate startOfMonth, @Param("today") LocalDate today);

    @Query(value = """
            SELECT
                po.id            as purchaseId,
                po.reference_no  as referenceNo,
                pd_agg.vehicle_no as vehicleNo,
                po.order_date    as purchaseDate,
                CASE WHEN tradein.unit_cost IS NOT NULL THEN tradein.unit_cost
                     ELSE po.total_amount END as amount,
                CASE WHEN tradein.unit_cost IS NOT NULL
                     THEN tradein.unit_cost - LEAST(tradein.sale_rate, tradein.unit_cost) - COALESCE(pp_sum.paid, 0)
                     ELSE GREATEST(0,
                              po.total_amount
                              - COALESCE(pp_sum.paid, 0)
                              - COALESCE(exp_sum.expense, 0)
                              - COALESCE(pr_sum.returned_unwind, 0))
                END as pendingAmount,
                pp_sum.last_payment_date as lastPaymentDate,
                v.name           as vendorName,
                v.mobile         as vendorMobile
            FROM app_purchase_order po
            JOIN app_vendor v ON v.id = po.vendor_id
            LEFT JOIN (
                SELECT purchase_order_id,
                       STRING_AGG(DISTINCT product_no, ', ') as vehicle_no
                FROM app_purchase_order_detail
                GROUP BY purchase_order_id
            ) pd_agg ON pd_agg.purchase_order_id = po.id
            LEFT JOIN (
                SELECT purchase_order_id,
                       SUM(amount)       as paid,
                       MAX(payment_date) as last_payment_date
                FROM app_purchase_payment
                WHERE deleted = false
                GROUP BY purchase_order_id
            ) pp_sum ON pp_sum.purchase_order_id = po.id
            LEFT JOIN (
                SELECT purchase_order_id,
                       SUM(amount) as expense
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
            WHERE po.deleted = false
              AND CASE WHEN tradein.unit_cost IS NOT NULL
                       THEN tradein.unit_cost - LEAST(tradein.sale_rate, tradein.unit_cost) - COALESCE(pp_sum.paid, 0)
                       ELSE GREATEST(0,
                                po.total_amount
                                - COALESCE(pp_sum.paid, 0)
                                - COALESCE(exp_sum.expense, 0)
                                - COALESCE(pr_sum.returned_unwind, 0))
                  END > 0
            ORDER BY po.order_date DESC
            """, nativeQuery = true)
    List<PayableRow> findPayables();

}
