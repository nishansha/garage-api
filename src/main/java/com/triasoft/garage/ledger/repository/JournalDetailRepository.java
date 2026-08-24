package com.triasoft.garage.ledger.repository;

import com.triasoft.garage.ledger.entity.JournalDetail;
import com.triasoft.garage.ledger.projection.AccountBalanceRow;
import com.triasoft.garage.ledger.projection.LedgerRow;
import com.triasoft.garage.ledger.projection.PartySourceBalanceRow;
import com.triasoft.garage.ledger.projection.SourceBalanceRow;
import com.triasoft.garage.projection.CompanyAmountRow;
import com.triasoft.garage.projection.WarehouseAmountRow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface JournalDetailRepository extends JpaRepository<JournalDetail, Long> {

    @Query("SELECT jd FROM JournalDetail jd WHERE jd.journal.id = :journalId ORDER BY jd.id ASC")
    List<JournalDetail> findByJournalId(@Param("journalId") Long journalId);

    // companyId nullable - null means "overall" (every company's accounts combined), same
    // convention as SaleRepository.getProfitReport for the entity-derived reports.
    @Query(value = """
            SELECT
              coa.id              as accountId,
              coa.code            as code,
              coa.name            as name,
              coa.label           as label,
              coa.type            as type,
              COALESCE(SUM(jd.debit_amount), 0)  as totalDebit,
              COALESCE(SUM(jd.credit_amount), 0) as totalCredit
            FROM fnd_chart_of_accounts coa
            LEFT JOIN app_journal_detail jd ON jd.account_id = coa.id
            LEFT JOIN app_journal j ON j.id = jd.journal_id
            WHERE coa.tenant_id = :tenantId
              AND (:companyId IS NULL OR coa.company_id = :companyId)
              AND (
                CAST(:asOfDate AS DATE) IS NULL
                OR j.journal_date <= CAST(:asOfDate AS DATE)
                OR j.id IS NULL
            )
            GROUP BY coa.id, coa.code, coa.name, coa.label, coa.type
            ORDER BY coa.code
            """, nativeQuery = true)
    List<AccountBalanceRow> getTrialBalance(@Param("tenantId") Long tenantId, @Param("companyId") Long companyId, @Param("asOfDate") LocalDate asOfDate);

    @Query(value = """
            SELECT
              coa.id              as accountId,
              coa.code            as code,
              coa.name            as name,
              coa.label           as label,
              coa.type            as type,
              COALESCE(SUM(CASE WHEN j.journal_date BETWEEN CAST(:fromDate AS DATE) AND CAST(:toDate AS DATE)
                                THEN jd.debit_amount ELSE 0 END), 0)  as totalDebit,
              COALESCE(SUM(CASE WHEN j.journal_date BETWEEN CAST(:fromDate AS DATE) AND CAST(:toDate AS DATE)
                                THEN jd.credit_amount ELSE 0 END), 0) as totalCredit
            FROM fnd_chart_of_accounts coa
            LEFT JOIN app_journal_detail jd ON jd.account_id = coa.id
            LEFT JOIN app_journal j ON j.id = jd.journal_id
            WHERE coa.tenant_id = :tenantId
              AND coa.type IN ('REVENUE', 'EXPENSE')
              AND (:companyId IS NULL OR coa.company_id = :companyId)
            GROUP BY coa.id, coa.code, coa.name, coa.label, coa.type
            ORDER BY coa.code
            """, nativeQuery = true)
    List<AccountBalanceRow> getPLAccountBalances(@Param("tenantId") Long tenantId,
                                                 @Param("companyId") Long companyId,
                                                 @Param("fromDate") LocalDate fromDate,
                                                 @Param("toDate") LocalDate toDate);

    @Query(value = """
            SELECT
              j.id                  as journalId,
              j.journal_date        as journalDate,
              j.reference_type      as referenceType,
              j.reference_id        as referenceId,
              j.description         as journalDescription,
              jd.description        as lineDescription,
              jd.debit_amount       as debit,
              jd.credit_amount      as credit
            FROM app_journal_detail jd
            JOIN app_journal j ON j.id = jd.journal_id
            WHERE jd.tenant_id = :tenantId
              AND jd.account_id = :accountId
              AND j.journal_date BETWEEN CAST(:fromDate AS DATE) AND CAST(:toDate AS DATE)
            ORDER BY j.journal_date ASC, j.id ASC, jd.id ASC
            """, nativeQuery = true)
    List<LedgerRow> getLedgerEntries(@Param("tenantId") Long tenantId,
                                     @Param("accountId") Long accountId,
                                     @Param("fromDate") LocalDate fromDate,
                                     @Param("toDate") LocalDate toDate);

    @Query(value = """
            SELECT
              COALESCE(SUM(jd.debit_amount), 0)  as debit,
              COALESCE(SUM(jd.credit_amount), 0) as credit
            FROM app_journal_detail jd
            JOIN app_journal j ON j.id = jd.journal_id
            WHERE jd.tenant_id = :tenantId
              AND jd.account_id = :accountId
              AND j.journal_date < CAST(:beforeDate AS DATE)
            """, nativeQuery = true)
    OpeningBalanceRow getOpeningBalance(@Param("tenantId") Long tenantId,
                                        @Param("accountId") Long accountId,
                                        @Param("beforeDate") LocalDate beforeDate);

    interface OpeningBalanceRow {
        java.math.BigDecimal getDebit();
        java.math.BigDecimal getCredit();
    }

    @Query(value = """
            SELECT
              COALESCE(SUM(jd.debit_amount), 0)  as debit,
              COALESCE(SUM(jd.credit_amount), 0) as credit
            FROM app_journal_detail jd
            JOIN app_journal j ON j.id = jd.journal_id
            JOIN fnd_chart_of_accounts coa ON coa.id = jd.account_id
            WHERE jd.tenant_id = :tenantId
              AND coa.company_id = :companyId
              AND coa.system_role = :systemRole
              AND j.journal_date BETWEEN CAST(:fromDate AS DATE) AND CAST(:toDate AS DATE)
            """, nativeQuery = true)
    OpeningBalanceRow sumBySystemRoleInPeriod(@Param("tenantId") Long tenantId,
                                              @Param("companyId") Long companyId,
                                              @Param("systemRole") String systemRole,
                                              @Param("fromDate") LocalDate fromDate,
                                              @Param("toDate") LocalDate toDate);

    // All-companies variant of sumBySystemRoleInPeriod, for comparison reports (e.g. exchange
    // gain per company in CompanyReportService) - grouped by the journal's own company_id
    // rather than filtered to one, since we want every company's total in one query.
    @Query(value = """
            SELECT
              j.company_id                       as companyId,
              COALESCE(SUM(jd.credit_amount - jd.debit_amount), 0) as amount
            FROM app_journal_detail jd
            JOIN app_journal j ON j.id = jd.journal_id
            JOIN fnd_chart_of_accounts coa ON coa.id = jd.account_id
            WHERE jd.tenant_id = :tenantId
              AND coa.system_role = :systemRole
              AND j.journal_date BETWEEN CAST(:fromDate AS DATE) AND CAST(:toDate AS DATE)
            GROUP BY j.company_id
            """, nativeQuery = true)
    List<CompanyAmountRow> sumBySystemRoleByCompany(@Param("tenantId") Long tenantId,
                                                     @Param("systemRole") String systemRole,
                                                     @Param("fromDate") LocalDate fromDate,
                                                     @Param("toDate") LocalDate toDate);

    // Warehouse-scoped totals for return/exchange system-role accounts - ledger entries in
    // general aren't warehouse-tagged, so each of these traces a specific role back to a
    // warehouse via the ONE journal reference type it's always posted on:
    //   - GAIN_ON_EXCHANGE_ADJ is posted on BOTH a SALE_RETURN journal (KEEP_AND_BUYBACK
    //     renegotiation gain) AND a PURCHASE_RETURN journal (purchase-return gain) -
    //     JournalService.java:419 and :510 - so it needs BOTH queries below, summed.
    //   - LOSS_RETURNED_EXCHANGE is SALE_RETURN-only (JournalService.java:405).
    //   - LOSS_PURCHASE_RETURN is PURCHASE_RETURN-only (JournalService.java:507).
    // sumBySystemRoleByCompany above doesn't need this split - company_id lives directly on
    // app_journal regardless of reference type, only the warehouse trace requires it.
    @Query(value = """
            SELECT
              i.warehouse_id                     as warehouseId,
              COALESCE(SUM(jd.credit_amount - jd.debit_amount), 0) as amount
            FROM app_journal_detail jd
            JOIN app_journal j ON j.id = jd.journal_id
            JOIN fnd_chart_of_accounts coa ON coa.id = jd.account_id
            JOIN app_sale_return sr ON sr.id = j.reference_id AND j.reference_type = 'SALE_RETURN'
            JOIN app_sale s ON s.id = sr.sale_id
            JOIN app_inventory i ON i.id = s.inventory_id
            WHERE jd.tenant_id = :tenantId
              AND coa.system_role = :systemRole
              AND j.journal_date BETWEEN CAST(:fromDate AS DATE) AND CAST(:toDate AS DATE)
            GROUP BY i.warehouse_id
            """, nativeQuery = true)
    List<WarehouseAmountRow> sumSaleReturnRoleByWarehouse(@Param("tenantId") Long tenantId,
                                                           @Param("systemRole") String systemRole,
                                                           @Param("fromDate") LocalDate fromDate,
                                                           @Param("toDate") LocalDate toDate);

    @Query(value = """
            SELECT
              po.warehouse_id                    as warehouseId,
              COALESCE(SUM(jd.credit_amount - jd.debit_amount), 0) as amount
            FROM app_journal_detail jd
            JOIN app_journal j ON j.id = jd.journal_id
            JOIN fnd_chart_of_accounts coa ON coa.id = jd.account_id
            JOIN app_purchase_return pr ON pr.id = j.reference_id AND j.reference_type = 'PURCHASE_RETURN'
            JOIN app_purchase_order po ON po.id = pr.purchase_id
            WHERE jd.tenant_id = :tenantId
              AND coa.system_role = :systemRole
              AND j.journal_date BETWEEN CAST(:fromDate AS DATE) AND CAST(:toDate AS DATE)
            GROUP BY po.warehouse_id
            """, nativeQuery = true)
    List<WarehouseAmountRow> sumPurchaseReturnRoleByWarehouse(@Param("tenantId") Long tenantId,
                                                               @Param("systemRole") String systemRole,
                                                               @Param("fromDate") LocalDate fromDate,
                                                               @Param("toDate") LocalDate toDate);

    @Query(value = """
            SELECT
              COALESCE(SUM(jd.debit_amount), 0)  as debit,
              COALESCE(SUM(jd.credit_amount), 0) as credit
            FROM app_journal_detail jd
            WHERE jd.tenant_id = :tenantId
              AND jd.journal_id = :journalId
            """, nativeQuery = true)
    OpeningBalanceRow getJournalTotals(@Param("tenantId") Long tenantId, @Param("journalId") Long journalId);

    // ─────────────────────────────────────────────────────────────────────────
    //  Party subledger (AR/AP etc. by customer/vendor) - see LedgerQueryService.getPartyLedger
    //
    // A party's ledger is just "their account" - own natural DR/CR sides from the raw
    // debit/credit sums of whatever lines were tagged with their party_type/party_id,
    // exactly like any other ledger account (a vendor naturally carries a CR balance
    // when we owe them - that's correct, not something to flip). No need to look at
    // which specific GL account each line hit, unlike getLedger's single-account case.
    // ─────────────────────────────────────────────────────────────────────────

    @Query(value = """
            SELECT
              j.id                  as journalId,
              j.journal_date        as journalDate,
              j.reference_type      as referenceType,
              j.reference_id        as referenceId,
              j.description         as journalDescription,
              jd.description        as lineDescription,
              jd.debit_amount       as debit,
              jd.credit_amount      as credit
            FROM app_journal_detail jd
            JOIN app_journal j ON j.id = jd.journal_id
            WHERE jd.tenant_id = :tenantId
              AND jd.party_type = :partyType
              AND jd.party_id = :partyId
              AND j.journal_date BETWEEN CAST(:fromDate AS DATE) AND CAST(:toDate AS DATE)
            ORDER BY j.journal_date ASC, j.id ASC, jd.id ASC
            """, nativeQuery = true)
    List<LedgerRow> getPartyLedgerEntries(@Param("tenantId") Long tenantId,
                                          @Param("partyType") String partyType,
                                          @Param("partyId") Long partyId,
                                          @Param("fromDate") LocalDate fromDate,
                                          @Param("toDate") LocalDate toDate);

    @Query(value = """
            SELECT
              COALESCE(SUM(jd.debit_amount), 0)  as debit,
              COALESCE(SUM(jd.credit_amount), 0) as credit
            FROM app_journal_detail jd
            JOIN app_journal j ON j.id = jd.journal_id
            WHERE jd.tenant_id = :tenantId
              AND jd.party_type = :partyType
              AND jd.party_id = :partyId
              AND j.journal_date < CAST(:beforeDate AS DATE)
            """, nativeQuery = true)
    OpeningBalanceRow getPartyOpeningBalance(@Param("tenantId") Long tenantId,
                                             @Param("partyType") String partyType,
                                             @Param("partyId") Long partyId,
                                             @Param("beforeDate") LocalDate beforeDate);

    // ─────────────────────────────────────────────────────────────────────────
    //  Open items (which Sale/Purchase still has a nonzero balance) - see
    //  JournalDetail.sourceType/sourceId. Deliberately raw debit/credit, not a signed
    //  "pending" figure - whether a nonzero balance means owed-to-us or owed-by-us
    //  depends on which GL accounts normally back that source type (receivable vs
    //  payable), which is domain knowledge this package doesn't have.
    // ─────────────────────────────────────────────────────────────────────────

    @Query(value = """
            SELECT
              jd.source_id                       as sourceId,
              COALESCE(SUM(jd.debit_amount), 0)  as debit,
              COALESCE(SUM(jd.credit_amount), 0) as credit
            FROM app_journal_detail jd
            WHERE jd.tenant_id = :tenantId
              AND jd.source_type = :sourceType
            GROUP BY jd.source_id
            HAVING SUM(jd.debit_amount) - SUM(jd.credit_amount) <> 0
            """, nativeQuery = true)
    List<SourceBalanceRow> getOpenSourceBalances(@Param("tenantId") Long tenantId, @Param("sourceType") String sourceType);

    // Same shape as getOpenSourceBalances, additionally scoped to ONE control account
    // (system_role). source_type alone is NOT a reliable subledger key: a PURCHASE source can
    // carry AP, RC_DUE_RECEIVABLE, and VENDOR_REFUND_RECEIVABLE lines all tagged to the same
    // vendor+purchase, and a SALE source can carry AR, FINANCE_RECEIVABLE, CUSTOMER_SETTLEMENT_
    // PAYABLE, and CUSTOMER_REFUND_PAYABLE lines all tagged to the same customer+sale. Summing
    // across all of them (plain getOpenSourceBalances) silently nets a receivable against a
    // payable, or nets two different parties' receivables together. Scoping to the single
    // control account a report actually means (AR for receivables, AP for payables, ...) is
    // what a real subledger is, and removes this whole bug class - use this, not the unscoped
    // version, for anything user-facing.
    @Query(value = """
            SELECT
              jd.source_id                       as sourceId,
              COALESCE(SUM(jd.debit_amount), 0)  as debit,
              COALESCE(SUM(jd.credit_amount), 0) as credit
            FROM app_journal_detail jd
            JOIN fnd_chart_of_accounts coa ON coa.id = jd.account_id
            WHERE jd.tenant_id = :tenantId
              AND coa.company_id = :companyId
              AND jd.source_type = :sourceType
              AND coa.system_role = :systemRole
            GROUP BY jd.source_id
            HAVING SUM(jd.debit_amount) - SUM(jd.credit_amount) <> 0
            """, nativeQuery = true)
    List<SourceBalanceRow> getOpenSourceBalancesByRole(@Param("tenantId") Long tenantId,
                                                        @Param("companyId") Long companyId,
                                                        @Param("sourceType") String sourceType,
                                                        @Param("systemRole") String systemRole);

    // Grouped by BOTH party_id and source_id, additionally scoped to ONE control account (see
    // getOpenSourceBalancesByRole) - needed wherever a source can carry lines for more than one
    // party under the SAME account (e.g. a financed sale's FINANCE_RECEIVABLE line and a plain
    // AR line share source_id but not party). Gives both the per-party total (sum across
    // source_id) and the per-source drill-down in one query.
    @Query(value = """
            SELECT
              jd.party_id                        as partyId,
              jd.source_id                       as sourceId,
              COALESCE(SUM(jd.debit_amount), 0)  as debit,
              COALESCE(SUM(jd.credit_amount), 0) as credit
            FROM app_journal_detail jd
            JOIN fnd_chart_of_accounts coa ON coa.id = jd.account_id
            WHERE jd.tenant_id = :tenantId
              AND coa.company_id = :companyId
              AND jd.party_type = :partyType
              AND jd.source_type = :sourceType
              AND coa.system_role = :systemRole
            GROUP BY jd.party_id, jd.source_id
            HAVING SUM(jd.debit_amount) - SUM(jd.credit_amount) <> 0
            """, nativeQuery = true)
    List<PartySourceBalanceRow> getOpenPartySourceBalances(@Param("tenantId") Long tenantId,
                                                            @Param("companyId") Long companyId,
                                                            @Param("partyType") String partyType,
                                                            @Param("sourceType") String sourceType,
                                                            @Param("systemRole") String systemRole);

}
