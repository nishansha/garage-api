package com.triasoft.garage.controller;

import com.triasoft.garage.dto.PurchaseDTO;
import com.triasoft.garage.model.common.ApiResponse;
import com.triasoft.garage.model.common.FilterRq;
import com.triasoft.garage.model.purchase.PurchasePaymentRq;
import com.triasoft.garage.model.purchase.PurchaseRq;
import com.triasoft.garage.model.purchase.PurchaseRs;
import com.triasoft.garage.model.purchase.PurchaseSummaryRs;
import com.triasoft.garage.service.impl.PurchaseService;
import com.triasoft.garage.util.UserUtil;
import jakarta.servlet.http.HttpServletRequest;
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
        return ResponseEntity.ok(ApiResponse.success(purchaseService.summary(UserUtil.getUser(request))));
    }

    @PostMapping(value = "/find", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<PurchaseRs>> search(@RequestBody FilterRq filterRq, @RequestParam("page") int page, @RequestParam("size") int size, HttpServletRequest request) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(ApiResponse.success(purchaseService.search(filterRq, pageable, UserUtil.getUser(request))));
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<PurchaseRs>> getAll(@RequestParam("page") int page, @RequestParam("size") int size, HttpServletRequest request) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(ApiResponse.success(purchaseService.getAll(pageable, UserUtil.getUser(request))));
    }

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<PurchaseRs>> create(@RequestBody PurchaseRq purchaseRq, HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.success(purchaseService.create(purchaseRq, UserUtil.getUser(request))));
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<PurchaseDTO>> get(@PathVariable("id") Long id, HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.success(purchaseService.get(id, UserUtil.getUser(request))));
    }

    @PutMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<PurchaseRs>> update(@RequestBody PurchaseRq purchaseRq, @PathVariable("id") Long id, HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.success(purchaseService.update(id, purchaseRq, UserUtil.getUser(request))));
    }

    @DeleteMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<PurchaseRs>> delete(@PathVariable("id") Long id, HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.success(purchaseService.delete(id, UserUtil.getUser(request))));
    }

    @PostMapping(value = "/{id}/payments", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<PurchaseRs>> recordPayment(@PathVariable("id") Long id, @RequestBody PurchasePaymentRq paymentRq, HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.success(purchaseService.recordPayment(id, paymentRq, UserUtil.getUser(request))));
    }

    @PutMapping(value = "/{id}/payments/{paymentId}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<PurchaseRs>> updatePayment(@PathVariable("id") Long id, @PathVariable("paymentId") Long paymentId, @RequestBody PurchasePaymentRq paymentRq, HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.success(purchaseService.updatePayment(id, paymentId, paymentRq, UserUtil.getUser(request))));
    }

    @DeleteMapping(value = "/{id}/payments/{paymentId}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<PurchaseRs>> deletePayment(@PathVariable("id") Long id, @PathVariable("paymentId") Long paymentId, HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.success(purchaseService.deletePayment(id, paymentId, UserUtil.getUser(request))));
    }
}