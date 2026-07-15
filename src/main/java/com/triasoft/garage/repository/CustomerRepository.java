package com.triasoft.garage.repository;

import com.triasoft.garage.entity.Customer;
import com.triasoft.garage.projection.CustomerBalanceRow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByMobile(String ownerMobileNo);

    /**
     * Returns every customer together with their net outstanding balance
     * (positive = the customer owes us). This is the sum of the still-pending
     * amount across their unpaid sales (mirroring {@link SaleRepository#findReceivables()})
     * minus any pending sale-return refunds we still owe them (mirroring
     * {@link SaleReturnRepository#findPayables()}).
     */
    @Query(value = """
            SELECT c.id                       as id,
                   c.name                     as name,
                   c.mobile                   as mobile,
                   c.address                  as address,
                   COALESCE(rec.outstanding, 0) - COALESCE(refund.payable, 0) as outstandingBalance
            FROM app_customer c
            LEFT JOIN (
                SELECT s.customer_id as customer_id,
                       SUM(s.net_sale_amount - COALESCE(sp_sum.paid, 0)) as outstanding
                FROM app_sale s
                LEFT JOIN (
                    SELECT sale_id, SUM(amount) as paid
                    FROM app_sale_payment
                    WHERE deleted = false
                    GROUP BY sale_id
                ) sp_sum ON sp_sum.sale_id = s.id
                WHERE s.deleted = false
                  AND s.payment_status IN ('PENDING', 'PARTIAL', 'FINANCE_PENDING')
                GROUP BY s.customer_id
            ) rec ON rec.customer_id = c.id
            LEFT JOIN (
                SELECT s.customer_id as customer_id,
                       SUM(sr.refund_amount - COALESCE(ref_sum.refunded, 0)) as payable
                FROM app_sale_return sr
                JOIN app_sale s ON s.id = sr.sale_id
                LEFT JOIN (
                    SELECT sale_return_id, SUM(amount) as refunded
                    FROM app_sale_refund_payment
                    WHERE deleted = false
                    GROUP BY sale_return_id
                ) ref_sum ON ref_sum.sale_return_id = sr.id
                WHERE sr.deleted = false
                  AND (sr.refund_amount - COALESCE(ref_sum.refunded, 0)) > 0
                GROUP BY s.customer_id
            ) refund ON refund.customer_id = c.id
            """,
            countQuery = "SELECT count(*) FROM app_customer",
            nativeQuery = true)
    Page<CustomerBalanceRow> findCustomersWithOutstandingBalance(Pageable pageable);
}
