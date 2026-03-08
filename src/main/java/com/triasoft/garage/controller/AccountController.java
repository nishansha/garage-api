package com.triasoft.garage.controller;

import com.triasoft.garage.dto.ChatOfAccountDTO;
import com.triasoft.garage.model.account.AccountRq;
import com.triasoft.garage.model.account.AccountRs;
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
    ResponseEntity<AccountRs> getChartOfAccounts(@RequestParam(name = "type", required = false) String type) {
        return ResponseEntity.ok(accountService.getAccounts(AccountRq.builder().type(type).build()));
    }

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<AccountRs> create(@RequestBody AccountRq accountRq, HttpServletRequest request) {
        return ResponseEntity.ok(accountService.create(accountRq, UserUtil.getUser(request)));
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ChatOfAccountDTO> get(@PathVariable("id") Long id, HttpServletRequest request) {
        return ResponseEntity.ok(accountService.get(id, UserUtil.getUser(request)));
    }

    @PutMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<AccountRs> update(@RequestBody AccountRq accountRq, @PathVariable("id") Long id, HttpServletRequest request) {
        return ResponseEntity.ok(accountService.update(id, accountRq, UserUtil.getUser(request)));
    }

    @DeleteMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<AccountRs> delete(@PathVariable("id") Long id, HttpServletRequest request) {
        return ResponseEntity.ok(accountService.delete(id, UserUtil.getUser(request)));
    }
}
