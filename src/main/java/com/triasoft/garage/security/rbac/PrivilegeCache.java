package com.triasoft.garage.security.rbac;

import com.triasoft.garage.constants.Privilege;
import com.triasoft.garage.entity.Resource;
import com.triasoft.garage.entity.Tenant;
import com.triasoft.garage.repository.ResourceRepository;
import com.triasoft.garage.repository.TenantRepository;
import com.triasoft.garage.security.tenant.TenantContext;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * In-memory (tenantId, role code) -> resource:privilege grant map. Loaded at startup and
 * reloaded whenever fnd_role_privilege changes so per-request privilege checks don't hit the DB.
 * <p>
 * Role and RolePrivilege are tenant-scoped (Hibernate {@code @TenantId}), so a single
 * {@code findAll()} only ever sees one tenant's rows at a time. Building a cache that spans
 * every tenant requires explicitly looping over each known tenant id and setting
 * {@link TenantContext} before each pass - there is no single cross-tenant query for this.
 */
@Component
@RequiredArgsConstructor
public class PrivilegeCache {

    private final TenantRepository tenantRepository;
    private final ResourceRepository resourceRepository;
    private final TenantScopedGrantLoader tenantScopedGrantLoader;

    private volatile Map<Long, Map<String, Set<String>>> grantsByTenantAndRoleCode = Map.of();

    @PostConstruct
    public void init() {
        refresh();
    }

    public synchronized void refresh() {
        // Resource is global/shared across tenants (no @TenantId) - one lookup covers every tenant.
        Map<Long, String> resourceCodesById = resourceRepository.findAll().stream()
                .collect(Collectors.toMap(Resource::getId, Resource::getCode));

        Long callerTenantId = TenantContext.get();
        Map<Long, Map<String, Set<String>>> next = new HashMap<>();
        try {
            for (Tenant tenant : tenantRepository.findAll()) {
                TenantContext.set(tenant.getId());
                next.put(tenant.getId(), tenantScopedGrantLoader.loadForCurrentTenant(resourceCodesById));
            }
        } finally {
            TenantContext.set(callerTenantId);
        }
        this.grantsByTenantAndRoleCode = next;
    }

    public boolean isGranted(Long tenantId, Collection<String> roleCodes, String resourceCode, Privilege privilege) {
        Map<String, Set<String>> tenantGrants = grantsByTenantAndRoleCode.getOrDefault(tenantId, Map.of());
        String key = grantKey(resourceCode, privilege);
        return roleCodes.stream().anyMatch(roleCode -> tenantGrants.getOrDefault(roleCode, Set.of()).contains(key));
    }

    /**
     * Union of all grants held by the given roles within the given tenant, grouped by resource
     * code. Used to build the "my permissions" response for the FE.
     */
    public Map<String, Set<Privilege>> resolve(Long tenantId, Collection<String> roleCodes) {
        Map<String, Set<String>> tenantGrants = grantsByTenantAndRoleCode.getOrDefault(tenantId, Map.of());
        Map<String, Set<Privilege>> resolved = new HashMap<>();
        for (String roleCode : roleCodes) {
            for (String key : tenantGrants.getOrDefault(roleCode, Set.of())) {
                int separator = key.lastIndexOf(':');
                String resourceCode = key.substring(0, separator);
                Privilege privilege = Privilege.valueOf(key.substring(separator + 1));
                resolved.computeIfAbsent(resourceCode, k -> EnumSet.noneOf(Privilege.class)).add(privilege);
            }
        }
        return resolved;
    }

    private static String grantKey(String resourceCode, Privilege privilege) {
        return resourceCode + ":" + privilege;
    }
}
