package com.triasoft.garage.controller;

import com.triasoft.garage.model.common.FilterRq;
import com.triasoft.garage.model.expense.ExpenseRs;
import com.triasoft.garage.model.expense.ExpenseSummaryRs;
import com.triasoft.garage.service.ExpenseService;
import com.triasoft.garage.util.UserUtil;
import jakarta.servlet.http.HttpServletRequest;
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
@RequestMapping("/api/v1/expense")
public class ExpenseController {

    private final ExpenseService expenseService;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ExpenseRs> expenses(@RequestParam("page") int page, @RequestParam("size") int size, @RequestParam("type") String type, HttpServletRequest request) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("created").descending());
        return ResponseEntity.ok(expenseService.expenses(pageable, UserUtil.getUser(request)));
    }

    @GetMapping(value = "/summary", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ExpenseSummaryRs> summary(HttpServletRequest request) {
        return ResponseEntity.ok(expenseService.summary(UserUtil.getUser(request)));
    }

    @PostMapping(value = "/find", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ExpenseRs> findExpenses(@RequestBody FilterRq filterRq, @RequestParam("page") int page, @RequestParam("size") int size, @RequestParam("type") String type, HttpServletRequest request) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("created").descending());
        return ResponseEntity.ok(expenseService.findExpenses(filterRq, pageable, UserUtil.getUser(request)));
    }
}
