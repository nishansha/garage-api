package com.triasoft.garage.locking;

import com.triasoft.garage.constants.ErrorCode;
import com.triasoft.garage.exception.BusinessException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Enforces optimistic-lock stale-edit protection on methods annotated with
 * {@link VersionCheck}. Runs before the target method: reads the client version
 * from a {@link Versioned} argument and compares it to the entity's current DB
 * version. Strict — a missing version is rejected, never silently bypassed.
 */
@Aspect
@Component
public class VersionCheckAspect {

    @PersistenceContext
    private EntityManager entityManager;

    @Before("@annotation(versionCheck)")
    public void check(JoinPoint joinPoint, VersionCheck versionCheck) {
        Long clientVersion = extractClientVersion(joinPoint.getArgs());
        if (clientVersion == null) {
            throw new BusinessException(ErrorCode.Concurrency.VERSION_REQUIRED);
        }

        Object idArg = joinPoint.getArgs()[versionCheck.idIndex()];
        if (!(idArg instanceof Long id)) {
            throw new IllegalStateException("@VersionCheck idIndex " + versionCheck.idIndex()
                    + " does not point to a Long id on " + joinPoint.getSignature());
        }

        List<Long> current = entityManager.createQuery(
                        "select e.version from " + versionCheck.entity().getSimpleName() + " e where e.id = :id", Long.class)
                .setParameter("id", id)
                .getResultList();
        if (current.isEmpty()) {
            // Entity doesn't exist — let the target method surface its own not-found error.
            return;
        }
        if (!clientVersion.equals(current.get(0))) {
            throw new ObjectOptimisticLockingFailureException(versionCheck.entity(), id);
        }
    }

    private Long extractClientVersion(Object[] args) {
        for (Object arg : args) {
            if (arg instanceof Versioned versioned) {
                return versioned.getVersion();
            }
        }
        return null;
    }
}
