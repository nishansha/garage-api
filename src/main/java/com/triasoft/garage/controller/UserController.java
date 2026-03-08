package com.triasoft.garage.controller;

import com.triasoft.garage.dto.SaleDTO;
import com.triasoft.garage.dto.UserDTO;
import com.triasoft.garage.model.user.UserRq;
import com.triasoft.garage.model.user.UserRs;
import com.triasoft.garage.service.impl.UserService;
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
@RequestMapping("/api/v1/user")
public class UserController {

    private final UserService userService;

    @GetMapping(value = "/staff", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<UserRs> getStaffs(HttpServletRequest request) {
        return ResponseEntity.ok(userService.getStaffs(UserUtil.getUser(request)));
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<UserRs> getAll(HttpServletRequest request) {
        return ResponseEntity.ok(userService.getAll(UserUtil.getUser(request)));
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<UserDTO> get(@PathVariable("id") Long id, HttpServletRequest request) {
        return ResponseEntity.ok(userService.get(id, UserUtil.getUser(request)));
    }

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<UserRs> create(@RequestBody UserRq userRq, HttpServletRequest request) {
        return ResponseEntity.ok(userService.create(userRq, UserUtil.getUser(request)));
    }

    @PutMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<UserRs> update(@RequestBody UserRq userRq, @PathVariable("id") Long id, HttpServletRequest request) {
        return ResponseEntity.ok(userService.update(id, userRq, UserUtil.getUser(request)));
    }

}
