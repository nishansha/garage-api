package com.triasoft.garage.filter;

import com.triasoft.garage.constants.ErrorCode;
import com.triasoft.garage.security.AuthEntryPoint;
import com.triasoft.garage.security.system.SystemPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

/**
 * Gates the tenant-provisioning endpoints (/api/v1/internal/tenants/**), which are called
 * before any tenant/user exists to authenticate against - JwtFilter has nothing to validate
 * there. A matching X-Internal-Api-Key header authenticates the request as SystemPrincipal;
 * a missing/wrong key leaves the request unauthenticated so the normal
 * anyRequest().authenticated() + AuthEntryPoint path rejects it with a 401, same as JwtFilter
 * does for a bad JWT (see AuthEntryPoint.AUTH_ERROR_ATTR).
 */
@Slf4j
@Component
public class InternalApiKeyFilter extends OncePerRequestFilter {

    private static final String HEADER = "X-Internal-Api-Key";
    private static final String PROTECTED_PREFIX = "/api/v1/internal/tenants";

    @Value("${app.internal-api-key}")
    private String configuredKey;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) throws ServletException, IOException {
        if (request.getRequestURI().startsWith(PROTECTED_PREFIX)) {
            String providedKey = request.getHeader(HEADER);
            if (providedKey != null && constantTimeEquals(providedKey, configuredKey)) {
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(SystemPrincipal.INSTANCE, null, List.of());
                SecurityContextHolder.getContext().setAuthentication(authToken);
            } else {
                request.setAttribute(AuthEntryPoint.AUTH_ERROR_ATTR, ErrorCode.Security.INVALID_ACCESS);
            }
        }
        filterChain.doFilter(request, response);
    }

    private boolean constantTimeEquals(String provided, String configured) {
        return configured != null
                && MessageDigest.isEqual(provided.getBytes(StandardCharsets.UTF_8), configured.getBytes(StandardCharsets.UTF_8));
    }
}
