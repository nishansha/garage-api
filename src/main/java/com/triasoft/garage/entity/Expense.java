package com.triasoft.garage.entity;

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
@Table(name = "app_expense")
@SoftDelete(columnName = "deleted")
public class Expense extends AuditGenericEntity {

    @Serial
    private static final long serialVersionUID = 5393592326235319975L;

    @ManyToOne(fetch = FetchType.LAZY)
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

}
