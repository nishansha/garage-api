package com.triasoft.garage.servicesale.repository;

import com.triasoft.garage.projection.LastPaymentDateProjection;
import com.triasoft.garage.servicesale.entity.ServiceSalePayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

public interface ServiceSalePaymentRepository extends JpaRepository<ServiceSalePayment, Long> {
    List<ServiceSalePayment> findByServiceSaleId(Long serviceSaleId);

    @Query("SELECT p.serviceSale.id as sourceId, MAX(p.paymentDate) as lastPaymentDate FROM ServiceSalePayment p WHERE p.serviceSale.id IN :serviceSaleIds GROUP BY p.serviceSale.id")
    List<LastPaymentDateProjection> findLastPaymentDatesByServiceSaleIds(@Param("serviceSaleIds") List<Long> serviceSaleIds);

    default BigDecimal sumAmountByServiceSaleId(Long serviceSaleId) {
        return findByServiceSaleId(serviceSaleId).stream()
                .map(ServiceSalePayment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
