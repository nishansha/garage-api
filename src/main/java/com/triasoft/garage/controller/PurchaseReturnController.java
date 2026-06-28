package com.triasoft.garage.controller;

import com.triasoft.garage.dto.PurchaseReturnDTO;
import com.triasoft.garage.model.common.ApiResponse;
import com.triasoft.garage.model.purchase.*;
import com.triasoft.garage.model.report.PurchaseReturnReceivablesSummaryRs;
import com.triasoft.garage.service.impl.PurchaseReturnService;
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

import java.time.LocalDate;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class PurchaseReturnController {

    private final PurchaseReturnService purchaseReturnService;

    @GetMapping(value = "/inventory/{inventoryId}/return/form-data", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<PurchaseReturnFormDataRs>> formData(@PathVariable("inventoryId") Long inventoryId,
                                                                   HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.success(purchaseReturnService.getFormData(inventoryId, UserUtil.getUser(request))));
    }

    @PostMapping(value = "/inventory/{inventoryId}/return", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<PurchaseReturnDTO>> create(@PathVariable("inventoryId") Long inventoryId,
                                                                             @RequestBody PurchaseReturnRq rq,
                                                                             HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.success(purchaseReturnService.create(inventoryId, rq, UserUtil.getUser(request))));
    }

    @GetMapping(value = "/purchase-returns", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<PurchaseReturnRs>> list(@RequestParam(value = "fromDate", required = false) LocalDate fromDate,
                                                       @RequestParam(value = "toDate", required = false) LocalDate toDate,
                                                       @RequestParam("page") int page,
                                                       @RequestParam("size") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("returnDate").descending());
        return ResponseEntity.ok(ApiResponse.success(purchaseReturnService.list(fromDate, toDate, pageable)));
    }

    @GetMapping(value = "/purchase-returns/receivables", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<PurchaseReturnReceivablesSummaryRs>> getReceivablesSummary() {
        return ResponseEntity.ok(ApiResponse.success(purchaseReturnService.getReceivablesSummary()));
    }

    @GetMapping(value = "/purchase-returns/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<PurchaseReturnDTO>> get(@PathVariable("id") Long id, HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.success(purchaseReturnService.get(id, UserUtil.getUser(request))));
    }

    @PostMapping(value = "/purchase-returns/{id}/receipts", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<ReceiptCreateRs>> recordReceipt(@PathVariable("id") Long id,
                                                                                           @RequestBody PurchaseReturnReceiptRq rq,
                                                                                           HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.success(purchaseReturnService.recordReceipt(id, rq, UserUtil.getUser(request))));
    }

    @PutMapping(value = "/purchase-returns/{id}/receipts/{receiptId}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<ReceiptCreateRs>> updateReceipt(@PathVariable("id") Long id,
                                                                                           @PathVariable("receiptId") Long receiptId,
                                                                                           @RequestBody PurchaseReturnReceiptRq rq,
                                                                                           HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.success(purchaseReturnService.updateReceipt(id, receiptId, rq, UserUtil.getUser(request))));
    }

    @DeleteMapping(value = "/purchase-returns/{id}/receipts/{receiptId}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<Void>> deleteReceipt(@PathVariable("id") Long id,
                                                    @PathVariable("receiptId") Long receiptId,
                                                    HttpServletRequest request) {
        purchaseReturnService.deleteReceipt(id, receiptId, UserUtil.getUser(request));
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
