package com.triasoft.garage.rbac;

import com.triasoft.garage.constants.Privilege;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as requiring a specific privilege on a resource. Enforced by
 * {@link PrivilegeAspect} before the method runs, against the caller's roles
 * (from the JWT) and the role -> resource -> privilege grants in {@link PrivilegeCache}.
 * SUPERADMIN bypasses this check entirely; every other role, including ADMIN,
 * must have an explicit grant.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface RequiresPrivilege {

    /**
     * Code of the fnd_resource row being guarded, e.g. "PURCHASE_ORDER".
     */
    String resource();

    Privilege privilege();
}
