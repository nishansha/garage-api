package com.triasoft.garage.entity;

import com.triasoft.garage.constants.NavbarPosition;
import com.triasoft.garage.constants.PreferredLanguage;
import com.triasoft.garage.constants.ThemePreference;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;

import java.io.Serial;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@DynamicUpdate
@Table(name = "user_preference")
public class UserPreference extends GenericEntity {

    @Serial
    private static final long serialVersionUID = -1023841028410238410L;

    @Column(name = "user_id", nullable = false, unique = true, updatable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "language", nullable = false)
    private PreferredLanguage language;

    @Enumerated(EnumType.STRING)
    @Column(name = "theme", nullable = false)
    private ThemePreference theme;

    @Enumerated(EnumType.STRING)
    @Column(name = "navbar_position", nullable = false)
    private NavbarPosition navbarPosition;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "modified_at")
    private LocalDateTime modifiedAt;

}
