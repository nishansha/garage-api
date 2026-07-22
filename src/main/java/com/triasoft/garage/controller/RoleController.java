package com.triasoft.garage.controller;

import com.triasoft.garage.constants.Privilege;
import com.triasoft.garage.model.common.ApiResponse;
import com.triasoft.garage.model.role.MyPermissionsRs;
import com.triasoft.garage.model.role.ResourceTreeRs;
import com.triasoft.garage.model.role.RolePrivilegeRq;
import com.triasoft.garage.model.role.RolePrivilegeRs;
import com.triasoft.garage.model.role.RoleRq;
import com.triasoft.garage.model.role.RoleRs;
import com.triasoft.garage.model.role.RolesRs;
import com.triasoft.garage.security.rbac.HasPrivilege;
import com.triasoft.garage.service.impl.RoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/roles")
public class RoleController {

    private final RoleService roleService;

    @GetMapping(value = "/me/permissions", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<MyPermissionsRs>> getMyPermissions() {
        return ResponseEntity.ok(ApiResponse.success(roleService.getMyPermissions()));
    }

    @HasPrivilege(resource = "ROLE", privilege = Privilege.VIEW)
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<RolesRs>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(roleService.getAll()));
    }

    @HasPrivilege(resource = "ROLE", privilege = Privilege.VIEW)
    @GetMapping(value = "/resources", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<ResourceTreeRs>> getResourceTree() {
        return ResponseEntity.ok(ApiResponse.success(roleService.getResourceTree()));
    }

    @HasPrivilege(resource = "ROLE", privilege = Privilege.CREATE)
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<RoleRs>> create(@RequestBody RoleRq rq) {
        return ResponseEntity.ok(ApiResponse.success(roleService.create(rq)));
    }

    @HasPrivilege(resource = "ROLE", privilege = Privilege.UPDATE)
    @PutMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<RoleRs>> update(@PathVariable("id") Long id, @RequestBody RoleRq rq) {
        return ResponseEntity.ok(ApiResponse.success(roleService.update(id, rq)));
    }

    @HasPrivilege(resource = "ROLE", privilege = Privilege.DELETE)
    @DeleteMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<Void>> delete(@PathVariable("id") Long id) {
        roleService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @HasPrivilege(resource = "ROLE", privilege = Privilege.VIEW)
    @GetMapping(value = "/{id}/privileges", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<RolePrivilegeRs>> getPrivileges(@PathVariable("id") Long id) {
        return ResponseEntity.ok(ApiResponse.success(roleService.getPrivileges(id)));
    }

    @HasPrivilege(resource = "ROLE", privilege = Privilege.UPDATE)
    @PutMapping(value = "/{id}/privileges", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<RolePrivilegeRs>> updatePrivileges(@PathVariable("id") Long id, @RequestBody RolePrivilegeRq rq) {
        return ResponseEntity.ok(ApiResponse.success(roleService.updatePrivileges(id, rq)));
    }
}
