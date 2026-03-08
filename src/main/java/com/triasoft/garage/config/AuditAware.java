package com.triasoft.garage.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.triasoft.garage.constants.ErrorCode;
import com.triasoft.garage.dto.UserDTO;
import com.triasoft.garage.exception.BusinessException;
import io.jsonwebtoken.Claims;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

public class AuditAware implements AuditorAware<Long> {

    @Override
    public Optional<Long> getCurrentAuditor() {
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .filter(auth -> auth.getPrincipal() instanceof Claims)
                .map(auth -> {
                    Claims claims = (Claims) auth.getPrincipal();
                    String userJson = claims.get("user", String.class);
                    UserDTO userDTO = null;
                    try {
                        userDTO = new ObjectMapper().readValue(userJson, UserDTO.class);
                    } catch (JsonProcessingException e) {
                        throw new BusinessException(ErrorCode.Security.INVALID_TOKEN);
                    }
                    return userDTO.getId();
                });
    }
}