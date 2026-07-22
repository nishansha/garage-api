package com.triasoft.garage.controller;

import com.triasoft.garage.constants.Privilege;
import com.triasoft.garage.model.admin.DataResetRs;
import com.triasoft.garage.model.common.ApiResponse;
import com.triasoft.garage.rbac.HasPrivilege;
import com.triasoft.garage.service.impl.AdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final AdminService adminService;

    /**
     * Clears all application-inserted (transactional) data for a test environment while keeping
     * seed/foundation tables, app configuration and user accounts intact.
     * Guarded by the app.data-reset.enabled flag (returns BUS_180 when disabled).
     */
    @HasPrivilege(resource = "DATA_RESET", privilege = Privilege.DELETE)
    @PostMapping(value = "/reset-data", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponse<DataResetRs>> resetData() {
        return ResponseEntity.ok(ApiResponse.success(adminService.resetData()));
    }
}
