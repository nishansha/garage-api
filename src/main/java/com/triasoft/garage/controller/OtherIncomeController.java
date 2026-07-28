package com.triasoft.garage.controller;

import com.triasoft.garage.dto.DirectEntryDTO;
import com.triasoft.garage.model.common.ApiResponse;
import com.triasoft.garage.model.common.FilterRq;
import com.triasoft.garage.model.entry.DirectEntryRq;
import com.triasoft.garage.model.entry.DirectEntryRs;
import com.triasoft.garage.service.impl.OtherIncomeService;
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
@RequestMapping("/api/v1/other-incomes")
public class OtherIncomeController {

    private final OtherIncomeService otherIncomeService;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<DirectEntryRs>> getAll(@RequestParam("page") int page, @RequestParam("size") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("entryDate").descending());
        return ResponseEntity.ok(ApiResponse.success(otherIncomeService.getAll(pageable)));
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<DirectEntryDTO>> get(@PathVariable("id") Long id) {
        return ResponseEntity.ok(ApiResponse.success(otherIncomeService.get(id)));
    }

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<DirectEntryRs>> create(@Valid @RequestBody DirectEntryRq rq) {
        log.info(":: OtherIncomeController - create () - {} ::", rq);
        return ResponseEntity.ok(ApiResponse.success(otherIncomeService.create(rq)));
    }

    @PutMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<DirectEntryRs>> update(@PathVariable("id") Long id, @Valid @RequestBody DirectEntryRq rq) {
        log.info(":: OtherIncomeController - update () - id-{}, {} ::", id, rq);
        return ResponseEntity.ok(ApiResponse.success(otherIncomeService.update(id, rq)));
    }

    @DeleteMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<DirectEntryRs>> delete(@PathVariable("id") Long id) {
        log.info(":: OtherIncomeController - delete () - id - {} ::", id);
        return ResponseEntity.ok(ApiResponse.success(otherIncomeService.delete(id)));
    }

    @PostMapping(value = "/find", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<DirectEntryRs>> find(@RequestBody FilterRq filter, @RequestParam("page") int page, @RequestParam("size") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("entryDate").descending());
        return ResponseEntity.ok(ApiResponse.success(otherIncomeService.search(filter, pageable)));
    }

}
