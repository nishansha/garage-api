package com.triasoft.garage.controller;

import com.triasoft.garage.model.common.FilterRq;
import com.triasoft.garage.model.stock.StockRs;
import com.triasoft.garage.model.stock.StockSummaryRs;
import com.triasoft.garage.service.StockService;
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
    ResponseEntity<StockRs> products(@RequestParam("page") int page, @RequestParam("size") int size, HttpServletRequest request) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("created").descending());
        return ResponseEntity.ok(stockService.products(pageable, UserUtil.getUser(request)));
    }

    @GetMapping(value = "/summary", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<StockSummaryRs> summary(HttpServletRequest request) {
        return ResponseEntity.ok(stockService.summary(UserUtil.getUser(request)));
    }

    @PostMapping(value = "/find", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<StockRs> findSales(@RequestBody FilterRq filterRq, @RequestParam("page") int page, @RequestParam("size") int size, HttpServletRequest request) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("created").descending());
        return ResponseEntity.ok(stockService.findProducts(filterRq, pageable, UserUtil.getUser(request)));
    }
}
