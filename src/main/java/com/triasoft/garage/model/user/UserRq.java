package com.triasoft.garage.model.user;

import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
@Builder
public class UserRq implements Serializable {
    @Serial
    private static final long serialVersionUID = -5448314308032610312L;
    private String userName;
    private String password;
    private String name;
    private String designation;
    /**
     * Role assignments for this user (fnd_role ids). Required on create; on update,
     * omit/null to leave existing role assignments unchanged, or pass a full list
     * (including an empty list) to replace them.
     */
    private List<Long> roleIds;
}
