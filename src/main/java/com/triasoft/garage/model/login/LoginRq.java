package com.triasoft.garage.model.login;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRq {

    @NotBlank(message = "REQUIRED")
    private String username;

    @NotBlank(message = "REQUIRED")
    private String password;
}
