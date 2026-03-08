package com.triasoft.garage.controller;

import com.triasoft.garage.model.login.LoginRq;
import com.triasoft.garage.model.login.LoginRs;
import com.triasoft.garage.service.impl.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    @PostMapping(value = "/login", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<LoginRs> authenticateUser(@RequestBody LoginRq request, HttpServletRequest httpServletRequest) {
        return ResponseEntity.ok(authService.login(request));
    }
}
