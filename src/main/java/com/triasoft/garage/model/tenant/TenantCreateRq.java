package com.triasoft.garage.model.tenant;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class TenantCreateRq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull(message = "REQUIRED")
    @Valid
    private TenantPart tenant;

    @NotNull(message = "REQUIRED")
    @Valid
    private SuperuserPart superuser;

    @Data
    public static class TenantPart implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        @NotBlank(message = "REQUIRED")
        private String code;

        @NotBlank(message = "REQUIRED")
        private String name;
    }

    @Data
    public static class SuperuserPart implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        @NotBlank(message = "REQUIRED")
        private String username;

        @NotBlank(message = "REQUIRED")
        private String password;

        @NotBlank(message = "REQUIRED")
        private String name;

        private String designation;
    }
}
