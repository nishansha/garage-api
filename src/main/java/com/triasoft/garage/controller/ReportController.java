package com.triasoft.garage.controller;

import com.triasoft.garage.exception.BusinessException;
import com.triasoft.garage.model.common.ApiResponse;
import com.triasoft.garage.model.report.BalanceSheetRs;
import com.triasoft.garage.model.report.MonthlyTrendRs;
import com.triasoft.garage.model.report.PLFromJournalRs;
import com.triasoft.garage.model.report.PLReportRs;
import com.triasoft.garage.model.report.PayablesSummaryRs;
import com.triasoft.garage.model.report.ReceivablesSummaryRs;
import com.triasoft.garage.model.report.TrialBalanceRs;
import com.triasoft.garage.service.impl.JournalQueryService;
import com.triasoft.garage.service.impl.JournalReportCsvWriter;
import com.triasoft.garage.service.impl.PLReportCsvWriter;
import com.triasoft.garage.service.impl.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/reports")
public class ReportController {

    private final ReportService reportService;
    private final JournalQueryService journalQueryService;
    private final PLReportCsvWriter plReportCsvWriter;
    private final JournalReportCsvWriter journalReportCsvWriter;

    @GetMapping(value = "/pl", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<PLReportRs>> getProfitAndLoss(@RequestParam(value = "month", required = false) String month) {
        YearMonth yearMonth = parseMonth(month);
        return ResponseEntity.ok(ApiResponse.success(reportService.getProfitAndLoss(yearMonth)));
    }

    @GetMapping(value = "/pl/csv", produces = "text/csv")
    ResponseEntity<byte[]> downloadProfitAndLossCsv(@RequestParam(value = "month", required = false) String month) {
        YearMonth yearMonth = parseMonth(month);
        String csv = plReportCsvWriter.toCsv(reportService.getProfitAndLoss(yearMonth));
        return csvResponse(csv, "business-summary-" + yearMonth + ".csv");
    }

    @GetMapping(value = "/trend", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<MonthlyTrendRs>> getMonthlyTrend(@RequestParam(value = "months", defaultValue = "6") int months) {
        if (months < 1 || months > 12) {
            throw new BusinessException("RPT_401", "months must be between 1 and 12");
        }
        return ResponseEntity.ok(ApiResponse.success(reportService.getMonthlyTrend(months)));
    }

    @GetMapping(value = "/trial-balance", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<TrialBalanceRs>> getTrialBalance(@RequestParam(value = "asOfDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate, @RequestParam(value = "includeZeroBalance", defaultValue = "false") boolean includeZeroBalance) {
        return ResponseEntity.ok(ApiResponse.success(journalQueryService.getTrialBalance(asOfDate, includeZeroBalance)));
    }

    @GetMapping(value = "/trial-balance/csv", produces = "text/csv")
    ResponseEntity<byte[]> downloadTrialBalanceCsv(@RequestParam(value = "asOfDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate, @RequestParam(value = "includeZeroBalance", defaultValue = "false") boolean includeZeroBalance) {
        var rs = journalQueryService.getTrialBalance(asOfDate, includeZeroBalance);
        return csvResponse(journalReportCsvWriter.trialBalanceCsv(rs), "trial-balance-" + rs.getAsOfDate() + ".csv");
    }

    @GetMapping(value = "/balance-sheet", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<BalanceSheetRs>> getBalanceSheet(@RequestParam(value = "asOfDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate) {
        return ResponseEntity.ok(ApiResponse.success(journalQueryService.getBalanceSheet(asOfDate)));
    }

    @GetMapping(value = "/balance-sheet/csv", produces = "text/csv")
    ResponseEntity<byte[]> downloadBalanceSheetCsv(@RequestParam(value = "asOfDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate) {
        var rs = journalQueryService.getBalanceSheet(asOfDate);
        return csvResponse(journalReportCsvWriter.balanceSheetCsv(rs), "balance-sheet-" + rs.getAsOfDate() + ".csv");
    }

    @GetMapping(value = "/receivables", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<ReceivablesSummaryRs>> getReceivablesSummary() {
        return ResponseEntity.ok(ApiResponse.success(reportService.getReceivablesSummary()));
    }

    @GetMapping(value = "/payables", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<PayablesSummaryRs>> getPayablesSummary() {
        return ResponseEntity.ok(ApiResponse.success(reportService.getPayablesSummary()));
    }

    @GetMapping(value = "/pl-from-journal", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<PLFromJournalRs>> getPLFromJournal(@RequestParam(value = "fromDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate, @RequestParam(value = "toDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        return ResponseEntity.ok(ApiResponse.success(journalQueryService.getPLFromJournal(fromDate, toDate)));
    }

    @GetMapping(value = "/pl-from-journal/csv", produces = "text/csv")
    ResponseEntity<byte[]> downloadPLFromJournalCsv(@RequestParam(value = "fromDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate, @RequestParam(value = "toDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        var rs = journalQueryService.getPLFromJournal(fromDate, toDate);
        return csvResponse(journalReportCsvWriter.plFromJournalCsv(rs), "pl-from-journal-" + rs.getFromDate() + "_" + rs.getToDate() + ".csv");
    }

    private ResponseEntity<byte[]> csvResponse(String csv, String filename) {
        byte[] body = ("\uFEFF" + csv).getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"").contentType(MediaType.parseMediaType("text/csv")).body(body);
    }

    private YearMonth parseMonth(String month) {
        if (month == null || month.isBlank()) {
            return YearMonth.now();
        }
        try {
            return YearMonth.parse(month);
        } catch (DateTimeParseException e) {
            throw new BusinessException("RPT_400", "Invalid month format. Use YYYY-MM (e.g. 2026-06)");
        }
    }

}
