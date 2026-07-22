package com.triasoft.garage.rbac;

import com.triasoft.garage.constants.Privilege;
import com.triasoft.garage.entity.Resource;
import com.triasoft.garage.entity.Role;
import com.triasoft.garage.entity.RolePrivilege;
import com.triasoft.garage.repository.ResourceRepository;
import com.triasoft.garage.repository.RolePrivilegeRepository;
import com.triasoft.garage.repository.RoleRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * In-memory role -> resource:privilege grant map. Loaded at startup and reloaded
 * whenever fnd_role_privilege changes (call {@link #refresh()} from wherever the
 * admin UI edits grants) so per-request privilege checks don't hit the DB.
 */
@Component
@RequiredArgsConstructor
public class PrivilegeCache {

    private final RoleRepository roleRepository;
    private final ResourceRepository resourceRepository;
    private final RolePrivilegeRepository rolePrivilegeRepository;

    private volatile Map<String, Set<String>> grantsByRoleCode = Map.of();

    @PostConstruct
    public void init() {
        refresh();
    }

    public synchronized void refresh() {
        Map<Long, String> roleCodesById = roleRepository.findAll().stream()
                .collect(Collectors.toMap(Role::getId, Role::getCode));
        Map<Long, String> resourceCodesById = resourceRepository.findAll().stream()
                .collect(Collectors.toMap(Resource::getId, Resource::getCode));

        Map<String, Set<String>> next = new HashMap<>();
        for (RolePrivilege grant : rolePrivilegeRepository.findAll()) {
            String roleCode = roleCodesById.get(grant.getRoleId());
            String resourceCode = resourceCodesById.get(grant.getResourceId());
            if (roleCode == null || resourceCode == null) {
                continue;
            }
            next.computeIfAbsent(roleCode, k -> new HashSet<>()).add(grantKey(resourceCode, grant.getPrivilege()));
        }
        this.grantsByRoleCode = next;
    }

    public boolean isGranted(Collection<String> roleCodes, String resourceCode, Privilege privilege) {
        String key = grantKey(resourceCode, privilege);
        return roleCodes.stream().anyMatch(roleCode -> grantsByRoleCode.getOrDefault(roleCode, Set.of()).contains(key));
    }

    /**
     * Union of all grants held by the given roles, grouped by resource code.
     * Used to build the "my permissions" response for the FE.
     */
    public Map<String, Set<Privilege>> resolve(Collection<String> roleCodes) {
        Map<String, Set<Privilege>> resolved = new HashMap<>();
        for (String roleCode : roleCodes) {
            for (String key : grantsByRoleCode.getOrDefault(roleCode, Set.of())) {
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
