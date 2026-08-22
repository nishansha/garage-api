package com.triasoft.garage.company.repository;

import com.triasoft.garage.company.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Long> {
    List<Company> findAllByOrderByIdAsc();

    // Id-only projection so callers outside this module's NamedInterface don't need to
    // depend on the Company entity itself (which lives outside the exposed "repository"/
    // "constants" named interfaces) — see SalaryRunWorkUnit for the consumer.
    @Query("select c.id from Company c order by c.id asc")
    List<Long> findAllCompanyIds();
}
