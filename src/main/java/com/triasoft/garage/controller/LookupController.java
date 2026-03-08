package com.triasoft.garage.controller;

import com.triasoft.garage.model.common.LookupRq;
import com.triasoft.garage.model.common.LookupRs;
import com.triasoft.garage.service.impl.LookupService;
import com.triasoft.garage.util.UserUtil;
import jakarta.servlet.http.HttpServletRequest;
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
    ResponseEntity<LookupRs> getLookupValues(@RequestParam(name = "type", required = false) String type) {
        return ResponseEntity.ok(lookupService.getLookupValues(LookupRq.builder().type(type).build()));
    }

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<LookupRs> create(@RequestBody LookupRq lookupRq, HttpServletRequest request) {
        return ResponseEntity.ok(lookupService.create(lookupRq, UserUtil.getUser(request)));
    }
}
