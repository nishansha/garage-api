package com.triasoft.garage.controller;

import com.triasoft.garage.dto.PurchaseDTO;
import com.triasoft.garage.model.common.ApiResponse;
import com.triasoft.garage.model.common.FilterRq;
import com.triasoft.garage.model.purchase.PurchasePaymentRq;
import com.triasoft.garage.model.purchase.PurchaseRq;
import com.triasoft.garage.model.purchase.PurchaseRs;
import com.triasoft.garage.model.purchase.PurchaseSummaryRs;
import com.triasoft.garage.model.report.PayablesSummaryRs;
import com.triasoft.garage.service.impl.PurchaseService;
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
@RequestMapping("/api/v1/purchase")
public class PurchaseController {

    private final PurchaseService purchaseService;

    @GetMapping(value = "/summary", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<PurchaseSummaryRs>> summary(HttpServletRequest request) {
        log.info(":: PurchaseController - summary() ::");
        return ResponseEntity.ok(ApiResponse.success(purchaseService.summary(UserUtil.getUser(request))));
    }

    @PostMapping(value = "/find", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<PurchaseRs>> find(@RequestBody FilterRq filterRq, @RequestParam("page") int page, @RequestParam("size") int size, HttpServletRequest request) {
        log.info(":: PurchaseController - find() ::");
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(ApiResponse.success(purchaseService.search(filterRq, pageable, UserUtil.getUser(request))));
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<PurchaseRs>> getAll(@RequestParam("page") int page, @RequestParam("size") int size, HttpServletRequest request) {
        log.info(":: PurchaseController - getAll() ::");
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(ApiResponse.success(purchaseService.getAll(pageable, UserUtil.getUser(request))));
    }

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<PurchaseRs>> create(@Valid @RequestBody PurchaseRq purchaseRq, HttpServletRequest request) {
        log.info(":: PurchaseController - create() ::");
        return ResponseEntity.ok(ApiResponse.success(purchaseService.create(purchaseRq, UserUtil.getUser(request))));
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<PurchaseDTO>> get(@PathVariable("id") Long id, HttpServletRequest request) {
        log.info(":: PurchaseController - get () -id {} ::", id);
        return ResponseEntity.ok(ApiResponse.success(purchaseService.get(id, UserUtil.getUser(request))));
    }

    @PutMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<PurchaseRs>> update(@Valid @RequestBody PurchaseRq purchaseRq, @PathVariable("id") Long id, HttpServletRequest request) {
        log.info(":: PurchaseController - update () -id {} ::", id);
        return ResponseEntity.ok(ApiResponse.success(purchaseService.update(id, purchaseRq, UserUtil.getUser(request))));
    }

    @DeleteMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<PurchaseRs>> delete(@PathVariable("id") Long id, HttpServletRequest request) {
        log.info(":: PurchaseController - delete() -id {} ::", id);
        return ResponseEntity.ok(ApiResponse.success(purchaseService.delete(id, UserUtil.getUser(request))));
    }

    @PostMapping(value = "/{id}/payments", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<PurchaseRs>> recordPayment(@PathVariable("id") Long id, @Valid @RequestBody PurchasePaymentRq paymentRq, HttpServletRequest request) {
        log.info(":: PurchaseController - recordPayment() - id {}::", id);
        return ResponseEntity.ok(ApiResponse.success(purchaseService.recordPayment(id, paymentRq, UserUtil.getUser(request))));
    }

    @PutMapping(value = "/{id}/payments/{paymentId}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<PurchaseRs>> updatePayment(@PathVariable("id") Long id, @PathVariable("paymentId") Long paymentId, @Valid @RequestBody PurchasePaymentRq paymentRq, HttpServletRequest request) {
        log.info(":: PurchaseController - updatePayment - id {} ::", id);
        return ResponseEntity.ok(ApiResponse.success(purchaseService.updatePayment(id, paymentId, paymentRq, UserUtil.getUser(request))));
    }

    @DeleteMapping(value = "/{id}/payments/{paymentId}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<PurchaseRs>> deletePayment(@PathVariable("id") Long id, @PathVariable("paymentId") Long paymentId, HttpServletRequest request) {
        log.info(":: PurchaseController - deletePayment () - id {} ::", id);
        return ResponseEntity.ok(ApiResponse.success(purchaseService.deletePayment(id, paymentId, UserUtil.getUser(request))));
    }

    @GetMapping(value = "/payables", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<PayablesSummaryRs>> getPayablesSummary() {
        log.info(":: PurchaseController - getPayablesSummary () ::");
        return ResponseEntity.ok(ApiResponse.success(purchaseService.getPayablesSummary()));
    }
}