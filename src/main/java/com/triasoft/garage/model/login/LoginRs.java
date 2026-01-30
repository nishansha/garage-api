package com.triasoft.garage.model.login;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class LoginRs implements Serializable {
    @Serial
    private static final long serialVersionUID = 7116522419429601379L;

    private String token;
    private String refreshToken;
    private String fullName;
}
