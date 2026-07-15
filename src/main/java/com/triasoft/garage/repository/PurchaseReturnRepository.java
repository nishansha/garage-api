package com.triasoft.garage.repository;

import com.triasoft.garage.entity.PurchaseReturn;
import com.triasoft.garage.projection.PurchaseReturnReceivableRow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PurchaseReturnRepository extends JpaRepository<PurchaseReturn, Long> {

    Optional<PurchaseReturn> findByInventoryId(Long inventoryId);

    boolean existsByInventoryId(Long inventoryId);

    List<PurchaseReturn> findByPurchaseId(Long purchaseId);

    @Query("SELECT pr FROM PurchaseReturn pr WHERE " +
            "(CAST(:fromDate AS date) IS NULL OR pr.returnDate >= :fromDate) AND " +
            "(CAST(:toDate AS date) IS NULL OR pr.returnDate <= :toDate) " +
            "ORDER BY pr.returnDate DESC")
    Page<PurchaseReturn> findByDateRange(@Param("fromDate") LocalDate fromDate,
                                         @Param("toDate") LocalDate toDate,
                                         Pageable pageable);

    @Query(value = """
            SELECT
                pr.id              as purchaseReturnId,
                po.id              as purchaseId,
                po.reference_no    as purchaseReferenceNo,
                pd_agg.vehicle_no  as vehicleNo,
                pr.return_date     as returnDate,
                (pr.return_amount - COALESCE(outstanding.ap, 0))             as cashRefundExpected,
                ((pr.return_amount - COALESCE(outstanding.ap, 0))
                    - COALESCE(rec_sum.received, 0))                          as pendingAmount,
                rec_sum.last_receipt_date                                     as lastReceiptDate,
                v.name             as vendorName,
                v.mobile           as vendorMobile
            FROM app_purchase_return pr
            JOIN app_purchase_order po ON po.id = pr.purchase_id
            JOIN app_vendor v ON v.id = po.vendor_id
            LEFT JOIN (
                SELECT purchase_order_id, STRING_AGG(DISTINCT product_no, ', ') as vehicle_no
                FROM app_purchase_order_detail
                GROUP BY purchase_order_id
            ) pd_agg ON pd_agg.purchase_order_id = po.id
            LEFT JOIN (
                SELECT po2.id as purchase_order_id,
                       GREATEST(0,
                           po2.total_amount
                           - COALESCE((SELECT SUM(e.amount) FROM app_expense e
                                       WHERE e.purchase_order_id = po2.id AND e.deleted = false), 0)
                           - COALESCE((SELECT SUM(pp.amount) FROM app_purchase_payment pp
                                       WHERE pp.purchase_order_id = po2.id AND pp.deleted = false), 0)
                       ) as ap
                FROM app_purchase_order po2
            ) outstanding ON outstanding.purchase_order_id = po.id
            LEFT JOIN (
                SELECT purchase_return_id,
                       SUM(amount)       as received,
                       MAX(payment_date) as last_receipt_date
                FROM app_purchase_return_receipt
                WHERE deleted = false
                GROUP BY purchase_return_id
            ) rec_sum ON rec_sum.purchase_return_id = pr.id
            WHERE pr.deleted = false
              AND ((pr.return_amount - COALESCE(outstanding.ap, 0))
                    - COALESCE(rec_sum.received, 0)) > 0
            ORDER BY pr.return_date DESC
            """, nativeQuery = true)
    List<PurchaseReturnReceivableRow> findReceivables();
}
