package com.triasoft.garage.model.finance;

import com.triasoft.garage.locking.Versioned;
import com.triasoft.garage.validation.NullOrNotBlank;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class FinanceCompanyRq implements Serializable, Versioned {

    @Serial
    private static final long serialVersionUID = -6172836451920384757L;

    private Long version;

    @NotBlank(message = "REQUIRED")
    @Size(max = 255, message = "MAX_LENGTH")
    private String name;

    @Size(max = 20, message = "MAX_LENGTH")
    @NullOrNotBlank
    private String contactNumber;

    @Size(max = 255, message = "MAX_LENGTH")
    @NullOrNotBlank
    private String email;

}
