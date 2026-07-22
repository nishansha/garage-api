package com.triasoft.garage.dto;

import com.triasoft.garage.constants.Privilege;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PrivilegeGrantDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private Long resourceId;
    private String resourceCode;
    private List<Privilege> privileges;
}
