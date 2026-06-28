package com.triasoft.garage.repository;

import com.triasoft.garage.entity.PurchaseReturnReceipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface PurchaseReturnReceiptRepository extends JpaRepository<PurchaseReturnReceipt, Long> {

    @Query("SELECT COALESCE(SUM(r.amount), 0) FROM PurchaseReturnReceipt r WHERE r.purchaseReturn.id = :purchaseReturnId")
    BigDecimal sumAmountByPurchaseReturnId(@Param("purchaseReturnId") Long purchaseReturnId);

    List<PurchaseReturnReceipt> findByPurchaseReturnIdOrderByPaymentDateDesc(Long purchaseReturnId);
}
