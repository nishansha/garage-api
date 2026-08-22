package com.triasoft.garage.servicesale.entity;

import com.triasoft.garage.constants.PaymentMethodEnum;
import com.triasoft.garage.entity.PaymentAccount;
import com.triasoft.garage.entity.TenantAwareAuditEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.SoftDelete;

import java.io.Serial;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@DynamicUpdate
@Table(name = "app_service_sale_payment")
@SoftDelete(columnName = "deleted")
public class ServiceSalePayment extends TenantAwareAuditEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "service_sale_id", nullable = false)
    private ServiceSale serviceSale;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private PaymentMethodEnum paymentMethod;

    @Column(name = "reference_no")
    private String referenceNo;

    @Column(name = "notes")
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_account_id", nullable = false)
    private PaymentAccount paymentAccount;
}
