package com.triasoft.garage.repository;

import com.triasoft.garage.entity.DirectEntry;
import com.triasoft.garage.projection.PLDirectEntryMetrics;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface DirectEntryRepository extends JpaRepository<DirectEntry, Long>, JpaSpecificationExecutor<DirectEntry> {

    Page<DirectEntry> findAllByOrderByEntryDateDescCreatedAtDesc(Pageable pageable);

    @Query(value = """
            SELECT
              COALESCE(SUM(CASE WHEN d.direction = 'IN' AND coa.type <> 'EQUITY'
                               THEN d.amount ELSE 0 END), 0) as totalIn,
              COALESCE(SUM(CASE WHEN d.direction = 'OUT' AND coa.type <> 'EQUITY'
                               THEN d.amount ELSE 0 END), 0) as totalOut
            FROM app_direct_entry d
            JOIN fnd_chart_of_accounts coa ON coa.id = d.coa_id
            WHERE d.deleted = false
              AND d.entry_date BETWEEN :startDate AND :endDate
            """, nativeQuery = true)
    PLDirectEntryMetrics getDirectEntryMetrics(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

}
