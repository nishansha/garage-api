package com.triasoft.garage.servicesale.controller;

import com.triasoft.garage.constants.Privilege;
import com.triasoft.garage.model.common.ApiResponse;
import com.triasoft.garage.model.common.FilterRq;
import com.triasoft.garage.security.rbac.HasPrivilege;
import com.triasoft.garage.servicesale.dto.ServiceSaleDTO;
import com.triasoft.garage.servicesale.model.ServiceSalePaymentRq;
import com.triasoft.garage.servicesale.model.ServiceSaleRq;
import com.triasoft.garage.servicesale.model.ServiceSaleRs;
import com.triasoft.garage.servicesale.service.ServiceSaleService;
import com.triasoft.garage.util.UserUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/service-sales")
public class ServiceSaleController {

    private final ServiceSaleService serviceSaleService;

    @HasPrivilege(resource = "SERVICE_SALE", privilege = Privilege.VIEW)
    @PostMapping(value = "/find", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<ServiceSaleRs>> getAll(@RequestParam("page") int page, @RequestParam("size") int size, @RequestBody(required = false) FilterRq filter) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(ApiResponse.success(serviceSaleService.getAll(filter != null ? filter : new FilterRq(), pageable)));
    }

    @HasPrivilege(resource = "SERVICE_SALE", privilege = Privilege.VIEW)
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<ServiceSaleDTO>> get(@PathVariable("id") Long id) {
        return ResponseEntity.ok(ApiResponse.success(serviceSaleService.get(id)));
    }

    @HasPrivilege(resource = "SERVICE_SALE", privilege = Privilege.CREATE)
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<ServiceSaleRs>> create(@Valid @RequestBody ServiceSaleRq rq, HttpServletRequest request) {
        log.info(":: ServiceSaleController - create () - {} ::", rq);
        return ResponseEntity.ok(ApiResponse.success(serviceSaleService.create(rq, UserUtil.getUser(request))));
    }

    @HasPrivilege(resource = "SERVICE_SALE", privilege = Privilege.UPDATE)
    @PutMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<ServiceSaleRs>> update(@PathVariable("id") Long id, @Valid @RequestBody ServiceSaleRq rq, HttpServletRequest request) {
        log.info(":: ServiceSaleController - update () - id {}, {} ::", id, rq);
        return ResponseEntity.ok(ApiResponse.success(serviceSaleService.update(id, rq, UserUtil.getUser(request))));
    }

    @HasPrivilege(resource = "SERVICE_SALE", privilege = Privilege.DELETE)
    @DeleteMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<Void>> delete(@PathVariable("id") Long id) {
        serviceSaleService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @HasPrivilege(resource = "SERVICE_SALE", privilege = Privilege.CREATE)
    @PostMapping(value = "/{id}/payments", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<ServiceSaleRs>> recordPayment(@PathVariable("id") Long id, @Valid @RequestBody ServiceSalePaymentRq rq) {
        log.info(":: ServiceSaleController - recordPayment() - id - {}, {} ::", id, rq);
        return ResponseEntity.ok(ApiResponse.success(serviceSaleService.recordPayment(id, rq)));
    }

    @HasPrivilege(resource = "SERVICE_SALE", privilege = Privilege.DELETE)
    @DeleteMapping(value = "/{id}/payments/{paymentId}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<Void>> deletePayment(@PathVariable("id") Long id, @PathVariable("paymentId") Long paymentId) {
        serviceSaleService.deletePayment(id, paymentId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

}
