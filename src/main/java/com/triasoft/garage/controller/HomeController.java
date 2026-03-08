package com.triasoft.garage.controller;

import com.triasoft.garage.model.home.ActivityRs;
import com.triasoft.garage.model.home.ChartRs;
import com.triasoft.garage.model.home.OverviewRs;
import com.triasoft.garage.model.home.SummaryRs;
import com.triasoft.garage.service.impl.HomeService;
import com.triasoft.garage.util.UserUtil;
import jakarta.servlet.http.HttpServletRequest;
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
@RequestMapping("/api/v1/home")
public class HomeController {

    private final HomeService homeService;

    @GetMapping(value = "/summary", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<SummaryRs> summaryData(HttpServletRequest request) {
        return ResponseEntity.ok(homeService.summaryData(UserUtil.getUser(request)));
    }

    @GetMapping(value = "/charts", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ChartRs> getChartData(@RequestParam("type") String type, HttpServletRequest request) {
        return ResponseEntity.ok(homeService.getChartData(type, UserUtil.getUser(request)));
    }

    @GetMapping(value = "/activities", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ActivityRs> getActivities(HttpServletRequest request) {
        return ResponseEntity.ok(homeService.getActivities(UserUtil.getUser(request)));
    }

    @GetMapping(value = "/overview", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<OverviewRs> getOverview(@RequestParam("months") Integer months, HttpServletRequest request) {
        return ResponseEntity.ok(homeService.getOverview(months, UserUtil.getUser(request)));
    }

}
