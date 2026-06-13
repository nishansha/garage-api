package com.triasoft.garage.entity;

import com.triasoft.garage.constants.TransactionDirectionEnum;
import com.triasoft.garage.constants.TransactionTypeEnum;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "app_transaction")
public class Transaction extends AuditGenericEntity {

    @Serial
    private static final long serialVersionUID = 8837261049182736450L;

    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private TransactionTypeEnum type;

    @Column(name = "reference_type", nullable = false)
    private String referenceType;

    @Column(name = "reference_id", nullable = false)
    private Long referenceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_account_id")
    private PaymentAccount paymentAccount;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "direction", nullable = false)
    private TransactionDirectionEnum direction;

    @Column(name = "description")
    private String description;

    @Column(name = "notes")
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reversal_of_id")
    private Transaction reversalOf;

    @Column(name = "reconciled", nullable = false)
    private boolean reconciled = false;

    @Column(name = "reconciled_at")
    private LocalDate reconciledAt;

}
