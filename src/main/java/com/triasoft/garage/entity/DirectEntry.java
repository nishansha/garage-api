package com.triasoft.garage.entity;

import com.triasoft.garage.constants.TransactionDirectionEnum;
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
@Table(name = "app_direct_entry")
@SoftDelete(columnName = "deleted")
@Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
@AuditOverride(forClass = AuditGenericEntity.class)
public class DirectEntry extends AuditGenericEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "coa_id", nullable = false)
    private ChartOfAccount chartOfAccount;

    @Enumerated(EnumType.STRING)
    @Column(name = "direction", nullable = false)
    private TransactionDirectionEnum direction;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_account_id", nullable = false)
    private PaymentAccount paymentAccount;

    @Column(name = "party_name")
    private String partyName;

    @Column(name = "reference_no")
    private String referenceNo;

    @Column(name = "description")
    private String description;

    @Column(name = "notes")
    private String notes;

}
