package com.triasoft.garage.model.role;

import com.triasoft.garage.constants.Privilege;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
@Builder
public class PrivilegeGrantRq implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull(message = "REQUIRED")
    private Long resourceId;

    private List<Privilege> privileges;
}
