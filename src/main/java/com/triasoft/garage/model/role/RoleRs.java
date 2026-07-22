package com.triasoft.garage.model.role;

import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
@Builder
public class RoleRs implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private Long id;
}
