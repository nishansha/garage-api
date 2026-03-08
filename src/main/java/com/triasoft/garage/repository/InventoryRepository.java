package com.triasoft.garage.repository;

import com.triasoft.garage.constants.StatusEnum;
import com.triasoft.garage.entity.Inventory;
import com.triasoft.garage.projection.StockMetrics;
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
            "WHERE i.deleted = false", nativeQuery = true)
    StockMetrics getStockSummaryMetrics(@Param("startOfMonth") LocalDateTime startOfMonth);

    Optional<Inventory> findByPurchaseOrderDetailPurchaseId(Long purchaseId);

    Optional<Inventory> findBySourceSaleId(Long id);

    List<Inventory> findAllByStatus(StatusEnum status);
}
