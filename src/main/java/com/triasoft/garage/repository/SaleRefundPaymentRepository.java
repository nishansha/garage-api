package com.triasoft.garage.repository;

import com.triasoft.garage.entity.SaleRefundPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface SaleRefundPaymentRepository extends JpaRepository<SaleRefundPayment, Long> {

    @Query("SELECT COALESCE(SUM(r.amount), 0) FROM SaleRefundPayment r WHERE r.saleReturn.id = :saleReturnId")
    BigDecimal sumAmountBySaleReturnId(@Param("saleReturnId") Long saleReturnId);

    List<SaleRefundPayment> findBySaleReturnIdOrderByPaymentDateDesc(Long saleReturnId);
}
