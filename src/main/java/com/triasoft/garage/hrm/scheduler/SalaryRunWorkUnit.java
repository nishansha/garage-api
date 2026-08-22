package com.triasoft.garage.hrm.scheduler;

import com.triasoft.garage.company.repository.CompanyRepository;
import com.triasoft.garage.hrm.service.SalaryPaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;

/**
 * A separate top-level bean (not a method on SalaryRunScheduler) specifically so
 * @Transactional(REQUIRES_NEW) goes through the Spring AOP proxy — a self-invoked method
 * on the same class would silently skip the proxy and reuse the caller's session/tenant,
 * same reasoning as TenantScopedGrantLoader being split out from PrivilegeCache.
 */
@Component
@RequiredArgsConstructor
class SalaryRunWorkUnit {

    private final CompanyRepository companyRepository;
    private final SalaryPaymentService salaryPaymentService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int generateForTenant(YearMonth period) {
        int created = 0;
        for (Long companyId : companyRepository.findAllCompanyIds()) {
            created += salaryPaymentService.generateForCompany(companyId, period);
        }
        return created;
    }
}
