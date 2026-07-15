package com.triasoft.garage.repository;

import com.triasoft.garage.entity.Vendor;
import com.triasoft.garage.projection.VendorBalanceRow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VendorRepository extends JpaRepository<Vendor, Long> {
    Optional<Vendor> findByMobile(String ownerMobileNo);

    /**
     * Returns every vendor together with their net outstanding balance
     * (positive = we owe the vendor). This is the sum of the still-pending
     * amount across their (non buyback-recorded) purchase orders (mirroring
     * {@link PurchaseRepository#findPayables()}) minus any pending purchase-return
     * refunds the vendor still owes us (mirroring
     * {@link PurchaseReturnRepository#findReceivables()}).
     */
    @Query(value = """
            SELECT v.id                       as id,
                   v.name                     as name,
                   v.mobile                   as mobile,
                   v.address                  as address,
                   COALESCE(bal.outstanding, 0) - COALESCE(prret.receivable, 0) as outstandingBalance
            FROM app_vendor v
            LEFT JOIN (
                SELECT po.vendor_id as vendor_id,
                       SUM(
                           CASE WHEN tradein.unit_cost IS NOT NULL
                                THEN tradein.unit_cost - LEAST(tradein.sale_rate, tradein.unit_cost) - COALESCE(pp_sum.paid, 0)
                                ELSE GREATEST(0,
                                         po.total_amount
                                         - COALESCE(pp_sum.paid, 0)
                                         - COALESCE(exp_sum.expense, 0)
                                         - COALESCE(pr_sum.returned_unwind, 0))
                           END
                       ) as outstanding
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
                    SELECT pod.purchase_order_id  as purchase_order_id,
                           MAX(pod.unit_cost)     as unit_cost,
                           MAX(s.sale_rate)       as sale_rate
                    FROM app_purchase_order_detail pod
                    JOIN app_inventory inv ON inv.purchase_order_detail_id = pod.id
                    JOIN app_sale s ON s.id = inv.source_sale_id
                    WHERE inv.source_sale_id IS NOT NULL
                    GROUP BY pod.purchase_order_id
                ) tradein ON tradein.purchase_order_id = po.id
                LEFT JOIN (
                    SELECT purchase_id, SUM(return_amount) as returned_unwind
                    FROM app_purchase_return
                    WHERE deleted = false
                    GROUP BY purchase_id
                ) pr_sum ON pr_sum.purchase_id = po.id
                WHERE po.deleted = false
                  AND po.buyback_recorded_at IS NULL
                GROUP BY po.vendor_id
            ) bal ON bal.vendor_id = v.id
            LEFT JOIN (
                SELECT po.vendor_id as vendor_id,
                       SUM((pr.return_amount - COALESCE(outstanding.ap, 0)) - COALESCE(rec_sum.received, 0)) as receivable
                FROM app_purchase_return pr
                JOIN app_purchase_order po ON po.id = pr.purchase_id
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
                    SELECT purchase_return_id, SUM(amount) as received
                    FROM app_purchase_return_receipt
                    WHERE deleted = false
                    GROUP BY purchase_return_id
                ) rec_sum ON rec_sum.purchase_return_id = pr.id
                WHERE pr.deleted = false
                  AND ((pr.return_amount - COALESCE(outstanding.ap, 0)) - COALESCE(rec_sum.received, 0)) > 0
                GROUP BY po.vendor_id
            ) prret ON prret.vendor_id = v.id
            """,
            countQuery = "SELECT count(*) FROM app_vendor",
            nativeQuery = true)
    Page<VendorBalanceRow> findVendorsWithOutstandingBalance(Pageable pageable);
}
