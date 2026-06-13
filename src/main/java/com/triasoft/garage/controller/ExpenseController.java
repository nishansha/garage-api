package com.triasoft.garage.controller;

import com.triasoft.garage.dto.ExpenseDTO;
import com.triasoft.garage.model.common.ApiResponse;
import com.triasoft.garage.model.common.FilterRq;
import com.triasoft.garage.model.expense.ExpenseRq;
import com.triasoft.garage.model.expense.ExpenseRs;
import com.triasoft.garage.model.expense.ExpenseSummaryRs;
import com.triasoft.garage.service.impl.ExpenseService;
import com.triasoft.garage.util.UserUtil;

import java.util.List;

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
    ResponseEntity<ApiResponse<?>> expenses(@RequestParam("page") int page, @RequestParam("size") int size, @RequestParam("type") String type, HttpServletRequest request) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        if ("P".equalsIgnoreCase(type)) {
            return ResponseEntity.ok(ApiResponse.success(expenseService.getPurchasesWithExpenses(pageable, UserUtil.getUser(request))));
        }
        return ResponseEntity.ok(ApiResponse.success(expenseService.getAll(pageable, UserUtil.getUser(request))));
    }

    @GetMapping(value = "/purchase/{purchaseId}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<List<ExpenseDTO>>> getByPurchase(@PathVariable("purchaseId") Long purchaseId, HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.success(expenseService.getExpensesByPurchase(purchaseId, UserUtil.getUser(request))));
    }

    @GetMapping(value = "/summary", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<ExpenseSummaryRs>> summary(HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.success(expenseService.summary(UserUtil.getUser(request))));
    }

    @PostMapping(value = "/find", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<ExpenseRs>> findExpenses(@RequestBody FilterRq filterRq, @RequestParam("page") int page, @RequestParam("size") int size, @RequestParam(name = "type", required = false) String type, HttpServletRequest request) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(ApiResponse.success(expenseService.search(filterRq, type, pageable, UserUtil.getUser(request))));
    }

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<ExpenseRs>> create(@RequestBody ExpenseRq expenseRq, HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.success(expenseService.create(expenseRq, UserUtil.getUser(request))));
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<ExpenseDTO>> get(@PathVariable("id") Long id, HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.success(expenseService.get(id, UserUtil.getUser(request))));
    }

    @PutMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<ExpenseRs>> update(@RequestBody ExpenseRq expenseRq, @PathVariable("id") Long id, HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.success(expenseService.update(id, expenseRq, UserUtil.getUser(request))));
    }

    @DeleteMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<ExpenseRs>> delete(@PathVariable("id") Long id, HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.success(expenseService.delete(id, UserUtil.getUser(request))));
    }
}