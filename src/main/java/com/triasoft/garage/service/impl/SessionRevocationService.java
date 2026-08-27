package com.triasoft.garage.service.impl;

import com.triasoft.garage.constants.SessionStatusEnum;
import com.triasoft.garage.entity.UserRefreshToken;
import com.triasoft.garage.entity.UserSession;
import com.triasoft.garage.repository.UserRefreshTokenRepository;
import com.triasoft.garage.repository.UserSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SessionRevocationService {

    private final UserSessionRepository sessionRepository;
    private final UserRefreshTokenRepository refreshTokenRepository;

    /**
     * Revokes every active session and refresh token for the user. Used for single-device
     * login, refresh-token reuse detection, and tenant deactivation.
     */
    @Transactional
    public void revokeSessionsAndTokensForUser(Long userId) {
        LocalDateTime now = LocalDateTime.now();

        List<UserSession> activeSessions = sessionRepository.findByUserIdAndStatus(userId, SessionStatusEnum.ACTIVE);
        activeSessions.forEach(session -> {
            session.setStatus(SessionStatusEnum.REVOKED);
            session.setEndedAt(now);
        });
        sessionRepository.saveAll(activeSessions);

        List<UserRefreshToken> activeTokens = refreshTokenRepository.findByUserIdAndRevokedFalse(userId);
        activeTokens.forEach(token -> {
            token.setRevoked(true);
            token.setRevokedAt(now);
        });
        refreshTokenRepository.saveAll(activeTokens);
    }
}
