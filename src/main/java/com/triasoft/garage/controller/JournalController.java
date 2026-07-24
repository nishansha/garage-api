package com.triasoft.garage.controller;

import com.triasoft.garage.constants.JournalStatusEnum;
import com.triasoft.garage.entity.Journal;
import com.triasoft.garage.model.common.ApiResponse;
import com.triasoft.garage.model.journal.JournalDetailRs;
import com.triasoft.garage.model.journal.JournalListRs;
import com.triasoft.garage.model.journal.JournalRq;
import com.triasoft.garage.model.journal.JournalRs;
import com.triasoft.garage.model.journal.LedgerRs;
import com.triasoft.garage.service.impl.JournalQueryService;
import com.triasoft.garage.service.impl.JournalReportCsvWriter;
import com.triasoft.garage.service.impl.JournalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/journals")
public class JournalController {

    private final JournalQueryService journalQueryService;
    private final JournalService journalService;
    private final JournalReportCsvWriter journalReportCsvWriter;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<JournalListRs>> list(@RequestParam(value = "referenceType", required = false) String referenceType, @RequestParam(value = "status", required = false) JournalStatusEnum status, @RequestParam(value = "fromDate", required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate fromDate, @RequestParam(value = "toDate", required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate toDate, @RequestParam("page") int page, @RequestParam("size") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("journalDate").descending().and(Sort.by("id").descending()));
        return ResponseEntity.ok(ApiResponse.success(journalQueryService.list(referenceType, status, fromDate, toDate, pageable)));
    }

    @GetMapping(value = "/by-reference", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<JournalDetailRs>> getByReference(@RequestParam("type") String referenceType, @RequestParam("id") Long referenceId) {
        return ResponseEntity.ok(ApiResponse.success(journalQueryService.getByReference(referenceType, referenceId)));
    }

    @GetMapping(value = "/ledger/{accountId}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<LedgerRs>> getLedger(@PathVariable("accountId") Long accountId, @RequestParam(value = "fromDate", required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate fromDate, @RequestParam(value = "toDate", required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate toDate) {
        return ResponseEntity.ok(ApiResponse.success(journalQueryService.getLedger(accountId, fromDate, toDate)));
    }

    @GetMapping(value = "/ledger/{accountId}/csv", produces = "text/csv")
    ResponseEntity<byte[]> downloadLedgerCsv(@PathVariable("accountId") Long accountId, @RequestParam(value = "fromDate", required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate fromDate, @RequestParam(value = "toDate", required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate toDate) {
        LedgerRs rs = journalQueryService.getLedger(accountId, fromDate, toDate);
        String code = rs.getAccount() != null ? rs.getAccount().getCode() : String.valueOf(accountId);
        String filename = "ledger-" + code + "-" + rs.getFromDate() + "_" + rs.getToDate() + ".csv";
        byte[] body = ("\uFEFF" + journalReportCsvWriter.ledgerCsv(rs)).getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"").contentType(MediaType.parseMediaType("text/csv")).body(body);
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<JournalDetailRs>> get(@PathVariable("id") Long id) {
        return ResponseEntity.ok(ApiResponse.success(journalQueryService.getById(id)));
    }

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<JournalRs>> create(@RequestBody JournalRq rq) {
        log.info(":: JournalController - create () - {} ::", rq);
        Journal journal = journalService.createManual(rq);
        return ResponseEntity.ok(ApiResponse.success(JournalRs.builder().id(journal.getId()).referenceType(journal.getReferenceType()).referenceId(journal.getReferenceId()).build()));
    }

}
