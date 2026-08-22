package com.triasoft.garage.company.controller;

import com.triasoft.garage.company.dto.CompanyDTO;
import com.triasoft.garage.company.model.CompanyComparisonRs;
import com.triasoft.garage.company.model.CompanyRq;
import com.triasoft.garage.company.model.CompanyRs;
import com.triasoft.garage.company.service.CompanyReportService;
import com.triasoft.garage.company.service.CompanyService;
import com.triasoft.garage.constants.Privilege;
import com.triasoft.garage.exception.BusinessException;
import com.triasoft.garage.model.common.ApiResponse;
import com.triasoft.garage.security.rbac.HasPrivilege;
import com.triasoft.garage.util.UserUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/companies")
public class CompanyController {

    private final CompanyService companyService;
    private final CompanyReportService companyReportService;

    @HasPrivilege(resource = "COMPANY", privilege = Privilege.VIEW)
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<CompanyRs>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(companyService.getAll()));
    }

    @HasPrivilege(resource = "COMPANY", privilege = Privilege.VIEW)
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<CompanyDTO>> get(@PathVariable("id") Long id) {
        return ResponseEntity.ok(ApiResponse.success(companyService.get(id)));
    }

    @HasPrivilege(resource = "COMPANY", privilege = Privilege.CREATE)
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<CompanyRs>> create(@Valid @RequestBody CompanyRq rq) {
        log.info(":: CompanyController - create () - {} ::", rq);
        return ResponseEntity.ok(ApiResponse.success(companyService.create(rq)));
    }

    @HasPrivilege(resource = "COMPANY", privilege = Privilege.UPDATE)
    @PutMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<CompanyRs>> update(@PathVariable("id") Long id, @Valid @RequestBody CompanyRq rq) {
        log.info(":: CompanyController - update () - id {}, {} ::", id, rq);
        return ResponseEntity.ok(ApiResponse.success(companyService.update(id, rq)));
    }

    @HasPrivilege(resource = "COMPANY", privilege = Privilege.DELETE)
    @DeleteMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<Void>> delete(@PathVariable("id") Long id) {
        companyService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @HasPrivilege(resource = "COMPANY", privilege = Privilege.VIEW)
    @GetMapping(value = "/{id}/access", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<List<Long>>> getGrantedUserIds(@PathVariable("id") Long id) {
        return ResponseEntity.ok(ApiResponse.success(companyService.getGrantedUserIds(id)));
    }

    @HasPrivilege(resource = "COMPANY", privilege = Privilege.UPDATE)
    @PostMapping(value = "/{id}/access/{userId}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<Void>> grantAccess(@PathVariable("id") Long id, @PathVariable("userId") Long userId) {
        log.info(":: CompanyController - grantAccess() - companyId {}, userId {} ::", id, userId);
        companyService.grantAccess(id, userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @HasPrivilege(resource = "COMPANY", privilege = Privilege.UPDATE)
    @DeleteMapping(value = "/{id}/access/{userId}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<Void>> revokeAccess(@PathVariable("id") Long id, @PathVariable("userId") Long userId) {
        log.info(":: CompanyController - revokeAccess() - companyId {}, userId {} ::", id, userId);
        companyService.revokeAccess(id, userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @HasPrivilege(resource = "COMPANY", privilege = Privilege.VIEW)
    @GetMapping(value = "/comparison", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<CompanyComparisonRs>> compare(@RequestParam(value = "month", required = false) String month, HttpServletRequest request) {
        YearMonth yearMonth = parseMonth(month);
        return ResponseEntity.ok(ApiResponse.success(companyReportService.compare(yearMonth, UserUtil.getUser(request))));
    }

    private YearMonth parseMonth(String month) {
        if (month == null || month.isBlank()) {
            return YearMonth.now();
        }
        try {
            return YearMonth.parse(month);
        } catch (DateTimeParseException e) {
            throw new BusinessException("RPT_400", "Invalid month format. Use YYYY-MM (e.g. 2026-06)");
        }
    }

}
