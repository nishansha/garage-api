package com.triasoft.garage.repository;

import com.triasoft.garage.entity.Purchase;
import com.triasoft.garage.projection.PayableRow;
import com.triasoft.garage.projection.PurchaseLineRow;
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
                   fuel.id as fuelTypeId, fuel.description as fuelType,
                   transmission.id as transmissionTypeId, transmission.description as transmissionType,
                   pd.unitCost as purchaseRate
            FROM Purchase p
            JOIN p.purchaseDetails pd
            JOIN pd.product prod
            LEFT JOIN prod.brand brand
            LEFT JOIN prod.model model
            LEFT JOIN prod.varient variant
            LEFT JOIN prod.fuelType fuel
            LEFT JOIN prod.transmissionType transmission
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
                   fuel.id as fuelTypeId, fuel.description as fuelType,
                   transmission.id as transmissionTypeId, transmission.description as transmissionType,
                   pd.unitCost as purchaseRate
            FROM Purchase p
            JOIN p.purchaseDetails pd
            JOIN pd.product prod
            LEFT JOIN prod.brand brand
            LEFT JOIN prod.model model
            LEFT JOIN prod.varient variant
            LEFT JOIN prod.fuelType fuel
            LEFT JOIN prod.transmissionType transmission
            JOIN p.vendor vendor
            WHERE (CAST(:fromDate AS date) IS NULL OR p.orderDate >= :fromDate)
              AND (CAST(:toDate AS date) IS NULL OR p.orderDate <= :toDate)
              AND (CAST(:brandId AS long) IS NULL OR brand.id = :brandId)
              AND (CAST(:modelId AS long) IS NULL OR model.id = :modelId)
              AND (CAST(:variantId AS long) IS NULL OR variant.id = :variantId)
              AND (CAST(:fuelTypeId AS long) IS NULL OR fuel.id = :fuelTypeId)
              AND (CAST(:vehicleNo AS string) IS NULL OR LOWER(pd.productNo) LIKE LOWER(CONCAT('%', CAST(:vehicleNo AS string), '%')))
              AND (CAST(:searchText AS string) IS NULL OR (
                   LOWER(p.referenceNo) LIKE LOWER(CONCAT('%', CAST(:searchText AS string), '%'))
                   OR LOWER(p.notes) LIKE LOWER(CONCAT('%', CAST(:searchText AS string), '%'))
                   OR LOWER(pd.productNo) LIKE LOWER(CONCAT('%', CAST(:searchText AS string), '%'))
                   OR LOWER(vendor.name) LIKE LOWER(CONCAT('%', CAST(:searchText AS string), '%'))))
            """,
            countQuery = """
            SELECT COUNT(p) FROM Purchase p
            JOIN p.purchaseDetails pd
            JOIN pd.product prod
            LEFT JOIN prod.brand brand
            LEFT JOIN prod.model model
            LEFT JOIN prod.varient variant
            LEFT JOIN prod.fuelType fuel
            LEFT JOIN prod.transmissionType transmission
            JOIN p.vendor vendor
            WHERE (CAST(:fromDate AS date) IS NULL OR p.orderDate >= :fromDate)
              AND (CAST(:toDate AS date) IS NULL OR p.orderDate <= :toDate)
              AND (CAST(:brandId AS long) IS NULL OR brand.id = :brandId)
              AND (CAST(:modelId AS long) IS NULL OR model.id = :modelId)
              AND (CAST(:variantId AS long) IS NULL OR variant.id = :variantId)
              AND (CAST(:fuelTypeId AS long) IS NULL OR fuel.id = :fuelTypeId)
              AND (CAST(:vehicleNo AS string) IS NULL OR LOWER(pd.productNo) LIKE LOWER(CONCAT('%', CAST(:vehicleNo AS string), '%')))
              AND (CAST(:searchText AS string) IS NULL OR (
                   LOWER(p.referenceNo) LIKE LOWER(CONCAT('%', CAST(:searchText AS string), '%'))
                   OR LOWER(p.notes) LIKE LOWER(CONCAT('%', CAST(:searchText AS string), '%'))
                   OR LOWER(pd.productNo) LIKE LOWER(CONCAT('%', CAST(:searchText AS string), '%'))
                   OR LOWER(vendor.name) LIKE LOWER(CONCAT('%', CAST(:searchText AS string), '%'))))
            """)
    Page<PurchaseListProjection> searchForList(
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("brandId") Long brandId,
            @Param("modelId") Long modelId,
            @Param("variantId") Long variantId,
            @Param("fuelTypeId") Long fuelTypeId,
            @Param("vehicleNo") String vehicleNo,
            @Param("searchText") String searchText,
            Pageable pageable);

    @Query(value = """
            SELECT p.id as id, p.orderDate as date, pd.uuid as code, pd.productNo as vehicleNo,
                   brand.description as brandName, model.description as modelName, variant.description as variantName,
                   fuel.id as fuelTypeId, fuel.description as fuelType,
                   transmission.id as transmissionTypeId, transmission.description as transmissionType,
                   pd.unitCost as purchaseRate
            FROM Purchase p
            JOIN p.purchaseDetails pd
            JOIN pd.product prod
            LEFT JOIN prod.brand brand
            LEFT JOIN prod.model model
            LEFT JOIN prod.varient variant
            LEFT JOIN prod.fuelType fuel
            LEFT JOIN prod.transmissionType transmission
            """,
            countQuery = """
            SELECT COUNT(p) FROM Purchase p
            JOIN p.purchaseDetails pd
            JOIN pd.product prod
            """)
    Page<PurchaseListProjection> findAllWithExpenses(Pageable pageable);

    @Query(value = """
            SELECT
              COALESCE((SELECT SUM(p.total_amount) FROM app_purchase_order p
                        WHERE p.order_date >= :startOfMonth AND p.deleted = false
                          AND NOT EXISTS (
                              SELECT 1 FROM app_purchase_order_detail pod
                              JOIN app_inventory inv ON inv.purchase_order_detail_id = pod.id
                              WHERE pod.purchase_order_id = p.id AND inv.source_sale_id IS NOT NULL
                          )), 0)
              + COALESCE((SELECT SUM(p.total_amount) FROM app_purchase_order p
                          WHERE p.buyback_recorded_at >= :startOfMonth
                            AND p.deleted = false), 0)
              -
              COALESCE((SELECT SUM(pr.inventory_landed_cost) FROM app_purchase_return pr
                        WHERE pr.return_date >= :startOfMonth AND pr.deleted = false), 0) as totalThisMonth,
              COALESCE((SELECT SUM(p.total_amount) FROM app_purchase_order p
                        WHERE p.order_date BETWEEN :startOfLastMonth AND :endOfLastMonth AND p.deleted = false
                          AND NOT EXISTS (
                              SELECT 1 FROM app_purchase_order_detail pod
                              JOIN app_inventory inv ON inv.purchase_order_detail_id = pod.id
                              WHERE pod.purchase_order_id = p.id AND inv.source_sale_id IS NOT NULL
                          )), 0)
              + COALESCE((SELECT SUM(p.total_amount) FROM app_purchase_order p
                          WHERE p.buyback_recorded_at BETWEEN :startOfLastMonth AND :endOfLastMonth
                            AND p.deleted = false), 0)
              -
              COALESCE((SELECT SUM(pr.inventory_landed_cost) FROM app_purchase_return pr
                        WHERE pr.return_date BETWEEN :startOfLastMonth AND :endOfLastMonth AND pr.deleted = false), 0) as totalLastMonth,
              COALESCE((SELECT COUNT(*) FROM app_purchase_order p
                        LEFT JOIN fnd_lookup_master ls ON ls.id = p.status_id
                        WHERE p.order_date = :today AND p.deleted = false
                          AND (ls.code IS NULL OR ls.code <> 'RETURNED')
                          AND NOT EXISTS (
                              SELECT 1 FROM app_purchase_order_detail pod
                              JOIN app_inventory inv ON inv.purchase_order_detail_id = pod.id
                              WHERE pod.purchase_order_id = p.id AND inv.source_sale_id IS NOT NULL
                          )), 0) as todayCount,
              COALESCE((SELECT COUNT(*) FROM app_purchase_order p
                        LEFT JOIN fnd_lookup_master ls ON ls.id = p.status_id
                        WHERE p.order_date >= :startOfMonth AND p.deleted = false
                          AND (ls.code IS NULL OR ls.code <> 'RETURNED')
                          AND NOT EXISTS (
                              SELECT 1 FROM app_purchase_order_detail pod
                              JOIN app_inventory inv ON inv.purchase_order_detail_id = pod.id
                              WHERE pod.purchase_order_id = p.id AND inv.source_sale_id IS NOT NULL
                          )), 0) as monthCount
            FROM (SELECT 1) data
            """, nativeQuery = true)
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
                JOIN app_purchase_order po_x ON po_x.id = pod.purchase_order_id
                WHERE inv.source_sale_id IS NOT NULL
                  AND po_x.buyback_recorded_at IS NULL
            ) tradein ON tradein.purchase_order_id = po.id
            LEFT JOIN (
                SELECT purchase_id, SUM(return_amount) as returned_unwind
                FROM app_purchase_return
                WHERE deleted = false
                GROUP BY purchase_id
            ) pr_sum ON pr_sum.purchase_id = po.id
            WHERE po.deleted = false
              -- Buyback-recorded exchange purchases are tracked via the SaleReturn
              -- Customer Refund Payable and shown in /sale-returns/payables, not here.
              AND po.buyback_recorded_at IS NULL
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

    @Query(value = """
            SELECT
                po.id           as purchaseId,
                po.reference_no as referenceNo,
                po.order_date   as purchaseDate,
                inv.product_no  as vehicleNo,
                ven.name        as vendorName,
                pod.unit_cost   as purchaseRate,
                (COALESCE(inv.landed_cost, 0) - COALESCE(pod.unit_cost, 0)) as purchaseExpenses,
                inv.landed_cost as landedCost,
                (pr.id IS NOT NULL) as returned,
                pr.return_amount as returnAmount,
                GREATEST(0, COALESCE(po.total_amount, 0)
                    - COALESCE((SELECT SUM(e.amount) FROM app_expense e
                                WHERE e.purchase_order_id = po.id AND e.deleted = false), 0)
                    - COALESCE((SELECT SUM(pp.amount) FROM app_purchase_payment pp
                                WHERE pp.purchase_order_id = po.id AND pp.deleted = false
                                  AND pp.payment_date <= :endDate), 0)
                    - COALESCE((SELECT SUM(prr.return_amount) FROM app_purchase_return prr
                                WHERE prr.purchase_id = po.id AND prr.deleted = false
                                  AND prr.return_date <= :endDate), 0)
                ) as pendingAmount,
                GREATEST(0, COALESCE(po.total_amount, 0)
                    - COALESCE((SELECT SUM(e.amount) FROM app_expense e
                                WHERE e.purchase_order_id = po.id AND e.deleted = false), 0)
                    - COALESCE((SELECT SUM(pp.amount) FROM app_purchase_payment pp
                                WHERE pp.purchase_order_id = po.id AND pp.deleted = false), 0)
                    - COALESCE((SELECT SUM(prr.return_amount) FROM app_purchase_return prr
                                WHERE prr.purchase_id = po.id AND prr.deleted = false), 0)
                ) as pendingTillDate
            FROM app_purchase_order po
            JOIN app_purchase_order_detail pod ON pod.purchase_order_id = po.id
            JOIN app_inventory inv ON inv.purchase_order_detail_id = pod.id
            JOIN app_vendor ven ON ven.id = po.vendor_id
            LEFT JOIN app_purchase_return pr ON pr.inventory_id = inv.id AND pr.deleted = false
                                            AND pr.return_date <= :endDate
            WHERE po.deleted = false
              AND inv.deleted = false
              AND inv.source_sale_id IS NULL
              AND po.buyback_recorded_at IS NULL
              AND po.order_date BETWEEN :startDate AND :endDate
            ORDER BY po.order_date, po.id
            """, nativeQuery = true)
    List<PurchaseLineRow> getPurchaseLinesByPeriod(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

}
