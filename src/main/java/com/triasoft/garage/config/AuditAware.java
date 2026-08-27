package com.triasoft.garage.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.triasoft.garage.constants.ErrorCode;
import com.triasoft.garage.dto.UserDTO;
import com.triasoft.garage.exception.BusinessException;
import com.triasoft.garage.security.system.SystemPrincipal;
import io.jsonwebtoken.Claims;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

public class AuditAware implements AuditorAware<Long> {

    @Override
    public Optional<Long> getCurrentAuditor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return Optional.empty();
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof SystemPrincipal) {
            return Optional.of(SystemPrincipal.SYSTEM_USER_ID);
        }
        if (principal instanceof Claims claims) {
            String userJson = claims.get("user", String.class);
            try {
                UserDTO userDTO = new ObjectMapper().readValue(userJson, UserDTO.class);
                return Optional.of(userDTO.getId());
            } catch (JsonProcessingException e) {
                throw new BusinessException(ErrorCode.Security.INVALID_TOKEN);
            }
        }
        return Optional.empty();
    }
}
