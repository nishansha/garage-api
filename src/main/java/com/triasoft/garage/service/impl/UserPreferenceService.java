package com.triasoft.garage.service.impl;

import com.triasoft.garage.constants.NavbarPosition;
import com.triasoft.garage.constants.PreferredLanguage;
import com.triasoft.garage.constants.ThemePreference;
import com.triasoft.garage.entity.UserPreference;
import com.triasoft.garage.model.user.UserPreferenceRq;
import com.triasoft.garage.model.user.UserPreferenceRs;
import com.triasoft.garage.repository.UserPreferenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserPreferenceService {

    private static final PreferredLanguage DEFAULT_LANGUAGE = PreferredLanguage.EN;
    private static final ThemePreference DEFAULT_THEME = ThemePreference.LIGHT;
    private static final NavbarPosition DEFAULT_NAVBAR_POSITION = NavbarPosition.LEFT;

    private final UserPreferenceRepository userPreferenceRepository;

    public UserPreferenceRs get(Long userId) {
        return userPreferenceRepository.findByUserId(userId)
                .map(this::toRs)
                .orElseGet(() -> UserPreferenceRs.builder()
                        .userId(userId)
                        .language(DEFAULT_LANGUAGE)
                        .theme(DEFAULT_THEME)
                        .navbarPosition(DEFAULT_NAVBAR_POSITION)
                        .build());
    }

    @Transactional
    public UserPreferenceRs update(Long userId, UserPreferenceRq rq) {
        UserPreference preference = userPreferenceRepository.findByUserId(userId)
                .orElseGet(() -> newDefault(userId));

        if (rq.getLanguage() != null) {
            preference.setLanguage(rq.getLanguage());
        }
        if (rq.getTheme() != null) {
            preference.setTheme(rq.getTheme());
        }
        if (rq.getNavbarPosition() != null) {
            preference.setNavbarPosition(rq.getNavbarPosition());
        }
        preference.setModifiedAt(LocalDateTime.now());

        return toRs(userPreferenceRepository.save(preference));
    }

    private UserPreference newDefault(Long userId) {
        UserPreference preference = new UserPreference();
        preference.setUserId(userId);
        preference.setLanguage(DEFAULT_LANGUAGE);
        preference.setTheme(DEFAULT_THEME);
        preference.setNavbarPosition(DEFAULT_NAVBAR_POSITION);
        preference.setCreatedAt(LocalDateTime.now());
        return preference;
    }

    private UserPreferenceRs toRs(UserPreference preference) {
        return UserPreferenceRs.builder()
                .userId(preference.getUserId())
                .language(preference.getLanguage())
                .theme(preference.getTheme())
                .navbarPosition(preference.getNavbarPosition())
                .build();
    }
}
