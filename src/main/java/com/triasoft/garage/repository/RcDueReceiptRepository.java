package com.triasoft.garage.repository;

import com.triasoft.garage.entity.RcDueReceipt;
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
}
