package com.triasoft.garage.service.impl;

import com.triasoft.garage.constants.ErrorCode;
import com.triasoft.garage.exception.BusinessException;
import com.triasoft.garage.model.admin.DataResetRs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    /**
     * Application (transactional) tables that get wiped on a test-environment reset.
     * Seed/foundation tables (fnd_*), configuration (app_configurations) and user_profile are
     * intentionally excluded so master/seed/login data survives the reset.
     * Ordered child-before-parent for readability; truncation happens in a single statement so
     * inter-table foreign keys within this set do not require a specific order.
     */
    static final List<String> CLEARED_TABLES = List.of(
            // Sales
            "app_sale_return_deduction",
            "app_sale_refund_payment",
            "app_sale_return",
            "app_sale_amount_split",
            "app_sale_payment",
            "app_sale",
            // Purchases
            "app_purchase_return_receipt",
            "app_purchase_return",
            "app_purchase_payment",
            "app_purchase_order_detail",
            "app_purchase_order",
            // Inventory & catalog
            "app_inventory",
            "app_product",
            // Ledger / journal / accounting
            "app_transaction",
            "app_journal_detail",
            "app_journal",
            "app_direct_entry",
            "app_expense",
            "app_payment_account",
            // Parties
            "app_customer",
            "app_vendor",
            // Auth session data (users themselves are kept in user_profile)
            "user_session",
            "user_refresh_token"
    );

    /**
     * Envers audit tables + the shared revinfo revision table. Wiped alongside the transactional
     * data so no audit rows are left pointing at ids that no longer exist. Truncated together
     * because every _aud table has a foreign key into revinfo.
     */
    static final List<String> AUDIT_TABLES = List.of(
            "app_sale_aud",
            "app_sale_payment_aud",
            "app_sale_return_aud",
            "app_sale_return_deduction_aud",
            "app_sale_refund_payment_aud",
            "app_purchase_order_aud",
            "app_purchase_payment_aud",
            "app_purchase_return_aud",
            "app_purchase_return_receipt_aud",
            "app_direct_entry_aud",
            "app_expense_aud",
            "revinfo"
    );

    private final JdbcTemplate jdbcTemplate;

    @Value("${app.data-reset.enabled:false}")
    private boolean dataResetEnabled;

    @Transactional
    public DataResetRs resetData() {
        if (!dataResetEnabled) {
            throw new BusinessException(ErrorCode.Business.DATA_RESET_DISABLED);
        }

        // Capture row counts before truncation so the caller sees what was cleared.
        Map<String, Long> deletedByTable = new LinkedHashMap<>();
        long total = 0L;
        for (String table : CLEARED_TABLES) {
            Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Long.class);
            long rows = count != null ? count : 0L;
            deletedByTable.put(table, rows);
            total += rows;
        }

        // Single TRUNCATE resets identities and satisfies FKs within the set atomically.
        // This also clears app_journal_detail and app_payment_account, removing every FK reference
        // into fnd_chart_of_accounts before we delete the orphaned payment-account CoA rows below.
        String truncateSql = "TRUNCATE TABLE " + String.join(", ", CLEARED_TABLES) + " RESTART IDENTITY";
        jdbcTemplate.execute(truncateSql);

        // Clear the Envers audit trail for the wiped data and reset the revision sequence.
        jdbcTemplate.execute("TRUNCATE TABLE " + String.join(", ", AUDIT_TABLES) + " RESTART IDENTITY");
        jdbcTemplate.execute("ALTER SEQUENCE revinfo_seq RESTART");

        // Payment accounts auto-create CoA rows named A-BNK-<id>/A-CSH-<id> (see PaymentAccountService).
        // With the payment accounts gone these are orphans; delete them while leaving seed CoA intact.
        int orphanCoaDeleted = jdbcTemplate.update(
                "DELETE FROM fnd_chart_of_accounts WHERE name LIKE 'A-BNK-%' OR name LIKE 'A-CSH-%'");
        total += orphanCoaDeleted;
        log.warn("DATA RESET executed: cleared {} tables + {} orphaned CoA rows, {} total rows deleted",
                CLEARED_TABLES.size(), orphanCoaDeleted, total);

        return DataResetRs.builder()
                .tablesCleared(CLEARED_TABLES.size())
                .totalRowsDeleted(total)
                .deletedRowsByTable(deletedByTable)
                .orphanCoaRowsDeleted(orphanCoaDeleted)
                .build();
    }
}
