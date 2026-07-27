package com.triasoft.garage.service.impl;

import com.triasoft.garage.constants.ErrorCode;
import com.triasoft.garage.entity.AppRevisionEntity;
import com.triasoft.garage.entity.DirectEntry;
import com.triasoft.garage.entity.Expense;
import com.triasoft.garage.entity.Purchase;
import com.triasoft.garage.entity.PurchasePayment;
import com.triasoft.garage.entity.PurchaseReturn;
import com.triasoft.garage.entity.PurchaseReturnReceipt;
import com.triasoft.garage.entity.Sale;
import com.triasoft.garage.entity.SalePayment;
import com.triasoft.garage.entity.SaleRefundPayment;
import com.triasoft.garage.entity.SaleReturn;
import com.triasoft.garage.entity.SaleReturnDeduction;
import com.triasoft.garage.entity.UserProfile;
import com.triasoft.garage.exception.BusinessException;
import com.triasoft.garage.model.audit.AuditRevisionRs;
import com.triasoft.garage.model.audit.DeletedRecordRs;
import com.triasoft.garage.model.audit.FieldChange;
import com.triasoft.garage.repository.UserProfileRepository;
import com.triasoft.garage.security.tenant.TenantContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceUnitUtil;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.EntityType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.query.AuditEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Read-side access to the Envers audit trail for the financing entities. Reconstructs the revision
 * history (who / what / when) for an audited record and exposes point-in-time snapshots.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    /**
     * Whitelist of externally-addressable entity types. Guards the read API so callers can only
     * query the entities we actually audit, never an arbitrary class.
     */
    private static final Map<String, Class<?>> AUDITED_TYPES = Map.ofEntries(
            Map.entry("sale", Sale.class),
            Map.entry("sale-payment", SalePayment.class),
            Map.entry("sale-return", SaleReturn.class),
            Map.entry("sale-return-deduction", SaleReturnDeduction.class),
            Map.entry("sale-refund-payment", SaleRefundPayment.class),
            Map.entry("purchase", Purchase.class),
            Map.entry("purchase-payment", PurchasePayment.class),
            Map.entry("purchase-return", PurchaseReturn.class),
            Map.entry("purchase-return-receipt", PurchaseReturnReceipt.class),
            Map.entry("direct-entry", DirectEntry.class),
            Map.entry("expense", Expense.class)
    );

    /** Audit-metadata fields excluded from the changed-fields diff (still present in the snapshot). */
    private static final Set<String> DIFF_IGNORED_FIELDS =
            Set.of("createdAt", "createdBy", "modifiedAt", "modifiedBy", "version");

    /**
     * Base table per audited type, used only to look up the LIVE row's tenant_id for ownership
     * checks. Envers excludes {@code @TenantId} fields from the audit trail entirely (same
     * treatment as {@code @Version}) - the _aud tables have no tenant_id column - so historical
     * revision snapshots can never carry tenant info and the check must go against the base table.
     * A plain native query bypasses both the Hibernate tenant filter and the @SoftDelete filter,
     * so it finds the row regardless of tenant or delete status - the check itself does the
     * tenant comparison explicitly.
     */
    private static final Map<String, String> AUDITED_TABLE_NAMES = Map.ofEntries(
            Map.entry("sale", "app_sale"),
            Map.entry("sale-payment", "app_sale_payment"),
            Map.entry("sale-return", "app_sale_return"),
            Map.entry("sale-return-deduction", "app_sale_return_deduction"),
            Map.entry("sale-refund-payment", "app_sale_refund_payment"),
            Map.entry("purchase", "app_purchase_order"),
            Map.entry("purchase-payment", "app_purchase_payment"),
            Map.entry("purchase-return", "app_purchase_return"),
            Map.entry("purchase-return-receipt", "app_purchase_return_receipt"),
            Map.entry("direct-entry", "app_direct_entry"),
            Map.entry("expense", "app_expense")
    );

    @PersistenceContext
    private EntityManager entityManager;

    private final UserProfileRepository userProfileRepository;

    /**
     * Full revision history of a single audited record, oldest first, including deletions.
     */
    @Transactional(readOnly = true)
    public List<AuditRevisionRs> getHistory(String entityType, Long id) {
        Class<?> type = resolveType(entityType);
        assertOwnedByCurrentTenant(entityType, id);
        AuditReader reader = AuditReaderFactory.get(entityManager);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = reader.createQuery()
                .forRevisionsOfEntity(type, false, true)
                .add(AuditEntity.id().eq(id))
                .addOrder(AuditEntity.revisionNumber().asc())
                .getResultList();

        Map<Long, String> usernameCache = new HashMap<>();
        List<AuditRevisionRs> revisions = rows.stream()
                .map(row -> toRevision(row, usernameCache))
                .collect(Collectors.toCollection(ArrayList::new));

        // Second pass: diff each revision's snapshot against the previous one.
        for (int i = 1; i < revisions.size(); i++) {
            revisions.get(i).setChangedFields(
                    diff(stateOf(revisions.get(i - 1)), stateOf(revisions.get(i))));
        }
        return revisions;
    }

    /**
     * Soft-deleted records of the given type, most recently deleted first — the per-entity
     * "recycle bin". Reconstructed from each record's DEL revision so who/when-deleted come from the
     * audit trail (a soft delete does not touch the row's modified_by/at).
     */
    @Transactional(readOnly = true)
    public List<DeletedRecordRs> getDeleted(String entityType) {
        Class<?> type = resolveType(entityType);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = AuditReaderFactory.get(entityManager).createQuery()
                .forRevisionsOfEntity(type, false, true)
                .add(AuditEntity.revisionType().eq(RevisionType.DEL))
                .addOrder(AuditEntity.revisionNumber().desc())
                .getResultList();

        Map<Long, String> usernameCache = new HashMap<>();
        Set<Long> seen = new HashSet<>();
        List<DeletedRecordRs> deleted = new ArrayList<>();
        for (Object[] row : rows) {
            Map<String, Object> state = toState(row[0]);
            Long id = state == null ? null : (Long) state.get("id");
            if (id == null || !seen.add(id) || !Objects.equals(liveTenantId(entityType, id), TenantContext.get())) {
                continue; // keep only the latest DEL per id, and only this tenant's records
            }
            AppRevisionEntity rev = (AppRevisionEntity) row[1];
            deleted.add(DeletedRecordRs.builder()
                    .id(id)
                    .revision(rev.getRev())
                    .deletedBy(rev.getUserId())
                    .deletedByName(resolveUsername(rev.getUserId(), usernameCache))
                    .deletedAt(toLocalDateTime(rev.getTimestamp()))
                    .entity(state)
                    .build());
        }
        return deleted;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> stateOf(AuditRevisionRs revision) {
        return (Map<String, Object>) revision.getEntity();
    }

    private Map<String, FieldChange> diff(Map<String, Object> previous, Map<String, Object> current) {
        if (previous == null || current == null) {
            return null;
        }
        Map<String, FieldChange> changes = new LinkedHashMap<>();
        current.forEach((field, newValue) -> {
            if (DIFF_IGNORED_FIELDS.contains(field)) {
                return; // audit metadata is conveyed by changeType/username/revisionAt, not the diff
            }
            Object oldValue = previous.get(field);
            if (!Objects.equals(oldValue, newValue)) {
                changes.put(field, new FieldChange(oldValue, newValue));
            }
        });
        return changes;
    }

    /**
     * Snapshot of an audited record as it existed at a specific revision (null if it did not yet
     * exist / was already deleted at that revision).
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getAtRevision(String entityType, Long id, Long revision) {
        Class<?> type = resolveType(entityType);
        assertOwnedByCurrentTenant(entityType, id);
        return toState(AuditReaderFactory.get(entityManager).find(type, id, revision));
    }

    /**
     * Rejects a lookup whose record belongs to another tenant with the same "not found"-shaped
     * error used for an unknown entity type, so a cross-tenant probe can't distinguish
     * "wrong tenant" from "doesn't exist".
     */
    private void assertOwnedByCurrentTenant(String entityType, Long id) {
        if (!Objects.equals(liveTenantId(entityType, id), TenantContext.get())) {
            throw new BusinessException(ErrorCode.Business.AUDIT_ENTITY_TYPE_INVALID);
        }
    }

    private Long liveTenantId(String entityType, Long id) {
        String table = AUDITED_TABLE_NAMES.get(entityType.toLowerCase());
        try {
            Object tenantId = entityManager.createNativeQuery("SELECT tenant_id FROM " + table + " WHERE id = :id")
                    .setParameter("id", id)
                    .getSingleResult();
            return tenantId == null ? null : ((Number) tenantId).longValue();
        } catch (jakarta.persistence.NoResultException e) {
            return null;
        }
    }

    private AuditRevisionRs toRevision(Object[] row, Map<Long, String> usernameCache) {
        Object entity = row[0];
        AppRevisionEntity rev = (AppRevisionEntity) row[1];
        RevisionType revType = (RevisionType) row[2];

        return AuditRevisionRs.builder()
                .revision(rev.getRev())
                .changeType(revType.name())
                .userId(rev.getUserId())
                .username(resolveUsername(rev.getUserId(), usernameCache))
                .revisionAt(toLocalDateTime(rev.getTimestamp()))
                .entity(toState(entity))
                .build();
    }

    /**
     * Flattens a reconstructed audited entity into a proxy-safe map of its own columns: scalar
     * values as-is and each @ManyToOne/@OneToOne association as its foreign-key id (read off the
     * proxy without initialising it). Collections are skipped (they are {@code @NotAudited}). This
     * mirrors the _aud table and avoids serialising lazy Hibernate proxies.
     */
    private Map<String, Object> toState(Object entity) {
        if (entity == null) {
            return null;
        }
        Class<?> type = Hibernate.getClass(entity);
        EntityType<?> entityType = entityManager.getMetamodel().entity(type);
        PersistenceUnitUtil idUtil = entityManager.getEntityManagerFactory().getPersistenceUnitUtil();

        Map<String, Object> state = new LinkedHashMap<>();
        state.put("id", idUtil.getIdentifier(entity));
        for (Attribute<?, ?> attr : entityType.getAttributes()) {
            if (attr.isCollection() || "id".equals(attr.getName())) {
                continue;
            }
            Object value = readValue(entity, attr);
            if (attr.isAssociation()) {
                state.put(attr.getName() + "Id", value == null ? null : idUtil.getIdentifier(value));
            } else {
                state.put(attr.getName(), value);
            }
        }
        return state;
    }

    private Object readValue(Object entity, Attribute<?, ?> attr) {
        try {
            Member member = attr.getJavaMember();
            if (member instanceof Field field) {
                field.setAccessible(true);
                return field.get(entity);
            }
            if (member instanceof Method method) {
                method.setAccessible(true);
                return method.invoke(entity);
            }
        } catch (Exception e) {
            log.debug("Could not read audited attribute {}: {}", attr.getName(), e.getMessage());
        }
        return null;
    }

    private Class<?> resolveType(String entityType) {
        Class<?> type = entityType == null ? null : AUDITED_TYPES.get(entityType.toLowerCase());
        if (type == null) {
            throw new BusinessException(ErrorCode.Business.AUDIT_ENTITY_TYPE_INVALID);
        }
        return type;
    }

    private String resolveUsername(Long userId, Map<Long, String> cache) {
        if (userId == null) {
            return null;
        }
        return cache.computeIfAbsent(userId, id -> userProfileRepository.findById(id)
                .map(UserProfile::getName)
                .orElse(null));
    }

    private LocalDateTime toLocalDateTime(long epochMillis) {
        return Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDateTime();
    }
}
