package com.triasoft.garage.controller;

import com.triasoft.garage.dto.PaymentAccountDTO;
import com.triasoft.garage.model.common.ApiResponse;
import com.triasoft.garage.model.payment.PaymentAccountRq;
import com.triasoft.garage.model.payment.PaymentAccountRs;
import com.triasoft.garage.model.payment.ReconcileRq;
import com.triasoft.garage.model.payment.ReconcileRs;
import com.triasoft.garage.model.payment.TransactionRs;
import com.triasoft.garage.service.impl.PaymentAccountService;
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
@RequestMapping("/api/v1/payment-accounts")
public class PaymentAccountController {

    private final PaymentAccountService paymentAccountService;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<PaymentAccountRs>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(paymentAccountService.getAll()));
    }

    @GetMapping(value = "/balance", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<PaymentAccountRs>> getAllWithBalance() {
        return ResponseEntity.ok(ApiResponse.success(paymentAccountService.getAllWithBalance()));
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<PaymentAccountDTO>> get(@PathVariable("id") Long id) {
        return ResponseEntity.ok(ApiResponse.success(paymentAccountService.get(id)));
    }

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<PaymentAccountRs>> create(@Valid @RequestBody PaymentAccountRq rq, HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.success(paymentAccountService.create(rq, UserUtil.getUser(request))));
    }

    @PutMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<PaymentAccountRs>> update(@PathVariable("id") Long id, @Valid @RequestBody PaymentAccountRq rq, HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.success(paymentAccountService.update(id, rq, UserUtil.getUser(request))));
    }

    @GetMapping(value = "/{id}/transactions", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<TransactionRs>> getTransactions(@PathVariable("id") Long id,
                                                               @RequestParam("page") int page,
                                                               @RequestParam("size") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("transactionDate").descending());
        return ResponseEntity.ok(ApiResponse.success(paymentAccountService.getTransactions(id, pageable)));
    }

    @PostMapping(value = "/{id}/transactions/{transactionId}/reverse", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<TransactionRs>> reverse(@PathVariable("id") Long id,
                                                       @PathVariable("transactionId") Long transactionId,
                                                       HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.success(paymentAccountService.reverse(transactionId, UserUtil.getUser(request))));
    }

    @PostMapping(value = "/{id}/reconcile", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<ReconcileRs>> reconcile(@PathVariable("id") Long id,
                                                       @RequestBody ReconcileRq rq) {
        return ResponseEntity.ok(ApiResponse.success(paymentAccountService.reconcile(id, rq)));
    }

    @GetMapping(value = "/{id}/unreconciled", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<TransactionRs>> getUnreconciled(@PathVariable("id") Long id,
                                                               @RequestParam("page") int page,
                                                               @RequestParam("size") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("transactionDate").descending());
        return ResponseEntity.ok(ApiResponse.success(paymentAccountService.getUnreconciledTransactions(id, pageable)));
    }

}
