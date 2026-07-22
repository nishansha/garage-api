package com.triasoft.garage.model.role;

import com.triasoft.garage.locking.Versioned;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
@Builder
public class RoleRq implements Serializable, Versioned {
    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "REQUIRED")
    private String code;

    @NotBlank(message = "REQUIRED")
    private String name;

    private String description;

    private Long version;
}
