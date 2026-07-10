package com.triasoft.garage.entity;

import com.triasoft.garage.constants.SessionStatusEnum;
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
@Table(name = "user_session")
public class UserSession extends GenericEntity {

    @Serial
    private static final long serialVersionUID = -5521092284401170841L;

    @Column(name = "session_id", nullable = false, unique = true, updatable = false)
    private String sessionId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SessionStatusEnum status;

    @Column(name = "login_at", nullable = false, updatable = false)
    private LocalDateTime loginAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

}
