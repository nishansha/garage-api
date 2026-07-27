package com.triasoft.garage.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;

/**
 * Carries a plain (non-discriminator) tenant_id column for entities that must be resolvable
 * before a request's tenant is known: identity/session bootstrapping runs against a bare
 * username, a refresh-token hash, or a session id, none of which imply a tenant up front.
 * Hibernate's {@code @TenantId} (see {@link TenantAwareEntity}) would silently return zero
 * rows for these lookups, since it filters every query by whatever tenant is currently
 * resolvable - which is nothing, at exactly the moment identity is still being established.
 * tenant_id here is set explicitly by application code instead of relying on the discriminator.
 */
@Getter
@Setter
@MappedSuperclass
public class TenantScopedEntity extends GenericEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private Long tenantId;
}
