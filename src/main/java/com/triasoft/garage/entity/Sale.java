package com.triasoft.garage.entity;

import com.triasoft.garage.constants.StatusEnum;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SoftDelete;

import java.io.Serial;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "app_sale")
@SoftDelete(columnName = "deleted")
public class Sale extends AuditGenericEntity {

    @Serial
    private static final long serialVersionUID = -4448482111894285227L;

    @Column(name = "invoice_no", unique = true, nullable = false)
    private String invoiceNo;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "inventory_id", nullable = false)
    private Inventory inventory;

    @Column(name = "sale_date", nullable = false)
    private LocalDate saleDate;

    @Column(name = "sale_rate", nullable = false)
    private BigDecimal saleRate;

    @Column(name = "landed_cost_at_sale")
    private BigDecimal landedCostAtSale;

    @Column(name = "is_exchanged")
    private boolean isExchanged;

    @Column(name = "exchange_amount")
    private BigDecimal exchangeAmount;

    @Column(name = "is_financed")
    private boolean isFinanced;

    @Column(name = "finance_company")
    private String financeCompany;

    @Column(name = "finance_amount")
    private BigDecimal financeAmount;

    @Column(name = "emi_amount")
    private BigDecimal emiAmount;

    @Column(name = "net_sale_amount")
    private BigDecimal netSaleAmount;

    @Column(name = "profit_amount")
    private BigDecimal profitAmount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "status_id", nullable = false)
    private LookupMaster status;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false)
    private StatusEnum paymentStatus;
}
