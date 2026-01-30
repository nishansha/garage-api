package com.triasoft.garage.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.triasoft.garage.constants.ErrorCode;
import com.triasoft.garage.dto.UserDTO;
import com.triasoft.garage.exception.BusinessException;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class UserUtil {

    public static UserDTO getUser(HttpServletRequest request)  {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Claims claims) {
            String userJson = claims.get("user", String.class);
            try {
                return new ObjectMapper().readValue(userJson, UserDTO.class);
            } catch (JsonProcessingException e) {
                throw new BusinessException(ErrorCode.Security.INVALID_TOKEN);
            }
        }
        return null;
    }
}
