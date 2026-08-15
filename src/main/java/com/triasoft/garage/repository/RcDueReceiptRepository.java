package com.triasoft.garage.repository;

import com.triasoft.garage.entity.RcDueReceipt;
import com.triasoft.garage.projection.LastPaymentDateProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface RcDueReceiptRepository extends JpaRepository<RcDueReceipt, Long> {

    @Query("SELECT COALESCE(SUM(r.amount), 0) FROM RcDueReceipt r WHERE r.purchase.id = :purchaseId")
    BigDecimal sumAmountByPurchaseId(@Param("purchaseId") Long purchaseId);

    List<RcDueReceipt> findByPurchaseIdOrderByReceiptDateDesc(Long purchaseId);

    @Query("SELECT r.purchase.id as sourceId, MAX(r.receiptDate) as lastPaymentDate FROM RcDueReceipt r WHERE r.purchase.id IN :purchaseIds GROUP BY r.purchase.id")
    List<LastPaymentDateProjection> findLastReceiptDatesByPurchaseIds(@Param("purchaseIds") List<Long> purchaseIds);
}
