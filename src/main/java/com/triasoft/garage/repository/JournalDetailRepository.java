package com.triasoft.garage.repository;

import com.triasoft.garage.entity.JournalDetail;
import com.triasoft.garage.projection.AccountBalanceRow;
import com.triasoft.garage.projection.LedgerRow;
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
            WHERE (
                CAST(:asOfDate AS DATE) IS NULL
                OR j.journal_date <= CAST(:asOfDate AS DATE)
                OR j.id IS NULL
            )
            GROUP BY coa.id, coa.code, coa.name, coa.label, coa.type
            ORDER BY coa.code
            """, nativeQuery = true)
    List<AccountBalanceRow> getTrialBalance(@Param("asOfDate") LocalDate asOfDate);

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
            WHERE coa.type IN ('REVENUE', 'EXPENSE')
            GROUP BY coa.id, coa.code, coa.name, coa.label, coa.type
            ORDER BY coa.code
            """, nativeQuery = true)
    List<AccountBalanceRow> getPLAccountBalances(@Param("fromDate") LocalDate fromDate,
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
            WHERE jd.account_id = :accountId
              AND j.journal_date BETWEEN CAST(:fromDate AS DATE) AND CAST(:toDate AS DATE)
            ORDER BY j.journal_date ASC, j.id ASC, jd.id ASC
            """, nativeQuery = true)
    List<LedgerRow> getLedgerEntries(@Param("accountId") Long accountId,
                                     @Param("fromDate") LocalDate fromDate,
                                     @Param("toDate") LocalDate toDate);

    @Query(value = """
            SELECT
              COALESCE(SUM(jd.debit_amount), 0)  as debit,
              COALESCE(SUM(jd.credit_amount), 0) as credit
            FROM app_journal_detail jd
            JOIN app_journal j ON j.id = jd.journal_id
            WHERE jd.account_id = :accountId
              AND j.journal_date < CAST(:beforeDate AS DATE)
            """, nativeQuery = true)
    OpeningBalanceRow getOpeningBalance(@Param("accountId") Long accountId,
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
            WHERE coa.system_role = :systemRole
              AND j.journal_date BETWEEN CAST(:fromDate AS DATE) AND CAST(:toDate AS DATE)
            """, nativeQuery = true)
    OpeningBalanceRow sumBySystemRoleInPeriod(@Param("systemRole") String systemRole,
                                              @Param("fromDate") LocalDate fromDate,
                                              @Param("toDate") LocalDate toDate);

    @Query(value = """
            SELECT
              COALESCE(SUM(jd.debit_amount), 0)  as debit,
              COALESCE(SUM(jd.credit_amount), 0) as credit
            FROM app_journal_detail jd
            WHERE jd.journal_id = :journalId
            """, nativeQuery = true)
    OpeningBalanceRow getJournalTotals(@Param("journalId") Long journalId);

}
