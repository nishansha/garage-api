package com.triasoft.garage.controller;

import com.triasoft.garage.model.common.ApiResponse;
import com.triasoft.garage.model.tenant.TenantCreateRq;
import com.triasoft.garage.model.tenant.TenantCreateRs;
import com.triasoft.garage.service.impl.TenantProvisioningService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Ops-only tenant onboarding/lifecycle endpoints. Gated by InternalApiKeyFilter (X-Internal-Api-Key
 * header) rather than a user JWT, since there is no authenticated user yet at tenant-creation time.
 * Not for use by the web/mobile apps.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/internal/tenants")
public class InternalTenantController {

    private final TenantProvisioningService tenantProvisioningService;

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<TenantCreateRs>> create(@Valid @RequestBody TenantCreateRq rq) {
        return ResponseEntity.ok(ApiResponse.success(tenantProvisioningService.createTenant(rq)));
    }

    @PatchMapping(value = "/{id}/deactivate", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<Void>> deactivate(@PathVariable("id") Long id) {
        tenantProvisioningService.deactivate(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PatchMapping(value = "/{id}/activate", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<Void>> activate(@PathVariable("id") Long id) {
        tenantProvisioningService.activate(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
