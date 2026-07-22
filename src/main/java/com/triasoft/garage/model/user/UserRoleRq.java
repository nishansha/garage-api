package com.triasoft.garage.model.user;

import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
@Builder
public class UserRoleRq implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private List<Long> roleIds;
}
