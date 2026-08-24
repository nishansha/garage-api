package com.triasoft.garage.repository;

import com.triasoft.garage.entity.SaleReturn;
import com.triasoft.garage.projection.CompanyAmountRow;
import com.triasoft.garage.projection.SaleReturnPayableRow;
import com.triasoft.garage.projection.WarehouseAmountRow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SaleReturnRepository extends JpaRepository<SaleReturn, Long> {

    Optional<SaleReturn> findBySaleId(Long saleId);

    boolean existsBySaleId(Long saleId);

    @Query("SELECT sr FROM SaleReturn sr WHERE " +
            "(CAST(:fromDate AS date) IS NULL OR sr.returnDate >= :fromDate) AND " +
            "(CAST(:toDate AS date) IS NULL OR sr.returnDate <= :toDate) " +
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
              AND sr.tenant_id = :tenantId
              AND (sr.refund_amount - COALESCE(ref_sum.refunded, 0)) > 0
            ORDER BY sr.return_date DESC
            """, nativeQuery = true)
    List<SaleReturnPayableRow> findPayables(@Param("tenantId") Long tenantId);

    // companyId/warehouseId nullable - null means "overall", see SaleRepository.getProfitReport's
    // comment. Joins app_sale for company_id and app_sale->app_inventory for warehouse_id since
    // app_sale_return has neither column of its own.
    @Query(value = """
            SELECT COALESCE(SUM(
                     COALESCE(sr.sold_vehicle_deduction_amount, 0)
                   + COALESCE(sr.exchange_vehicle_deduction_amount, 0)), 0)
            FROM app_sale_return sr
            JOIN app_sale s ON s.id = sr.sale_id
            JOIN app_inventory inv ON inv.id = s.inventory_id
            WHERE sr.deleted = false
              AND sr.tenant_id = :tenantId
              AND sr.return_date BETWEEN :startDate AND :endDate
              AND (:companyId IS NULL OR s.company_id = :companyId)
              AND (:warehouseId IS NULL OR inv.warehouse_id = :warehouseId)
            """, nativeQuery = true)
    BigDecimal sumDeductionIncomeByPeriod(@Param("tenantId") Long tenantId, @Param("companyId") Long companyId, @Param("warehouseId") Long warehouseId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    // Company/warehouse-scoped variants of sumDeductionIncomeByPeriod, for the comparison
    // reports (CompanyReportService/WarehouseReportService) — these retained deductions are
    // real profit earned on the return, same as ReportService.getProfitAndLoss already adds
    // back tenant-wide, but the comparison reports were missing it entirely until now.
    @Query(value = """
            SELECT s.company_id as companyId,
                   COALESCE(SUM(
                       COALESCE(sr.sold_vehicle_deduction_amount, 0)
                     + COALESCE(sr.exchange_vehicle_deduction_amount, 0)), 0) as amount
            FROM app_sale_return sr
            JOIN app_sale s ON s.id = sr.sale_id
            WHERE sr.deleted = false
              AND sr.tenant_id = :tenantId
              AND sr.return_date BETWEEN :startDate AND :endDate
            GROUP BY s.company_id
            """, nativeQuery = true)
    List<CompanyAmountRow> sumDeductionIncomeByCompany(@Param("tenantId") Long tenantId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query(value = """
            SELECT i.warehouse_id as warehouseId,
                   COALESCE(SUM(
                       COALESCE(sr.sold_vehicle_deduction_amount, 0)
                     + COALESCE(sr.exchange_vehicle_deduction_amount, 0)), 0) as amount
            FROM app_sale_return sr
            JOIN app_sale s ON s.id = sr.sale_id
            JOIN app_inventory i ON i.id = s.inventory_id
            WHERE sr.deleted = false
              AND sr.tenant_id = :tenantId
              AND sr.return_date BETWEEN :startDate AND :endDate
            GROUP BY i.warehouse_id
            """, nativeQuery = true)
    List<WarehouseAmountRow> sumDeductionIncomeByWarehouse(@Param("tenantId") Long tenantId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}
