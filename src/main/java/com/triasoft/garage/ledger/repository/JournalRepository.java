package com.triasoft.garage.ledger.repository;

import com.triasoft.garage.ledger.entity.Journal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JournalRepository extends JpaRepository<Journal, Long>, JpaSpecificationExecutor<Journal> {

    @Query("""
            SELECT j FROM Journal j
            WHERE j.referenceType = :referenceType
              AND j.referenceId   = :referenceId
              AND j.status        = com.triasoft.garage.ledger.constants.JournalStatusEnum.POSTED
              AND j.reversalOf IS NULL
            """)
    Optional<Journal> findActiveByReferenceTypeAndReferenceId(
            @Param("referenceType") String referenceType,
            @Param("referenceId") Long referenceId);

    @Query("""
            SELECT j FROM Journal j
            WHERE j.referenceType = :referenceType
              AND j.referenceId   = :referenceId
            ORDER BY j.createdAt DESC
            """)
    Optional<Journal> findLatestByReferenceTypeAndReferenceId(
            @Param("referenceType") String referenceType,
            @Param("referenceId") Long referenceId);

}
