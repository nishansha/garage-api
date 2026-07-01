package com.triasoft.garage.repository;

import com.triasoft.garage.entity.ChartOfAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChartOfAccountRepository extends JpaRepository<ChartOfAccount, Long> {
    List<ChartOfAccount> findByType(String type);

    Optional<ChartOfAccount> findByCode(String code);

    Optional<ChartOfAccount> findBySystemRole(String systemRole);

    List<ChartOfAccount> findByIsDirectPostableTrue();

    List<ChartOfAccount> findByTypeAndIsDirectPostableTrue(String type);

    Optional<ChartOfAccount> findFirstByTypeOrderByCodeDesc(String type);

    Optional<ChartOfAccount> findByTypeAndLabelIgnoreCase(String type, String label);

    @Query(value = "SELECT COALESCE(MAX(CAST(code AS BIGINT)), 1599) FROM fnd_chart_of_accounts WHERE type = :type AND code ~ '^[0-9]+$'", nativeQuery = true)
    Long findMaxNumericCodeByType(@Param("type") String type);
}
