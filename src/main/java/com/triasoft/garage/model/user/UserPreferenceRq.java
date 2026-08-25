package com.triasoft.garage.model.user;

import com.triasoft.garage.constants.NavbarPosition;
import com.triasoft.garage.constants.PreferredLanguage;
import com.triasoft.garage.constants.ThemePreference;
import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
@Builder
public class UserPreferenceRq implements Serializable {
    @Serial
    private static final long serialVersionUID = -8322104710239481023L;

    private PreferredLanguage language;
    private ThemePreference theme;
    private NavbarPosition navbarPosition;
}
