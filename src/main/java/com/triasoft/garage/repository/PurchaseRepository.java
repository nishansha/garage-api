package com.triasoft.garage.repository;

import com.triasoft.garage.entity.Purchase;
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

}
