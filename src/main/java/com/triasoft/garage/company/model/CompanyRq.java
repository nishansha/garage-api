package com.triasoft.garage.company.model;

import com.triasoft.garage.locking.Versioned;
import com.triasoft.garage.validation.NullOrNotBlank;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class CompanyRq implements Serializable, Versioned {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long version;

    @NotBlank(message = "REQUIRED")
    @Size(max = 50, message = "MAX_LENGTH")
    private String code;

    @NotBlank(message = "REQUIRED")
    @Size(max = 255, message = "MAX_LENGTH")
    private String name;

    @Size(max = 255, message = "MAX_LENGTH")
    @NullOrNotBlank
    private String registrationNo;

    @Size(max = 255, message = "MAX_LENGTH")
    @NullOrNotBlank
    private String address;

    private boolean active = true;
}
