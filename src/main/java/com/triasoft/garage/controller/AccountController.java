package com.triasoft.garage.controller;

import com.triasoft.garage.dto.ChatOfAccountDTO;
import com.triasoft.garage.model.account.AccountRq;
import com.triasoft.garage.model.account.AccountRs;
import com.triasoft.garage.model.common.ApiResponse;
import com.triasoft.garage.service.impl.AccountService;
import com.triasoft.garage.util.UserUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/account")
public class AccountController {

    private final AccountService accountService;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<AccountRs>> getChartOfAccounts(
            @RequestParam(name = "type", required = false) String type,
            @RequestParam(name = "directPostable", required = false) Boolean directPostable) {
        return ResponseEntity.ok(ApiResponse.success(accountService.getAccounts(
                AccountRq.builder().type(type).directPostable(directPostable).build())));
    }

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<AccountRs>> create(@RequestBody AccountRq accountRq, HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.success(accountService.create(accountRq, UserUtil.getUser(request))));
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<ChatOfAccountDTO>> get(@PathVariable("id") Long id, HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.success(accountService.get(id, UserUtil.getUser(request))));
    }

    @PutMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<AccountRs>> update(@RequestBody AccountRq accountRq, @PathVariable("id") Long id, HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.success(accountService.update(id, accountRq, UserUtil.getUser(request))));
    }

    @DeleteMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<AccountRs>> delete(@PathVariable("id") Long id, HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.success(accountService.delete(id, UserUtil.getUser(request))));
    }
}