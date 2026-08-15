package com.triasoft.garage.repository;

import com.triasoft.garage.entity.FinanceCompany;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FinanceCompanyRepository extends JpaRepository<FinanceCompany, Long> {

    Optional<FinanceCompany> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

}
