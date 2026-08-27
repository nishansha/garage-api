package com.triasoft.garage.service.impl;

import com.triasoft.garage.constants.ErrorCode;
import com.triasoft.garage.constants.TenantStatus;
import com.triasoft.garage.entity.Tenant;
import com.triasoft.garage.entity.UserProfile;
import com.triasoft.garage.exception.BusinessException;
import com.triasoft.garage.model.tenant.TenantCreateRq;
import com.triasoft.garage.model.tenant.TenantCreateRs;
import com.triasoft.garage.repository.TenantRepository;
import com.triasoft.garage.repository.UserProfileRepository;
import com.triasoft.garage.security.rbac.PrivilegeCache;
import com.triasoft.garage.security.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Orchestrates tenant onboarding as two independently-committing phases:
 * <p>
 * Phase 1 (this class, no @Transactional wrapper - the repository .save() call below commits
 * on its own): insert fnd_tenant with status PENDING. Tenant carries no @TenantId, so this is
 * safe regardless of whatever tenant, if any, was previously resolvable on this thread.
 * <p>
 * Phase 2 (TenantContentsProvisioningService): create the tenant's SUPERADMIN/ADMIN roles, its
 * first user, and the role assignment, then flip status to ACTIVE. Must run in a Hibernate
 * session that resolves the NEW tenant's id via CurrentTenantResolver, because Role is
 * Hibernate @TenantId-scoped and that resolves once per session.
 * <p>
 * {@code @Transactional(REQUIRES_NEW)} alone is NOT sufficient for that here, despite being the
 * pattern used elsewhere (PrivilegeCache.refresh(), RoleService.resolveRoleCodesForUser()): Spring Boot's
 * Open-Session-In-View pre-binds one Hibernate session to the whole request thread before this
 * method even runs, and that pre-bound session's tenant identifier gets resolved (as NO_TENANT,
 * -1) the moment Phase 1 touches it - before we know the new tenant's id, so TenantContext can't
 * be set yet. REQUIRES_NEW under OSIV does not reliably escape that pre-bound session (confirmed
 * empirically: the roles ended up with tenant_id=-1 despite REQUIRES_NEW). Disabling OSIV
 * app-wide fixes it too, but was reverted after it surfaced a live LazyInitializationException in
 * PaymentAccountService, which relies on OSIV to lazily load a ChartOfAccount outside its
 * transaction - too large a blast radius to accept for this one endpoint. Running Phase 2 on a
 * genuinely separate thread sidesteps the whole problem: that thread has no OSIV-bound
 * EntityManager at all, so its @Transactional call is guaranteed a fresh session with the
 * correct tenant. SecurityContext is copied onto the worker thread too, since AuditAware reads
 * it (via SecurityContextHolder, itself ThreadLocal-based) to populate created_by.
 * <p>
 * If phase 2 throws, the tenant is left PENDING rather than partially provisioned silently -
 * identifiable for cleanup/retry rather than needing compensating deletes.
 */
@Service
@RequiredArgsConstructor
public class TenantProvisioningService {

    private final TenantRepository tenantRepository;
    private final UserProfileRepository userProfileRepository;
    private final TenantContentsProvisioningService tenantContentsProvisioningService;
    private final SessionRevocationService sessionRevocationService;
    private final PrivilegeCache privilegeCache;

    public TenantCreateRs createTenant(TenantCreateRq rq) {
        String code = rq.getTenant().getCode().trim();
        String username = rq.getSuperuser().getUsername().trim();

        if (tenantRepository.existsByCodeIgnoreCase(code)) {
            throw new BusinessException(ErrorCode.Business.TENANT_CODE_EXISTS);
        }
        if (userProfileRepository.findByUsername(username) != null) {
            throw new BusinessException(ErrorCode.Business.USER_EXISTS);
        }

        Tenant tenant = new Tenant();
        tenant.setCode(code);
        tenant.setName(rq.getTenant().getName());
        tenant.setStatus(TenantStatus.PENDING);
        tenantRepository.save(tenant);

        UserProfile superuser = provisionContentsOnFreshThread(tenant.getId(), rq.getSuperuser());

        privilegeCache.refresh();
        return TenantCreateRs.builder().tenantId(tenant.getId()).userId(superuser.getId()).build();
    }

    private UserProfile provisionContentsOnFreshThread(Long tenantId, TenantCreateRq.SuperuserPart superuserRq) {
        SecurityContext callerSecurityContext = SecurityContextHolder.getContext();
        CompletableFuture<UserProfile> future = new CompletableFuture<>();

        Thread worker = new Thread(() -> {
            TenantContext.set(tenantId);
            SecurityContextHolder.setContext(callerSecurityContext);
            try {
                future.complete(tenantContentsProvisioningService.provisionSuperuserAndRoles(tenantId, superuserRq));
            } catch (Throwable t) {
                future.completeExceptionally(t);
            } finally {
                TenantContext.clear();
                SecurityContextHolder.clearContext();
            }
        }, "tenant-provisioning-" + tenantId);
        worker.start();

        try {
            return future.get(30, TimeUnit.SECONDS);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException re) {
                throw re;
            }
            throw new IllegalStateException("Tenant contents provisioning failed", cause);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Tenant contents provisioning interrupted", e);
        } catch (TimeoutException e) {
            throw new IllegalStateException("Tenant contents provisioning timed out", e);
        }
    }

    @Transactional
    public void deactivate(Long tenantId) {
        Tenant tenant = findTenant(tenantId);
        tenant.setStatus(TenantStatus.INACTIVE);
        tenantRepository.save(tenant);
        userProfileRepository.findAllByTenantId(tenantId)
                .forEach(user -> sessionRevocationService.revokeSessionsAndTokensForUser(user.getId()));
    }

    @Transactional
    public void activate(Long tenantId) {
        Tenant tenant = findTenant(tenantId);
        tenant.setStatus(TenantStatus.ACTIVE);
        tenantRepository.save(tenant);
    }

    private Tenant findTenant(Long tenantId) {
        return tenantRepository.findById(tenantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.Business.TENANT_NOT_FOUND));
    }
}
