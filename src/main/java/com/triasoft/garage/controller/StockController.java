package com.triasoft.garage.controller;

import com.triasoft.garage.dto.StockDTO;
import com.triasoft.garage.model.common.ApiResponse;
import com.triasoft.garage.model.common.FilterRq;
import com.triasoft.garage.model.common.LookupRs;
import com.triasoft.garage.model.stock.StockRs;
import com.triasoft.garage.model.stock.StockSummaryRs;
import com.triasoft.garage.service.impl.StockService;
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
@RequestMapping("/api/v1/stock")
public class StockController {

    private final StockService stockService;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<StockRs>> getAll(@RequestParam("page") int page, @RequestParam("size") int size, HttpServletRequest request) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(ApiResponse.success(stockService.getAll(pageable, UserUtil.getUser(request))));
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<StockDTO>> get(@PathVariable("id") Long id, HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.success(stockService.get(id, UserUtil.getUser(request))));
    }

    @GetMapping(value = "/products", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<LookupRs>> products(HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.success(stockService.stockProducts(UserUtil.getUser(request))));
    }

    @GetMapping(value = "/summary", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<StockSummaryRs>> summary(HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.success(stockService.summary(UserUtil.getUser(request))));
    }

    @PostMapping(value = "/find", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<StockRs>> findSales(@RequestBody FilterRq filterRq, @RequestParam("page") int page, @RequestParam("size") int size, HttpServletRequest request) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(ApiResponse.success(stockService.findProducts(filterRq, pageable, UserUtil.getUser(request))));
    }
}