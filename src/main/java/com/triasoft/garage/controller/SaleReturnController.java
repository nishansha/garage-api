package com.triasoft.garage.controller;

import com.triasoft.garage.dto.SaleReturnDTO;
import com.triasoft.garage.model.common.ApiResponse;
import com.triasoft.garage.model.report.SaleReturnPayablesSummaryRs;
import com.triasoft.garage.model.sale.RefundCreateResponse;
import com.triasoft.garage.model.sale.RefundPaymentRq;
import com.triasoft.garage.model.sale.SaleReturnRs;
import com.triasoft.garage.service.impl.SaleReturnService;
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

import java.time.LocalDate;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/sale-returns")
public class SaleReturnController {

    private final SaleReturnService saleReturnService;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<SaleReturnRs>> list(@RequestParam(value = "fromDate", required = false) LocalDate fromDate, @RequestParam(value = "toDate", required = false) LocalDate toDate, @RequestParam("page") int page, @RequestParam("size") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("returnDate").descending());
        return ResponseEntity.ok(ApiResponse.success(saleReturnService.list(fromDate, toDate, pageable)));
    }

    @GetMapping(value = "/payables", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<SaleReturnPayablesSummaryRs>> getPayablesSummary() {
        return ResponseEntity.ok(ApiResponse.success(saleReturnService.getPayablesSummary()));
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<SaleReturnDTO>> get(@PathVariable("id") Long id, HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.success(saleReturnService.get(id, UserUtil.getUser(request))));
    }

    @PostMapping(value = "/{id}/refunds", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<RefundCreateResponse>> recordRefund(@PathVariable("id") Long id, @Valid @RequestBody RefundPaymentRq rq, HttpServletRequest request) {
        log.info(":: SaleReturnController - recordRefund () - id {}, {} ::", id, rq);
        return ResponseEntity.ok(ApiResponse.success(saleReturnService.recordRefund(id, rq, UserUtil.getUser(request))));
    }

    @PutMapping(value = "/{id}/refunds/{refundId}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<RefundCreateResponse>> updateRefund(@PathVariable("id") Long id, @PathVariable("refundId") Long refundId, @Valid @RequestBody RefundPaymentRq rq, HttpServletRequest request) {
        log.info(":: SaleReturnController - updateRefund () - id {}, refundId {}, {} ::", id, refundId, rq);
        return ResponseEntity.ok(ApiResponse.success(saleReturnService.updateRefund(id, refundId, rq, UserUtil.getUser(request))));
    }

    @DeleteMapping(value = "/{id}/refunds/{refundId}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<Void>> deleteRefund(@PathVariable("id") Long id, @PathVariable("refundId") Long refundId, HttpServletRequest request) {
        log.info(":: SaleReturnController - updateRefund () - id {}, refundId {} ::", id, refundId);
        saleReturnService.deleteRefund(id, refundId, UserUtil.getUser(request));
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
