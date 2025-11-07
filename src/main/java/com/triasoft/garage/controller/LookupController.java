package com.triasoft.garage.controller;

import com.triasoft.garage.model.common.LookupRq;
import com.triasoft.garage.model.common.LookupRs;
import com.triasoft.garage.service.LookupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/lookup")
public class LookupController {

    private final LookupService lookupService;

    @GetMapping
    ResponseEntity<LookupRs> getLookupValues(@RequestParam(name = "type", required = false) String type) {
        return ResponseEntity.ok(lookupService.getLookupValues(LookupRq.builder().type(type).build()));
    }
}
