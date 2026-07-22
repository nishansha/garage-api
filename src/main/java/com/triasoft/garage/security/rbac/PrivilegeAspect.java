package com.triasoft.garage.security.rbac;

import com.triasoft.garage.constants.ErrorCode;
import com.triasoft.garage.dto.UserDTO;
import com.triasoft.garage.exception.SecurityException;
import com.triasoft.garage.util.UserUtil;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Enforces {@link HasPrivilege} on annotated methods. Runs before the
 * target method: reads the caller's roles off the current UserDTO (resolved
 * from JWT claims by UserUtil) and checks them against {@link PrivilegeCache}.
 * SUPERADMIN bypasses the check entirely.
 */
@Aspect
@Component
@RequiredArgsConstructor
public class PrivilegeAspect {

    private static final String SUPERADMIN = "SUPERADMIN";

    private final PrivilegeCache privilegeCache;

    @Before("@annotation(hasPrivilege)")
    public void check(HasPrivilege hasPrivilege) {
        UserDTO user = UserUtil.getUser();
        List<String> roles = user != null && user.getRoles() != null ? user.getRoles() : List.of();
        if (roles.contains(SUPERADMIN)) {
            return;
        }
        if (!privilegeCache.isGranted(roles, hasPrivilege.resource(), hasPrivilege.privilege())) {
            throw new SecurityException(ErrorCode.Security.FORBIDDEN);
        }
    }
}
