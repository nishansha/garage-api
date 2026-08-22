package com.triasoft.garage.hrm.entity;

import com.triasoft.garage.constants.StatusEnum;
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

import java.io.Serial;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * One row per employee per pay period. Generated PENDING by SalaryRunScheduler (or created
 * manually); journal posting only happens when marked PAID — same invoice-vs-settlement split
 * as Sale/SalePayment, so payroll staff can review/adjust before it hits the books.
 */
@Getter
@Setter
@Entity
@DynamicUpdate
@Table(name = "app_salary_payment")
public class SalaryPayment extends TenantAwareAuditEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "pay_period_month", nullable = false)
    private int payPeriodMonth;

    @Column(name = "pay_period_year", nullable = false)
    private int payPeriodYear;

    @Column(name = "gross_amount", nullable = false)
    private BigDecimal grossAmount;

    @Column(name = "net_amount", nullable = false)
    private BigDecimal netAmount;

    @Column(name = "payment_date")
    private LocalDate paymentDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_account_id")
    private PaymentAccount paymentAccount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private StatusEnum status;

    @Column(name = "notes")
    private String notes;
}
