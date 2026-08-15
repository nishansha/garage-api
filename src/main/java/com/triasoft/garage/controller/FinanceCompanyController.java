package com.triasoft.garage.controller;

import com.triasoft.garage.constants.Privilege;
import com.triasoft.garage.dto.FinanceCompanyDTO;
import com.triasoft.garage.model.common.ApiResponse;
import com.triasoft.garage.model.finance.FinanceCompanyRq;
import com.triasoft.garage.model.finance.FinanceCompanyRs;
import com.triasoft.garage.security.rbac.HasPrivilege;
import com.triasoft.garage.service.impl.FinanceCompanyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/finance-companies")
public class FinanceCompanyController {

    private final FinanceCompanyService financeCompanyService;

    @HasPrivilege(resource = "FINANCE_COMPANY", privilege = Privilege.VIEW)
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<FinanceCompanyRs>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(financeCompanyService.getAll()));
    }

    @HasPrivilege(resource = "FINANCE_COMPANY", privilege = Privilege.VIEW)
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<FinanceCompanyDTO>> get(@PathVariable("id") Long id) {
        return ResponseEntity.ok(ApiResponse.success(financeCompanyService.get(id)));
    }

    @HasPrivilege(resource = "FINANCE_COMPANY", privilege = Privilege.CREATE)
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<FinanceCompanyRs>> create(@Valid @RequestBody FinanceCompanyRq rq) {
        log.info(":: FinanceCompanyController - create () - {} ::", rq);
        return ResponseEntity.ok(ApiResponse.success(financeCompanyService.create(rq)));
    }

    @HasPrivilege(resource = "FINANCE_COMPANY", privilege = Privilege.UPDATE)
    @PutMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<FinanceCompanyRs>> update(@PathVariable("id") Long id, @Valid @RequestBody FinanceCompanyRq rq) {
        log.info(":: FinanceCompanyController - update () - id {}, {} ::", id, rq);
        return ResponseEntity.ok(ApiResponse.success(financeCompanyService.update(id, rq)));
    }

    @HasPrivilege(resource = "FINANCE_COMPANY", privilege = Privilege.DELETE)
    @DeleteMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<Void>> delete(@PathVariable("id") Long id) {
        financeCompanyService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

}
