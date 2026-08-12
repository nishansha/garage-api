package com.triasoft.garage.ledger.repository;

import com.triasoft.garage.ledger.entity.JournalDetail;
import com.triasoft.garage.ledger.projection.AccountBalanceRow;
import com.triasoft.garage.ledger.projection.LedgerRow;
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
              AND (
                CAST(:asOfDate AS DATE) IS NULL
                OR j.journal_date <= CAST(:asOfDate AS DATE)
                OR j.id IS NULL
            )
            GROUP BY coa.id, coa.code, coa.name, coa.label, coa.type
            ORDER BY coa.code
            """, nativeQuery = true)
    List<AccountBalanceRow> getTrialBalance(@Param("tenantId") Long tenantId, @Param("asOfDate") LocalDate asOfDate);

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
            GROUP BY coa.id, coa.code, coa.name, coa.label, coa.type
            ORDER BY coa.code
            """, nativeQuery = true)
    List<AccountBalanceRow> getPLAccountBalances(@Param("tenantId") Long tenantId,
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
              AND coa.system_role = :systemRole
              AND j.journal_date BETWEEN CAST(:fromDate AS DATE) AND CAST(:toDate AS DATE)
            """, nativeQuery = true)
    OpeningBalanceRow sumBySystemRoleInPeriod(@Param("tenantId") Long tenantId,
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

}
