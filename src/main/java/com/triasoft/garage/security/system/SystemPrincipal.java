package com.triasoft.garage.security.system;

/**
 * Marks requests authenticated via a pre-shared internal API key rather than a user JWT
 * (e.g. tenant-provisioning endpoints called before any tenant/user exists). AuditAware
 * resolves this to a fixed system auditor id instead of pulling a user id out of JWT claims.
 */
public final class SystemPrincipal {

    public static final long SYSTEM_USER_ID = 0L;

    public static final SystemPrincipal INSTANCE = new SystemPrincipal();

    private SystemPrincipal() {
    }
}
