package com.triasoft.garage.model.audit;

import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * One revision of an audited entity as reconstructed from the Envers audit tables.
 */
@Data
@Builder
public class AuditRevisionRs implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** Envers revision number (revinfo.rev). */
    private Long revision;

    /** ADD (created), MOD (updated) or DEL (deleted). */
    private String changeType;

    /** Id of the user that made the change (revinfo.user_id); null for system/anonymous actions. */
    private Long userId;

    /** Display name of the user, resolved from user_profile; null when unknown. */
    private String username;

    /** When the revision was committed. */
    private LocalDateTime revisionAt;

    /** Full snapshot of the entity at this revision. */
    private Object entity;

    /**
     * Fields that changed relative to the previous revision, keyed by field name. Null on the first
     * (ADD) revision; empty when a revision changed no audited business fields (e.g. a soft delete).
     */
    private Map<String, FieldChange> changedFields;
}
