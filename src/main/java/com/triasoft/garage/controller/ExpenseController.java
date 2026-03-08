package com.triasoft.garage.controller;

import com.triasoft.garage.dto.ExpenseDTO;
import com.triasoft.garage.model.common.FilterRq;
import com.triasoft.garage.model.expense.ExpenseRq;
import com.triasoft.garage.model.expense.ExpenseRs;
import com.triasoft.garage.model.expense.ExpenseSummaryRs;
import com.triasoft.garage.service.impl.ExpenseService;
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
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(expenseService.getAll(pageable,type, UserUtil.getUser(request)));
    }

    @GetMapping(value = "/summary", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ExpenseSummaryRs> summary(HttpServletRequest request) {
        return ResponseEntity.ok(expenseService.summary(UserUtil.getUser(request)));
    }

    @PostMapping(value = "/find", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ExpenseRs> findExpenses(@RequestBody FilterRq filterRq, @RequestParam("page") int page, @RequestParam("size") int size, @RequestParam("type") String type, HttpServletRequest request) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(expenseService.search(filterRq,type, pageable, UserUtil.getUser(request)));
    }

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ExpenseRs> create(@RequestBody ExpenseRq expenseRq, HttpServletRequest request) {
        return ResponseEntity.ok(expenseService.create(expenseRq, UserUtil.getUser(request)));
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ExpenseDTO> get(@PathVariable("id") Long id, HttpServletRequest request) {
        return ResponseEntity.ok(expenseService.get(id, UserUtil.getUser(request)));
    }

    @PutMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ExpenseRs> update(@RequestBody ExpenseRq expenseRq, @PathVariable("id") Long id, HttpServletRequest request) {
        return ResponseEntity.ok(expenseService.update(id, expenseRq, UserUtil.getUser(request)));
    }

    @DeleteMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ExpenseRs> delete(@PathVariable("id") Long id, HttpServletRequest request) {
        return ResponseEntity.ok(expenseService.delete(id, UserUtil.getUser(request)));
    }
}
