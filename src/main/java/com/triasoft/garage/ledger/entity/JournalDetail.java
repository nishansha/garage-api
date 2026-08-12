package com.triasoft.garage.ledger.entity;

import com.triasoft.garage.entity.TenantAwareAuditEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;

import java.io.Serial;
import java.math.BigDecimal;

@Getter
@Setter
@Entity
@DynamicUpdate
@Table(name = "app_journal_detail")
public class JournalDetail extends TenantAwareAuditEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_id", nullable = false)
    private Journal journal;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "account_id", nullable = false)
    private ChartOfAccount account;

    @Column(name = "debit_amount", nullable = false)
    private BigDecimal debitAmount = BigDecimal.ZERO;

    @Column(name = "credit_amount", nullable = false)
    private BigDecimal creditAmount = BigDecimal.ZERO;

    @Column(name = "description")
    private String description;

    // Subledger dimension: which customer/vendor (etc.) this line belongs to, for
    // AR/AP/receivable/payable lines. No FK to Customer/Vendor deliberately - this
    // entity is meant to stay generic/reusable, resolving party identity is the
    // domain layer's job (see JournalService). Null for lines with no party (COGS,
    // Inventory, Expense, etc).
    @Column(name = "party_type")
    private String partyType;

    @Column(name = "party_id")
    private Long partyId;

}
