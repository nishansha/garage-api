package com.triasoft.garage.model.login;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RefreshRq {

    @NotBlank(message = "REQUIRED")
    private String refreshToken;
}
