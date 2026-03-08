package com.triasoft.garage.service.impl;

import com.triasoft.garage.constants.ErrorCode;
import com.triasoft.garage.dto.UserDTO;
import com.triasoft.garage.entity.UserProfile;
import com.triasoft.garage.exception.BusinessException;
import com.triasoft.garage.model.user.UserRq;
import com.triasoft.garage.model.user.UserRs;
import com.triasoft.garage.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserProfileRepository userProfileRepository;

    public UserDTO loadUser(String userName) {
        UserProfile userProfile = userProfileRepository.findByUsername(userName);
        return UserDTO.builder().id(userProfile.getId()).userName(userProfile.getUsername()).role(userProfile.getRole()).name(userProfile.getName()).build();
    }

    public UserRs getStaffs(UserDTO user) {
        List<UserProfile> staffs = userProfileRepository.findByRole("STAFF");
        List<UserDTO> users = staffs.stream().map(staff -> UserDTO.builder().id(staff.getId()).userName(staff.getName()).build()).toList();
        return UserRs.builder().users(users).build();
    }

    public UserRs create(UserRq userRq, UserDTO user) {
        UserProfile userProfile = userProfileRepository.findByUsername(userRq.getUserName());
        if (Objects.nonNull(userProfile)) {
            throw new BusinessException(ErrorCode.Business.USER_EXISTS);
        }
        UserProfile newUser = new UserProfile();
        newUser.setRole(userRq.getRole());
        newUser.setName(userRq.getName());
        newUser.setUsername(userRq.getUserName().trim());
        newUser.setPassword(new BCryptPasswordEncoder().encode(userRq.getPassword().trim()));
        newUser.setDesignation(userRq.getDesignation());
        userProfileRepository.save(newUser);
        return UserRs.builder().build();
    }

    public UserRs update(Long id, UserRq userRq, UserDTO user) {
        UserProfile userProfile = userProfileRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.Business.USER_NOT_FOUND));
        if(StringUtils.hasLength(userRq.getPassword()))
            userProfile.setPassword(new BCryptPasswordEncoder().encode(userRq.getPassword().trim()));
        userProfile.setName(userRq.getName());
        userProfile.setRole(userRq.getRole());
        userProfile.setDesignation(userRq.getDesignation());
        userProfileRepository.save(userProfile);
        return UserRs.builder().build();
    }

    public UserDTO get(Long id, UserDTO user) {
        UserProfile userProfile = userProfileRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.Business.USER_NOT_FOUND));
        return UserDTO.builder().id(userProfile.getId())
                .userName(userProfile.getUsername())
                .name(userProfile.getName())
                .role(userProfile.getRole())
                .designation(userProfile.getDesignation())
                .build();
    }

    public UserRs getAll(UserDTO user) {
        List<UserProfile> userProfiles = userProfileRepository.findAll();
        return UserRs.builder().users(userProfiles.stream().map(userProfile -> UserDTO.builder()
                        .id(userProfile.getId())
                        .userName(userProfile.getUsername())
                        .name(userProfile.getName())
                        .role(userProfile.getRole())
                        .designation(userProfile.getDesignation())
                        .build()).toList())
                .build();
    }
}
