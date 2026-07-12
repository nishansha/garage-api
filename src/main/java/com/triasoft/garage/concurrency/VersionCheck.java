package com.triasoft.garage.concurrency;

import com.triasoft.garage.entity.AuditGenericEntity;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an update method for optimistic-lock stale-edit protection. Before the
 * method runs, {@code VersionCheckAspect} compares the client-supplied version
 * (from a {@link Versioned} argument) against the current DB version of
 * {@link #entity()} identified by the method argument at {@link #idIndex()}.
 * A missing version is rejected (strict); a mismatch raises a 409.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface VersionCheck {

    /**
     * Entity whose version is guarded.
     */
    Class<? extends AuditGenericEntity> entity();

    /**
     * Index of the method argument holding the entity id (defaults to the first).
     */
    int idIndex() default 0;
}
