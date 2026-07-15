package com.triasoft.garage.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.triasoft.garage.dto.UserDTO;
import com.triasoft.garage.entity.AppRevisionEntity;
import io.jsonwebtoken.Claims;
import org.hibernate.envers.RevisionListener;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Populates {@link AppRevisionEntity#getUserId()} on every new Envers revision.
 *
 * <p>Envers instantiates this listener itself (not Spring), so the current user is resolved
 * statically from the {@link SecurityContextHolder}, mirroring {@link AuditAware}. Resolution is
 * best-effort: for unauthenticated flows (login, token refresh, scheduled jobs) the user id is left
 * null rather than failing the transaction.
 */
public class AppRevisionListener implements RevisionListener {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public void newRevision(Object revisionEntity) {
        ((AppRevisionEntity) revisionEntity).setUserId(currentUserId());
    }

    private Long currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Claims claims)) {
            return null;
        }
        try {
            String userJson = claims.get("user", String.class);
            if (userJson == null) {
                return null;
            }
            UserDTO userDTO = OBJECT_MAPPER.readValue(userJson, UserDTO.class);
            return userDTO.getId();
        } catch (Exception e) {
            // Never fail the business transaction because auditing metadata could not be resolved.
            return null;
        }
    }
}
