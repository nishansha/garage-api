package com.triasoft.garage.hrm.controller;

import com.triasoft.garage.constants.Privilege;
import com.triasoft.garage.hrm.model.SalaryPaymentMarkPaidRq;
import com.triasoft.garage.hrm.model.SalaryPaymentRs;
import com.triasoft.garage.hrm.service.SalaryPaymentService;
import com.triasoft.garage.model.common.ApiResponse;
import com.triasoft.garage.security.rbac.HasPrivilege;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/salary-payments")
public class SalaryPaymentController {

    private final SalaryPaymentService salaryPaymentService;

    @HasPrivilege(resource = "SALARY_PAYMENT", privilege = Privilege.VIEW)
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<SalaryPaymentRs>> getAll(@RequestParam(value = "companyId", required = false) Long companyId) {
        return ResponseEntity.ok(ApiResponse.success(salaryPaymentService.getAll(companyId)));
    }

    // Manual trigger, in addition to SalaryRunScheduler's automatic monthly run —
    // idempotent (skips employees who already have a row for the given period).
    @HasPrivilege(resource = "SALARY_PAYMENT", privilege = Privilege.CREATE)
    @PostMapping(value = "/generate", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<Integer>> generate(@RequestParam("companyId") Long companyId, @RequestParam(value = "period", required = false) String period) {
        YearMonth yearMonth = period != null ? YearMonth.parse(period) : YearMonth.now();
        log.info(":: SalaryPaymentController - generate() - companyId {}, period {} ::", companyId, yearMonth);
        return ResponseEntity.ok(ApiResponse.success(salaryPaymentService.generateForCompany(companyId, yearMonth)));
    }

    @HasPrivilege(resource = "SALARY_PAYMENT", privilege = Privilege.UPDATE)
    @PutMapping(value = "/{id}/mark-paid", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<SalaryPaymentRs>> markPaid(@PathVariable("id") Long id, @Valid @RequestBody SalaryPaymentMarkPaidRq rq) {
        log.info(":: SalaryPaymentController - markPaid() - id {}, {} ::", id, rq);
        return ResponseEntity.ok(ApiResponse.success(salaryPaymentService.markPaid(id, rq)));
    }

    @HasPrivilege(resource = "SALARY_PAYMENT", privilege = Privilege.DELETE)
    @DeleteMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<Void>> delete(@PathVariable("id") Long id) {
        salaryPaymentService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

}
