package com.triasoft.garage.filter;

import com.triasoft.garage.constants.ErrorCode;
import com.triasoft.garage.constants.SessionStatusEnum;
import com.triasoft.garage.repository.UserSessionRepository;
import com.triasoft.garage.security.AuthEntryPoint;
import com.triasoft.garage.service.impl.TokenService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final TokenService jwtService;
    private final UserDetailsService userDetailsService;
    private final UserSessionRepository sessionRepository;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String username;

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }
        jwt = authHeader.substring(7);

        try {
            username = jwtService.extractUsername(jwt);
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);
                if (jwtService.isTokenValid(jwt, userDetails)) {
                    if (isSessionActive(jwt)) {
                        Claims claims = jwtService.extractAllClaims(jwt);
                        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(claims, null, userDetails.getAuthorities());
                        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                    } else {
                        request.setAttribute(AuthEntryPoint.AUTH_ERROR_ATTR, ErrorCode.Security.SESSION_TERMINATED);
                    }
                }
            }
            filterChain.doFilter(request, response);
        } catch (ExpiredJwtException e) {
            // Access token expired: the FE should attempt a token refresh.
            request.setAttribute(AuthEntryPoint.AUTH_ERROR_ATTR, ErrorCode.Security.TOKEN_EXPIRED);
            filterChain.doFilter(request, response);
        } catch (Exception e) {
            logger.warn("JWT token processing error: " + e.getMessage());
            request.setAttribute(AuthEntryPoint.AUTH_ERROR_ATTR, ErrorCode.Security.INVALID_TOKEN);
            filterChain.doFilter(request, response);
        }
    }

    private boolean isSessionActive(String jwt) {
        String sessionId = jwtService.extractSessionId(jwt);
        if (sessionId == null) {
            return false;
        }
        return sessionRepository.existsBySessionIdAndStatus(sessionId, SessionStatusEnum.ACTIVE);
    }
}