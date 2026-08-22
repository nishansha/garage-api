package com.triasoft.garage.servicesale.controller;

import com.triasoft.garage.constants.Privilege;
import com.triasoft.garage.model.common.ApiResponse;
import com.triasoft.garage.security.rbac.HasPrivilege;
import com.triasoft.garage.servicesale.dto.ServiceOfferingDTO;
import com.triasoft.garage.servicesale.model.ServiceOfferingRq;
import com.triasoft.garage.servicesale.model.ServiceOfferingRs;
import com.triasoft.garage.servicesale.service.ServiceOfferingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/services")
public class ServiceOfferingController {

    private final ServiceOfferingService serviceOfferingService;

    @HasPrivilege(resource = "SERVICE_OFFERING", privilege = Privilege.VIEW)
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<ServiceOfferingRs>> getAll(@RequestParam(value = "warehouseId", required = false) Long warehouseId) {
        return ResponseEntity.ok(ApiResponse.success(serviceOfferingService.getAll(warehouseId)));
    }

    @HasPrivilege(resource = "SERVICE_OFFERING", privilege = Privilege.VIEW)
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<ServiceOfferingDTO>> get(@PathVariable("id") Long id) {
        return ResponseEntity.ok(ApiResponse.success(serviceOfferingService.get(id)));
    }

    @HasPrivilege(resource = "SERVICE_OFFERING", privilege = Privilege.CREATE)
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<ServiceOfferingRs>> create(@Valid @RequestBody ServiceOfferingRq rq) {
        log.info(":: ServiceOfferingController - create () - {} ::", rq);
        return ResponseEntity.ok(ApiResponse.success(serviceOfferingService.create(rq)));
    }

    @HasPrivilege(resource = "SERVICE_OFFERING", privilege = Privilege.UPDATE)
    @PutMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<ServiceOfferingRs>> update(@PathVariable("id") Long id, @Valid @RequestBody ServiceOfferingRq rq) {
        log.info(":: ServiceOfferingController - update () - id {}, {} ::", id, rq);
        return ResponseEntity.ok(ApiResponse.success(serviceOfferingService.update(id, rq)));
    }

    @HasPrivilege(resource = "SERVICE_OFFERING", privilege = Privilege.DELETE)
    @DeleteMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<Void>> delete(@PathVariable("id") Long id) {
        serviceOfferingService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

}
