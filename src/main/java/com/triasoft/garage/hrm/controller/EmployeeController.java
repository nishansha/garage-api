package com.triasoft.garage.hrm.controller;

import com.triasoft.garage.constants.Privilege;
import com.triasoft.garage.hrm.dto.EmployeeDTO;
import com.triasoft.garage.hrm.model.EmployeeRq;
import com.triasoft.garage.hrm.model.EmployeeRs;
import com.triasoft.garage.hrm.service.EmployeeService;
import com.triasoft.garage.model.common.ApiResponse;
import com.triasoft.garage.security.rbac.HasPrivilege;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/employees")
public class EmployeeController {

    private final EmployeeService employeeService;

    @HasPrivilege(resource = "EMPLOYEE", privilege = Privilege.VIEW)
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<EmployeeRs>> getAll(@RequestParam(value = "companyId", required = false) Long companyId) {
        return ResponseEntity.ok(ApiResponse.success(employeeService.getAll(companyId)));
    }

    @HasPrivilege(resource = "EMPLOYEE", privilege = Privilege.VIEW)
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<EmployeeDTO>> get(@PathVariable("id") Long id) {
        return ResponseEntity.ok(ApiResponse.success(employeeService.get(id)));
    }

    @HasPrivilege(resource = "EMPLOYEE", privilege = Privilege.CREATE)
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<EmployeeRs>> create(@Valid @RequestBody EmployeeRq rq) {
        log.info(":: EmployeeController - create () - {} ::", rq);
        return ResponseEntity.ok(ApiResponse.success(employeeService.create(rq)));
    }

    @HasPrivilege(resource = "EMPLOYEE", privilege = Privilege.UPDATE)
    @PutMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<EmployeeRs>> update(@PathVariable("id") Long id, @Valid @RequestBody EmployeeRq rq) {
        log.info(":: EmployeeController - update () - id {}, {} ::", id, rq);
        return ResponseEntity.ok(ApiResponse.success(employeeService.update(id, rq)));
    }

    @HasPrivilege(resource = "EMPLOYEE", privilege = Privilege.DELETE)
    @DeleteMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<Void>> delete(@PathVariable("id") Long id) {
        employeeService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

}
