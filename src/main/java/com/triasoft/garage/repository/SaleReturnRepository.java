package com.triasoft.garage.repository;

import com.triasoft.garage.entity.SaleReturn;
import com.triasoft.garage.projection.SaleReturnPayableRow;
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
public interface SaleReturnRepository extends JpaRepository<SaleReturn, Long> {

    Optional<SaleReturn> findBySaleId(Long saleId);

    boolean existsBySaleId(Long saleId);

    @Query("SELECT sr FROM SaleReturn sr WHERE " +
            "(:fromDate IS NULL OR sr.returnDate >= :fromDate) AND " +
            "(:toDate IS NULL OR sr.returnDate <= :toDate) " +
            "ORDER BY sr.returnDate DESC")
    Page<SaleReturn> findByDateRange(@Param("fromDate") LocalDate fromDate,
                                     @Param("toDate") LocalDate toDate,
                                     Pageable pageable);

    @Query(value = """
            SELECT
                sr.id              as saleReturnId,
                s.id               as saleId,
                s.invoice_no       as invoiceNo,
                inv.product_no     as vehicleNo,
                sr.return_date     as returnDate,
                sr.refund_amount   as refundAmount,
                (sr.refund_amount - COALESCE(ref_sum.refunded, 0)) as pendingAmount,
                ref_sum.last_refund_date as lastRefundDate,
                c.name             as customerName,
                c.mobile           as customerMobile
            FROM app_sale_return sr
            JOIN app_sale s ON s.id = sr.sale_id
            JOIN app_inventory inv ON inv.id = s.inventory_id
            JOIN app_customer c ON c.id = s.customer_id
            LEFT JOIN (
                SELECT sale_return_id,
                       SUM(amount)       as refunded,
                       MAX(payment_date) as last_refund_date
                FROM app_sale_refund_payment
                WHERE deleted = false
                GROUP BY sale_return_id
            ) ref_sum ON ref_sum.sale_return_id = sr.id
            WHERE sr.deleted = false
              AND (sr.refund_amount - COALESCE(ref_sum.refunded, 0)) > 0
            ORDER BY sr.return_date DESC
            """, nativeQuery = true)
    List<SaleReturnPayableRow> findPayables();
}
