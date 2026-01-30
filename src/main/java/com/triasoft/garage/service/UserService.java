package com.triasoft.garage.service;

import com.triasoft.garage.dto.UserDTO;
import com.triasoft.garage.entity.UserProfile;
import com.triasoft.garage.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserProfileRepository userProfileRepository;

    public UserDTO loadUser(String userName){
        UserProfile userProfile = userProfileRepository.findByUsername(userName);
        return UserDTO.builder().id(userProfile.getId()).userName(userProfile.getUsername()).build();
    }
}
