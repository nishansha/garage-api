package com.triasoft.garage.repository;

import com.triasoft.garage.constants.StatusEnum;
import com.triasoft.garage.entity.Inventory;
import com.triasoft.garage.projection.PurchaseInventoryStatusProjection;
import com.triasoft.garage.projection.StockMetrics;
import com.triasoft.garage.projection.WarehouseStockMetrics;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;



@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long>, JpaSpecificationExecutor<Inventory> {

    @Query(value = "SELECT " +
            "SUM(CASE WHEN i.status = 'AVAILABLE' THEN i.landed_cost ELSE 0 END) as totalStockValue, " +
            "SUM(CASE WHEN i.status = 'AVAILABLE' AND i.received_date < :startOfMonth THEN i.landed_cost ELSE 0 END) as totalStockValueLastMonth, " +
            "COUNT(CASE WHEN i.status = 'AVAILABLE' THEN 1 END) as totalItems, " +
            "COUNT(CASE WHEN i.status = 'AVAILABLE' AND i.received_date >= :startOfMonth THEN 1 END) as itemsAddedThisMonth " +
            "FROM app_inventory i " +
            "WHERE i.deleted = false AND i.tenant_id = :tenantId", nativeQuery = true)
    StockMetrics getStockSummaryMetrics(@Param("tenantId") Long tenantId, @Param("startOfMonth") LocalDateTime startOfMonth);

    // Same AVAILABLE-only aggregation as getStockSummaryMetrics above, grouped per warehouse.
    // The UNION ALL branch buckets inventory with no warehouse assigned (warehouseId is
    // nullable) as "Unassigned" so sum(per-warehouse) + Unassigned reconciles exactly with
    // the tenant-wide totalStockValue/totalItems above.
    @Query(value = "SELECT " +
            "w.id as warehouseId, w.code as warehouseCode, w.name as warehouseName, " +
            "COALESCE(SUM(CASE WHEN i.status = 'AVAILABLE' THEN i.landed_cost ELSE 0 END), 0) as totalStockValue, " +
            "COALESCE(SUM(CASE WHEN i.status = 'AVAILABLE' AND i.received_date < :startOfMonth THEN i.landed_cost ELSE 0 END), 0) as totalStockValueLastMonth, " +
            "COALESCE(COUNT(CASE WHEN i.status = 'AVAILABLE' THEN 1 END), 0) as totalItems, " +
            "COALESCE(COUNT(CASE WHEN i.status = 'AVAILABLE' AND i.received_date >= :startOfMonth THEN 1 END), 0) as itemsAddedThisMonth " +
            "FROM inf_warehouse w " +
            "LEFT JOIN app_inventory i ON i.warehouse_id = w.id AND i.deleted = false AND i.tenant_id = :tenantId " +
            "WHERE w.tenant_id = :tenantId " +
            "GROUP BY w.id, w.code, w.name " +
            "UNION ALL " +
            "SELECT " +
            "NULL as warehouseId, 'UNASSIGNED' as warehouseCode, 'Unassigned' as warehouseName, " +
            "COALESCE(SUM(CASE WHEN i.status = 'AVAILABLE' THEN i.landed_cost ELSE 0 END), 0) as totalStockValue, " +
            "COALESCE(SUM(CASE WHEN i.status = 'AVAILABLE' AND i.received_date < :startOfMonth THEN i.landed_cost ELSE 0 END), 0) as totalStockValueLastMonth, " +
            "COALESCE(COUNT(CASE WHEN i.status = 'AVAILABLE' THEN 1 END), 0) as totalItems, " +
            "COALESCE(COUNT(CASE WHEN i.status = 'AVAILABLE' AND i.received_date >= :startOfMonth THEN 1 END), 0) as itemsAddedThisMonth " +
            "FROM app_inventory i " +
            "WHERE i.deleted = false AND i.tenant_id = :tenantId AND i.warehouse_id IS NULL",
            nativeQuery = true)
    List<WarehouseStockMetrics> getStockSummaryByWarehouse(@Param("tenantId") Long tenantId, @Param("startOfMonth") LocalDateTime startOfMonth);

    Optional<Inventory> findByPurchaseOrderDetailPurchaseId(Long purchaseId);

    List<Inventory> findByPurchaseOrderDetailPurchaseIdIn(List<Long> purchaseIds);

    Optional<Inventory> findBySourceSaleId(Long id);

    List<Inventory> findAllByStatus(StatusEnum status);

    boolean existsByPurchaseOrderDetailPurchaseIdAndStatusNot(Long purchaseId, StatusEnum status);

    boolean existsByWarehouseId(Long warehouseId);

    boolean existsByProductNoIgnoreCaseAndStatusIn(String productNo, List<StatusEnum> statuses);

    boolean existsByProductNoIgnoreCaseAndStatusInAndPurchaseOrderDetailPurchaseIdNot(String productNo, List<StatusEnum> statuses, Long purchaseId);

    Page<Inventory> findByStatusIn(List<StatusEnum> statuses, Pageable pageable);

    @Query("SELECT i.purchaseOrderDetail.purchase.id as purchaseId, i.status as status, i.sourceSaleId as sourceSaleId FROM Inventory i WHERE i.purchaseOrderDetail.purchase.id IN :ids")
    List<PurchaseInventoryStatusProjection> findStatusByPurchaseIdIn(@Param("ids") List<Long> ids);
}
