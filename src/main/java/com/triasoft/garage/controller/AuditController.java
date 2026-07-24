package com.triasoft.garage.controller;

import com.triasoft.garage.constants.Privilege;
import com.triasoft.garage.model.audit.AuditRevisionRs;
import com.triasoft.garage.model.audit.DeletedRecordRs;
import com.triasoft.garage.model.common.ApiResponse;
import com.triasoft.garage.security.rbac.HasPrivilege;
import com.triasoft.garage.service.impl.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Exposes the Envers audit trail for the financing entities.
 * {@code entityType} is a whitelisted key (e.g. {@code sale}, {@code purchase}, {@code expense});
 * see {@link AuditService} for the full mapping. Gated by a single cross-cutting AUDIT/RECYCLE_BIN
 * privilege rather than per-entity-type, since "can see change history" is one permission
 * regardless of which record it's for.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/audit")
public class AuditController {

    private final AuditService auditService;

    @HasPrivilege(resource = "AUDIT", privilege = Privilege.VIEW)
    @GetMapping(value = "/{entityType}/{id}/history", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<List<AuditRevisionRs>>> history(@PathVariable String entityType, @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(auditService.getHistory(entityType, id)));
    }

    @HasPrivilege(resource = "RECYCLE_BIN", privilege = Privilege.VIEW)
    @GetMapping(value = "/{entityType}/deleted", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<List<DeletedRecordRs>>> deleted(@PathVariable String entityType) {
        return ResponseEntity.ok(ApiResponse.success(auditService.getDeleted(entityType)));
    }

    @HasPrivilege(resource = "AUDIT", privilege = Privilege.VIEW)
    @GetMapping(value = "/{entityType}/{id}/revisions/{revision}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<Object>> atRevision(@PathVariable String entityType, @PathVariable Long id, @PathVariable Long revision) {
        return ResponseEntity.ok(ApiResponse.success(auditService.getAtRevision(entityType, id, revision)));
    }
}
