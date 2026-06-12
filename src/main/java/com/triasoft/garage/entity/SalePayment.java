package com.triasoft.garage.entity;

import com.triasoft.garage.constants.PayerTypeEnum;
import com.triasoft.garage.constants.PaymentMethodEnum;
import jakarta.persistence.*;
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
@Table(name = "app_sale_payment")
@SoftDelete(columnName = "deleted")
public class SalePayment extends AuditGenericEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "sale_id", nullable = false)
    private Sale sale;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private PaymentMethodEnum paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "payer_type", nullable = false)
    private PayerTypeEnum payerType;

    @Column(name = "reference_no")
    private String referenceNo;

    @Column(name = "notes")
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_account_id")
    private PaymentAccount paymentAccount;

}
