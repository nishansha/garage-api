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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the two-phase tenant-onboarding orchestration in {@link TenantProvisioningService}.
 * The load-bearing behavior under test is that phase 2 runs on a dedicated worker thread with
 * TenantContext set to the new tenant's id on THAT thread, while the calling (test) thread's own
 * TenantContext is never touched at all - see the class doc on TenantProvisioningService for why
 * a plain method call (even {@code @Transactional(REQUIRES_NEW)}) isn't sufficient here (Open-
 * Session-In-View pre-binds one Hibernate session to the request thread, defeating REQUIRES_NEW's
 * usual "fresh session" guarantee). TenantContentsProvisioningService itself is mocked here since
 * its own behavior depends on a live Hibernate session (@TenantId stamping) that a plain Mockito
 * test can't exercise - that belongs in an IT-style test alongside TenantIsolationIT/
 * TenantProvisioningHttpIT.
 */
@ExtendWith(MockitoExtension.class)
class TenantProvisioningServiceTest {

    @Mock private TenantRepository tenantRepository;
    @Mock private UserProfileRepository userProfileRepository;
    @Mock private TenantContentsProvisioningService tenantContentsProvisioningService;
    @Mock private SessionRevocationService sessionRevocationService;
    @Mock private PrivilegeCache privilegeCache;

    private TenantProvisioningService tenantProvisioningService;

    private final AtomicLong tenantIdSeq = new AtomicLong(1);

    @AfterEach
    void clearTenantContext() {
        // Belt-and-suspenders: a test failure partway through must not leak a tenant id onto
        // whatever test runs next on this thread.
        TenantContext.clear();
    }

    private void initService() {
        tenantProvisioningService = new TenantProvisioningService(
                tenantRepository, userProfileRepository, tenantContentsProvisioningService,
                sessionRevocationService, privilegeCache);
    }

    private TenantCreateRq buildRq() {
        TenantCreateRq rq = new TenantCreateRq();
        TenantCreateRq.TenantPart tenantPart = new TenantCreateRq.TenantPart();
        tenantPart.setCode("ACME");
        tenantPart.setName("Acme Motors");
        rq.setTenant(tenantPart);

        TenantCreateRq.SuperuserPart superuserPart = new TenantCreateRq.SuperuserPart();
        superuserPart.setUsername("acme_owner");
        superuserPart.setPassword("Secret123!");
        superuserPart.setName("Acme Owner");
        superuserPart.setDesignation("Owner");
        rq.setSuperuser(superuserPart);
        return rq;
    }

    @Test
    void createTenant_success_provisionsContentsOnWorkerThreadWithoutTouchingCallerThreadContext() {
        initService();
        TenantContext.set(999L); // simulates whatever context happened to be on the calling (request) thread
        when(tenantRepository.existsByCodeIgnoreCase("ACME")).thenReturn(false);
        when(userProfileRepository.findByUsername("acme_owner")).thenReturn(null);
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(inv -> {
            Tenant tenant = inv.getArgument(0);
            tenant.setId(tenantIdSeq.getAndIncrement());
            return tenant;
        });

        // Mutated on the worker thread, read here after future.get() returns - safe under JMM
        // since CompletableFuture.complete() happens-before the subsequent get().
        List<Long> tenantContextDuringPhase2 = new java.util.ArrayList<>();
        UserProfile createdSuperuser = new UserProfile();
        createdSuperuser.setId(7L);
        when(tenantContentsProvisioningService.provisionSuperuserAndRoles(eq(1L), any(TenantCreateRq.SuperuserPart.class)))
                .thenAnswer(inv -> {
                    tenantContextDuringPhase2.add(TenantContext.get());
                    return createdSuperuser;
                });

        TenantCreateRs result = tenantProvisioningService.createTenant(buildRq());

        assertThat(result.getTenantId()).isEqualTo(1L);
        assertThat(result.getUserId()).isEqualTo(7L);
        assertThat(tenantContextDuringPhase2).containsExactly(1L);
        // Calling thread's own TenantContext was never touched - still whatever it was before.
        assertThat(TenantContext.get()).isEqualTo(999L);

        ArgumentCaptor<Tenant> tenantCaptor = ArgumentCaptor.forClass(Tenant.class);
        verify(tenantRepository).save(tenantCaptor.capture());
        assertThat(tenantCaptor.getValue().getCode()).isEqualTo("ACME");
        assertThat(tenantCaptor.getValue().getStatus()).isEqualTo(TenantStatus.PENDING);

        verify(privilegeCache).refresh();
    }

