package com.triasoft.garage.controller;

import com.triasoft.garage.dto.SaleDTO;
import com.triasoft.garage.model.common.FilterRq;
import com.triasoft.garage.model.sale.SaleSummaryRs;
import com.triasoft.garage.model.sale.SalesRq;
import com.triasoft.garage.model.sale.SalesRs;
import com.triasoft.garage.service.impl.SalesService;
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
@RequestMapping("/api/v1/sales")
public class SalesController {

    private final SalesService salesService;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<SalesRs> getAll(@RequestParam("page") int page, @RequestParam("size") int size, HttpServletRequest request) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(salesService.getAll(pageable, UserUtil.getUser(request)));
    }

    @GetMapping(value = "/summary", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<SaleSummaryRs> summary(HttpServletRequest request) {
        return ResponseEntity.ok(salesService.summary(UserUtil.getUser(request)));
    }

    @PostMapping(value = "/find", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<SalesRs> search(@RequestBody FilterRq filterRq, @RequestParam("page") int page, @RequestParam("size") int size, HttpServletRequest request) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(salesService.search(filterRq, pageable, UserUtil.getUser(request)));
    }

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<SalesRs> create(@RequestBody SalesRq salesRq, HttpServletRequest request) {
        return ResponseEntity.ok(salesService.create(salesRq, UserUtil.getUser(request)));
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<SaleDTO> get(@PathVariable("id") Long id, HttpServletRequest request) {
        return ResponseEntity.ok(salesService.get(id, UserUtil.getUser(request)));
    }

    @PutMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<SalesRs> update(@RequestBody SalesRq salesRq, @PathVariable("id") Long id, HttpServletRequest request) {
        return ResponseEntity.ok(salesService.update(id, salesRq, UserUtil.getUser(request)));
    }

    @DeleteMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<SalesRs> delete(@PathVariable("id") Long id, HttpServletRequest request) {
        return ResponseEntity.ok(salesService.delete(id, UserUtil.getUser(request)));
    }
}
