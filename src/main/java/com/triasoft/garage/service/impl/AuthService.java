package com.triasoft.garage.service.impl;

import com.triasoft.garage.constants.ErrorCode;
import com.triasoft.garage.dto.UserDTO;
import com.triasoft.garage.entity.UserRefreshToken;
import com.triasoft.garage.exception.BusinessException;
import com.triasoft.garage.model.login.LoginRq;
import com.triasoft.garage.model.login.LoginRs;
import com.triasoft.garage.model.login.RefreshRq;
import com.triasoft.garage.repository.UserRefreshTokenRepository;
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

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;
    private final UserService userService;
    private final EncryptionUtil encryptionUtil;
    private final UserDetailsService userDetailsService;
    private final UserRefreshTokenRepository refreshTokenRepository;

    @Transactional
    public LoginRs login(LoginRq request) {
        String password = encryptionUtil.decrypt(request.getPassword());
        Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.getUsername(), password));
        UserDetails userPrincipal = (UserDetails) authentication.getPrincipal();
        UserDTO user = userService.loadUser(userPrincipal.getUsername());
        return issueTokens(userPrincipal, user, null);
    }

    @Transactional
    public LoginRs refresh(RefreshRq request) {
        String tokenHash = tokenService.hashToken(request.getRefreshToken());
        UserRefreshToken currentRefreshToken = refreshTokenRepository.findByTokenHash(tokenHash).orElseThrow(() -> new BusinessException(ErrorCode.Security.INVALID_TOKEN));

        if (currentRefreshToken.isRevoked()) {
            revokeAllForUser(currentRefreshToken.getUserId());
            throw new BusinessException(ErrorCode.Security.REFRESH_TOKEN_REUSED);
        }

        if (currentRefreshToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.Security.TOKEN_EXPIRED);
        }

        currentRefreshToken.setRevoked(true);
        currentRefreshToken.setRevokedAt(LocalDateTime.now());
        refreshTokenRepository.save(currentRefreshToken);

        UserDTO user = userService.loadUser(userService.get(currentRefreshToken.getUserId(), null).getUserName());
        UserDetails userPrincipal = userDetailsService.loadUserByUsername(user.getUserName());
        return issueTokens(userPrincipal, user, currentRefreshToken.getId());
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
        });
    }

    private LoginRs issueTokens(UserDetails userPrincipal, UserDTO user, Long parentId) {
        String accessToken = tokenService.generateToken(userPrincipal, user);
        String refreshToken = tokenService.generateRefreshToken();

        LocalDateTime now = LocalDateTime.now();
        UserRefreshToken entity = new UserRefreshToken();
        entity.setUserId(user.getId());
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

    private void revokeAllForUser(Long userId) {
        List<UserRefreshToken> active = refreshTokenRepository.findByUserIdAndRevokedFalse(userId);
        LocalDateTime now = LocalDateTime.now();
        active.forEach(token -> {
            token.setRevoked(true);
            token.setRevokedAt(now);
        });
        refreshTokenRepository.saveAll(active);
    }
}
