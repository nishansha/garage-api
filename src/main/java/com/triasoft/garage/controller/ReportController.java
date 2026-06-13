package com.triasoft.garage.controller;

import com.triasoft.garage.exception.BusinessException;
import com.triasoft.garage.model.common.ApiResponse;
import com.triasoft.garage.model.report.MonthlyTrendRs;
import com.triasoft.garage.model.report.PLReportRs;
import com.triasoft.garage.service.impl.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;
import java.time.format.DateTimeParseException;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/reports")
public class ReportController {

    private final ReportService reportService;

    @GetMapping(value = "/pl", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<PLReportRs>> getProfitAndLoss(
            @RequestParam(value = "month", required = false) String month) {
        YearMonth yearMonth = parseMonth(month);
        return ResponseEntity.ok(ApiResponse.success(reportService.getProfitAndLoss(yearMonth)));
    }

    @GetMapping(value = "/trend", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<MonthlyTrendRs>> getMonthlyTrend(
            @RequestParam(value = "months", defaultValue = "6") int months) {
        if (months < 1 || months > 12) {
            throw new BusinessException("RPT_401", "months must be between 1 and 12");
        }
        return ResponseEntity.ok(ApiResponse.success(reportService.getMonthlyTrend(months)));
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
