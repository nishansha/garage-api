package com.triasoft.garage.servicesale.repository;

import com.triasoft.garage.projection.WarehouseServiceSaleMetrics;
import com.triasoft.garage.servicesale.entity.ServiceSale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ServiceSaleRepository extends JpaRepository<ServiceSale, Long>, JpaSpecificationExecutor<ServiceSale> {

    @Query(value = "SELECT nextval('sso_ref_no_seq')", nativeQuery = true)
    Long getNextReferenceNumber();

    // warehouse_id is NOT NULL on app_service_sale (unlike app_sale, reached indirectly via
    // inventory), so no "unassigned" bucket is needed here.
    @Query(value = "SELECT ss.warehouse_id as warehouseId, " +
            "COALESCE(COUNT(ss.id), 0) as serviceSaleCount, " +
            "COALESCE(SUM(ss.total_amount), 0) as serviceRevenue " +
            "FROM app_service_sale ss " +
            "WHERE ss.tenant_id = :tenantId AND ss.deleted = false " +
            "  AND ss.sale_date BETWEEN :startDate AND :endDate " +
            "GROUP BY ss.warehouse_id",
            nativeQuery = true)
    List<WarehouseServiceSaleMetrics> getServiceSalePerformanceByWarehouse(@Param("tenantId") Long tenantId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}
