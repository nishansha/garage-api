package com.triasoft.garage.controller;

import com.triasoft.garage.dto.SaleDTO;
import com.triasoft.garage.dto.SaleReturnDTO;
import com.triasoft.garage.model.common.ApiResponse;
import com.triasoft.garage.model.common.FilterRq;
import com.triasoft.garage.model.report.ReceivablesSummaryRs;
import com.triasoft.garage.model.sale.ReturnFormDataRs;
import com.triasoft.garage.model.sale.SalePaymentRq;
import com.triasoft.garage.model.sale.SaleReturnRq;
import com.triasoft.garage.model.sale.SaleSummaryRs;
import com.triasoft.garage.model.sale.SalesRq;
import com.triasoft.garage.model.sale.SalesRs;
import com.triasoft.garage.service.impl.SaleReturnService;
import com.triasoft.garage.service.impl.SalesService;
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
@RequestMapping("/api/v1/sales")
public class SalesController {

    private final SalesService salesService;
    private final SaleReturnService saleReturnService;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<SalesRs>> getAll(@RequestParam("page") int page, @RequestParam("size") int size, HttpServletRequest request) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(ApiResponse.success(salesService.getAll(pageable, UserUtil.getUser(request))));
    }

    @GetMapping(value = "/summary", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<SaleSummaryRs>> summary(HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.success(salesService.summary(UserUtil.getUser(request))));
    }

    @PostMapping(value = "/find", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<SalesRs>> search(@RequestBody FilterRq filterRq, @RequestParam("page") int page, @RequestParam("size") int size, HttpServletRequest request) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(ApiResponse.success(salesService.search(filterRq, pageable, UserUtil.getUser(request))));
    }

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<SalesRs>> create(@Valid @RequestBody SalesRq salesRq, HttpServletRequest request) {
        log.info(":: SalesController - create() - {} ::", salesRq);
        return ResponseEntity.ok(ApiResponse.success(salesService.create(salesRq, UserUtil.getUser(request))));
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<SaleDTO>> get(@PathVariable("id") Long id, HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.success(salesService.get(id, UserUtil.getUser(request))));
    }

    @PutMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<SalesRs>> update(@Valid @RequestBody SalesRq salesRq, @PathVariable("id") Long id, HttpServletRequest request) {
        log.info(":: SalesController - update() - id - {}, {} ::", id, salesRq);
        return ResponseEntity.ok(ApiResponse.success(salesService.update(id, salesRq, UserUtil.getUser(request))));
    }

    @DeleteMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<SalesRs>> delete(@PathVariable("id") Long id, HttpServletRequest request) {
        log.info(":: SalesController - delete() - id - {} ::", id);
        return ResponseEntity.ok(ApiResponse.success(salesService.delete(id, UserUtil.getUser(request))));
    }

    @PostMapping(value = "/{id}/payments", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<SalesRs>> recordPayment(@PathVariable("id") Long id, @Valid @RequestBody SalePaymentRq paymentRq, HttpServletRequest request) {
        log.info(":: SalesController - recordPayment() - id - {}, {} ::", id, paymentRq);
        return ResponseEntity.ok(ApiResponse.success(salesService.recordPayment(id, paymentRq, UserUtil.getUser(request))));
    }

    @PutMapping(value = "/{id}/payments/{paymentId}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<SalesRs>> updatePayment(@PathVariable("id") Long id, @PathVariable("paymentId") Long paymentId, @Valid @RequestBody SalePaymentRq paymentRq, HttpServletRequest request) {
        log.info(":: SalesController - updatePayment() - id - {},paymentId- {}, {} ::", id, paymentId, paymentRq);
        return ResponseEntity.ok(ApiResponse.success(salesService.updatePayment(id, paymentId, paymentRq, UserUtil.getUser(request))));
    }

    @DeleteMapping(value = "/{id}/payments/{paymentId}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<SalesRs>> deletePayment(@PathVariable("id") Long id, @PathVariable("paymentId") Long paymentId, HttpServletRequest request) {
        log.info(":: SalesController - deletePayment() - id - {},paymentId- {} ::", id, paymentId);
        return ResponseEntity.ok(ApiResponse.success(salesService.deletePayment(id, paymentId, UserUtil.getUser(request))));
    }

    @GetMapping(value = "/receivables", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<ReceivablesSummaryRs>> getReceivablesSummary() {
        return ResponseEntity.ok(ApiResponse.success(salesService.getReceivablesSummary()));
    }

    @GetMapping(value = "/{id}/return/form-data", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<ReturnFormDataRs.Body>> returnFormData(@PathVariable("id") Long id, HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.success(saleReturnService.getFormData(id, UserUtil.getUser(request))));
    }

    @PostMapping(value = "/{id}/return", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<SaleReturnDTO>> createReturn(@PathVariable("id") Long id, @Valid @RequestBody SaleReturnRq rq, HttpServletRequest request) {
        log.info(":: SalesController - createReturn() - id - {}, {} ::", id, rq);
        return ResponseEntity.ok(ApiResponse.success(saleReturnService.create(id, rq, UserUtil.getUser(request))));
    }
}