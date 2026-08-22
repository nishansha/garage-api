package com.triasoft.garage.hrm.scheduler;

import com.triasoft.garage.entity.Tenant;
import com.triasoft.garage.repository.TenantRepository;
import com.triasoft.garage.security.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.YearMonth;

/**
 * Monthly PENDING SalaryPayment generation for every active employee, across every
 * tenant/company. Company/Employee are @TenantId-scoped, so — same reasoning as
 * PrivilegeCache.refresh() — this loops over tenants explicitly, setting TenantContext
 * before each pass; there is no single cross-tenant query for this.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SalaryRunScheduler {

    private final TenantRepository tenantRepository;
    private final SalaryRunWorkUnit salaryRunWorkUnit;

    // 1st of every month at 03:00 server time.
    @Scheduled(cron = "0 0 3 1 * *")
    public void generateMonthlySalaryPayments() {
        YearMonth period = YearMonth.now();
        Long callerTenantId = TenantContext.get();
        try {
            for (Tenant tenant : tenantRepository.findAll()) {
                TenantContext.set(tenant.getId());
                int created = salaryRunWorkUnit.generateForTenant(period);
                if (created > 0) {
                    log.info("SalaryRunScheduler - generated {} PENDING salary payment(s) for tenant {} ({})",
                            created, tenant.getId(), period);
                }
            }
        } finally {
            TenantContext.set(callerTenantId);
        }
    }
}
