package com.triasoft.garage.service.impl;

import com.triasoft.garage.constants.ErrorCode;
import com.triasoft.garage.constants.TenantStatus;
import com.triasoft.garage.entity.Role;
import com.triasoft.garage.entity.Tenant;
import com.triasoft.garage.entity.UserProfile;
import com.triasoft.garage.entity.UserRole;
import com.triasoft.garage.exception.BusinessException;
import com.triasoft.garage.model.tenant.TenantCreateRq;
import com.triasoft.garage.repository.RoleRepository;
import com.triasoft.garage.repository.TenantRepository;
import com.triasoft.garage.repository.UserProfileRepository;
import com.triasoft.garage.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Phase 2 of tenant provisioning: everything that is tenant-scoped via Hibernate's
 * {@code @TenantId} (Role) must be created in a session opened AFTER TenantContext is set to
 * the new tenant's id - REQUIRES_NEW forces exactly that fresh session, same reasoning as
 * PrivilegeCache.refresh()/RoleService.resolveRoleCodesForUser. The caller
 * (TenantProvisioningService) is responsible for setting TenantContext before invoking this
 * and clearing it afterward.
 */
@Service
@RequiredArgsConstructor
public class TenantContentsProvisioningService {

    private final RoleRepository roleRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserRoleRepository userRoleRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public UserProfile provisionSuperuserAndRoles(Long tenantId, TenantCreateRq.SuperuserPart superuserRq) {
        Role superAdminRole = createSystemRole("SUPERADMIN", "Super Admin", "Unrestricted access; bypasses privilege checks entirely");
        createSystemRole("ADMIN", "Admin", "Administrative access; privileges granted explicitly like any other role");

        UserProfile user = new UserProfile();
        user.setTenantId(tenantId);
        user.setUsername(superuserRq.getUsername().trim());
        user.setPassword(passwordEncoder.encode(superuserRq.getPassword().trim()));
        user.setName(superuserRq.getName());
        user.setDesignation(superuserRq.getDesignation());
        user.setRole("SUPERADMIN");
        userProfileRepository.save(user);

        UserRole userRole = new UserRole();
        userRole.setTenantId(tenantId);
        userRole.setUserId(user.getId());
        userRole.setRoleId(superAdminRole.getId());
        userRoleRepository.save(userRole);

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.Business.TENANT_NOT_FOUND));
        tenant.setStatus(TenantStatus.ACTIVE);
        tenantRepository.save(tenant);

        return user;
    }

    private Role createSystemRole(String code, String name, String description) {
        // No setTenantId(...) here: Role.tenantId carries @TenantId, which Hibernate stamps
        // automatically from CurrentTenantResolver/TenantContext at insert time (see
        // RoleService.create() for the same pattern).
        Role role = new Role();
        role.setCode(code);
        role.setName(name);
        role.setDescription(description);
        role.setSystem(true);
        return roleRepository.save(role);
    }
}
