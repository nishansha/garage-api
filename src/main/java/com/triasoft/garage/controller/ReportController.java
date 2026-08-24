package com.triasoft.garage.controller;

import com.triasoft.garage.company.service.CompanyResolver;
import com.triasoft.garage.exception.BusinessException;
import com.triasoft.garage.model.common.ApiResponse;
import com.triasoft.garage.model.report.BalanceSheetRs;
import com.triasoft.garage.model.report.FinanceReceivablesSummaryRs;
import com.triasoft.garage.model.report.MonthlyTrendRs;
import com.triasoft.garage.model.report.PLFromJournalRs;
import com.triasoft.garage.model.report.PLReportRs;
import com.triasoft.garage.model.report.PayablesSummaryRs;
import com.triasoft.garage.model.report.ReceivablesSummaryRs;
import com.triasoft.garage.model.report.SalaryPayablesSummaryRs;
import com.triasoft.garage.model.report.ServiceReceivablesSummaryRs;
import com.triasoft.garage.model.report.TrialBalanceRs;
import com.triasoft.garage.model.report.WarehouseComparisonRs;
import com.triasoft.garage.ledger.service.LedgerQueryService;
import com.triasoft.garage.service.impl.JournalReportCsvWriter;
import com.triasoft.garage.service.impl.PLReportCsvWriter;
import com.triasoft.garage.service.impl.ReportService;
import com.triasoft.garage.service.impl.WarehouseReportService;
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
    private final LedgerQueryService journalQueryService;
    private final WarehouseReportService warehouseReportService;
    private final PLReportCsvWriter plReportCsvWriter;
    private final JournalReportCsvWriter journalReportCsvWriter;
    private final CompanyResolver companyResolver;

    // companyId omitted = overall report combining every company's books (ReportService.
    // getProfitAndLoss aggregates across all companies when passed null) - unlike most other
    // report endpoints below, which still require disambiguating once there's more than one
    // company (see CompanyResolver.resolveCompanyId).
    // warehouseId omitted = every warehouse (within whatever companyId scope applies) - see
    // ReportService.getProfitAndLoss for which figures aren't warehouse-attributable.
    @GetMapping(value = "/pl", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<PLReportRs>> getProfitAndLoss(@RequestParam(value = "month", required = false) String month, @RequestParam(value = "companyId", required = false) Long companyId, @RequestParam(value = "warehouseId", required = false) Long warehouseId) {
        YearMonth yearMonth = parseMonth(month);
        return ResponseEntity.ok(ApiResponse.success(reportService.getProfitAndLoss(yearMonth, companyResolver.resolveOptionalCompanyId(companyId), warehouseId)));
    }

    @GetMapping(value = "/pl/csv", produces = "text/csv")
    ResponseEntity<byte[]> downloadProfitAndLossCsv(@RequestParam(value = "month", required = false) String month, @RequestParam(value = "companyId", required = false) Long companyId, @RequestParam(value = "warehouseId", required = false) Long warehouseId) {
        YearMonth yearMonth = parseMonth(month);
        String csv = plReportCsvWriter.toCsv(reportService.getProfitAndLoss(yearMonth, companyResolver.resolveOptionalCompanyId(companyId), warehouseId));
        return csvResponse(csv, "business-summary-" + yearMonth + ".csv");
    }

    @GetMapping(value = "/warehouse-comparison", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<WarehouseComparisonRs>> getWarehouseComparison(@RequestParam(value = "month", required = false) String month, @RequestParam(value = "warehouseId", required = false) Long warehouseId) {
        YearMonth yearMonth = parseMonth(month);
        return ResponseEntity.ok(ApiResponse.success(warehouseReportService.compare(yearMonth, warehouseId)));
    }

    @GetMapping(value = "/trend", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<MonthlyTrendRs>> getMonthlyTrend(@RequestParam(value = "months", defaultValue = "6") int months) {
        if (months < 1 || months > 12) {
            throw new BusinessException("RPT_401", "months must be between 1 and 12");
        }
        return ResponseEntity.ok(ApiResponse.success(reportService.getMonthlyTrend(months)));
    }

    // companyId omitted = combined trial balance across every company's accounts, same
    // "overall" convention as /pl above.
    @GetMapping(value = "/trial-balance", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<TrialBalanceRs>> getTrialBalance(@RequestParam(value = "asOfDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate, @RequestParam(value = "includeZeroBalance", defaultValue = "false") boolean includeZeroBalance, @RequestParam(value = "companyId", required = false) Long companyId) {
        return ResponseEntity.ok(ApiResponse.success(journalQueryService.getTrialBalance(asOfDate, includeZeroBalance, companyResolver.resolveOptionalCompanyId(companyId))));
    }

    @GetMapping(value = "/trial-balance/csv", produces = "text/csv")
    ResponseEntity<byte[]> downloadTrialBalanceCsv(@RequestParam(value = "asOfDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate, @RequestParam(value = "includeZeroBalance", defaultValue = "false") boolean includeZeroBalance, @RequestParam(value = "companyId", required = false) Long companyId) {
        var rs = journalQueryService.getTrialBalance(asOfDate, includeZeroBalance, companyResolver.resolveOptionalCompanyId(companyId));
        return csvResponse(journalReportCsvWriter.trialBalanceCsv(rs), "trial-balance-" + rs.getAsOfDate() + ".csv");
    }

    @GetMapping(value = "/balance-sheet", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<BalanceSheetRs>> getBalanceSheet(@RequestParam(value = "asOfDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate, @RequestParam(value = "companyId", required = false) Long companyId) {
        return ResponseEntity.ok(ApiResponse.success(journalQueryService.getBalanceSheet(asOfDate, companyResolver.resolveOptionalCompanyId(companyId))));
    }

    @GetMapping(value = "/balance-sheet/csv", produces = "text/csv")
    ResponseEntity<byte[]> downloadBalanceSheetCsv(@RequestParam(value = "asOfDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate, @RequestParam(value = "companyId", required = false) Long companyId) {
        var rs = journalQueryService.getBalanceSheet(asOfDate, companyResolver.resolveOptionalCompanyId(companyId));
        return csvResponse(journalReportCsvWriter.balanceSheetCsv(rs), "balance-sheet-" + rs.getAsOfDate() + ".csv");
    }

    @GetMapping(value = "/receivables", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<ReceivablesSummaryRs>> getReceivablesSummary(@RequestParam(value = "companyId", required = false) Long companyId) {
        return ResponseEntity.ok(ApiResponse.success(reportService.getReceivablesSummary(companyResolver.resolveCompanyId(companyId))));
    }

    @GetMapping(value = "/service-receivables", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<ServiceReceivablesSummaryRs>> getServiceReceivablesSummary(@RequestParam(value = "companyId", required = false) Long companyId) {
        return ResponseEntity.ok(ApiResponse.success(reportService.getServiceReceivablesSummary(companyResolver.resolveCompanyId(companyId))));
    }

    @GetMapping(value = "/finance-receivables", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<FinanceReceivablesSummaryRs>> getFinanceReceivablesSummary(@RequestParam(value = "companyId", required = false) Long companyId) {
        return ResponseEntity.ok(ApiResponse.success(reportService.getFinanceReceivablesSummary(companyResolver.resolveCompanyId(companyId))));
    }

    @GetMapping(value = "/payables", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<PayablesSummaryRs>> getPayablesSummary(@RequestParam(value = "companyId", required = false) Long companyId) {
        return ResponseEntity.ok(ApiResponse.success(reportService.getPayablesSummary(companyResolver.resolveCompanyId(companyId))));
    }

    @GetMapping(value = "/salary-payables", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<SalaryPayablesSummaryRs>> getSalaryPayablesSummary(@RequestParam(value = "companyId", required = false) Long companyId) {
        return ResponseEntity.ok(ApiResponse.success(reportService.getSalaryPayablesSummary(companyResolver.resolveCompanyId(companyId))));
    }

    @GetMapping(value = "/pl-from-journal", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<PLFromJournalRs>> getPLFromJournal(@RequestParam(value = "fromDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate, @RequestParam(value = "toDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate, @RequestParam(value = "companyId", required = false) Long companyId) {
        return ResponseEntity.ok(ApiResponse.success(journalQueryService.getPLFromJournal(fromDate, toDate, companyResolver.resolveOptionalCompanyId(companyId))));
    }

    @GetMapping(value = "/pl-from-journal/csv", produces = "text/csv")
    ResponseEntity<byte[]> downloadPLFromJournalCsv(@RequestParam(value = "fromDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate, @RequestParam(value = "toDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate, @RequestParam(value = "companyId", required = false) Long companyId) {
        var rs = journalQueryService.getPLFromJournal(fromDate, toDate, companyResolver.resolveOptionalCompanyId(companyId));
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
