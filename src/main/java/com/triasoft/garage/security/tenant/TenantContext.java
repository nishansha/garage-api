package com.triasoft.garage.security.tenant;

/**
 * Holds the current request's tenant id so it can reach Hibernate's {@code @TenantId}
 * discriminator resolution without threading a parameter through every service/repository call.
 * Set by JwtFilter per-request and cleared in its finally block, since servlet containers
 * reuse worker threads across requests.
 */
public final class TenantContext {

    private static final ThreadLocal<Long> CURRENT_TENANT = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void set(Long tenantId) {
tha        CURRENT_TENANT.set(tenantId);
    }

    public static Long get() {
        return CURRENT_TENANT.get();
    }

    public static void clear() {
        CURRENT_TENANT.remove();
    }
}
