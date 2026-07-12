package com.triasoft.garage.controller;

import com.triasoft.garage.dto.DirectEntryDTO;
import com.triasoft.garage.model.common.ApiResponse;
import com.triasoft.garage.model.common.FilterRq;
import com.triasoft.garage.model.entry.DirectEntryRq;
import com.triasoft.garage.model.entry.DirectEntryRs;
import com.triasoft.garage.service.impl.DirectEntryService;
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
@RequestMapping("/api/v1/direct-entries")
public class DirectEntryController {

    private final DirectEntryService directEntryService;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<DirectEntryRs>> getAll(
            @RequestParam("page") int page,
            @RequestParam("size") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("entryDate").descending());
        return ResponseEntity.ok(ApiResponse.success(directEntryService.getAll(pageable)));
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<DirectEntryDTO>> get(@PathVariable("id") Long id) {
        return ResponseEntity.ok(ApiResponse.success(directEntryService.get(id)));
    }

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<DirectEntryRs>> create(@Valid @RequestBody DirectEntryRq rq) {
        return ResponseEntity.ok(ApiResponse.success(directEntryService.create(rq)));
    }

    @PutMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<DirectEntryRs>> update(
            @PathVariable("id") Long id,
            @Valid @RequestBody DirectEntryRq rq) {
        return ResponseEntity.ok(ApiResponse.success(directEntryService.update(id, rq)));
    }

    @DeleteMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<DirectEntryRs>> delete(@PathVariable("id") Long id) {
        return ResponseEntity.ok(ApiResponse.success(directEntryService.delete(id)));
    }

    @PostMapping(value = "/find", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<DirectEntryRs>> find(
            @RequestBody FilterRq filter,
            @RequestParam("page") int page,
            @RequestParam("size") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("entryDate").descending());
        return ResponseEntity.ok(ApiResponse.success(directEntryService.search(filter, pageable)));
    }

}
