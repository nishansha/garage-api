package com.triasoft.garage.repository;

import com.triasoft.garage.entity.Purchase;
import com.triasoft.garage.projection.PurchaseMetrics;
import com.triasoft.garage.projection.SummaryMetrics;
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
