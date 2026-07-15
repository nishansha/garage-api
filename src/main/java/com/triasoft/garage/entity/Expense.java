package com.triasoft.garage.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;
import org.hibernate.envers.AuditOverride;
import org.hibernate.envers.RelationTargetAuditMode;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.SoftDelete;

import java.io.Serial;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@DynamicUpdate
@Table(name = "app_expense")
@SoftDelete(columnName = "deleted")
@Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
@AuditOverride(forClass = AuditGenericEntity.class)
public class Expense extends AuditGenericEntity {

    @Serial
    private static final long serialVersionUID = 5393592326235319975L;

    @ManyToOne
    @JoinColumn(name = "purchase_order_id")
    private Purchase purchase;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "expense_account_id", nullable = false)
    private ChartOfAccount expenseAccount;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Column(name = "other_expense")
    private String otherExpense;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "description")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_account_id")
    private PaymentAccount paymentAccount;

}
