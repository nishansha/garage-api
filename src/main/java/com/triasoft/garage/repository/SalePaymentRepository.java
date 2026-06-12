package com.triasoft.garage.repository;

import com.triasoft.garage.entity.SalePayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface SalePaymentRepository extends JpaRepository<SalePayment, Long> {

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM SalePayment p WHERE p.sale.id = :saleId")
    BigDecimal sumAmountBySaleId(@Param("saleId") Long saleId);

    List<SalePayment> findBySaleIdOrderByPaymentDateDesc(Long saleId);

}
