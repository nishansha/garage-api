package com.triasoft.garage.controller;

import com.triasoft.garage.constants.Privilege;
import com.triasoft.garage.dto.UserDTO;
import com.triasoft.garage.model.common.ApiResponse;
import com.triasoft.garage.model.user.UserRoleRq;
import com.triasoft.garage.model.user.UserRoleRs;
import com.triasoft.garage.model.user.UserRq;
import com.triasoft.garage.model.user.UserRs;
import com.triasoft.garage.security.rbac.HasPrivilege;
import com.triasoft.garage.service.impl.RoleService;
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
    private final RoleService roleService;

    @GetMapping(value = "/staff", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<UserRs>> getStaffs(HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.success(userService.getStaffs(UserUtil.getUser(request))));
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<UserRs>> getAll(HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.success(userService.getAll(UserUtil.getUser(request))));
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<UserDTO>> get(@PathVariable("id") Long id, HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.success(userService.get(id, UserUtil.getUser(request))));
    }

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<UserRs>> create(@RequestBody UserRq userRq, HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.success(userService.create(userRq, UserUtil.getUser(request))));
    }

    @PutMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<UserRs>> update(@RequestBody UserRq userRq, @PathVariable("id") Long id, HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.success(userService.update(id, userRq, UserUtil.getUser(request))));
    }

    @HasPrivilege(resource = "USER", privilege = Privilege.VIEW)
    @GetMapping(value = "/{id}/roles", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<UserRoleRs>> getRoles(@PathVariable("id") Long id) {
        return ResponseEntity.ok(ApiResponse.success(roleService.getUserRoles(id)));
    }

    @HasPrivilege(resource = "USER", privilege = Privilege.UPDATE)
    @PutMapping(value = "/{id}/roles", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<UserRoleRs>> assignRoles(@PathVariable("id") Long id, @RequestBody UserRoleRq rq) {
        return ResponseEntity.ok(ApiResponse.success(roleService.assignUserRoles(id, rq)));
    }
}