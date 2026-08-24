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
     * Application (transactional) tables that get wiped on a test-environment reset — everything
     * belonging to a company, so a reset leaves only seed/foundation data, the tenant record, and
     * users. This includes app_company/inf_warehouse themselves and fnd_chart_of_accounts (despite
     * the fnd_ prefix, CoA rows are seeded per-company by CompanyService.seedChartOfAccounts, not
     * genuine tenant-wide seed data - see the multi-company refactor). True seed/foundation tables
     * (fnd_role, fnd_resource, fnd_lookup_master, fnd_tenant, ...), configuration
     * (app_configurations) and user_profile are intentionally excluded so master/seed/login data
     * survives the reset.
     * Ordered child-before-parent for readability; truncation happens in a single statement so
     * inter-table foreign keys within this set do not require a specific order.
     */
    static final List<String> CLEARED_TABLES = List.of(
            // Sales
            "app_sale_return_deduction",
            "app_sale_refund_payment",
            "app_sale_return",
            "app_sale_amount_split",
            "app_rc_due_receipt",
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
            "app_finance_company",
            // Service sales
            "app_service_sale_payment",
            "app_service_sale_item",
            "app_service_sale",
            "app_service",
            // Payroll
            "app_salary_payment",
            "app_employee",
            // Company / warehouse structure itself, and the per-company CoA it owns
            "user_company_access",
            "warehouse_business_line",
            "inf_warehouse",
            "fnd_chart_of_accounts",
            "app_company",
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
            "app_rc_due_receipt_aud",
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

        // user_profile.default_company_id and fnd_lookup_master.offset_coa_id are the only FK
        // references INTO the tables below from tables that are NOT themselves being truncated
        // (users must survive the reset; fnd_lookup_master is seed data). Both are optional,
        // nullable fields, but TRUNCATE checks the constraint's existence regardless of current
        // row values - unlike DELETE, nulling the columns alone does not satisfy it. Drop both
        // FKs for the duration of the TRUNCATE and recreate them immediately after; this whole
        // method is @Transactional, so any failure mid-way rolls back the drop too.
        jdbcTemplate.update("UPDATE user_profile SET default_company_id = NULL");
        jdbcTemplate.update("UPDATE fnd_lookup_master SET offset_coa_id = NULL");
        jdbcTemplate.execute("ALTER TABLE user_profile DROP CONSTRAINT user_profile_default_company_id_fkey");
        jdbcTemplate.execute("ALTER TABLE fnd_lookup_master DROP CONSTRAINT fnd_lookup_master_offset_coa_id_fkey");

        // Single TRUNCATE resets identities and satisfies FKs within the set atomically.
        String truncateSql = "TRUNCATE TABLE " + String.join(", ", CLEARED_TABLES) + " RESTART IDENTITY";
        jdbcTemplate.execute(truncateSql);

        jdbcTemplate.execute("ALTER TABLE user_profile ADD CONSTRAINT user_profile_default_company_id_fkey " +
                "FOREIGN KEY (default_company_id) REFERENCES app_company(id)");
        jdbcTemplate.execute("ALTER TABLE fnd_lookup_master ADD CONSTRAINT fnd_lookup_master_offset_coa_id_fkey " +
                "FOREIGN KEY (offset_coa_id) REFERENCES fnd_chart_of_accounts(id)");

        // Clear the Envers audit trail for the wiped data and reset the revision sequence.
        jdbcTemplate.execute("TRUNCATE TABLE " + String.join(", ", AUDIT_TABLES) + " RESTART IDENTITY");
        jdbcTemplate.execute("ALTER SEQUENCE revinfo_seq RESTART");

        log.warn("DATA RESET executed: cleared {} tables, {} total rows deleted", CLEARED_TABLES.size(), total);

        return DataResetRs.builder()
                .tablesCleared(CLEARED_TABLES.size())
                .totalRowsDeleted(total)
                .deletedRowsByTable(deletedByTable)
                .build();
    }
}
