package com.triasoft.garage.controller;

import com.triasoft.garage.constants.Privilege;
import com.triasoft.garage.dto.WarehouseDTO;
import com.triasoft.garage.model.common.ApiResponse;
import com.triasoft.garage.model.warehouse.WarehouseRq;
import com.triasoft.garage.model.warehouse.WarehouseRs;
import com.triasoft.garage.security.rbac.HasPrivilege;
import com.triasoft.garage.service.impl.WarehouseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/warehouses")
public class WarehouseController {

    private final WarehouseService warehouseService;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<WarehouseRs>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(warehouseService.getAll()));
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<WarehouseDTO>> get(@PathVariable("id") Long id) {
        return ResponseEntity.ok(ApiResponse.success(warehouseService.get(id)));
    }

    @HasPrivilege(resource = "WAREHOUSE", privilege = Privilege.CREATE)
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<WarehouseRs>> create(@Valid @RequestBody WarehouseRq rq) {
        log.info(":: WarehouseController - create () - {} ::", rq);
        return ResponseEntity.ok(ApiResponse.success(warehouseService.create(rq)));
    }

    @HasPrivilege(resource = "WAREHOUSE", privilege = Privilege.UPDATE)
    @PutMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<WarehouseRs>> update(@PathVariable("id") Long id, @Valid @RequestBody WarehouseRq rq) {
        log.info(":: WarehouseController - update () - id {}, {} ::", id, rq);
        return ResponseEntity.ok(ApiResponse.success(warehouseService.update(id, rq)));
    }

    @HasPrivilege(resource = "WAREHOUSE", privilege = Privilege.DELETE)
    @DeleteMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<Void>> delete(@PathVariable("id") Long id) {
        warehouseService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

}
