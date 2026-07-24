package com.triasoft.garage.controller;

import com.triasoft.garage.model.common.ApiResponse;
import com.triasoft.garage.model.common.LookupRq;
import com.triasoft.garage.model.common.LookupRs;
import com.triasoft.garage.service.impl.LookupService;
import com.triasoft.garage.util.UserUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/lookup")
public class LookupController {

    private final LookupService lookupService;

    @GetMapping
    ResponseEntity<ApiResponse<LookupRs>> getLookupValues(@RequestParam(name = "type", required = false) String type) {
        return ResponseEntity.ok(ApiResponse.success(lookupService.getLookupValues(LookupRq.builder().type(type).build())));
    }

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<LookupRs>> create(@Valid @RequestBody LookupRq lookupRq, HttpServletRequest request) {
        log.info(":: LookupController - create () - {} ::", lookupRq);
        return ResponseEntity.ok(ApiResponse.success(lookupService.create(lookupRq, UserUtil.getUser(request))));
    }

    @PutMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<LookupRs>> update(@PathVariable("id") Long lookupId, @Valid @RequestBody LookupRq lookupRq, HttpServletRequest request) {
        log.info(":: LookupController - create () -id {}, {} ::", lookupId, lookupRq);
        return ResponseEntity.ok(ApiResponse.success(lookupService.update(lookupId, lookupRq, UserUtil.getUser(request))));
    }
}