package com.triasoft.garage.model.admin;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class DataResetRs {

    /** Number of tables cleared. */
    private int tablesCleared;

    /** Total number of rows deleted across all cleared tables. */
    private long totalRowsDeleted;

    /** Per-table row counts that were present (and deleted) before truncation, keyed by table name. */
    private Map<String, Long> deletedRowsByTable;
}