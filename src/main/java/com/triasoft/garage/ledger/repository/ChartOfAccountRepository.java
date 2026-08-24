package com.triasoft.garage.ledger.repository;

import com.triasoft.garage.ledger.entity.ChartOfAccount;
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

    Optional<ChartOfAccount> findBySystemRoleAndCompanyId(String systemRole, Long companyId);

    List<ChartOfAccount> findByIsDirectPostableTrue();

    List<ChartOfAccount> findByTypeAndIsDirectPostableTrue(String type);

    List<ChartOfAccount> findByCompanyId(Long companyId);

    List<ChartOfAccount> findByIsDirectPostableTrueAndCompanyId(Long companyId);

    List<ChartOfAccount> findByTypeAndCompanyId(String type, Long companyId);

    List<ChartOfAccount> findByTypeAndIsDirectPostableTrueAndCompanyId(String type, Long companyId);

    Optional<ChartOfAccount> findFirstByTypeAndCompanyIdOrderByCodeDesc(String type, Long companyId);

    Optional<ChartOfAccount> findByTypeAndLabelIgnoreCaseAndCompanyId(String type, String label, Long companyId);

    Optional<ChartOfAccount> findByTypeAndLabelIgnoreCaseAndIdNotAndCompanyId(String type, String label, Long id, Long companyId);

    @Query(value = "SELECT COALESCE(MAX(CAST(code AS BIGINT)), 1599) FROM fnd_chart_of_accounts WHERE tenant_id = :tenantId AND type = :type AND code ~ '^[0-9]+$'", nativeQuery = true)
    Long findMaxNumericCodeByType(@Param("tenantId") Long tenantId, @Param("type") String type);
}
