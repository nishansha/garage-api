package com.triasoft.garage.service.impl;

import com.triasoft.garage.constants.AppConfig;
import com.triasoft.garage.constants.ClientChannel;
import com.triasoft.garage.constants.ErrorCode;
import com.triasoft.garage.constants.SessionStatusEnum;
import com.triasoft.garage.dto.UserDTO;
import com.triasoft.garage.entity.UserRefreshToken;
import com.triasoft.garage.entity.UserSession;
import com.triasoft.garage.exception.SecurityException;
import com.triasoft.garage.model.login.LoginRq;
import com.triasoft.garage.model.login.LoginRs;
import com.triasoft.garage.model.login.RefreshRq;
import com.triasoft.garage.repository.UserRefreshTokenRepository;
import com.triasoft.garage.repository.UserSessionRepository;
import com.triasoft.garage.util.EncryptionUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;
    private final UserService userService;
    private final EncryptionUtil encryptionUtil;
    private final UserDetailsService userDetailsService;
    private final UserRefreshTokenRepository refreshTokenRepository;
    private final UserSessionRepository sessionRepository;
    private final AppConfigurationService appConfigurationService;

    @Transactional
    public LoginRs login(LoginRq request, ClientChannel channel) {
        String password = encryptionUtil.decrypt(request.getPassword());
        Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.getUsername(), password));
        UserDetails userPrincipal = (UserDetails) authentication.getPrincipal();
        UserDTO user = userService.loadUser(userPrincipal.getUsername());
        if (!appConfigurationService.isFlagEnabled(AppConfig.CONCURRENT_LOGIN_ALLOWED, channel)) {
            revokeSessionsAndTokensForUser(user.getId());
        }
        String sessionId = createSession(user.getId());
        return issueTokens(userPrincipal, user, null, sessionId);
    }

    @Transactional
    public LoginRs refresh(RefreshRq request) {
        String tokenHash = tokenService.hashToken(request.getRefreshToken());
        UserRefreshToken currentRefreshToken = refreshTokenRepository.findByTokenHash(tokenHash).orElseThrow(() -> new SecurityException(ErrorCode.Security.INVALID_TOKEN));

        if (currentRefreshToken.isRevoked()) {
            revokeSessionsAndTokensForUser(currentRefreshToken.getUserId());
            throw new SecurityException(ErrorCode.Security.REFRESH_TOKEN_REUSED);
        }

        if (currentRefreshToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new SecurityException(ErrorCode.Security.TOKEN_EXPIRED);
        }

        String sessionId = resolveActiveSession(currentRefreshToken);
        currentRefreshToken.setRevoked(true);
        currentRefreshToken.setRevokedAt(LocalDateTime.now());
        refreshTokenRepository.save(currentRefreshToken);

        UserDTO user = userService.loadUser(userService.get(currentRefreshToken.getUserId(), null).getUserName());
        UserDetails userPrincipal = userDetailsService.loadUserByUsername(user.getUserName());
        return issueTokens(userPrincipal, user, currentRefreshToken.getId(), sessionId);
    }

    @Transactional
    public void logout(RefreshRq request) {
        String tokenHash = tokenService.hashToken(request.getRefreshToken());
        refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(token -> {
            if (!token.isRevoked()) {
                token.setRevoked(true);
                token.setRevokedAt(LocalDateTime.now());
                refreshTokenRepository.save(token);
            }
            endSession(token.getSessionId(), SessionStatusEnum.LOGGED_OUT);
        });
    }

    private LoginRs issueTokens(UserDetails userPrincipal, UserDTO user, Long parentId, String sessionId) {
        String accessToken = tokenService.generateToken(userPrincipal, user, sessionId);
        String refreshToken = tokenService.generateRefreshToken();

        LocalDateTime now = LocalDateTime.now();
        UserRefreshToken entity = new UserRefreshToken();
        entity.setUserId(user.getId());
        entity.setSessionId(sessionId);
        entity.setTokenHash(tokenService.hashToken(refreshToken));
        entity.setParentId(parentId);
        entity.setRevoked(false);
        entity.setCreatedAt(now);
        entity.setExpiresAt(now.plus(tokenService.getRefreshExpirationMs(), ChronoUnit.MILLIS));
        refreshTokenRepository.save(entity);

        LoginRs rs = new LoginRs();
        rs.setToken(accessToken);
        rs.setRefreshToken(refreshToken);
        rs.setFullName(user.getName());
        rs.setRole(user.getRole());
        return rs;
    }


    private String createSession(Long userId) {
        UserSession session = new UserSession();
        session.setSessionId(UUID.randomUUID().toString());
        session.setUserId(userId);
        session.setStatus(SessionStatusEnum.ACTIVE);
        session.setLoginAt(LocalDateTime.now());
        return sessionRepository.save(session).getSessionId();
    }

    private String resolveActiveSession(UserRefreshToken currentRefreshToken) {
        String sessionId = currentRefreshToken.getSessionId();
        if (sessionId == null) {
            throw new SecurityException(ErrorCode.Security.INVALID_TOKEN);
        }
        UserSession session = sessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new SecurityException(ErrorCode.Security.INVALID_TOKEN));
        if (session.getStatus() != SessionStatusEnum.ACTIVE) {
            throw new SecurityException(ErrorCode.Security.SESSION_TERMINATED);
        }
        return sessionId;
    }

    private void endSession(String sessionId, SessionStatusEnum status) {
        if (sessionId == null) {
            return;
        }
        sessionRepository.findBySessionId(sessionId).ifPresent(session -> {
            if (session.getStatus() == SessionStatusEnum.ACTIVE) {
                session.setStatus(status);
                session.setEndedAt(LocalDateTime.now());
                sessionRepository.save(session);
            }
        });
    }

    /**
     * Revokes every active session and refresh token for the user.For
     * single-device login and to kick the user out on refresh-token reuse.
     */
    private void revokeSessionsAndTokensForUser(Long userId) {
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
