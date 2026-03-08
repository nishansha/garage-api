package com.triasoft.garage.controller;

import com.triasoft.garage.dto.PurchaseDTO;
import com.triasoft.garage.model.common.FilterRq;
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
    ResponseEntity<PurchaseSummaryRs> summary(HttpServletRequest request) {
        return ResponseEntity.ok(purchaseService.summary(UserUtil.getUser(request)));
    }

    @PostMapping(value = "/find", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<PurchaseRs> search(@RequestBody FilterRq filterRq, @RequestParam("page") int page, @RequestParam("size") int size, HttpServletRequest request) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(purchaseService.search(filterRq, pageable, UserUtil.getUser(request)));
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<PurchaseRs> getAll(@RequestParam("page") int page, @RequestParam("size") int size, HttpServletRequest request) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(purchaseService.getAll(pageable, UserUtil.getUser(request)));
    }

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<PurchaseRs> create(@RequestBody PurchaseRq purchaseRq, HttpServletRequest request) {
        return ResponseEntity.ok(purchaseService.create(purchaseRq, UserUtil.getUser(request)));
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<PurchaseDTO> get(@PathVariable("id") Long id, HttpServletRequest request) {
        return ResponseEntity.ok(purchaseService.get(id, UserUtil.getUser(request)));
    }

    @PutMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<PurchaseRs> update(@RequestBody PurchaseRq purchaseRq, @PathVariable("id") Long id, HttpServletRequest request) {
        return ResponseEntity.ok(purchaseService.update(id, purchaseRq, UserUtil.getUser(request)));
    }

    @DeleteMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<PurchaseRs> delete(@PathVariable("id") Long id, HttpServletRequest request) {
        return ResponseEntity.ok(purchaseService.delete(id, UserUtil.getUser(request)));
    }

}
