package com.triasoft.garage.model.audit;

import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * A soft-deleted audited record, reconstructed from its final (DEL) revision. Backs the per-entity
 * "recycle bin" so deleted records remain reachable even though they no longer appear on the live
 * detail screen.
 */
@Data
@Builder
public class DeletedRecordRs implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    /** Envers revision at which the record was deleted. */
    private Long revision;

    private Long deletedBy;
    private String deletedByName;
    private LocalDateTime deletedAt;

    /** Flattened snapshot of the record as it was just before deletion (for list labels). */
    private Map<String, Object> entity;
}
