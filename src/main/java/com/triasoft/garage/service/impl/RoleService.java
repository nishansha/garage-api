package com.triasoft.garage.service.impl;

import com.triasoft.garage.concurrency.VersionCheck;
import com.triasoft.garage.constants.ErrorCode;
import com.triasoft.garage.dto.ModuleDTO;
import com.triasoft.garage.dto.PrivilegeGrantDTO;
import com.triasoft.garage.dto.ResourceDTO;
import com.triasoft.garage.dto.RoleDTO;
import com.triasoft.garage.entity.FndModule;
import com.triasoft.garage.entity.Resource;
import com.triasoft.garage.entity.Role;
import com.triasoft.garage.entity.RolePrivilege;
import com.triasoft.garage.entity.UserProfile;
import com.triasoft.garage.entity.UserRole;
import com.triasoft.garage.constants.Privilege;
import com.triasoft.garage.dto.UserDTO;
import com.triasoft.garage.exception.BusinessException;
import com.triasoft.garage.model.role.MyPermissionsRs;
import com.triasoft.garage.model.role.PrivilegeGrantRq;
import com.triasoft.garage.model.role.ResourceTreeRs;
import com.triasoft.garage.model.role.RolePrivilegeRq;
import com.triasoft.garage.model.role.RolePrivilegeRs;
import com.triasoft.garage.model.role.RoleRq;
import com.triasoft.garage.model.role.RoleRs;
import com.triasoft.garage.model.role.RolesRs;
import com.triasoft.garage.model.user.UserRoleRq;
import com.triasoft.garage.model.user.UserRoleRs;
import com.triasoft.garage.rbac.PrivilegeCache;
import com.triasoft.garage.repository.FndModuleRepository;
import com.triasoft.garage.repository.ResourceRepository;
import com.triasoft.garage.repository.RolePrivilegeRepository;
import com.triasoft.garage.repository.RoleRepository;
import com.triasoft.garage.repository.UserProfileRepository;
import com.triasoft.garage.repository.UserRoleRepository;
import com.triasoft.garage.util.UserUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoleService {

    private static final String SUPERADMIN_CODE = "SUPERADMIN";

    private final RoleRepository roleRepository;
    private final FndModuleRepository moduleRepository;
    private final ResourceRepository resourceRepository;
    private final RolePrivilegeRepository rolePrivilegeRepository;
    private final UserRoleRepository userRoleRepository;
    private final UserProfileRepository userProfileRepository;
    private final PrivilegeCache privilegeCache;

    public RolesRs getAll() {
        return RolesRs.builder().roles(roleRepository.findAll().stream().filter(r -> !"SUPERADMIN".equalsIgnoreCase(r.getCode())).map(this::toDTO).toList()).build();
    }

    @Transactional
    public RoleRs create(RoleRq rq) {
        String code = rq.getCode().trim().toUpperCase();
        if (roleRepository.findByCodeIgnoreCase(code) != null) {
            throw new BusinessException(ErrorCode.Business.ROLE_CODE_EXISTS);
        }
        Role role = new Role();
        role.setCode(code);
        role.setName(rq.getName());
        role.setDescription(rq.getDescription());
        role.setSystem(false);
        roleRepository.save(role);
        return RoleRs.builder().id(role.getId()).build();
    }

    @Transactional
    @VersionCheck(entity = Role.class)
    public RoleRs update(Long id, RoleRq rq) {
        Role role = findById(id);
        if (role.isSystem()) {
            throw new BusinessException(ErrorCode.Business.ROLE_SYSTEM_PROTECTED);
        }
        String code = rq.getCode().trim().toUpperCase();
        if (!role.getCode().equalsIgnoreCase(code) && roleRepository.findByCodeIgnoreCase(code) != null) {
            throw new BusinessException(ErrorCode.Business.ROLE_CODE_EXISTS);
        }
        role.setCode(code);
        role.setName(rq.getName());
        role.setDescription(rq.getDescription());
        roleRepository.save(role);
        return RoleRs.builder().id(role.getId()).build();
    }

    @Transactional
    public void delete(Long id) {
        Role role = findById(id);
        if (role.isSystem()) {
            throw new BusinessException(ErrorCode.Business.ROLE_SYSTEM_PROTECTED);
        }
        rolePrivilegeRepository.deleteByRoleId(id);
        userRoleRepository.deleteByRoleId(id);
        roleRepository.delete(role);
        privilegeCache.refresh();
    }

    public ResourceTreeRs getResourceTree() {
        List<ModuleDTO> modules = moduleRepository.findByActiveTrue().stream()
                .sorted(Comparator.comparing(FndModule::getSortOrder, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(this::toDTO)
                .toList();
        return ResourceTreeRs.builder().modules(modules).build();
    }

    public RolePrivilegeRs getPrivileges(Long roleId) {
        findById(roleId);
        Map<Long, List<RolePrivilege>> byResource = rolePrivilegeRepository.findByRoleId(roleId).stream()
                .collect(Collectors.groupingBy(RolePrivilege::getResourceId));
        List<PrivilegeGrantDTO> grants = byResource.entrySet().stream()
                .map(entry -> {
                    Resource resource = resourceRepository.findById(entry.getKey()).orElse(null);
                    return PrivilegeGrantDTO.builder()
                            .resourceId(entry.getKey())
                            .resourceCode(resource != null ? resource.getCode() : null)
                            .privileges(entry.getValue().stream().map(RolePrivilege::getPrivilege).toList())
                            .build();
                })
                .toList();
        return RolePrivilegeRs.builder().grants(grants).build();
    }

    @Transactional
    public RolePrivilegeRs updatePrivileges(Long roleId, RolePrivilegeRq rq) {
        Role role = findById(roleId);
        if (SUPERADMIN_CODE.equals(role.getCode())) {
            throw new BusinessException(ErrorCode.Business.ROLE_SYSTEM_PROTECTED);
        }
        rolePrivilegeRepository.deleteByRoleId(roleId);
        rolePrivilegeRepository.flush();
        List<RolePrivilege> toSave = new ArrayList<>();
        for (PrivilegeGrantRq grant : rq.getGrants()) {
            Resource resource = resourceRepository.findById(grant.getResourceId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.Business.RESOURCE_NOT_FOUND));
            for (var privilege : grant.getPrivileges()) {
                RolePrivilege rp = new RolePrivilege();
                rp.setRoleId(roleId);
                rp.setResourceId(resource.getId());
                rp.setPrivilege(privilege);
                toSave.add(rp);
            }
        }
        rolePrivilegeRepository.saveAll(toSave);
        privilegeCache.refresh();
        return getPrivileges(roleId);
    }

    public UserRoleRs getUserRoles(Long userId) {
        findUserById(userId);
        List<Long> roleIds = userRoleRepository.findByUserId(userId).stream().map(UserRole::getRoleId).toList();
        return UserRoleRs.builder().roles(roleRepository.findAllById(roleIds).stream().map(this::toDTO).toList()).build();
    }

    @Transactional
    public UserRoleRs assignUserRoles(Long userId, UserRoleRq rq) {
        findUserById(userId);
        List<Role> roles = roleRepository.findAllById(rq.getRoleIds());
        if (roles.size() != rq.getRoleIds().size()) {
            throw new BusinessException(ErrorCode.Business.ROLE_NOT_FOUND);
        }
        userRoleRepository.deleteAll(userRoleRepository.findByUserId(userId));
        userRoleRepository.flush();
        List<UserRole> toSave = roles.stream().map(role -> {
            UserRole ur = new UserRole();
            ur.setUserId(userId);
            ur.setRoleId(role.getId());
            return ur;
        }).toList();
        userRoleRepository.saveAll(toSave);
        return UserRoleRs.builder().roles(roles.stream().map(this::toDTO).toList()).build();
    }

    public MyPermissionsRs getMyPermissions() {
        UserDTO user = UserUtil.getUser();
        List<String> roles = user != null && user.getRoles() != null ? user.getRoles() : List.of();
        boolean superAdmin = roles.contains(SUPERADMIN_CODE);
        Map<String, Set<Privilege>> resolved = superAdmin ? Map.of() : privilegeCache.resolve(roles);
        List<PrivilegeGrantDTO> permissions = resolved.entrySet().stream()
                .map(entry -> {
                    Resource resource = resourceRepository.findByCodeIgnoreCase(entry.getKey());
                    return PrivilegeGrantDTO.builder()
                            .resourceId(resource != null ? resource.getId() : null)
                            .resourceCode(entry.getKey())
                            .privileges(entry.getValue().stream().toList())
                            .build();
                })
                .toList();
        return MyPermissionsRs.builder().superAdmin(superAdmin).roles(roles).permissions(permissions).build();
    }

    private Role findById(Long id) {
        return roleRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.Business.ROLE_NOT_FOUND));
    }

    private UserProfile findUserById(Long userId) {
        return userProfileRepository.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.Business.USER_NOT_FOUND));
    }

    private RoleDTO toDTO(Role role) {
        return RoleDTO.builder()
                .id(role.getId())
                .code(role.getCode())
                .name(role.getName())
                .description(role.getDescription())
                .system(role.isSystem())
                .version(role.getVersion())
                .build();
    }

    private ModuleDTO toDTO(FndModule module) {
        return ModuleDTO.builder()
                .id(module.getId())
                .code(module.getCode())
                .description(module.getDescription())
                .resources(resourceRepository.findByModuleIdAndActiveTrue(module.getId()).stream()
                        .map(this::toDTO)
                        .toList())
                .build();
    }

    private ResourceDTO toDTO(Resource resource) {
        return ResourceDTO.builder().id(resource.getId()).code(resource.getCode()).description(resource.getDescription()).build();
    }
}
