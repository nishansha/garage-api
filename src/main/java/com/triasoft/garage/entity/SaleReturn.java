package com.triasoft.garage.entity;

import com.triasoft.garage.constants.ExchangeHandlingEnum;
import com.triasoft.garage.constants.ReturnStatusEnum;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.SoftDelete;

import java.io.Serial;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@DynamicUpdate
@Table(name = "app_sale_return")
@SoftDelete(columnName = "deleted")
public class SaleReturn extends AuditGenericEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "sale_id", nullable = false, unique = true)
    private Sale sale;

    @Column(name = "return_date", nullable = false)
    private LocalDate returnDate;

    @Column(name = "reason")
    private String reason;

    @Column(name = "notes")
    private String notes;

    @Column(name = "customer_paid_amount", nullable = false)
    private BigDecimal customerPaidAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "exchange_handling", nullable = false)
    private ExchangeHandlingEnum exchangeHandling;

    @Column(name = "exchange_buyback_amount")
    private BigDecimal exchangeBuybackAmount;

    @Column(name = "sold_vehicle_deduction_amount", nullable = false)
    private BigDecimal soldVehicleDeductionAmount = BigDecimal.ZERO;

    @Column(name = "exchange_vehicle_deduction_amount", nullable = false)
    private BigDecimal exchangeVehicleDeductionAmount = BigDecimal.ZERO;

    @Column(name = "refund_amount", nullable = false)
    private BigDecimal refundAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ReturnStatusEnum status = ReturnStatusEnum.PENDING;

    @OneToMany(mappedBy = "saleReturn", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SaleReturnDeduction> deductions = new ArrayList<>();

    @OneToMany(mappedBy = "saleReturn", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SaleRefundPayment> refunds = new ArrayList<>();
}
