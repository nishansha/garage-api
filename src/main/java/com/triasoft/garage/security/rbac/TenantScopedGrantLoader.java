package com.triasoft.garage.security.rbac;

import com.triasoft.garage.entity.Role;
import com.triasoft.garage.entity.RolePrivilege;
import com.triasoft.garage.repository.RolePrivilegeRepository;
import com.triasoft.garage.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Loads one tenant's role/privilege grants on a brand-new transaction (and therefore a brand-new
 * Hibernate session). Hibernate resolves {@code @TenantId} exactly once, when a session opens -
 * not per query - so if this ran on whatever transaction the caller happens to already be in
 * (e.g. PrivilegeCache.refresh() invoked from inside RoleService.updatePrivileges(), which is
 * @Transactional), every "per tenant" iteration would silently reuse the caller's own tenant
 * instead of the one being loaded. REQUIRES_NEW forces a fresh session per call, so the tenant
 * set on TenantContext right before this call is the one actually resolved.
 */
@Component
@RequiredArgsConstructor
class TenantScopedGrantLoader {

    private final RoleRepository roleRepository;
    private final RolePrivilegeRepository rolePrivilegeRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public Map<String, Set<String>> loadForCurrentTenant(Map<Long, String> resourceCodesById) {
        Map<Long, String> roleCodesById = roleRepository.findAll().stream()
                .collect(Collectors.toMap(Role::getId, Role::getCode));

        Map<String, Set<String>> grants = new HashMap<>();
        for (RolePrivilege grant : rolePrivilegeRepository.findAll()) {
            String roleCode = roleCodesById.get(grant.getRoleId());
            String resourceCode = resourceCodesById.get(grant.getResourceId());
            if (roleCode == null || resourceCode == null) {
                continue;
            }
            grants.computeIfAbsent(roleCode, k -> new HashSet<>()).add(grantKey(resourceCode, grant));
        }
        return grants;
    }

    private static String grantKey(String resourceCode, RolePrivilege grant) {
        return resourceCode + ":" + grant.getPrivilege();
    }
}
