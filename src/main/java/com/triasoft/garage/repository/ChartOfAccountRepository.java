package com.triasoft.garage.repository;

import com.triasoft.garage.entity.ChartOfAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChartOfAccountRepository extends JpaRepository<ChartOfAccount, Long> {
    List<ChartOfAccount> findByType(String type);

    Optional<Long> findFirstByTypeOrderByCodeDesc(String type);

    Optional<ChartOfAccount> findByTypeAndLabelIgnoreCase(String type, String label);
}
