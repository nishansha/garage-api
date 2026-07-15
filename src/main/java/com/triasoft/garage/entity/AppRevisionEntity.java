package com.triasoft.garage.entity;

import com.triasoft.garage.config.AppRevisionListener;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.RevisionEntity;
import org.hibernate.envers.RevisionNumber;
import org.hibernate.envers.RevisionTimestamp;

import java.io.Serial;
import java.io.Serializable;

/**
 * Custom Envers revision entity backing the {@code revinfo} table. One row is written per
 * transaction that touches an audited entity. {@link AppRevisionListener} stamps {@link #userId}
 * from the authenticated principal so every audit revision records who made the change.
 */
@Getter
@Setter
@Entity
@Table(name = "revinfo")
@RevisionEntity(AppRevisionListener.class)
public class AppRevisionEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "revinfo_seq_gen")
    @SequenceGenerator(name = "revinfo_seq_gen", sequenceName = "revinfo_seq", allocationSize = 1)
    @RevisionNumber
    @Column(name = "rev")
    private Long rev;

    /** Epoch milliseconds, populated by Envers. */
    @RevisionTimestamp
    @Column(name = "rev_timestamp")
    private long timestamp;

    /** Id of the authenticated user that triggered this revision; null for system/anonymous actions. */
    @Column(name = "user_id")
    private Long userId;
}
