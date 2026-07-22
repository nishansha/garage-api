package com.triasoft.garage.model.role;

import com.triasoft.garage.dto.PrivilegeGrantDTO;
import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
@Builder
public class RolePrivilegeRs implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private List<PrivilegeGrantDTO> grants;
}
