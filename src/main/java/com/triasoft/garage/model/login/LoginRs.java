package com.triasoft.garage.model.login;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
public class LoginRs implements Serializable {
    @Serial
    private static final long serialVersionUID = 7116522419429601379L;

    private String token;
    private String refreshToken;
    private String fullName;
    /**
     * @deprecated single-role legacy field; use {@link #roles} instead.
     */
    @Deprecated
    private String role;
    private List<String> roles;
}