    @Test
    void createTenant_duplicateTenantCode_throwsBeforeCreatingAnything() {
        initService();
        when(tenantRepository.existsByCodeIgnoreCase("ACME")).thenReturn(true);

        assertThatThrownBy(() -> tenantProvisioningService.createTenant(buildRq()))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(ErrorCode.Business.TENANT_CODE_EXISTS.getCode());

        verify(tenantRepository, never()).save(any());
        verifyNoInteractions(tenantContentsProvisioningService, privilegeCache);
    }

    @Test
    void createTenant_duplicateUsername_throwsBeforeCreatingTenant() {
        initService();
        when(tenantRepository.existsByCodeIgnoreCase("ACME")).thenReturn(false);
        when(userProfileRepository.findByUsername("acme_owner")).thenReturn(new UserProfile());

        assertThatThrownBy(() -> tenantProvisioningService.createTenant(buildRq()))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(ErrorCode.Business.USER_EXISTS.getCode());

        verify(tenantRepository, never()).save(any());
        verifyNoInteractions(tenantContentsProvisioningService, privilegeCache);
    }

    @Test
    void createTenant_phase2Fails_originalExceptionPropagatesAndPrivilegeCacheNeverRefreshed() {
        initService();
        TenantContext.set(999L);
        when(tenantRepository.existsByCodeIgnoreCase("ACME")).thenReturn(false);
        when(userProfileRepository.findByUsername("acme_owner")).thenReturn(null);
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(inv -> {
            Tenant tenant = inv.getArgument(0);
            tenant.setId(1L);
            return tenant;
        });
        when(tenantContentsProvisioningService.provisionSuperuserAndRoles(eq(1L), any(TenantCreateRq.SuperuserPart.class)))
                .thenThrow(new RuntimeException("boom"));

        // The RuntimeException is thrown on the worker thread, wrapped in ExecutionException by
        // CompletableFuture.get(), and must be unwrapped back to the original type/message - not
        // surfaced as an opaque ExecutionException or IllegalStateException.
        assertThatThrownBy(() -> tenantProvisioningService.createTenant(buildRq()))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("boom");

        // Calling thread's own TenantContext was never touched by this flow at all.
        assertThat(TenantContext.get()).isEqualTo(999L);
        // The tenant row itself is left PENDING (not cleaned up) - identifiable for retry/cleanup,
        // per TenantProvisioningService's class doc.
        verifyNoInteractions(privilegeCache);
    }

    @Test
    void deactivate_revokesSessionsForEveryUserInTenantAndFlipsStatus() {
        initService();
        Tenant tenant = new Tenant();
        tenant.setId(5L);
        tenant.setStatus(TenantStatus.ACTIVE);
        when(tenantRepository.findById(5L)).thenReturn(Optional.of(tenant));

        UserProfile user1 = new UserProfile();
        user1.setId(10L);
        UserProfile user2 = new UserProfile();
        user2.setId(11L);
        when(userProfileRepository.findAllByTenantId(5L)).thenReturn(List.of(user1, user2));

        tenantProvisioningService.deactivate(5L);

        assertThat(tenant.getStatus()).isEqualTo(TenantStatus.INACTIVE);
        verify(tenantRepository).save(tenant);
        verify(sessionRevocationService).revokeSessionsAndTokensForUser(10L);
        verify(sessionRevocationService).revokeSessionsAndTokensForUser(11L);
    }

    @Test
    void deactivate_tenantNotFound_throwsAndNeverTouchesSessions() {
        initService();
        when(tenantRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tenantProvisioningService.deactivate(404L))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(ErrorCode.Business.TENANT_NOT_FOUND.getCode());

        verifyNoInteractions(sessionRevocationService, userProfileRepository);
    }

    @Test
    void activate_flipsStatusToActiveWithoutTouchingSessions() {
        initService();
        Tenant tenant = new Tenant();
        tenant.setId(5L);
        tenant.setStatus(TenantStatus.INACTIVE);
        when(tenantRepository.findById(5L)).thenReturn(Optional.of(tenant));

        tenantProvisioningService.activate(5L);

        assertThat(tenant.getStatus()).isEqualTo(TenantStatus.ACTIVE);
        verify(tenantRepository, times(1)).save(tenant);
        verifyNoInteractions(sessionRevocationService);
    }
}
