package com.triasoft.garage.entity;

import com.triasoft.garage.constants.JournalStatusEnum;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;

import java.io.Serial;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@DynamicUpdate
@Table(name = "app_journal")
public class Journal extends AuditGenericEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "journal_date", nullable = false)
    private LocalDate journalDate;

    @Column(name = "reference_type", nullable = false)
    private String referenceType;

    @Column(name = "reference_id", nullable = false)
    private Long referenceId;

    @Column(name = "description")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private JournalStatusEnum status = JournalStatusEnum.POSTED;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reversal_of_id")
    private Journal reversalOf;

}
