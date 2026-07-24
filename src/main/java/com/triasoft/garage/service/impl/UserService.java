package com.triasoft.garage.service.impl;

import com.triasoft.garage.constants.ErrorCode;
import com.triasoft.garage.dto.UserDTO;
import com.triasoft.garage.entity.UserProfile;
import com.triasoft.garage.exception.BusinessException;
import com.triasoft.garage.model.user.UserRoleRq;
import com.triasoft.garage.model.user.UserRq;
import com.triasoft.garage.model.user.UserRs;
import com.triasoft.garage.repository.UserProfileRepository;
import com.triasoft.garage.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserProfileRepository userProfileRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleService roleService;
    private final PasswordEncoder passwordEncoder;

    public UserDTO loadUser(String userName) {
        UserProfile userProfile = userProfileRepository.findByUsername(userName);
        return toDTO(userProfile);
    }

    public UserRs getStaffs(UserDTO user) {
        List<UserProfile> staffs = userProfileRepository.findAll();
        List<UserDTO> users = staffs.stream()
                .filter(usr -> !"SUPERADMIN".equalsIgnoreCase(user.getRole()))
                .map(staff -> UserDTO.builder().id(staff.getId()).userName(staff.getName()).build()).toList();
        return UserRs.builder().users(users).build();
    }

    @Transactional
    public UserRs create(UserRq userRq, UserDTO user) {
        UserProfile userProfile = userProfileRepository.findByUsername(userRq.getUserName());
        if (Objects.nonNull(userProfile)) {
            throw new BusinessException(ErrorCode.Business.USER_EXISTS);
        }
        if (userRq.getRoleIds() == null || userRq.getRoleIds().isEmpty()) {
            throw new BusinessException(ErrorCode.Business.ROLE_REQUIRED);
        }
        UserProfile newUser = new UserProfile();
        newUser.setName(userRq.getName());
        newUser.setUsername(userRq.getUserName().trim());
        newUser.setPassword(passwordEncoder.encode(userRq.getPassword().trim()));
        newUser.setDesignation(userRq.getDesignation());
        userProfileRepository.save(newUser);
        roleService.assignUserRoles(newUser.getId(), UserRoleRq.builder().roleIds(userRq.getRoleIds()).build());
        return UserRs.builder().build();
    }

    @Transactional
    public UserRs update(Long id, UserRq userRq, UserDTO user) {
        UserProfile userProfile = userProfileRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.Business.USER_NOT_FOUND));
        if (StringUtils.hasLength(userRq.getPassword()))
            userProfile.setPassword(passwordEncoder.encode(userRq.getPassword().trim()));
        userProfile.setName(userRq.getName());
        userProfile.setDesignation(userRq.getDesignation());
        userProfileRepository.save(userProfile);
        if (userRq.getRoleIds() != null) {
            roleService.assignUserRoles(id, UserRoleRq.builder().roleIds(userRq.getRoleIds()).build());
        }
        return UserRs.builder().build();
    }

    public UserDTO get(Long id, UserDTO user) {
        UserProfile userProfile = userProfileRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.Business.USER_NOT_FOUND));
        return toDTO(userProfile);
    }

    public UserRs getAll(UserDTO user) {
        List<UserProfile> userProfiles = userProfileRepository.findAll();
        return UserRs.builder().users(userProfiles.stream()
                .filter(usr -> !"SUPERADMIN".equalsIgnoreCase(usr.getRole()))
                .map(this::toDTO).toList())
                .build();
    }

    private UserDTO toDTO(UserProfile userProfile) {
        return UserDTO.builder()
                .id(userProfile.getId())
                .userName(userProfile.getUsername())
                .name(userProfile.getName())
                .role(userProfile.getRole())
                .designation(userProfile.getDesignation())
                .roles(resolveRoles(userProfile))
                .build();
    }

    private List<String> resolveRoles(UserProfile userProfile) {
        List<String> roles = userRoleRepository.findRoleCodesByUserId(userProfile.getId());
        if (roles.isEmpty() && StringUtils.hasLength(userProfile.getRole())) {
            return List.of(userProfile.getRole().toUpperCase());
        }
        return roles;
    }
}
