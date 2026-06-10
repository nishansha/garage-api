package com.triasoft.garage.repository;

import com.triasoft.garage.entity.PurchasePayment;
import com.triasoft.garage.projection.PurchasePaidProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface PurchasePaymentRepository extends JpaRepository<PurchasePayment, Long> {

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM PurchasePayment p WHERE p.purchase.id = :purchaseId")
    BigDecimal sumAmountByPurchaseId(@Param("purchaseId") Long purchaseId);

    @Query("SELECT p.purchase.id AS purchaseId, COALESCE(SUM(p.amount), 0) AS totalPaid FROM PurchasePayment p WHERE p.purchase.id IN :purchaseIds GROUP BY p.purchase.id")
    List<PurchasePaidProjection> getTotalPaidByPurchaseIds(@Param("purchaseIds") List<Long> purchaseIds);

    List<PurchasePayment> findByPurchaseIdOrderByPaymentDateDesc(Long purchaseId);

}
