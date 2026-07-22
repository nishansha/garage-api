package com.triasoft.garage.model.user;

import com.triasoft.garage.dto.RoleDTO;
import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
@Builder
public class UserRoleRs implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private List<RoleDTO> roles;
}
